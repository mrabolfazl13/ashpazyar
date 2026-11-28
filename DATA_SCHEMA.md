# طرح داده دانشنامه قاشق

## وضعیت
این سند اکنون **پیاده‌سازی‌شده** را توصیف می‌کند، نه طرح آینده. خروجی مرجع:
`ashpazyar_dataset/database/ashpazyar.db` با SQLite 3.49.1 و FTS5.

## موجودیت‌های اصلی
- `recipes`: ۵٬۷۹۰ دستور مستقل. شامل نام فارسی/اصلی، دسته/زیردسته اصلی، زمان‌ها، تعداد نفرات، روش پخت، مهارت، وعده، وضعیت آموزش و وضعیت تأیید.
- `recipe_ingredients`: ۵۵٬۰۴۱ خط ماده. مقدار عددی، حداکثر بازه، واحد، متن خام، مقدار کیفی، اطمینان استخراج و یادداشت.
- `ingredients`: ۲٬۸۲۹ ماده یکتا با نام اصلی، نام نرمال، مترادف‌ها، نوع واژه‌نامه‌ای و آلرژن‌های احتمالی.
- `units`: ۲۶ واحد/ظرف، شامل `QUALITATIVE` و `UNSPECIFIED`.
- `categories`: ۸ دسته اصلی و ۵۰ زیردسته، با حفظ نام اصلی و والد.
- `recipe_steps`: جدول کامل مراحل؛ **۰ ردیف** چون منبع مرحله‌ای ندارد.
- `content_articles`: ۵۲ رکورد غیرغذایی یکتا (۲۸ آموزش، ۲۳ تزئین، ۱ تکنیک). این رکوردها در `recipes` نیستند.
- `default_servings`: فقط کپی عددی `suitable_for` است؛ `servings_status=SOURCE_VALUE_UNVERIFIED`. این مقدار "تعداد نفرات تأییدشده" نیست.
- `source_records`: ۶٬۶۶۰ ردیف ورودی با وضعیت `RECIPE_PRIMARY`، `EXACT_DUPLICATE`، `NON_FOOD` یا `REJECTED` و شناسه مرجع.
- `recipe_duplicates`: ۱۰۰ جفت مشابه اسمی برای بازبینی؛ ادغام خودکار نشده‌اند.
- `recipe_nutrition` و `recipe_media`: آماده، بدون داده ساختگی.
- `sources` و `licenses`: منبع Persian_Cooking در Hugging Face و مجوز `MIT` با انتساب ثبت‌شده.

## کلیدها و یکپارچگی
- `recipes.id` متنی و پایدار؛ `slug` یکتا.
- `recipe_ingredients` کلید مرکب `(recipe_id, position)`.
- `source_records.row_number` کلید اصلی و پیوسته ۱ تا ۶٬۶۶۰.
- `PRAGMA foreign_keys=ON` و بررسی `foreign_key_check` در ساخت و تست.
- `PRAGMA integrity_check` برابر `ok`.

## جستجو
- `recipes_fts`: عنوان فارسی، عنوان اصلی، توضیح، مواد، دسته و برچسب‌ها.
- `content_fts`: عنوان، خلاصه، متن جایگزین و دسته محتوای غیرغذایی.
- توکنایزر `unicode61`.
- نمونه:
```sql
SELECT r.title_fa
FROM recipes_fts f JOIN recipes r ON r.rowid = f.rowid
WHERE recipes_fts MATCH 'قورمه'
LIMIT 10;
```

## وضعیت‌ها
- `VERIFIED`: فقط با منبع، مجوز، مراحل کامل و بازبینی انسانی. **در این خروجی صفر است.**
- `REVIEW_REQUIRED`: همه ۵٬۷۹۰ دستور و ۵۲ محتوا.
- `REJECTED`: ردیف نامعتبر/بدون ماده. در این ورودی صفر است.
- `instructions_status=MISSING` برای همه دستورها.

## محدودیت شناخته‌شده
- فیلدهای مرحله، دما، تغذیه و تجهیزات ساختگی پر نشده‌اند.
- `extraction_confidence` فقط کیفیت استخراج مقدار/واحد را نشان می‌دهد، نه صحت دستور.
- نوع ماده و آلرژن احتمالی مبتنی بر واژه‌نامه است و مبنای ایمنی نیست.
