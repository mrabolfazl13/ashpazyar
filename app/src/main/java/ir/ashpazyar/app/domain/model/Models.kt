package ir.ashpazyar.app.domain.model

import ir.ashpazyar.app.core.util.Dimension
import kotlinx.serialization.Serializable

enum class Difficulty(val label: String) {
    EASY("آسان"), MEDIUM("متوسط"), HARD("حرفه‌ای");
    companion object { fun from(value: String) = entries.firstOrNull { it.name == value } ?: MEDIUM }
}

enum class Season(val label: String) {
    ALL("همه فصل‌ها"), SPRING("بهار"), SUMMER("تابستان"), AUTUMN("پاییز"), WINTER("زمستان");
    companion object { fun from(value: String) = entries.firstOrNull { it.name == value } ?: ALL }
}

enum class BudgetLevel(val label: String) {
    BUDGET("اقتصادی"), MEDIUM("متعادل"), PREMIUM("ویژه");
    companion object { fun from(value: String) = entries.firstOrNull { it.name == value } ?: MEDIUM }
}

enum class MealType(val label: String, val shortLabel: String) {
    BREAKFAST("صبحانه", "صبحانه"),
    LUNCH("ناهار", "ناهار"),
    DINNER("شام", "شام"),
    SNACK("میان‌وعده", "میان‌وعده");
    companion object { fun from(value: String) = entries.firstOrNull { it.name == value } ?: LUNCH }
}

enum class StorageLocation(val label: String) {
    FRIDGE("یخچال"), FREEZER("فریزر"), PANTRY("کابینت"), STORAGE("انبار"), OTHER("سایر");
    companion object { fun from(value: String) = entries.firstOrNull { it.name == value } ?: PANTRY }
}

enum class InventoryStatus(val label: String) {
    FRESH("تازه"), NEAR_EXPIRY("نزدیک انقضا"), EXPIRED("منقضی‌شده"), NO_DATE("بدون تاریخ")
}

enum class IngredientUnit(val label: String, val symbol: String) {
    GRAM("گرم", "گ"), KILOGRAM("کیلوگرم", "ک‌گ"), MILLILITER("میلی‌لیتر", "می‌ل"),
    LITER("لیتر", "ل"), TEASPOON("قاشق چای‌خوری", "ق‌چ"), TABLESPOON("قاشق غذاخوری", "ق‌غ"),
    CUP("پیمانه", "پیمانه"), PIECE("عدد", "عدد"), CLOVE("حبه", "حبه"),
    BUNCH("دسته", "دسته"), SLICE("برش", "برش"), PINCH("نوک‌قاشق", "نوک"),
    CAN("قوطی", "قوطی");
    companion object {
        fun from(value: String) = entries.firstOrNull { it.name == value.uppercase() } ?: GRAM
    }
}

data class Ingredient(
    val id: String,
    val name: String,
    val nameEn: String,
    val category: String,
    val categoryTitle: String,
    val defaultUnit: IngredientUnit,
    val cupWeightGrams: Double? = null,
    val pieceWeightGrams: Double? = null,
    val referencePricePerKg: Double? = null
) {
    val profile get() = IngredientProfile(id, name, defaultUnit, cupWeightGrams, pieceWeightGrams)
}

data class IngredientProfile(
    val id: String,
    val name: String,
    val defaultUnit: IngredientUnit,
    val cupWeightGrams: Double? = null,
    val pieceWeightGrams: Double? = null
)

data class RecipeIngredient(
    val ingredientId: String,
    val ingredientName: String,
    val amount: Double,
    val unit: IngredientUnit,
    val note: String = "",
    val optional: Boolean = false
)

data class RecipeStep(
    val number: Int,
    val title: String,
    val instruction: String,
    val timerSeconds: Int? = null
)

data class Recipe(
    val id: String,
    val title: String,
    val titleEn: String?,
    val description: String,
    val categoryIds: List<String>,
    val tags: List<String>,
    val servings: Int,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val difficulty: Difficulty,
    val calories: Int?,
    val season: Season,
    val budgetLevel: BudgetLevel,
    val heroStyle: String,
    val isVegetarian: Boolean,
    val isVegan: Boolean,
    val isGlutenFree: Boolean,
    val isDairyFree: Boolean,
    val isFamilyFriendly: Boolean,
    val allergens: List<String>,
    val tips: List<String>,
    val ingredients: List<RecipeIngredient>,
    val steps: List<RecipeStep>,
    val isUserCreated: Boolean = false,
    val imageUri: String? = null,
    val editorialOrder: Int = 1000
) {
    val totalMinutes: Int get() = prepMinutes + cookMinutes
    val isQuick: Boolean get() = totalMinutes <= 30
    val isEconomical: Boolean get() = budgetLevel == BudgetLevel.BUDGET || tags.any { it.contains("اقتصادی") }
    val matchesCount: Int get() = categoryIds.size
}

