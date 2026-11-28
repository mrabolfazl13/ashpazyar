package ir.ashpazyar.app.data.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedRecipeFile(val recipes: List<SeedRecipe> = emptyList())

@Serializable
data class SeedRecipe(
    val id: String,
    val title: String,
    val titleEn: String? = null,
    val description: String = "",
    val categoryIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val servings: Int = 4,
    val prepMinutes: Int = 15,
    val cookMinutes: Int = 30,
    val difficulty: String = "EASY",
    val calories: Int? = null,
    val season: String = "ALL",
    val budgetLevel: String = "MEDIUM",
    val heroStyle: String = "CREAM",
    val isVegetarian: Boolean = false,
    val isVegan: Boolean = false,
    val isGlutenFree: Boolean = false,
    val isDairyFree: Boolean = false,
    val isFamilyFriendly: Boolean = true,
    val allergens: List<String> = emptyList(),
    val ingredients: List<SeedRecipeIngredient> = emptyList(),
    val steps: List<SeedStep> = emptyList(),
    val tips: List<String> = emptyList(),
    val region: String? = null,
    val history: String? = null,
    val resultDescription: String? = null,
    val restMinutes: Int = 0,
    val equipment: List<String> = emptyList(),
    val ovenTemperatureC: Int? = null,
    val prepGuide: List<String> = emptyList(),
    val servingGuide: List<String> = emptyList(),
    val storageGuide: String? = null,
    val troubleshooting: List<String> = emptyList(),
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val license: String? = null,
    val validationStatus: String = "REVIEW_REQUIRED"
)

@Serializable
data class SeedRecipeIngredient(
    val ingredientId: String,
    val amount: Double,
    val unit: String = "GRAM",
    val note: String = "",
    val optional: Boolean = false
)

@Serializable
data class SeedStep(
    val title: String = "",
    val instruction: String,
    val timerSeconds: Int? = null,
    val ingredientRefs: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val temperatureC: Int? = null,
    val heatLevel: String? = null,
    val successIndicators: List<String> = emptyList(),
    val commonMistakes: List<String> = emptyList(),
    val recoveryInstructions: String? = null,
    val safetyNotes: List<String> = emptyList()
)

@Serializable
data class SeedIngredient(
    val id: String,
    val name: String,
    val nameEn: String = "",
    val category: String = "OTHER",
    val categoryTitle: String = "سایر",
    val defaultUnit: String = "GRAM",
    val aliases: List<String> = emptyList(),
    val cupWeightGrams: Double? = null,
    val pieceWeightGrams: Double? = null,
    val referencePricePerKg: Double? = null
)

@Serializable
data class SeedCategory(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val accent: String = "CREAM",
    val icon: String = "pot"
)

@Serializable
data class SeedTip(
    val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val content: String,
    val safety: Boolean = false
)

@Serializable
data class SeedSubstituteFile(val substitutes: List<SeedSubstitute> = emptyList())

@Serializable
data class SeedSubstitute(
    val ingredientId: String,
    val substituteIngredientId: String,
    val recipeId: String? = null,
    val ratio: Double = 1.0,
    val note: String = "",
    val contextTag: String = ""
)
