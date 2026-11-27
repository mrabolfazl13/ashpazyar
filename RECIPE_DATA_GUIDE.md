# راهنمای داده دستور غذا — آشپزیار

این فایل قرارداد منبع داده اولیه است. داده‌ها در `app/src/main/assets/seed/recipes.json` نگهداری می‌شوند و هنگام نخستین اجرا در Room درج می‌شوند.

## وضعیت دیتاست

- نسخه داده: `seed_version = 2`
- ۲۰۰۰ دستور یکتا: ۱۵۰ دستور پایه دست‌نویس و ۱۸۵۰ نسخه تولیدشده با قواعد آشپزی
- ۲۲۴ ماده اولیه، ۱۵ دسته، ۲۴ نکته و ۴۴ قاعده جایگزینی
- منبع پایه در `tools/base-recipes.json` ثابت است تا تولید دوباره، دیتاست را روی خودش بازتولید نکند.
- تولید: `node tools/generate-recipes.js`
- اعتبارسنجی: `node tools/validate-recipes.js`
- Workflow پیش از Gradle، اعتبارسنجی داده را اجرا می‌کند.

## ساختار فایل

```json
{
  "recipes": [
    {
      "id": "ghormeh-sabzi",
      "title": "قورمه سبزی",
      "titleEn": "Ghormeh Sabzi",
      "description": "خورش عطرآگین ایرانی با سبزی تازه، لپه و گوشت.",
      "categoryIds": ["iranian", "stew", "family"],
      "tags": ["سنتی", "خورش", "خانوادگی"],
      "servings": 4,
      "prepMinutes": 20,
      "cookMinutes": 120,
      "difficulty": "MEDIUM",
      "calories": 420,
      "season": "ALL",
      "budgetLevel": "MEDIUM",
      "heroStyle": "TERRACOTTA",
      "isVegetarian": false,
      "isVegan": false,
      "isGlutenFree": true,
      "isDairyFree": true,
      "isFamilyFriendly": true,
      "allergens": [],
      "ingredients": [
        { "ingredientId": "lamb", "amount": 500, "unit": "GRAM", "note": "خردشده", "optional": false }
      ],
      "steps": [
        { "title": "تفت دادن", "instruction": "پیاز را ...", "timerSeconds": 600 }
      ],
      "tips": ["نکته اول", "نکته دوم"],
      "editorialOrder": 0
    }
  ]
}
```

## قواعد الزامی

- شناسه هر دستور یکتا و انگلیسی kebab-case باشد.
- فقط از شناسه‌های موجود در `ingredients.json` استفاده شود.
- دسته‌ها فقط از شناسه‌های `categories.json` انتخاب شوند.
- `difficulty`: `EASY` | `MEDIUM` | `HARD`
- `season`: `ALL` | `SPRING` | `SUMMER` | `AUTUMN` | `WINTER`
- `budgetLevel`: `BUDGET` | `MEDIUM` | `PREMIUM`
- `heroStyle`: `PEACH` | `TERRACOTTA` | `SAGE` | `CREAM` | `DARK`
- واحدها: `GRAM`, `KILOGRAM`, `MILLILITER`, `LITER`, `TEASPOON`, `TABLESPOON`, `PINCH`, `CUP`, `PIECE`, `CLOVE`, `BUNCH`, `SLICE`, `CAN`.
- حداقل ۴ و حداکثر ۱۲ ماده؛ حداقل ۴ و حداکثر ۹ مرحله برای هر دستور.
- هر ماده در یک دستور فقط یک بار تکرار شود.
- `allergens` فقط از `gluten`, `dairy`, `egg`, `nuts`, `sesame`, `fish`, `shellfish`, `soy`, `celery` انتخاب شود.
- `calories` عدد تقریبی معتبر و غیرمنفی باشد؛ ادعای پزشکی ممنوع است.
- تصویر این نسخه به‌صورت هنری و آفلاین در Compose تولید می‌شود؛ فیلد تصویر در JSON لازم نیست.
- دستورهای گیاهی باید بدون گوشت، مرغ، ماهی و میگو باشند.
- دستورهای وگان نباید گوشت، لبنیات، تخم‌مرغ یا عسل داشته باشند.
- دستورهای بدون گلوتن نباید گندم، جو، نان، ماکارونی یا مواد حاوی گلوتن داشته باشند.
- دستورهای بدون لبنیات نباید شیر، ماست، کره، پنیر، خامه یا کشک داشته باشند.
- نام‌ها و مراحل فارسی روان، واقعی و قابل اجرا باشند.
- عنوان نسخه‌های تولیدشده باید با ماده افزوده‌شده هم‌خوان باشد؛ مثلاً «با مرغ» باید مرغ داشته باشد.
- ترکیب‌های متناقض مانند «گیاهی با ماهی»، «پیتزا با نان» یا «دوغ با توت‌فرنگی» مجاز نیستند.

## تولید نسخه‌های متنوع

`tools/generate-recipes.js` از ۱۵۰ دستور پایه، نسخه‌های واقعی‌تر می‌سازد:

- جایگزینی پروتئین حیوانی با مرغ، گوشت، ماهی، میگو، عدس، نخود، لوبیا، قارچ، بادمجان، اسفناج و سیب‌زمینی؛
- افزودن قارچ، پنیر، گوجه، فلفل دلمه‌ای، سبزیجات، حبوبات، خرما، عسل، میوه و ادویه؛
- نسخه‌های اقتصادی، سریع، مجلسی، رژیمی، تند، لیمویی، زعفرانی و گیاهی؛
- حذف خودکار ترکیب‌های تکراری، بی‌معنا و بدون تغییر واقعی؛
- بازنویسی متن مراحل هنگام تعویض پروتئین تا مراحل با مواد هم‌خوان بمانند.

پس از هر تغییر:

```bash
node tools/generate-recipes.js
node tools/validate-recipes.js
```

اگر دیتاست پایه تغییر کرد، `tools/base-recipes.json` را نیز هم‌زمان به‌روز کنید. `SEED_VERSION` در `SeedLoader.kt` باید پس از تغییر دیتاست افزایش یابد تا نصب‌های قبلی دوباره Seed شوند.
