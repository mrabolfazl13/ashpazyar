# راهنمای داده دستور غذا — آشپزیار

این فایل قرارداد منبع داده اولیه است. داده‌ها در `app/src/main/assets/seed/recipes.json` نگهداری می‌شوند و هنگام نخستین اجرا در Room درج می‌شوند.

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
      "heroStyle": "FOREST",
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
      "tips": ["نکته اول", "نکته دوم"]
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
- `heroStyle`: `FOREST` | `TERRACOTTA` | `SAGE` | `PEACH` | `HARVEST` | `NIGHT`
- واحدها: `GRAM`, `KILOGRAM`, `MILLILITER`, `LITER`, `TEASPOON`, `TABLESPOON`, `CUP`, `PIECE`, `CLOVE`, `BUNCH`, `SLICE`, `PINCH`, `CAN`.
- حداقل ۴ ماده و ۴ مرحله برای هر دستور.
- `allergens` فقط از `gluten`, `dairy`, `egg`, `nuts`, `peanut`, `sesame`, `fish`, `shellfish`, `soy`, `celery` انتخاب شود.
- `calories` فقط عدد تقریبی معتبر یا `null`؛ ادعای پزشکی ممنوع.
- تصویر این نسخه به‌صورت هنری و آفلاین در Compose تولید می‌شود؛ فیلد تصویر در JSON لازم نیست.
- دستورهای گیاهی باید بدون گوشت، مرغ، ماهی و میگو باشند.
- دستورهای بدون گلوتن نباید گندم، جو، نان، ماکارونی یا مواد حاوی گلوتن داشته باشند.
- دستورهای بدون لبنیات نباید شیر، ماست، کره، پنیر، خامه یا کشک داشته باشند.
- نام‌ها و مراحل فارسی روان، واقعی و قابل اجرا باشند.
