# گزارش تست قاشق

## نتیجه کلی

| مورد | نتیجه |
|---|---|
| Workflow | GitHub Actions — اجرای موفق |
| شناسه اجرا | `46112048750` |
| Commit | `d84a281` |
| تست واحد | **۸۰ از ۸۰ موفق** |
| تست Instrumented/Compose | **۶ از ۶ موفق** (Emulator API 30) |
| خطای تست | ۰ |
| Lint | ۰ خطا، ۲۱ هشدار غیرمسدودکننده |
| اعتبارسنجی کاتالوگ | موفق — ۷٬۷۹۰ دستور |
| بیلد Debug / Release APK / Release AAB | موفق |

پیوند اجرا: https://github.com/mrabolfazl13/ashpazyar/actions/runs/46112048750

## محیط اجرا

- Runner: `ubuntu-latest` در GitHub Actions (دو Job موازی)
- JDK: Temurin 17
- Android SDK: 35، Build Tools 35.0.0
- Gradle: 8.9 با Wrapper مخزن
- Emulator: API 30، `google_apis`، `x86_64`، پروفایل `pixel`
- Node.js برای اعتبارسنجی دیتاست تحریریه و ممیزی منبع
- Python برای اعتبارسنجی کاتالوگ و رجیستری به‌روزرسانی
- تست‌ها فقط در CI اجرا شده‌اند؛ هیچ Gradle، Lint یا تست اندروید محلی اجرا نشده است.

## اعتبارسنجی داده

گام‌های پیش از Gradle موفق بودند:

- `Validate recipe dataset` — ۲٬۰۰۰ دستور تحریریه، ارجاع‌های معتبر، واحدها و قواعد گیاهی/آلرژن.
- `Validate bundled full catalog` — ۷٬۷۹۰ دستور، ۲٬۸۲۹ ماده کاتالوگ، ۰ مرحله در رکوردهای کاتالوگ، وضعیت `REVIEW_REQUIRED`.
- `Validate update product registry` — محصولات `ashpazyar`، `khodroyar` و `sanadyar`.
- `Audit recipe provenance and educational completeness` — ثبت صریح شکاف آموزش و منبع.

نتیجه اعتبارسنجی محلی کاتالوگ نیز `PASS` بود:

```json
{
  "status": "PASS",
  "editorial_recipes": 2000,
  "catalog_recipes": 5790,
  "total_recipes": 7790,
  "catalog_ingredients": 2829,
  "catalog_with_instructions": 0,
  "catalog_validation_status": "REVIEW_REQUIRED"
}
```

## تست‌های واحد

| کلاس | تعداد | شکست |
|---|---:|---:|
| `UnitConverterTest` | ۱۵ | ۰ |
| `PlannerAndFilterTest` | ۱۲ | ۰ |
| `IngredientMatcherTest` | ۱۰ | ۰ |
| `ShoppingEngineTest` | ۱۰ | ۰ |
| `UpdatePolicyEngineTest` | ۷ | ۰ |
| `CookingStepFlowTest` | ۷ | ۰ |
| `CostEngineTest` | ۶ | ۰ |
| `UpdateManifestVerifierTest` | ۶ | ۰ |
| `SemVerTest` | ۵ | ۰ |
| `BazaarLinksTest` | ۲ | ۰ |
| **جمع** | **۸۰** | **۰** |

پوشش جدید در این نسخه:

- ترتیب و اعتبار SemVer، از جمله pre-release و رد صفرهای پیشرو.
- اعتبارسنجی امضای RSA/SHA-256 مانیفست، ساختار دقیق ریشه، SHA-256، زمان ISO و `version_code`.
- رد مانیفست دست‌کاری‌شده، محصول/کانال نادرست و URL غیربازار.
- سیاست‌های `optional`، `forced` و `minimum_version`؛ غیرفعال بودن اجبار بدون `force_update=true` و جلوگیری از نصب نسخه پایین‌تر.
- پذیرش فقط لینک‌های رسمی بازار و رد GitHub/APK مستقیم.
- پذیرش مانیفست امضاشده با ابزار انتشار.

## تست‌های Instrumented/Compose

شش تست روی Emulator واقعی API 30 موفق شدند:

1. نمایش و کلیک‌پذیری `PrimaryButton`؛
2. نمایش متن فارسی در تم قاشق؛
3. امکان «بعداً» در به‌روزرسانی اختیاری؛
4. نبود دکمه تعویق در به‌روزرسانی الزامی؛
5. فراخوانی فقط Callback بازار با کلیک «رفتن به بازار»؛
6. Seed کامل کاتالوگ در کنار ۲٬۰۰۰ دستور تحریریه و یافتن `rcp-fd98deb11b09e4` (تست یکپارچگی داده).

گزارش کامل: Artifact با نام `ui-test-reports-31`.

## Lint

- `lintDebug` موفق؛ ۰ خطا و ۲۱ هشدار.
- هشدارها غیرمسدودکننده‌اند و شامل `DefaultLocale`، `OldTargetApi` و چند هشدار دسترس‌پذیری/Modifier هستند.
- گزارش کامل: Artifact با نام `lint-report-31`.

## بازبینی خروجی

- `ashpazyar-release.apk` باز شد و هر سه دارایی کاتالوگ با نام‌های `.gzip` داخل آن وجود دارند.
- `ashpazyar-release.aab` دارای گواهی امضای معتبر `CN=AshpazYar` است.
- نسخه اپلیکیشن `1.3.0` (versionCode 6) است.
- فایل `.bin` بازار جداگانه ساخته و امضا شد؛ جزو سه خروجی اصلی نیست.

## آزمون‌های انجام‌نشده

- نصب روی دستگاه فیزیکی Android 8 تا 15 و بررسی RTL، فونت بزرگ، حالت تاریک و عملکرد ضعیف.
- Seed کامل ۷٬۷۹۰ رکورد روی دستگاه واقعی از نظر زمان و حافظه.
- تست شبکه واقعی مانیفست روی GitHub Pages؛ مانیفست هنوز منتشر نشده است.
- باز شدن واقعی بازار و نصب بسته از بازار؛ بازار در Emulator نصب نیست.
- آپلود AAB در Play Console و `.bin` در بازار؛ هیچ تأیید فروشگاهی وجود ندارد.
- بازبینی انسانی دستورها و افزودن مراحل پخت دارای مجوز؛ ۰ رکورد `VERIFIED` است.

## محدودیت‌ها

- ۱٬۸۵۰ دستور تحریریه با قواعد تولید شده‌اند و بازبینی سلیقه‌ای/دستی تک‌تک ندارند.
- ۵٬۷۹۰ رکورد کاتالوگ هیچ مرحله پختی ندارند؛ UI این را صریح نشان می‌دهد و حالت پخت را غیرفعال می‌کند.
- مجوز منبع MIT است، اما کارت دیتاست فایل LICENSE جداگانه ندارد و مالکیت متن دستورها مستقل تأیید نشده است.
- این گزارش جای QA انسانی و تأیید فروشگاه را نمی‌گیرد.
