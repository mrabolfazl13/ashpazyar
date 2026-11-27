# طرح داده دانشنامه آشپزیار

## هدف
طرح داده‌ای که بتواند ۵۰٬۰۰۰ دستور را با منبع، مجوز، آموزش مرحله‌ای، جستجوی FTS5 و صفحه‌بندی پشتیبانی کند. این سند **طرح** است؛ داده و مهاجرت نهایی هنوز انجام نشده است.

## موجودیت‌ها

### Recipe
- `id`, `title`, `titleEn`, `description`, `history`, `region`, `cuisine`, `categoryIds`
- `prepMinutes`, `cookMinutes`, `restMinutes`, `totalMinutes`, `servings`, `difficulty`
- `calories`, `nutritionJson`, `allergens`, `dietTags`
- `equipment`, `ovenTemperatureC`, `prepGuide`, `servingGuide`, `storageGuide`, `troubleshooting`
- `sourceName`, `sourceUrl`, `sourceVersion`, `licenseId`, `attribution`, `validationStatus`, `reviewedAt`
- `searchText`, `ftsTitle`, `ftsIngredients`

### RecipeIngredient
- `recipeId`, `ingredientId`, `amount`, `unit`, `preparation`, `optional`, `substituteIds`, `substitutionEffect`, `sortOrder`

### RecipeStep
- `recipeId`, `stepNumber`, `title`, `instruction`
- `ingredientRefsJson`, `equipment`, `durationSeconds`, `temperatureC`, `heatLevel`
- `successIndicators`, `commonMistakes`, `recoveryInstructions`, `safetyNotes`, `imageReference`

### Ingredient
- `id`, `name`, `nameEn`, `aliases`, `category`, `defaultUnit`, `cupWeightGrams`, `pieceWeightGrams`, `allergenTags`, `substituteIds`

### Source / License
- `sourceId`, `name`, `url`, `licenseId`, `licenseUrl`, `termsReviewedAt`, `commercialUse`, `offlineDistribution`, `shareAlike`, `attributionText`

## وضعیت اعتبارسنجی
- `VERIFIED`: منبع و مجوز کامل، بازبینی انسانی، آموزش حداقلی کامل.
- `REVIEW_REQUIRED`: ساختار دارد اما منبع/آموزش/بازبینی کامل نیست.
- `REJECTED`: نقض مجوز، تکراری، ناقص یا غیرقابل استفاده.

## ایندکس و جستجو
- جدول مجازی FTS5 برای نام، نام اصلی، مواد و مترادف‌ها.
- ایندکس‌های ترکیبی: `(validationStatus, cuisine, difficulty)`, `(validationStatus, totalMinutes)`, `(validationStatus, calories)`.
- صفحه‌بندی با `LIMIT/OFFSET` یا keyset؛ ممنوعیت بارگذاری کل جدول در حافظه.
- جستجوی مواد با جدول واسط و `EXISTS`، نه `LIKE` روی فیلد متنی بزرگ.

## وضعیت فعلی پیاده‌سازی
- Room نسخه ۲، ۲۰ موجودیت؛ فیلدهای منبع/مجوز و جزئیات مرحله اضافه شده‌اند.
- Seed فعلی از JSON به Room منتقل می‌شود؛ برای ۵۰٬۰۰۰ رکورد به دیتابیس پیش‌ساخته SQLite و FTS5 نیاز است.
- `RecipeDao.observeAllWithDetails()` هنوز کل دستورها را بارگذاری می‌کند؛ برای مقیاس هدف باید صفحه‌بندی شود.
- هیچ ادعای تکمیل طرح یا داده وجود ندارد.
