# طرح پایگاه داده آشپزیار

## مشخصات
- موتور: Room
- نام پایگاه داده: `ashpazyar.db`
- نسخه: ۱
- خروجی Schema: `app/schemas`
- کلیدهای خارجی: فعال
- مهاجرت: برای نسخه ۱ مهاجرتی لازم نیست؛ هر تغییر ساختار در نسخه‌های بعدی باید Migration صریح داشته باشد.

## موجودیت‌ها

### recipes
جدول اصلی دستورها، شامل هر دو دستور داخلی و دستور ساخت کاربر.

| ستون | نوع | توضیح |
|---|---|---|
| id | TEXT PK | شناسه یکتا |
| title, titleEn, description | TEXT | عنوان فارسی/انگلیسی و توضیح |
| categoryIds, tags, allergens, tips | TEXT | فهرست‌های جدا‌شده با جداکننده داخلی |
| servings, prepMinutes, cookMinutes | INTEGER | نفرات و زمان |
| difficulty, season, budgetLevel, heroStyle | TEXT | شماره‌گذاری‌شده دامنه |
| calories | INTEGER NULL | کالری تقریبی |
| isVegetarian ... isFamilyFriendly | INTEGER | پرچم‌های رژیمی/خانوادگی |
| searchText | TEXT | متن نرمال‌شده جست‌وجو |
| isUserCreated | INTEGER | ساخت کاربر |
| imageUri | TEXT NULL | مسیر تصویر محلی اختیاری |
| editorialOrder, createdAt, updatedAt | INTEGER | ترتیب و زمان |

ایندکس‌ها: `searchText`، `title`، `editorialOrder`.

### recipe_ingredients
خطوط مواد دستور؛ FK به `recipes.id` با CASCADE.
- `id`، `recipeId`، `ingredientId`، `amount`، `unit`، `note`، `optional`، `sortOrder`.
- ایندکس: `recipeId`، `ingredientId`.

### recipe_steps
مراحل پخت؛ FK به `recipes.id` با CASCADE.
- `stepNumber`، `title`، `instruction`، `timerSeconds`.

### ingredients
مواد اولیه مرجع.
- `id`، `name`، `nameEn`، `category`، `categoryTitle`، `defaultUnit`، `nameNormalized`.
- `cupWeightGrams` و `pieceWeightGrams` برای تبدیل دقیق.
- `referencePricePerKg` برای برآورد هزینه.
- ایندکس: `nameNormalized`، `category`.

### ingredient_aliases
نام‌های جایگزین برای جست‌وجو؛ FK به `ingredients.id` با CASCADE.
- `alias`، `aliasNormalized` (یکتا) و `ingredientId`.

### ingredient_substitutes
قواعد جایگزینی مواد.
- `ingredientId`، `substituteIngredientId`، `recipeId` (اختیاری)، `ratio`، `note`، `contextTag`.
- هر دو FK با CASCADE. قواعد سراسری هنگام Seed مجدد پاک و بازنویسی می‌شوند.

### kitchen_inventory
موجودی انبار.
- `ingredientId` (FK RESTRICT)، `amount`، `unit`، `location`، `purchaseDate`، `expiryDate`، `note`، `pricePaid`.
- ایندکس: `ingredientId`، `expiryDate`، `location`.

### inventory_transactions
دفتر تغییرات انبار؛ مستقل از دستور تا حذف دستور تاریخچه را نبرد.
- `groupId` برای گروه‌بندی یک عملیات، `inventoryId`، `ingredientId`، نام و واحد Snapshot.
- `deltaBaseAmount`، `dimension`، `reason`، `referenceType`، `referenceId`، `canUndo`.

### meal_plan_items
وعده‌های برنامه هفتگی.
- `planDate` (EpochDay)، `mealType`، `recipeId` (FK SET NULL)، `customTitle`، `servings`، `isCompleted`، `note`.
- قید یکتا روی `(planDate, mealType)`؛ هر وعده در هر روز یکی است.

### shopping_lists و shopping_list_items
- فهرست فعال/آرشیو با `status`.
- آیتم‌ها شامل نام، دسته، مقدار، واحد، قیمت برآوردی/واقعی، وضعیت خرید، منبع و FK اختیاری به ماده.
- حذف فهرست، آیتم‌های آن را CASCADE حذف می‌کند.

### favorite_recipes و user_recipes
- علاقه‌مندی با FK CASCADE به دستور.
- دستور کاربر با یادداشت شخصی، دستور مبدأ و زمان‌ها.

### food_preferences
یک رکورد (`id = 1`) شامل پرچم‌های رژیمی، حساسیت‌ها و مواد نپسندیده.

### cooking_timers
تایمرهای آشپزی؛ `endAt`، `remainingSeconds`، `isRunning` و FK اختیاری به دستور (SET NULL).

### food_leftovers
غذای باقی‌مانده؛ `preparedDate`، `portions`، `suggestedUseBy`، `isConsumed`.

### ingredient_prices
قیمت هر کیلوگرم ثبت‌شده توسط کاربر؛ `ingredientId` PK.

### recipe_categories و cooking_tips
دسته‌ها و نکته‌های پخت داخلی، همراه با ترتیب نمایش و پرچم ایمنی.

### app_meta
کلید/مقدار برای نسخه Seed و فراداده‌های سبک.

## رابطه‌های کلیدی
```
recipes 1 ─── N recipe_ingredients
recipes 1 ─── N recipe_steps
recipes 1 ─── N favorite_recipes
recipes 1 ─── N user_recipes
ingredients 1 ─── N kitchen_inventory
ingredients 1 ─── N ingredient_aliases
ingredients 1 ─── N ingredient_substitutes
shopping_lists 1 ─── N shopping_list_items
meal_plan_items N ─── 1 recipes (SET NULL)
```

## قواعد داده
- Seed فقط در تراکنش اجرا می‌شود.
- حذف دستور، مواد و مراحل و علاقه‌مندی را CASCADE حذف می‌کند اما تراکنش‌های انبار را نگه می‌دارد.
- حذف ماده‌ای که در انبار استفاده شده به‌دلیل RESTRICT ممکن نیست.
- فهرست خرید تاریخی، Snapshot نام ماده را دارد و به حذف ماده وابسته نیست.
- داده‌های کاربر در پشتیبان‌گیری JSON با SAF ذخیره می‌شوند.
