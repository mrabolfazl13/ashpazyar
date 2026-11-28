# گزارش بیلد قاشق

## بیلد معتبر نهایی

| مورد | مقدار |
|---|---|
| Workflow | `.github/workflows/android-ci.yml` |
| شناسه اجرا | `46112048750` |
| Commit | `d84a281` |
| نتیجه | ✅ موفق |
| لینک | https://github.com/mrabolfazl13/ashpazyar/actions/runs/46112048750 |
| مدت کل | ۹ دقیقه و ۹ ثانیه |

همه مراحل موفق بودند: اعتبارسنجی دیتاست، اعتبارسنجی کاتالوگ کامل، اعتبارسنجی رجیستری به‌روزرسانی، ممیزی منبع، Unit Tests، کامپایل تست‌های Instrumented، Lint، آماده‌سازی امضا، ساخت Debug APK، Release APK و Release AAB، بسته‌بندی خروجی‌ها و ساخت `.bin` بازار.

مدت Job اصلی ۸ دقیقه و ۵۶ ثانیه و Job تست Compose ۵ دقیقه و ۱ ثانیه بود.

## خروجی‌های اصلی

این سه فایل، خروجی‌های اصلی و موردنیاز هستند:

| فایل | حجم (بایت) | SHA-256 |
|---|---:|---|
| `ashpazyar-debug.apk` | 21,915,114 | `2336C08A201B24B18A1F5B88AB502B644AD5EAAE69FD3EF5122E680578553F0A` |
| `ashpazyar-release.apk` | 3,994,503 | `0F4F2F23BC9179E83315F2B2A408953487DDF1961F4AD9980C8265764F35324B` |
| `ashpazyar-release.aab` | 7,151,004 | `950374F1FD0C09362DBA065C0A6FA58DE164B4D2BA82350AEC337C4E8F44A157` |

خروجی جداگانه بازار (جزو سه خروجی اصلی نیست):

| فایل | حجم (بایت) | SHA-256 |
|---|---:|---|
| `ghashogh-bazaar.bin` | 211,586 | `96C980E514D88498DE32434D0BE8658192E969BEF389EC7B7425FD584612F195` |

دریافت مجدد:

```bash
gh run download 46112048750 -D dist/run46112048750
```

مسیر محلی تأییدشده: `dist/run46112048750/` (در Git نادیده گرفته می‌شود). این هش‌ها مربوط به Commit کد `d84a281` و همین Artifactها هستند. Commitهای مستندات بعدی کد اپلیکیشن را تغییر نمی‌دهند، اما چون ZIP در هر بیلد دوباره ساخته می‌شود، هش فایل‌ها می‌تواند متفاوت باشد.

## امضا

- گام `Prepare release signing` موفق شد و Secretهای امضا در Workflow موجود بودند.
- خروجی AAB با `keytool -printcert -jarfile` بررسی شد؛ گواهی معتبر است:
  - `CN=AshpazYar, OU=Mobile, O=AshpazYar, L=Tehran, C=IR`
  - RSA 4096-bit؛ `SHA384withRSA`
  - SHA-256 گواهی: `0C:DF:A4:25:29:1D:0E:CC:7D:63:18:2E:37:37:0F:C1:A1:7C:11:3E:6F:D1:78:6E:C1:33:6D:5D:31:7A:9F:5C`
- Release APK با همان `signingConfig` و همان Keystore ساخته شد. بررسی مستقل `apksigner` در محیط محلی انجام نشد؛ بنابراین تأیید امضای APK فقط بر پایه موفقیت گام امضا و پیکربندی بیلد است، نه ادعای بررسی v2/v3.
- Debug APK با گواهی استاندارد Debug امضا می‌شود.
- کلیدهای امضا خارج از Git هستند: `keystore/release.jks` و `keystore/keystore.properties`.
- **پشتیبان‌گیری رمزگذاری‌شده از Keystore و رمزها همچنان الزامی است.**

## پیکربندی بیلد

- `applicationId`: `ir.ashpazyar.app`
- نام نمایشی: **قاشق**
- نسخه: `1.3.0` (versionCode 6)
- `minSdk`: 26
- `compileSdk` / `targetSdk`: 35
- Java 17، Kotlin 2.0.21، AGP 8.7.3، Gradle 8.9
- Release با R8 و `shrinkResources` بهینه شده است.
- Debug با پسوند `.debug` و بدون Minify ساخته می‌شود.

## داده همراه اپ

- `recipes.json` شامل ۲٬۰۰۰ دستور نمونه با مراحل است.
- کاتالوگ `Persian_Cooking` شامل ۵٬۷۹۰ رکورد بدون مرحله پخت است.
- مجموع ۷٬۷۹۰ دستور؛ `seed_version = "4"`؛ نسخه Room = ۲ با مهاجرت صریح ۱→۲.
- دارایی‌های کاتالوگ:
  - `app/src/main/assets/seed/catalog_recipes.jsonl.gzip` — ۹۵۵٬۲۵۴ بایت
  - `app/src/main/assets/seed/catalog_ingredients.json.gzip` — ۶۶٬۷۸۵ بایت
  - `app/src/main/assets/seed/catalog_manifest.json` — ۸۵۹ بایت
- نام پسوند `.gzip` عمدی است: AAPT فایل‌های `.gz` را خودکار باز می‌کند و پسوند را حذف می‌کند؛ استفاده از `.gzip` از این رفتار جلوگیری می‌کند.
- وجود هر سه دارایی کاتالوگ در `ashpazyar-release.apk` با `jar tf` بررسی شد.
- در `ashpazyar-release.aab` نیز دارایی‌ها در `base/assets/seed/` قرار دارند.

## محدودیت‌ها و وضعیت انتشار

- این گزارش فقط موفقیت ساخت و تست خودکار است. **هیچ آپلود یا تأییدی در Google Play یا بازار انجام نشده است.**
- آزمون دستگاه فیزیکی، RTL با فونت بزرگ، حالت تاریک، Seed روی دستگاه ضعیف و تایمر پس از بستن اپ انجام نشده و QA انسانی می‌خواهد.
- منبع و مجوز بازنشر تجاری `train.csv` نامشخص است؛ ۰ رکورد تأییدشده انسانی وجود دارد.
- مانیفست به‌روزرسانی هنوز در GitHub Pages منتشر نشده است؛ انتشار فقط پس از وجود خروجی واقعی همان نسخه مجاز است.
- دانلود و نصب فقط از بازار است؛ در اپ هیچ دانلودر یا نصب‌کننده APK وجود ندارد.
