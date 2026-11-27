package ir.ashpazyar.app.data

import ir.ashpazyar.app.core.util.Dimension
import ir.ashpazyar.app.data.local.entity.*
import ir.ashpazyar.app.domain.model.*

private const val LIST_SEPARATOR = "|"
private const val TIP_SEPARATOR = "\u001F"

fun String.toListParts(): List<String> =
    if (isBlank()) emptyList() else split(LIST_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

fun List<String>.toStoredList(): String = joinToString(LIST_SEPARATOR)

fun RecipeWithDetails.toDomain(ingredientsById: Map<String, Ingredient>): Recipe {
    return Recipe(
        id = recipe.id,
        title = recipe.title,
        titleEn = recipe.titleEn,
        description = recipe.description,
        categoryIds = recipe.categoryIds.toListParts(),
        tags = recipe.tags.toListParts(),
        servings = recipe.servings,
        prepMinutes = recipe.prepMinutes,
        cookMinutes = recipe.cookMinutes,
        difficulty = Difficulty.from(recipe.difficulty),
        calories = recipe.calories,
        season = Season.from(recipe.season),
        budgetLevel = BudgetLevel.from(recipe.budgetLevel),
        heroStyle = recipe.heroStyle,
        isVegetarian = recipe.isVegetarian,
        isVegan = recipe.isVegan,
        isGlutenFree = recipe.isGlutenFree,
        isDairyFree = recipe.isDairyFree,
        isFamilyFriendly = recipe.isFamilyFriendly,
        allergens = recipe.allergens.toListParts(),
        tips = if (recipe.tips.isBlank()) emptyList() else recipe.tips.split(TIP_SEPARATOR).filter { it.isNotBlank() },
        ingredients = orderedIngredients().map {
            RecipeIngredient(
                ingredientId = it.ingredientId,
                ingredientName = ingredientsById[it.ingredientId]?.name ?: it.ingredientId,
                amount = it.amount,
                unit = IngredientUnit.from(it.unit),
                note = it.note,
                optional = it.optional
            )
        },
        steps = orderedSteps().map {
            RecipeStep(it.stepNumber, it.title, it.instruction, it.timerSeconds)
        },
        isUserCreated = recipe.isUserCreated,
        imageUri = recipe.imageUri,
        editorialOrder = recipe.editorialOrder
    )
}

fun IngredientEntity.toDomain(): Ingredient = Ingredient(
    id = id,
    name = name,
    nameEn = nameEn,
    category = category,
    categoryTitle = categoryTitle,
    defaultUnit = IngredientUnit.from(defaultUnit),
    cupWeightGrams = cupWeightGrams,
    pieceWeightGrams = pieceWeightGrams,
    referencePricePerKg = referencePricePerKg
)

fun InventoryWithIngredient.toDomain(): InventoryItem = InventoryItem(
    id = item.id,
    ingredient = ingredient?.toDomain(),
    ingredientId = item.ingredientId,
    amount = item.amount,
    unit = IngredientUnit.from(item.unit),
    location = StorageLocation.from(item.location),
    purchaseDate = item.purchaseDate,
    expiryDate = item.expiryDate,
    note = item.note,
    pricePaid = item.pricePaid,
    createdAt = item.createdAt
)

fun InventoryTransactionEntity.toDomain(): InventoryTransaction = InventoryTransaction(
    id = id,
    groupId = groupId,
    ingredientId = ingredientId,
    ingredientName = ingredientNameSnapshot,
    deltaBaseAmount = deltaBaseAmount,
    dimension = runCatching { Dimension.valueOf(dimension) }.getOrDefault(Dimension.MASS),
    unitLabel = unitSnapshot,
    reason = reason,
    canUndo = canUndo,
    createdAt = createdAt
)

fun MealPlanWithRecipe.toDomain(ingredientsById: Map<String, Ingredient>): MealPlanEntry = MealPlanEntry(
    id = item.id,
    date = item.planDate,
    mealType = MealType.from(item.mealType),
    recipe = recipe?.let {
        RecipeWithDetails(it, emptyList(), emptyList()).toDomain(ingredientsById)
    },
    recipeId = item.recipeId,
    customTitle = item.customTitle,
    servings = item.servings,
    isCompleted = item.isCompleted,
    note = item.note
)

fun ShoppingListEntity.toDomain(): ShoppingList = ShoppingList(id, title, status, createdAt)

fun ShoppingListItemEntity.toDomain(): ShoppingItem = ShoppingItem(
    id = id,
    listId = shoppingListId,
    ingredientId = ingredientId,
    name = name,
    category = category,
    amount = amount,
    unit = IngredientUnit.from(unit),
    estimatedPrice = estimatedPrice,
    actualPrice = actualPrice,
    isChecked = isChecked,
    source = source,
    note = note
)

fun RecipeCategoryEntity.toDomain(): Category = Category(id, title, subtitle, accent, icon, sortOrder)

fun CookingTipEntity.toDomain(): CookingTip = CookingTip(id, category, title, summary, content, safety)

fun FoodPreferenceEntity.toDomain(): FoodPreferences = FoodPreferences(
    vegetarian = vegetarian,
    noMeat = noMeat,
    lowCost = lowCost,
    quickMeals = quickMeals,
    lowSalt = lowSalt,
    dairyFree = dairyFree,
    glutenFree = glutenFree,
    dislikedIngredients = dislikedIngredients.toListParts(),
    allergies = allergies.toListParts()
)

fun FoodPreferences.toEntity(): FoodPreferenceEntity = FoodPreferenceEntity(
    vegetarian = vegetarian,
    noMeat = noMeat,
    lowCost = lowCost,
    quickMeals = quickMeals,
    lowSalt = lowSalt,
    dairyFree = dairyFree,
    glutenFree = glutenFree,
    dislikedIngredients = dislikedIngredients.toStoredList(),
    allergies = allergies.toStoredList(),
    updatedAt = System.currentTimeMillis()
)

fun CookingTimerEntity.toDomain(): CookingTimer = CookingTimer(id, recipeId, label, totalSeconds, remainingSeconds, endAt, isRunning)

fun FoodLeftoverEntity.toDomain(): Leftover = Leftover(id, title, recipeId, preparedDate, portions, suggestedUseBy, note, isConsumed)

fun IngredientPriceEntity.toDomain(): IngredientPrice = IngredientPrice(ingredientId, pricePerKg, updatedAt)
