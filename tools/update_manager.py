#!/usr/bin/env python3
"""Create, sign, and verify signed update manifests.

Android manifests are accepted only when they point at Cafe Bazaar. No APK is
ever downloaded by the app; Bazaar performs distribution and installation.
"""

from __future__ import annotations

import argparse
import base64
import datetime as dt
import hashlib
import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parents[1]
UPDATES_DIR = ROOT / "updates"
PUBLIC_KEY = ROOT / "app" / "src" / "main" / "res" / "raw" / "update_public_key.pem"
SEMVER = re.compile(r"^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z.-]+))?(?:\+([0-9A-Za-z.-]+))?$")
POLICIES = {"optional", "minimum_version", "forced"}
CHANNELS = {"stable", "beta", "dev"}
PLATFORMS = {"android", "windows"}


def fail(message: str) -> None:
    raise SystemExit("خطا: " + message)


def load_products() -> dict:
    with (UPDATES_DIR / "products.json").open(encoding="utf-8") as handle:
        return json.load(handle)


def canonical_json(value: dict) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def parse_version(value: str) -> tuple:
    match = SEMVER.fullmatch(value)
    if not match:
        fail(f"نسخه «{value}» با Semantic Versioning سازگار نیست")
    return tuple(int(part) for part in match.group(1, 2, 3))


def validate_payload(payload: dict) -> None:
    required = [
        "product_id", "platform", "latest_version", "force_update",
        "update_policy", "release_channel", "download_url", "sha256",
        "release_notes", "published_at",
    ]
    missing = [key for key in required if key not in payload]
    if missing:
        fail("فیلدهای ضروری وجود ندارند: " + ", ".join(missing))
    products = {item["id"]: item for item in load_products()["products"]}
    product = products.get(payload["product_id"])
    if not product:
        fail("محصول ناشناخته است")
    platform = payload["platform"]
    if platform not in PLATFORMS or platform not in product["platforms"]:
        fail("پلتفرم برای این محصول مجاز نیست")
    channel = payload["release_channel"]
    if channel not in CHANNELS or channel not in product["channels"]:
        fail("کانال انتشار مجاز نیست")
    parse_version(payload["latest_version"])
    minimum = payload.get("minimum_supported_version")
    if minimum is not None:
        parse_version(minimum)
        if parse_version(minimum) > parse_version(payload["latest_version"]):
            fail("حداقل نسخه نمی‌تواند از نسخه جدید بزرگ‌تر باشد")
    policy = payload["update_policy"]
    if policy not in POLICIES:
        fail("سیاست به‌روزرسانی ناشناخته است")
    if policy == "minimum_version" and not minimum:
        fail("سیاست minimum_version به حداقل نسخه نیاز دارد")
    if not isinstance(payload["force_update"], bool):
        fail("force_update باید true یا false باشد")
    if payload["force_update"] and policy == "optional":
        fail("force_update با سیاست optional مجاز نیست")
    if policy == "forced" and not payload["force_update"]:
        fail("سیاست forced نیازمند force_update=true است")
    if not re.fullmatch(r"[0-9a-fA-F]{64}", payload["sha256"]):
        fail("sha256 باید ۶۴ رقم هگز باشد")
    if len(payload["release_notes"]) > 10_000:
        fail("توضیحات نسخه بیش از حد طولانی است")
    try:
        published = dt.datetime.fromisoformat(payload["published_at"].replace("Z", "+00:00"))
    except ValueError:
        fail("published_at باید ISO-8601 باشد")
    if published.tzinfo is None:
        fail("published_at باید منطقه زمانی داشته باشد")
    url = payload["download_url"]
    if platform == "android":
        code = payload.get("version_code")
        if not isinstance(code, int) or code <= 0:
            fail("version_code برای اندروید الزامی و باید عدد مثبت باشد")
        package_name = product.get("package_name")
        if not package_name:
            fail("package_name محصول اندروید تعریف نشده است")
        allowed = {
            f"https://cafebazaar.ir/app/{package_name}",
            f"https://www.cafebazaar.ir/app/{package_name}",
            f"bazaar://details?id={package_name}",
        }
        if url not in allowed:
            fail("دانلود اندروید فقط از طریق صفحه رسمی محصول در بازار مجاز است")
    elif not url.startswith("https://"):
        fail("لینک ویندوز باید HTTPS باشد")


def validate_config() -> None:
    data = load_products()
    ids = set()
    for product in data["products"]:
        product_id = product["id"]
        if product_id in ids:
            fail(f"شناسه محصول تکراری: {product_id}")
        ids.add(product_id)
        if not product.get("platforms") or not set(product["platforms"]) <= PLATFORMS:
            fail(f"پلتفرم‌های نامعتبر برای {product_id}")
        if not product.get("channels") or not set(product["channels"]) <= CHANNELS:
            fail(f"کانال‌های نامعتبر برای {product_id}")
        if "android" in product["platforms"] and not product.get("package_name"):
            fail(f"package_name اندروید برای {product_id} لازم است")
    print(json.dumps({"status": "PASS", "products": sorted(ids)}, ensure_ascii=False, indent=2))


