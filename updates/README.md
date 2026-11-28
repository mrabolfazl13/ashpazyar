# سامانه به‌روزرسانی قاشق

این پوشه مانیفست‌های امضاشده و فهرست محصولات را نگه می‌دارد. سامانه بدون سرور و حساب کاربری کار می‌کند: GitHub Actions مانیفست را امضا می‌کند، GitHub Pages آن را منتشر می‌کند و کلاینت اندروید فقط برای بررسی نسخه یک درخواست HTTPS می‌فرستد.

## قواعد قطعی

- **دانلود و نصب اندروید فقط از برنامه بازار انجام می‌شود.** برنامه هیچ APKای دانلود یا نصب نمی‌کند، `REQUEST_INSTALL_PACKAGES` ندارد و FileProvider برای نصب ندارد.
- مانیفست اندروید فقط وقتی پذیرفته می‌شود که `download_url` دقیقاً صفحه رسمی همین برنامه در بازار باشد: `https://cafebazaar.ir/app/ir.ashpazyar.app` یا `bazaar://details?id=ir.ashpazyar.app`.
- برنامه ابتدا مانیفست را با کلید عمومی داخل APK اعتبارسنجی می‌کند؛ سپس کاربر را با دیپ‌لینک به بازار می‌فرستد. نصب و بررسی امضای بسته توسط بازار و اندروید انجام می‌شود.
- `sha256` برای حسابرسی فایل تحویلی است. برنامه اندروید آن را برای نصب مستقیم استفاده نمی‌کند، چون فایلی دریافت نمی‌کند.
- هیچ به‌روزرسانی‌ای فقط به دلیل وجود نسخه جدید اجباری نمی‌شود. `force_update` باید صریحاً `true` باشد و سیاست نیز اجازه بدهد.
- خطای شبکه، امضای نامعتبر، مانیفست ناقص یا نبود بازار نباید برنامه را از کار بیندازد. فقط مانیفست معتبر و الزام صریح می‌تواند صفحه به‌روزرسانی اجباری را نشان دهد.
- درخواست بررسی نسخه هیچ شناسه کاربر، پایگاه داده، محتوای محلی یا عادت مصرف را ارسال نمی‌کند.

## محصولات

| شناسه | نام | پلتفرم | کانال‌ها | توزیع |
|---|---|---|---|---|
| `ashpazyar` | قاشق | Android | stable, beta | فقط بازار |
| `sanadyar` | سندیار | Windows | stable, beta | لینک HTTPS اعلام‌شده در مانیفست |
| `khodroyar` | خودرویار | Windows | stable, beta | لینک HTTPS اعلام‌شده در مانیفست |

کلاینت‌های ویندوز در این مخزن نیستند. آن‌ها باید همین قالب مانیفست، همان کلید عمومی و همان قواعد سیاست را پیاده کنند؛ فقط قاعده «فقط بازار» مخصوص اندروید است.

## مسیر انتشار

برای هر محصول، پلتفرم و کانال یک فایل مستقل وجود دارد:

```text
updates/<product>/<platform>/<channel>.json
```

نمونه پس از انتشار:

```text
https://mrabolfazl13.github.io/ashpazyar/ashpazyar/android/stable.json
https://mrabolfazl13.github.io/ashpazyar/sanadyar/windows/stable.json
```

Workflow فایل‌های داخل `updates/` را به ریشه شاخه `gh-pages` منتشر می‌کند. در تنظیمات GitHub باید Pages روی شاخه `gh-pages` و پوشه `/ (root)` تنظیم شود.

## قالب مانیفست

فایل منتشرشده دو کلید دارد:

```json
{
  "payload": {
    "product_id": "ashpazyar",
    "platform": "android",
    "latest_version": "1.3.0",
    "minimum_supported_version": "1.2.0",
    "force_update": false,
    "update_policy": "optional",
    "release_channel": "stable",
    "download_url": "https://cafebazaar.ir/app/ir.ashpazyar.app",
    "sha256": "<64 hex>",
    "release_notes": "تغییرات این نسخه",
    "published_at": "2026-11-28T10:00:00Z",
    "version_code": 6
  },
  "manifest_signature": "<base64 RSA signature>"
}
```

امضا روی JSON فشرده و مرتب‌شده `payload` است، نه روی فایل زیباسازی‌شده. قالب canonical در Kotlin (`CanonicalJson.kt`) و Python (`tools/update_manager.py`) یکسان است. هر تغییر در payload، امضا را باطل می‌کند.

