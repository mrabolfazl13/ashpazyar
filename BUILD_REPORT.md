# گزارش بیلد چاشیکا

## بیلد معتبر نهایی

| مورد | مقدار |
|---|---|
| Workflow | `.github/workflows/android-ci.yml` |
| شناسه اجرا | `46114927159` |
| Commit | `6b633ae` |
| نتیجه | ✅ موفق |
| لینک | https://github.com/mrabolfazl13/ashpazyar/actions/runs/46114927159 |
| مدت کل | ۸ دقیقه و ۶ ثانیه |

همه مراحل موفق بودند: اعتبارسنجی دیتاست، اعتبارسنجی کاتالوگ کامل، اعتبارسنجی رجیستری به‌روزرسانی، ممیزی منبع، Unit Tests، کامپایل تست‌های Instrumented، Lint، آماده‌سازی امضا، ساخت Debug APK، Release APK و Release AAB، بسته‌بندی خروجی‌ها و ساخت `.bin` بازار.

مدت Job اصلی ۷ دقیقه و ۴۲ ثانیه و Job تست Compose ۵ دقیقه و ۱۱ ثانیه بود.

## خروجی‌های اصلی

این سه فایل، خروجی‌های اصلی و موردنیاز هستند:

| فایل | حجم (بایت) | SHA-256 |
|---|---:|---|
| `ashpazyar-debug.apk` | 21,918,111 | `BD46A11A410D5DEE8A4C12CA9D04784C48D255281D4E8B4E3C3EE2172559824B` |
| `ashpazyar-release.apk` | 3,997,488 | `2F314636EA7BA2C007624E601B6360D5D76CC58A60BBD4723F79F6B4F83AEF16` |
| `ashpazyar-release.aab` | 7,154,244 | `2F2C2F84E4C5E013449A27C4162ED04F304C82B118F4797B252F82F95A451D72` |

خروجی جداگانه بازار (جزو سه خروجی اصلی نیست):

| فایل | حجم (بایت) | SHA-256 |
|---|---:|---|
| `chashika-bazaar.bin` | 212,178 | `836795830B20611EF333F3252E0945654DC5FED7B5846246175472826D1318C2` |

دریافت مجدد:

```bash
gh run download 46114927159 -D dist/run46114927159
```

مسیر محلی تأییدشده: `dist/run-46114927159/` (در Git نادیده گرفته می‌شود). این هش‌ها مربوط به Commit کد `6b633ae` و همین Artifactها هستند. Commitهای مستندات بعدی کد اپلیکیشن را تغییر نمی‌دهند، اما چون ZIP در هر بیلد دوباره ساخته می‌شود، هش فایل‌ها می‌تواند متفاوت باشد.

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

- `applicationId`: `ir.chashika.app`
- `namespace` داخلی کد: `ir.ashpazyar.app`
- نام نمایشی: **چاشیکا**
- نسخه: `1.3.2` (versionCode 8)
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
  - `app/src/main/assets/seed/catalog_recipes.jsonl.gzip` — ۹۵۷٬۴۸۵ بایت
  - `app/src/main/assets/seed/catalog_ingredients.json.gzip` — ۶۶٬۷۸۵ بایت
  - `app/src/main/assets/seed/catalog_manifest.json` — ۱٬۱۲۱ بایت
- نام پسوند `.gzip` عمدی است: AAPT فایل‌های `.gz` را خودکار باز می‌کند و پسوند را حذف می‌کند؛ استفاده از `.gzip` از این رفتار جلوگیری می‌کند.
- وجود هر سه دارایی کاتالوگ در `ashpazyar-release.apk` با `jar tf` بررسی شد.
- در `ashpazyar-release.aab` نیز دارایی‌ها در `base/assets/seed/` قرار دارند.

## محدودیت‌ها و وضعیت انتشار

- این گزارش فقط موفقیت ساخت و تست خودکار است. **هیچ آپلود یا تأییدی در Google Play یا بازار انجام نشده است.**
- آزمون دستگاه فیزیکی، RTL با فونت بزرگ، حالت تاریک، Seed روی دستگاه ضعیف و تایمر پس از بستن اپ انجام نشده و QA انسانی می‌خواهد.
- منبع `Persian_Cooking` با مجوز MIT و انتساب ثبت‌شده است؛ با این حال ۰ رکورد تأییدشده انسانی و هیچ مرحله پخت برای ۵٬۷۹۰ رکورد کاتالوگ وجود ندارد.
- مانیفست به‌روزرسانی هنوز در GitHub Pages منتشر نشده است؛ انتشار فقط پس از وجود خروجی واقعی همان نسخه مجاز است.
- دانلود و نصب فقط از بازار است؛ در اپ هیچ دانلودر یا نصب‌کننده APK وجود ندارد.