def default_published_at() -> str:
    return dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def create(args: argparse.Namespace) -> None:
    products = {item["id"]: item for item in load_products()["products"]}
    product = products.get(args.product)
    if not product:
        fail("محصول ناشناخته است")
    if args.platform not in product["platforms"]:
        fail("پلتفرم برای این محصول مجاز نیست")
    if args.channel not in product["channels"]:
        fail("کانال برای این محصول مجاز نیست")
    download_url = args.download_url
    if not download_url and args.platform == "android":
        download_url = f"https://cafebazaar.ir/app/{product.get('package_name')}"
    payload = {
        "product_id": args.product,
        "platform": args.platform,
        "latest_version": args.version,
        "force_update": args.force_update,
        "update_policy": args.policy,
        "release_channel": args.channel,
        "download_url": download_url,
        "sha256": args.sha256.lower(),
        "release_notes": args.notes,
        "published_at": args.published_at or default_published_at(),
    }
    if args.minimum_version:
        payload["minimum_supported_version"] = args.minimum_version
    if args.platform == "android":
        payload["version_code"] = args.version_code
    validate_payload(payload)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"ساخته شد: {out}")


def run_openssl(args: list[str]) -> None:
    result = subprocess.run(args, capture_output=True, text=True)
    if result.returncode != 0:
        fail("openssl ناموفق بود: " + (result.stderr.strip() or result.stdout.strip()))


def sign(args: argparse.Namespace) -> None:
    payload_path = Path(args.payload)
    key_path = Path(args.key)
    if not payload_path.exists():
        fail("فایل payload پیدا نشد")
    if not key_path.exists():
        fail("کلید خصوصی پیدا نشد")
    payload = json.loads(payload_path.read_text(encoding="utf-8"))
    validate_payload(payload)
    canonical = canonical_json(payload)
    with tempfile.TemporaryDirectory() as temporary:
        temp = Path(temporary)
        data_path = temp / "payload.json"
        sig_path = temp / "payload.sig"
        data_path.write_bytes(canonical.encode("utf-8"))
        run_openssl([args.openssl, "dgst", "-sha256", "-sign", str(key_path), "-out", str(sig_path), str(data_path)])
        signature = base64.b64encode(sig_path.read_bytes()).decode("ascii")
    manifest = {"payload": payload, "manifest_signature": signature}
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"امضا شد: {out}")
    verify(Path(args.public_key), out)


def verify(public_key: Path, manifest_path: Path) -> None:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if set(manifest) != {"payload", "manifest_signature"}:
        fail("ساختار مانیفست امضاشده ناشناخته است")
    payload = manifest["payload"]
    validate_payload(payload)
    signature = base64.b64decode(manifest["manifest_signature"], validate=True)
    canonical = canonical_json(payload)
    with tempfile.TemporaryDirectory() as temporary:
        temp = Path(temporary)
        data_path = temp / "payload.json"
        sig_path = temp / "payload.sig"
        data_path.write_bytes(canonical.encode("utf-8"))
        sig_path.write_bytes(signature)
        run_openssl([args_openssl(), "dgst", "-sha256", "-verify", str(public_key), "-signature", str(sig_path), str(data_path)])
    print(json.dumps({
        "status": "PASS",
        "manifest": str(manifest_path),
        "product": payload["product_id"],
        "platform": payload["platform"],
        "channel": payload["release_channel"],
        "version": payload["latest_version"],
        "policy": payload["update_policy"],
        "force_update": payload["force_update"],
        "payload_sha256": sha256_text(canonical),
    }, ensure_ascii=False, indent=2))


def args_openssl() -> str:
    return "openssl"


def main() -> None:
    parser = argparse.ArgumentParser(description="مدیریت مانیفست به‌روزرسانی قاشق")
    subparsers = parser.add_subparsers(dest="command", required=True)

    config_parser = subparsers.add_parser("validate-config", help="اعتبارسنجی فهرست محصولات")
    config_parser.set_defaults(handler=lambda _: validate_config())

    create_parser = subparsers.add_parser("create", help="ساخت payload بدون امضا")
    create_parser.add_argument("--product", required=True)
    create_parser.add_argument("--platform", required=True, choices=sorted(PLATFORMS))
    create_parser.add_argument("--channel", required=True, choices=sorted(CHANNELS))
    create_parser.add_argument("--version", required=True)
    create_parser.add_argument("--version-code", type=int, default=0)
    create_parser.add_argument("--download-url", default="")
    create_parser.add_argument("--sha256", required=True)
    create_parser.add_argument("--notes", default="")
    create_parser.add_argument("--policy", default="optional", choices=sorted(POLICIES))
    create_parser.add_argument("--minimum-version", default="")
    create_parser.add_argument("--force-update", action="store_true")
    create_parser.add_argument("--published-at", default="")
    create_parser.add_argument("--out", required=True)
    create_parser.set_defaults(handler=create)

    sign_parser = subparsers.add_parser("sign", help="امضای payload با کلید خصوصی")
    sign_parser.add_argument("--payload", required=True)
    sign_parser.add_argument("--key", required=True)
    sign_parser.add_argument("--out", required=True)
    sign_parser.add_argument("--public-key", default=str(PUBLIC_KEY))
    sign_parser.add_argument("--openssl", default="openssl")
    sign_parser.set_defaults(handler=sign)

    verify_parser = subparsers.add_parser("verify", help="اعتبارسنجی امضا و سیاست مانیفست")
    verify_parser.add_argument("--manifest", required=True)
    verify_parser.add_argument("--public-key", default=str(PUBLIC_KEY))
    verify_parser.set_defaults(handler=lambda args: verify(Path(args.public_key), Path(args.manifest)))

    args = parser.parse_args()
    args.handler(args)


if __name__ == "__main__":
    main()
