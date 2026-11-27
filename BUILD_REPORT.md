# گزارش بیلد آشپزیار

## بیلد معتبر نهایی

| مورد | مقدار |
|---|---|
| Workflow | `.github/workflows/android-ci.yml` |
| اجرا | `45991411248` |
| Commit | `6435e26` |
| نتیجه | ✅ موفق |
| لینک | https://github.com/mrabolfazl13/ashpazyar/actions/runs/45991411248 |

همه مراحل اجرا موفق بودند: Validate، اعتبارسنجی دیتاست، Unit Tests، کامپایل تست‌های Compose، Lint، آماده‌سازی امضا، Debug APK، Release APK، Release AAB، بسته‌بندی و آپلود.

## خروجی‌ها

| فایل | حجم (بایت) | SHA-256 |
|---|---:|---|
| `ashpazyar-debug.apk` | 20,799,969 | `7E76655801C727C6B49374F0009B193CAB8AD4A2986B5D8BD40A24D177FA1610` |
| `ashpazyar-release.apk` | 2,945,297 | `F4C9DF5E7BF0979120988CD1F96B29C640CCE3F2299244E39543068C22CB4CF4` |
| `ashpazyar-release.aab` | 6,041,167 | `B9D42D793E180E88038C633EE8B49BFB6B514F751F26C81592467B53F32E1088` |

مسیر محلی نسخه دانلودشده: `dist/ci-45991411248/` (در Git نادیده گرفته می‌شود).

دریافت مجدد از GitHub:

```bash
gh run download 45991411248 -n ashpazyar-build-17 -D dist
```

این هش‌ها مربوط به خروجی‌های Commit `6435e26` هستند. Commitهای مستندات بعدی کد اپلیکیشن را تغییر نمی‌دهند و Workflow آن‌ها نیز سبز است، اما چون ZIP در هر بیلد دوباره ساخته می‌شود، هش فایل‌ها می‌تواند متفاوت باشد.

## امضای Release

- هر دو خروجی Release **امضا شده‌اند**.
- APK دارای بلوک APK Signature Scheme v2 است. امضای رمزنگاری‌شده با کلید گواهی بررسی و تأیید شد.
- الگوریتم امضای APK: RSA PKCS#1 v1.5 با SHA-512 (شناسه `0x0104`).
- AAB دارای `META-INF/ASHPAZYA.RSA`، `META-INF/ASHPAZYA.SF` و `META-INF/MANIFEST.MF` است؛ `jarsigner -verify` نتیجه `jar verified` داد.
- گواهی: `CN=AshpazYar, OU=Mobile, O=AshpazYar, L=Tehran, C=IR`
- SHA-256 گواهی:

```text
0C:DF:A4:25:29:1D:0E:CC:7D:63:18:2E:37:37:0F:C1:A1:7C:11:3E:6F:D1:78:6E:C1:33:6D:5D:31:7A:9F:5C
```

- کلید: RSA 4096-bit؛ امضای گواهی: SHA384withRSA.
- اعتبار گواهی: از ۲۷ نوامبر ۲۰۲۶ تا ۱۴ آوریل ۲۰۵۴.
- Debug APK با گواهی استاندارد Android Debug امضا شده است.
- کلید در GitHub Secrets با نام‌های `ANDROID_KEYSTORE_BASE64`، `ANDROID_KEYSTORE_PASSWORD`، `ANDROID_KEY_ALIAS` و `ANDROID_KEY_PASSWORD` تنظیم شده است.
- نسخه پشتیبان محلی کلید در `keystore/release.jks` و رمز آن در `keystore/keystore.properties` قرار دارد؛ هر دو با `.gitignore` از مخزن خارج‌اند.

### هشدار مهم

این کلید، هویت انتشار اپلیکیشن است. نگهداری نسخه پشتیبان رمزگذاری‌شده از `release.jks` و رمز آن الزامی است. گم شدن کلید، به‌روزرسانی نسخه‌های بعدی در Google Play را غیرممکن می‌کند. فایل کلید یا رمز آن نباید در Git، Issue، چت عمومی یا لاگ CI قرار گیرد.

## پیکربندی بیلد

- `applicationId`: `ir.ashpazyar.app`
- نسخه: `1.2.0` (versionCode 3)
- `minSdk`: 26
- `compileSdk` / `targetSdk`: 35
- Java 17، Kotlin 2.0.21، AGP 8.7.3، Gradle 8.9
- Release با R8 و shrinkResources بهینه شده است.
- Debug با پسوند `.debug` و بدون Minify ساخته می‌شود.

## داده همراه اپ

- `app/src/main/assets/seed/recipes.json` در نسخه ۱٫۲٫۰ شامل ۲۰۰۰ دستور است (حدود ۸٫۲۶ مگابایت JSON).
- `seed_version = 2` است؛ نصب‌های نسخه ۱٫۰٫۰ در نخستین اجرا دوباره Seed می‌شوند.
- وجود ۲۰۰۰ دستور در `assets/seed/recipes.json` هر دو خروجی APK و AAB بررسی شد.
- افزایش حجم خروجی نسبت به نسخه ۱٫۰٫۰ عمدتاً در Debug APK دیده می‌شود؛ Release به دلیل فشرده‌سازی داده به ۲٫۹ مگابایت رسیده است.

## قابلیت انتشار

خروجی‌های فنی برای انتشار آماده‌اند: AAB امضاشده برای Google Play و APK امضاشده برای توزیع مستقیم. با این حال، این گزارش به معنی آپلود در Play Console یا تأیید سیاست‌های فروشگاه نیست. پیش از انتشار عمومی باید QA دستگاه، تصاویر فروشگاه، توضیحات، سیاست حریم خصوصی و تأیید دستی انجام شود.
