#!/usr/bin/env python3
"""Validate the generated AshpazYar dataset and write a test report."""

from __future__ import annotations

import csv
import hashlib
import json
import sqlite3
import zipfile
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "ashpazyar_dataset"
DB = OUT / "database" / "ashpazyar.db"
ZIP = ROOT / "dist" / "ashpazyar_dataset.zip"
REPORT = OUT / "reports" / "TEST_REPORT.md"

results: list[tuple[str, bool, str]] = []


def check(name: str, condition: bool, detail: str = "") -> None:
    results.append((name, bool(condition), detail))


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def read_jsonl(path: Path) -> list[dict]:
    rows = []
    with path.open(encoding="utf-8") as fh:
        for line in fh:
            if line.strip():
                rows.append(json.loads(line))
    return rows


def main() -> None:
    stats = load_json(OUT / "reports" / "stats.json")
    recipes = read_jsonl(OUT / "jsonl" / "recipes.jsonl")
    content = []
    for name in ("articles.jsonl", "tutorials.jsonl", "decorations.jsonl"):
        content.extend(read_jsonl(OUT / "jsonl" / name))
    review = read_jsonl(OUT / "reports" / "review_required.jsonl")
    cats = load_json(OUT / "categories" / "main_categories.json")
    subcats = load_json(OUT / "categories" / "subcategories.json")
    ingredients = load_json(OUT / "ingredients" / "ingredients.json")
    units = load_json(OUT / "ingredients" / "units.json")
    sources = load_json(OUT / "sources" / "sources.json")
    licenses = load_json(OUT / "sources" / "licenses.json")
    manifest = load_json(OUT / "MANIFEST.json")

    check("ورودی ۶۶۶۰ ردیف", stats.get("input_rows") == 6660, str(stats.get("input_rows")))
    check("تعداد دستورها با JSONL", len(recipes) == stats.get("recipes"), f"{len(recipes)} / {stats.get('recipes')}")
    check("تعداد محتوا با JSONL", len(content) == stats.get("content_records"), f"{len(content)} / {stats.get('content_records')}")
    check("تعداد صف بازبینی", len(review) == len(recipes) + len(content), str(len(review)))
    check("همه رکوردها صف بازبینی", len(review) == stats.get("recipes", 0) + stats.get("content_records", 0), str(len(review)))
    check("VERIFIED صفر", stats.get("verified_recipes") == 0, str(stats.get("verified_recipes")))
    check("REJECTED صفر", stats.get("rejected_rows") == 0, str(stats.get("rejected_rows")))
    check("غیرغذایی ۸۴", stats.get("non_food_rows") == 84, str(stats.get("non_food_rows")))
    check("آموزش ۲۸ یکتا", stats.get("content_tutorials") == 28, str(stats.get("content_tutorials")))
    check("تزئین ۲۳ یکتا", stats.get("content_decorations") == 23, str(stats.get("content_decorations")))
    check("تکنیک ۱", stats.get("content_techniques") == 1, str(stats.get("content_techniques")))
    check("دسته ۸", stats.get("categories") == 8, str(stats.get("categories")))
    check("زیردسته ۵۰", stats.get("subcategories") == 50, str(stats.get("subcategories")))

    recipe_ids = {r.get("id") for r in recipes}
    content_ids = {c.get("id") for c in content}
    check("شناسه دستور یکتا", len(recipe_ids) == len(recipes), str(len(recipe_ids)))
    check("شناسه محتوا یکتا", len(content_ids) == len(content), str(len(content_ids)))
    check("شناسه‌ها بدون تهی", "" not in recipe_ids and None not in recipe_ids, "")

    required_recipe = ["id", "slug", "title_fa", "title_original", "content_type", "primary_category_id", "subcategory_id", "instructions_status", "verification_status", "source_id", "source_license_id", "source_row", "ingredients", "steps", "tags"]
    missing = [r.get("id") for r in recipes if any(k not in r for k in required_recipe)]
    check("فیلدهای الزامی دستور", not missing, str(missing[:3]))
    check("نوع دستور RECIPE", all(r.get("content_type") == "RECIPE" for r in recipes), "")
    check("وضعیت REVIEW_REQUIRED", all(r.get("verification_status") == "REVIEW_REQUIRED" for r in recipes), "")
    check("مراحل MISSING", all(r.get("instructions_status") == "MISSING" for r in recipes), "")
    check("مراحل خالی", all(r.get("steps") == [] for r in recipes), "")
    check("مواد غیرخالی", all(len(r.get("ingredients", [])) > 0 for r in recipes), "")
    check("عنوان اصلی غیرخالی", all(r.get("title_original", "").strip() for r in recipes), "")
    check("وضعیت تعداد نفرات", all(r.get("servings_status") in ("SOURCE_VALUE_UNVERIFIED", "UNKNOWN") for r in recipes), "")
    check("نگاشت تعداد نفرات", all((r.get("default_servings") is None) == (r.get("servings_status") == "UNKNOWN") for r in recipes), "")

    allowed_content = {"COOKING_TUTORIAL", "FOOD_DECORATION", "TECHNIQUE_GUIDE"}
    check("نوع محتوای غیرغذایی", all(c.get("content_type") in allowed_content for c in content), "")
    check("محتوا در recipes نیست", all(c.get("content_type") not in ("RECIPE",) for c in content), "")
    check("وضعیت محتوا REVIEW_REQUIRED", all(c.get("verification_status") == "REVIEW_REQUIRED" for c in content), "")
    check("شناسه محتوا با recipes تداخل ندارد", not (recipe_ids & content_ids), "")

    ingredient_ids = {i.get("id") for i in ingredients}
    unit_ids = {u.get("id") for u in units}
    check("مواد یکتا", len(ingredient_ids) == len(ingredients), str(len(ingredient_ids)))
    check("واحد یکتا", len(unit_ids) == len(units), str(len(unit_ids)))
    check("واحد QUALITATIVE موجود", "QUALITATIVE" in unit_ids, "")
    check("واحد UNSPECIFIED موجود", "UNSPECIFIED" in unit_ids, "")
    bad_ingredient_refs = []
    bad_unit_refs = []
    bad_quantities = []
    for r in recipes:
        positions = [x.get("position") for x in r.get("ingredients", [])]
        if len(positions) != len(set(positions)):
            bad_ingredient_refs.append(r.get("id"))
        for x in r.get("ingredients", []):
            if x.get("ingredient_id") not in ingredient_ids:
                bad_ingredient_refs.append(x.get("ingredient_id"))
            if x.get("unit_id") not in unit_ids:
                bad_unit_refs.append(x.get("unit_id"))
            q = x.get("quantity")
            qmax = x.get("quantity_max")
            if q is not None and (not isinstance(q, (int, float)) or q < 0):
                bad_quantities.append((r.get("id"), q))
            if qmax is not None and (not isinstance(qmax, (int, float)) or qmax < 0):
                bad_quantities.append((r.get("id"), qmax))
            if q is not None and qmax is not None and qmax < q:
                bad_quantities.append((r.get("id"), "max<min"))
            if x.get("qualitative") == 1 and q is not None:
                bad_quantities.append((r.get("id"), "qualitative with quantity"))
            if q is None and x.get("qualitative") != 1:
                bad_quantities.append((r.get("id"), "null quantity not qualitative"))
    check("ارجاع مواد معتبر", not bad_ingredient_refs, str(bad_ingredient_refs[:3]))
    check("ارجاع واحد معتبر", not bad_unit_refs, str(bad_unit_refs[:3]))
    check("مقدارها معتبر", not bad_quantities, str(bad_quantities[:3]))

    cat_ids = {c.get("id") for c in cats}
    sub_ids = {s.get("id") for s in subcats}
    check("دسته یکتا", len(cat_ids) == len(cats), str(len(cat_ids)))
    check("زیردسته یکتا", len(sub_ids) == len(subcats), str(len(sub_ids)))
    check("والد زیردسته معتبر", all(s.get("parent_id") in cat_ids for s in subcats), "")
    check("دسته اصلی بدون والد", all(c.get("parent_id") is None for c in cats), "")
    check("نام اصلی دسته‌ها", all(c.get("name_original", "").strip() and s.get("name_original", "").strip() for c in cats for s in subcats), "")
    check("نوع محتوای دسته‌ها", all(c.get("content_type") in (allowed_content | {"RECIPE"}) for c in cats), "")

    check("منبع مجوز صفر", all(s.get("commercial_use_allowed") == 0 and s.get("redistribution_allowed") == 0 for s in sources), "")
    check("مجوز مجاز نیست", all(l.get("commercial_use_allowed") == 0 and l.get("redistribution_allowed") == 0 for l in licenses), "")
    check("منبع نامشخص", all(s.get("verification_status") == "UNRESOLVED" for s in sources), "")

    con = sqlite3.connect(DB)
    con.row_factory = sqlite3.Row
    integrity = con.execute("PRAGMA integrity_check").fetchone()[0]
    fk = con.execute("PRAGMA foreign_key_check").fetchall()
    check("integrity_check", integrity == "ok", integrity)
    check("foreign_key_check", len(fk) == 0, str(len(fk)))
    counts = {
        "recipes": con.execute("SELECT COUNT(*) FROM recipes").fetchone()[0],
        "content": con.execute("SELECT COUNT(*) FROM content_articles").fetchone()[0],
        "source_records": con.execute("SELECT COUNT(*) FROM source_records").fetchone()[0],
        "ingredients": con.execute("SELECT COUNT(*) FROM ingredients").fetchone()[0],
        "lines": con.execute("SELECT COUNT(*) FROM recipe_ingredients").fetchone()[0],
    }
    check("تعداد recipes در DB", counts["recipes"] == len(recipes), str(counts["recipes"]))
    check("تعداد content در DB", counts["content"] == len(content), str(counts["content"]))
    check("تعداد source_records", counts["source_records"] == 6660, str(counts["source_records"]))
    check("تعداد مواد در DB", counts["ingredients"] == len(ingredients), str(counts["ingredients"]))
    check("تعداد خط مواد در DB", counts["lines"] == stats.get("recipe_ingredient_lines"), str(counts["lines"]))
    check("همه ردیف‌ها حساب شده", counts["source_records"] == 6660, str(counts["source_records"]))
    check("VERIFIED در DB صفر", con.execute("SELECT COUNT(*) FROM recipes WHERE verification_status='VERIFIED'").fetchone()[0] == 0, "")
    check("وضعیت DB همه REVIEW_REQUIRED", con.execute("SELECT COUNT(*) FROM recipes WHERE verification_status!='REVIEW_REQUIRED'").fetchone()[0] == 0, "")
    check("مراحل DB خالی", con.execute("SELECT COUNT(*) FROM recipe_steps").fetchone()[0] == 0, "")
    check("تغذیه DB خالی", con.execute("SELECT COUNT(*) FROM recipe_nutrition").fetchone()[0] == 0, "")
    check("رسانه DB خالی", con.execute("SELECT COUNT(*) FROM recipe_media").fetchone()[0] == 0, "")
    check("FTS recipes", con.execute("SELECT COUNT(*) FROM recipes_fts").fetchone()[0] == counts["recipes"], "")
    check("FTS content", con.execute("SELECT COUNT(*) FROM content_fts").fetchone()[0] == counts["content"], "")
    check("جستجوی کیک", con.execute("SELECT COUNT(*) FROM recipes_fts WHERE recipes_fts MATCH ?", ("کیک",)).fetchone()[0] > 0, "")
    check("جستجوی قورمه", con.execute("SELECT COUNT(*) FROM recipes_fts WHERE recipes_fts MATCH ?", ("قورمه",)).fetchone()[0] > 0, "")
    check("جستجوی محتوا", con.execute("SELECT COUNT(*) FROM content_fts WHERE content_fts MATCH ?", ("آموزش",)).fetchone()[0] > 0, "")
    disp = dict(con.execute("SELECT disposition, COUNT(*) FROM source_records GROUP BY disposition").fetchall())
    check("توزیع وضعیت ردیف‌ها", sum(disp.values()) == 6660 and disp.get("RECIPE_PRIMARY", 0) == 5790 and disp.get("EXACT_DUPLICATE", 0) == 818 and disp.get("NON_FOOD", 0) == 52, str(disp))
    check("وضعیت بدون تهی", con.execute("SELECT COUNT(*) FROM source_records WHERE disposition IS NULL OR reason IS NULL").fetchone()[0] == 0, "")
    check("ردیف‌های پیوسته", tuple(con.execute("SELECT MIN(row_number), MAX(row_number), COUNT(DISTINCT row_number) FROM source_records").fetchone()) == (1, 6660, 6660), "")
    check("canonical معتبر", con.execute("SELECT COUNT(*) FROM source_records s WHERE s.disposition='RECIPE_PRIMARY' AND NOT EXISTS (SELECT 1 FROM recipes r WHERE r.id=s.canonical_id)").fetchone()[0] == 0, "")
    check("canonical محتوا معتبر", con.execute("SELECT COUNT(*) FROM source_records s WHERE s.disposition='NON_FOOD' AND NOT EXISTS (SELECT 1 FROM content_articles c WHERE c.id=s.canonical_id)").fetchone()[0] == 0, "")
    check("canonical تکراری معتبر", con.execute("SELECT COUNT(*) FROM source_records s WHERE s.disposition='EXACT_DUPLICATE' AND NOT EXISTS (SELECT 1 FROM recipes r WHERE r.id=s.canonical_id) AND NOT EXISTS (SELECT 1 FROM content_articles c WHERE c.id=s.canonical_id)").fetchone()[0] == 0, "")
    con.close()

    # Exact source preservation: compare every stored original field with the
    # corresponding raw CSV field.
    with (ROOT / "train.csv").open(encoding="utf-8-sig", newline="") as fh:
        raw = list(csv.DictReader(fh))
    raw_by_row = {str(i): r for i, r in enumerate(raw, 1)}
    bad_original = []
    for records in (recipes, content):
        for item in records:
            source = raw_by_row.get(str(item.get("source_row")))
            if not source:
                bad_original.append(item.get("id"))
                continue
            if item.get("title_original") != source.get("name"):
                bad_original.append((item.get("id"), "name"))
            if item.get("primary_category_original") != source.get("cat"):
                bad_original.append((item.get("id"), "cat"))
            if item.get("subcategory_original") != source.get("subcat"):
                bad_original.append((item.get("id"), "subcat"))
    check("حفظ نام/دسته/زیردسته اصلی", not bad_original, str(bad_original[:3]))

    dup_path = OUT / "reports" / "duplicate_report.csv"
    with dup_path.open(encoding="utf-8-sig", newline="") as fh:
        dup_rows = list(csv.DictReader(fh))
    check("گزارش تکراری‌ها", len(dup_rows) == stats.get("exact_duplicate_rows", 0) + stats.get("semantic_duplicate_pairs", 0), str(len(dup_rows)))
    check("نوع تکراری‌ها", all(x.get("duplicate_type") in ("EXACT", "SEMANTIC_NAME_VARIANT") for x in dup_rows), "")
    check("ردیف ردشده", (OUT / "reports" / "rejected_records.csv").exists(), "")
    same_path = OUT / "reports" / "same_name_variants.csv"
    with same_path.open(encoding="utf-8-sig", newline="") as fh:
        same_rows = list(csv.DictReader(fh))
    check("گزارش نام‌های یکسان", len(same_rows) == stats.get("same_name_variant_recipes"), f"{len(same_rows)} / {stats.get('same_name_variant_recipes')}")
    check("گروه‌های نام یکسان", len({x.get("group_id") for x in same_rows}) == stats.get("same_name_variant_groups"), "")
    check("اندازه گروه نام یکسان", all(int(x.get("group_size", 0)) >= 2 for x in same_rows), "")
    flagged = {r.get("id") for r in recipes if "SAME_NAME_VARIANT" in r.get("quality_flags", [])}
    check("پرچم همه نام‌های یکسان", {x.get("recipe_id") for x in same_rows} <= flagged, "")

    check("ساختار ZIP", ZIP.exists(), str(ZIP))
    if ZIP.exists():
        with zipfile.ZipFile(ZIP) as zf:
            bad = zf.testzip()
            names = zf.namelist()
        check("ZIP سالم", bad is None, str(bad))
        check("ZIP شامل دیتابیس", "ashpazyar_dataset/database/ashpazyar.db" in names, "")
        check("ZIP شامل recipes", "ashpazyar_dataset/jsonl/recipes.jsonl" in names, "")
        sha_file = ROOT / "dist" / "ashpazyar_dataset.sha256"
        if sha_file.exists():
            expected = sha_file.read_text(encoding="utf-8").split()[0]
            actual = hashlib.sha256(ZIP.read_bytes()).hexdigest()
            check("هش ZIP", expected == actual, actual)
        else:
            check("فایل هش ZIP", False, "missing")

    bad_manifest = []
    for item in manifest.get("files", []):
        path = OUT / item.get("path", "")
        if not path.is_file():
            bad_manifest.append(item.get("path"))
        elif hashlib.sha256(path.read_bytes()).hexdigest() != item.get("sha256"):
            bad_manifest.append(item.get("path"))
    check("مانیفست فایل‌ها", not bad_manifest, str(bad_manifest[:3]))

    passed = sum(1 for _, ok, _ in results if ok)
    failed = len(results) - passed
    now = datetime.now(timezone.utc).replace(microsecond=0).isoformat()
    lines = [
        "# گزارش تست دیتاست آشپزیار",
        "",
        f"- زمان: {now}",
        f"- نتیجه: {'PASS' if failed == 0 else 'FAIL'}",
        f"- تعداد بررسی: {len(results)}",
        f"- موفق: {passed}",
        f"- ناموفق: {failed}",
        "",
        "| # | بررسی | نتیجه | جزئیات |",
        "|---:|---|---|---|",
    ]
    for i, (name, ok, detail) in enumerate(results, 1):
        detail = str(detail).replace("|", "/").replace("\n", " ")[:160]
        lines.append(f"| {i} | {name} | {'PASS' if ok else 'FAIL'} | {detail} |")
    REPORT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps({"total": len(results), "passed": passed, "failed": failed, "report": str(REPORT)}, ensure_ascii=False, indent=2))
    if failed:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