data class RecipeDraft(
    val id: String? = null,
    val title: String = "",
    val titleEn: String = "",
    val description: String = "",
    val categoryIds: Set<String> = emptySet(),
    val tags: List<String> = emptyList(),
    val servings: Int = 4,
    val prepMinutes: Int = 15,
    val cookMinutes: Int = 30,
    val difficulty: Difficulty = Difficulty.EASY,
    val calories: Int? = null,
    val ingredients: List<DraftIngredient> = emptyList(),
    val steps: List<DraftStep> = emptyList(),
    val tips: List<String> = emptyList(),
    val imageUri: String? = null
)

data class DraftIngredient(
    val ingredientId: String,
    val ingredientName: String,
    val amount: Double,
    val unit: IngredientUnit,
    val note: String = "",
    val optional: Boolean = false
)

data class DraftStep(
    val title: String,
    val instruction: String,
    val timerMinutes: Int? = null
)

data class InventoryItem(
    val id: Long,
    val ingredient: Ingredient?,
    val ingredientId: String,
    val amount: Double,
    val unit: IngredientUnit,
    val location: StorageLocation,
    val purchaseDate: Long?,
    val expiryDate: Long?,
    val note: String,
    val pricePaid: Double?,
    val createdAt: Long
) {
    fun status(now: Long = System.currentTimeMillis(), warningDays: Int = 3): InventoryStatus {
        val expiry = expiryDate ?: return InventoryStatus.NO_DATE
        val today = java.time.LocalDate.now()
        val exp = java.time.Instant.ofEpochMilli(expiry).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return when {
            exp.isBefore(today) -> InventoryStatus.EXPIRED
            !exp.isAfter(today.plusDays(warningDays.toLong())) -> InventoryStatus.NEAR_EXPIRY
            else -> InventoryStatus.FRESH
        }
    }
    val displayName: String get() = ingredient?.name ?: ingredientId
}

data class InventoryTransaction(
    val id: Long,
    val groupId: String,
    val ingredientId: String,
    val ingredientName: String,
    val deltaBaseAmount: Double,
    val dimension: Dimension,
    val unitLabel: String,
    val reason: String,
    val canUndo: Boolean,
    val createdAt: Long
)

data class MealPlanEntry(
    val id: Long,
    val date: Long,
    val mealType: MealType,
    val recipe: Recipe?,
    val recipeId: String?,
    val customTitle: String?,
    val servings: Int,
    val isCompleted: Boolean,
    val note: String
) {
    val title get() = recipe?.title ?: customTitle ?: "بدون نام"
}

data class ShoppingList(
    val id: Long,
    val title: String,
    val status: String,
    val createdAt: Long
)

data class ShoppingItem(
    val id: Long,
    val listId: Long,
    val ingredientId: String?,
    val name: String,
    val category: String,
    val amount: Double,
    val unit: IngredientUnit,
    val estimatedPrice: Double?,
    val actualPrice: Double?,
    val isChecked: Boolean,
    val source: String,
    val note: String
) {
    val linePrice: Double? get() = actualPrice ?: estimatedPrice
}

data class Favorite(val recipeId: String)

data class Category(
    val id: String,
    val title: String,
    val subtitle: String,
    val accent: String,
    val icon: String,
    val sortOrder: Int
)

data class CookingTip(
    val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val content: String,
    val safety: Boolean
)

data class FoodPreferences(
    val vegetarian: Boolean = false,
    val noMeat: Boolean = false,
    val lowCost: Boolean = false,
    val quickMeals: Boolean = false,
    val lowSalt: Boolean = false,
    val dairyFree: Boolean = false,
    val glutenFree: Boolean = false,
    val dislikedIngredients: List<String> = emptyList(),
    val allergies: List<String> = emptyList()
)

data class CookingTimer(
    val id: Long,
    val recipeId: String?,
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val endAt: Long,
    val isRunning: Boolean
)

data class Leftover(
    val id: Long,
    val title: String,
    val recipeId: String?,
    val preparedDate: Long,
    val portions: Int,
    val suggestedUseBy: Long,
    val note: String,
    val isConsumed: Boolean
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = suggestedUseBy < now
}

data class IngredientPrice(
    val ingredientId: String,
    val pricePerKg: Double?,
    val updatedAt: Long
)

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val darkTheme: Boolean = false,
    val themeMode: String = "SYSTEM",
    val expirationWarningDays: Int = 3,
    val familySize: Int = 4,
    val weeklyBudget: Long = 0,
    val mealReminders: Boolean = true,
    val expiryNotifications: Boolean = true,
    val measurementSystem: String = "METRIC"
)

data class CostBreakdown(
    val recipeTitle: String,
    val servings: Int,
    val lines: List<CostLine>,
    val total: Double,
    val perServing: Double
) {
    val mostExpensive: CostLine? get() = lines.maxByOrNull { it.cost }
}

data class CostLine(
    val ingredientName: String,
    val amountLabel: String,
    val cost: Double
)