### سیاست‌ها

| سیاست | `force_update` | رفتار |
|---|---|---|
| `optional` | `false` | دیالوگ اختیاری با گزینه «بعداً» |
| `minimum_version` | `true` و نسخه فعلی کمتر از حداقل | اجباری؛ در غیر این صورت اختیاری |
| `forced` | `true` | اجباری برای همه نسخه‌های قدیمی‌تر |
| هر سیاستی | `false` | هرگز اجباری نمی‌شود |

`minimum_supported_version` باید کمتر یا مساوی `latest_version` باشد. برای اندروید `version_code` باید مثبت و بزرگ‌تر از versionCode فعلی باشد؛ کاهش نسخه رد می‌شود.

## امضا و کلید

- الگوریتم: RSA-3072، `SHA256withRSA`.
- کلید عمومی داخل برنامه: `app/src/main/res/raw/update_public_key.pem`.
- کلید خصوصی محلی: `keystore/update_signing_private.pem` (در `.gitignore`). هرگز آن را commit یا در لاگ چاپ نکنید.
- Secret گیت‌هاب: `UPDATE_SIGNING_PRIVATE_KEY_PEM` با محتوای کامل PEM.
- اگر کلید خصوصی گم شود، نمی‌توان مانیفست سازگار با نسخه‌های نصب‌شده امضا کرد. از کلید و Secret نسخه پشتیبان امن بگیرید. چرخش کلید نیازمند انتشار نسخه‌ای با کلید عمومی جدید است.

## انتشار یک نسخه

1. فایل تحویلی را بسازید و SHA-256 آن را بگیرید.
2. در GitHub، Actions → «Publish signed update manifest» → Run workflow.
3. محصول، پلتفرم، کانال، نسخه، `version_code` (اندروید)، هش، توضیحات و سیاست را وارد کنید.
4. برای اندروید `download_url` را خالی بگذارید تا صفحه رسمی بازار ساخته شود. برای ویندوز لینک HTTPS را وارد کنید.
5. برای `forced` باید `force_update=true` بزنید. پیش‌فرض خاموش است.
6. Workflow مانیفست را می‌سازد، با کلید Secret امضا می‌کند، با کلید عمومی داخل مخزن بررسی می‌کند و فقط در صورت موفقیت منتشر می‌کند.

اجرای محلی ابزار (بدون بیلد اندروید):

```bash
python tools/update_manager.py validate-config
python tools/update_manager.py create \
  --product ashpazyar --platform android --channel stable \
  --version 1.3.0 --version-code 6 --sha256 <64-hex> \
  --notes "تغییرات" --policy optional --out /tmp/payload.json
python tools/update_manager.py sign --payload /tmp/payload.json \
  --key keystore/update_signing_private.pem --out /tmp/stable.json
python tools/update_manager.py verify --manifest /tmp/stable.json
```

## رفتار کلاینت

- بررسی خودکار فقط یک‌بار در شروع برنامه انجام می‌شود؛ بررسی دستی از تنظیمات → «به‌روزرسانی قاشق».
- نسخه‌های debug بررسی را غیرفعال می‌کنند.
- نسخه ردشده اختیاری تا نسخه بعدی دوباره نمایش داده نمی‌شود؛ بررسی دستی آن را دوباره نشان می‌دهد.
- صفحه اجباری گزینه «بعداً» ندارد. پس از باز شدن بازار، کاربر باید نسخه را نصب کند و برنامه را دوباره باز کند.
- اگر بازار نصب نباشد، صفحه وب رسمی بازار در مرورگر باز می‌شود؛ هیچ دانلود مستقیمی از GitHub انجام نمی‌شود.

## محدودیت‌های واقعی

- این سامانه تأیید انتشار در بازار، تأیید مجوز محتوا یا صحت دستورهای غذا را ثابت نمی‌کند.
- مانیفست تا زمانی که Workflow منتشر نشود روی GitHub Pages وجود ندارد؛ در این حالت بررسی خودکار بی‌صدا و بررسی دستی با خطا نمایش داده می‌شود.
- GitHub Pages می‌تواند IP درخواست‌دهنده را در لاگ خود ببیند؛ هیچ داده دیگری ارسال نمی‌شود.
- Windows clients must perform their own signature check and HTTPS download; this repository only provides the shared manifest tooling and policy design.
