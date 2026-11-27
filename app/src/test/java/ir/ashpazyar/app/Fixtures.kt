package ir.ashpazyar.app

import ir.ashpazyar.app.domain.model.*

object Fixtures {
    fun ingredient(
        id: String,
        name: String = id,
        unit: Unit = Unit.GRAM,
        cupWeight: Double? = null,
        pieceWeight: Double? = null,
        price: Double? = 100000.0
    ) = Ingredient(
        id = id,
        name = name,
        nameEn = id,
        category = "test",
        categoryTitle = "آزمون",
        defaultUnit = unit,
        cupWeightGrams = cupWeight,
        pieceWeightGrams = pieceWeight,
        referencePricePerKg = price
    )

    fun line(
        id: String,
        amount: Double,
        unit: Unit = Unit.GRAM,
        name: String = id,
        optional: Boolean = false
    ) = RecipeIngredient(id, name, amount, unit, optional = optional)

    fun recipe(
        id: String = "recipe-1",
        title: String = "برنج ساده",
        ingredients: List<RecipeIngredient> = listOf(line("rice", 100.0)),
        servings: Int = 4,
        prep: Int = 10,
        cook: Int = 20,
        categories: List<String> = listOf("main"),
        tags: List<String> = emptyList(),
        vegetarian: Boolean = false,
        budget: BudgetLevel = BudgetLevel.MID,
        order: Int = 1
    ) = Recipe(
        id = id,
        title = title,
        titleEn = null,
        description = "دستور آزمون",
        categoryIds = categories,
        tags = tags,
        servings = servings,
        prepMinutes = prep,
        cookMinutes = cook,
        difficulty = Difficulty.EASY,
        calories = 300,
        season = Season.ALL,
        budgetLevel = budget,
        heroStyle = "grain",
        isVegetarian = vegetarian,
        isVegan = false,
        isGlutenFree = true,
        isDairyFree = true,
        isFamilyFriendly = true,
        allergens = emptyList(),
        tips = emptyList(),
        ingredients = ingredients,
        steps = listOf(RecipeStep(1, "آماده‌سازی", "مواد را آماده کنید.", null)),
        editorialOrder = order
    )

    fun entry(
        recipe: Recipe,
        servings: Int = recipe.servings,
        mealType: MealType = MealType.LUNCH,
        date: Long = 20000
    ) = MealPlanEntry(
        id = 0,
        date = date,
        mealType = mealType,
        recipe = recipe,
        recipeId = recipe.id,
        customTitle = null,
        servings = servings,
        isCompleted = false,
        note = ""
    )

    fun inventory(
        ingredient: Ingredient,
        amount: Double,
        unit: Unit = ingredient.defaultUnit
    ) = InventoryItem(
        id = 1,
        ingredient = ingredient,
        ingredientId = ingredient.id,
        amount = amount,
        unit = unit,
        location = StorageLocation.PANTRY,
        purchaseDate = null,
        expiryDate = null,
        note = "",
        pricePaid = null,
        createdAt = 0
    )
}
