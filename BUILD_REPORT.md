# گزارش بیلد آشپزیار

## بیلد معتبر نهایی

| مورد | مقدار |
|---|---|
| Workflow | `.github/workflows/android-ci.yml` |
| اجرا | `45926032572` |
| Commit | `8154418` |
| نتیجه | ✅ موفق |
| لینک | https://github.com/mrabolfazl13/ashpazyar/actions/runs/45926032572 |

همه مراحل اجرا موفق بودند: Validate، Unit Tests، کامپایل تست‌های Compose، Lint، آماده‌سازی امضا، Debug APK، Release APK، Release AAB، بسته‌بندی و آپلود.

## خروجی‌ها

| فایل | حجم (بایت) | SHA-256 |
|---|---:|---|
| `ashpazyar-debug.apk` | 19,805,305 | `A20903CF99187C682AF86C4AED46CE79D0C5998D6A097D1EAB92A93A9F097B57` |
| `ashpazyar-release.apk` | 2,326,493 | `90B84F2D12BC437AAF995C3E56F16F25E77ED7ED2412CA701DF75CBC7115279A` |
| `ashpazyar-release.aab` | 5,396,347 | `DF4443E07F87E9F7DD152469EB5B656B0E3ADC5D72CA4857C393502B63826280` |

مسیر محلی نسخه دانلودشده: `dist/` (در Git نادیده گرفته می‌شود).

دریافت مجدد از GitHub:

```bash
gh run download 45926032572 -n ashpazyar-build-11 -D dist
```

## امضای Release

- هر دو خروجی Release **امضا شده‌اند**.
- APK دارای بلوک امضای APK Signature Scheme v2 است.
- AAB دارای `META-INF/ASHPAZYA.RSA` و `META-INF/ASHPAZYA.SF` است.
- گواهی: `CN=AshpazYar, OU=Mobile, O=AshpazYar, L=Tehran, C=IR`
- SHA-256 گواهی:

```text
0C:DF:A4:25:29:1D:0E:CC:7D:63:18:2E:37:37:0F:C1:A1:7C:11:3E:6F:D1:78:6E:C1:33:6D:5D:31:7A:9F:5C
```

- الگوریتم: RSA 4096-bit، SHA384withRSA.
- اعتبار گواهی: ۱۰۰۰۰ روز.
- کلید در GitHub Secrets با نام‌های `ANDROID_KEYSTORE_BASE64`، `ANDROID_KEYSTORE_PASSWORD`، `ANDROID_KEY_ALIAS` و `ANDROID_KEY_PASSWORD` تنظیم شده است.
- نسخه پشتیبان محلی کلید در `keystore/release.jks` و رمز آن در `keystore/keystore.properties` قرار دارد؛ هر دو با `.gitignore` از مخزن خارج‌اند.

### هشدار مهم

این کلید، هویت انتشار اپلیکیشن است. نگهداری نسخه پشتیبان رمزگذاری‌شده از `release.jks` و رمز آن الزامی است. گم شدن کلید، به‌روزرسانی نسخه‌های بعدی در Google Play را غیرممکن می‌کند. فایل کلید یا رمز آن نباید در Git، Issue، چت عمومی یا لاگ CI قرار گیرد.

## پیکربندی بیلد

- `applicationId`: `ir.ashpazyar.app`
- نسخه: `1.0.0` (versionCode 1)
- `minSdk`: 26
- `compileSdk` / `targetSdk`: 35
- Java 17، Kotlin 2.0.21، AGP 8.7.3، Gradle 8.9
- Release با R8 و shrinkResources بهینه شده است.
- Debug با پسوند `.debug` و بدون Minify ساخته می‌شود.

## قابلیت انتشار

خروجی‌های فنی برای انتشار آماده‌اند: AAB امضاشده برای Google Play و APK امضاشده برای توزیع مستقیم. با این حال، این گزارش به معنی آپلود در Play Console یا تأیید سیاست‌های فروشگاه نیست. پیش از انتشار عمومی باید QA دستگاه، تصاویر فروشگاه، توضیحات، سیاست حریم خصوصی و تأیید دستی انجام شود.