data class MatchResult(
    val recipe: Recipe,
    val matchPercent: Int,
    val availableCount: Int,
    val missingCount: Int,
    val totalRequired: Int,
    val missing: List<MissingIngredient>,
    val substitutesUsed: List<SubstituteUse>,
    val fullyAvailable: Boolean
)

data class MissingIngredient(
    val ingredientId: String,
    val name: String,
    val requiredAmount: Double,
    val availableAmount: Double,
    val unit: IngredientUnit,
    val optional: Boolean,
    val suggestedAmountLabel: String
)

data class SubstituteUse(
    val originalName: String,
    val substituteName: String,
    val note: String
)

data class Recommendation(
    val recipe: Recipe,
    val score: Int,
    val reasons: List<String>,
    val match: MatchResult? = null
)

data class PlanGenerationInput(
    val startDate: Long,
    val familySize: Int,
    val budget: Long,
    val maxMinutes: Int?,
    val includeBreakfast: Boolean,
    val includeLunch: Boolean,
    val includeDinner: Boolean,
    val variety: Boolean,
    val noMeat: Boolean,
    val vegetarian: Boolean,
    val lowCost: Boolean,
    val pantryIngredientIds: Set<String>,
    val dislikedIngredients: Set<String>,
    val allergens: Set<String>
)

data class PlanProposal(
    val entries: List<ProposedMeal>,
    val notes: List<String>,
    val estimatedMatch: Int
)

data class ProposedMeal(
    val date: Long,
    val mealType: MealType,
    val recipe: Recipe,
    val servings: Int
)

data class ShoppingAggregate(
    val ingredientId: String?,
    val name: String,
    val category: String,
    val amount: Double,
    val unit: IngredientUnit,
    val sourceRecipeIds: Set<String>
)

data class ShoppingBuildResult(
    val items: List<ShoppingAggregate>,
    val skippedIncompatible: List<String>,
    val totalNeededCount: Int,
    val alreadyCoveredCount: Int
)

@Serializable
data class BackupEnvelope(
    val format: String = "ashpazyar-backup",
    val version: Int = 1,
    val exportedAt: Long,
    val userRecipes: List<BackupRecipe> = emptyList(),
    val inventory: List<BackupInventory> = emptyList(),
    val mealPlan: List<BackupMeal> = emptyList(),
    val shoppingItems: List<BackupShoppingItem> = emptyList(),
    val preferences: BackupPreferences = BackupPreferences(),
    val favorites: List<String> = emptyList(),
    val prices: List<BackupPrice> = emptyList(),
    val leftovers: List<BackupLeftover> = emptyList(),
    val settings: BackupSettings = BackupSettings()
)

@Serializable
data class BackupPreferences(
    val vegetarian: Boolean = false,
    val noMeat: Boolean = false,
    val lowCost: Boolean = false,
    val quickMeals: Boolean = false,
    val lowSalt: Boolean = false,
    val dairyFree: Boolean = false,
    val glutenFree: Boolean = false,
    val dislikedIngredients: List<String> = emptyList(),
    val allergies: List<String> = emptyList()
)

@Serializable
data class BackupSettings(
    val themeMode: String = "SYSTEM",
    val expirationWarningDays: Int = 3,
    val familySize: Int = 4,
    val weeklyBudget: Long = 0,
    val mealReminders: Boolean = true,
    val expiryNotifications: Boolean = true
)

@Serializable
data class BackupPrice(val ingredientId: String, val pricePerKg: Double? = null)

@Serializable
data class BackupLeftover(
    val title: String,
    val recipeId: String? = null,
    val preparedDate: Long,
    val portions: Int = 1,
    val suggestedUseBy: Long,
    val note: String = "",
    val isConsumed: Boolean = false
)

@Serializable
data class BackupRecipe(
    val id: String,
    val title: String,
    val description: String,
    val servings: Int,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val difficulty: String,
    val categoryIds: List<String>,
    val tags: List<String>,
    val ingredients: List<BackupRecipeIngredient>,
    val steps: List<BackupStep>,
    val tips: List<String> = emptyList()
)

@Serializable
data class BackupRecipeIngredient(val ingredientId: String, val amount: Double, val unit: String, val note: String = "", val optional: Boolean = false)
@Serializable
data class BackupStep(val title: String, val instruction: String, val timerSeconds: Int? = null)

@Serializable
data class BackupInventory(val ingredientId: String, val amount: Double, val unit: String, val location: String, val expiryDate: Long? = null, val note: String = "", val pricePaid: Double? = null)

@Serializable
data class BackupMeal(val date: Long, val mealType: String, val recipeId: String? = null, val customTitle: String? = null, val servings: Int = 4)

@Serializable
data class BackupShoppingItem(val name: String, val ingredientId: String? = null, val category: String, val amount: Double, val unit: String, val estimatedPrice: Double? = null, val actualPrice: Double? = null, val isChecked: Boolean = false)
