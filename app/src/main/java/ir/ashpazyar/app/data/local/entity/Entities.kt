package ir.ashpazyar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(
    tableName = "recipes",
    indices = [Index("searchText"), Index("title"), Index("editorialOrder")]
)
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val titleEn: String? = null,
    val description: String,
    val categoryIds: String,
    val tags: String,
    val servings: Int,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val difficulty: String,
    val calories: Int? = null,
    val season: String = "ALL",
    val budgetLevel: String = "MEDIUM",
    val heroStyle: String = "FOREST",
    val isVegetarian: Boolean = false,
    val isVegan: Boolean = false,
    val isGlutenFree: Boolean = false,
    val isDairyFree: Boolean = false,
    val isFamilyFriendly: Boolean = true,
    val allergens: String = "",
    val tips: String = "",
    val searchText: String,
    val isUserCreated: Boolean = false,
    val imageUri: String? = null,
    val editorialOrder: Int = 1000,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId"), Index("ingredientId")]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val ingredientId: String,
    val amount: Double,
    val unit: String,
    val note: String = "",
    val optional: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "recipe_steps",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId")]
)
data class RecipeStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val timerSeconds: Int? = null
)

@Entity(tableName = "ingredients", indices = [Index("nameNormalized"), Index("category")])
data class IngredientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameEn: String = "",
    val category: String,
    val categoryTitle: String,
    val defaultUnit: String,
    val nameNormalized: String,
    val cupWeightGrams: Double? = null,
    val pieceWeightGrams: Double? = null,
    val referencePricePerKg: Double? = null
)

@Entity(
    tableName = "ingredient_aliases",
    foreignKeys = [ForeignKey(IngredientEntity::class, ["id"], ["ingredientId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["aliasNormalized"], unique = true), Index("ingredientId")]
)
data class IngredientAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ingredientId: String,
    val alias: String,
    val aliasNormalized: String
)

@Entity(
    tableName = "ingredient_substitutes",
    foreignKeys = [
        ForeignKey(IngredientEntity::class, ["id"], ["ingredientId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(IngredientEntity::class, ["id"], ["substituteIngredientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("ingredientId"), Index("recipeId")]
)
data class IngredientSubstituteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ingredientId: String,
    val substituteIngredientId: String,
    val recipeId: String? = null,
    val ratio: Double = 1.0,
    val note: String = "",
    val contextTag: String = ""
)

@Entity(
    tableName = "kitchen_inventory",
    foreignKeys = [ForeignKey(IngredientEntity::class, ["id"], ["ingredientId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("ingredientId"), Index("expiryDate"), Index("location")]
)
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ingredientId: String,
    val amount: Double,
    val unit: String,
    val location: String = "FRIDGE",
    val purchaseDate: Long? = null,
    val expiryDate: Long? = null,
    val note: String = "",
    val pricePaid: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory_transactions",
    indices = [Index("ingredientId"), Index("createdAt"), Index("groupId"), Index("inventoryId")]
)
data class InventoryTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: String,
    val inventoryId: Long,
    val ingredientId: String,
    val ingredientNameSnapshot: String,
    val deltaBaseAmount: Double,
    val dimension: String,
    val unitSnapshot: String,
    val reason: String,
    val referenceType: String = "",
    val referenceId: String? = null,
    val canUndo: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "meal_plan_items",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["planDate", "mealType"], unique = true), Index("recipeId")]
)
data class MealPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planDate: Long,
    val mealType: String,
    val recipeId: String? = null,
    val customTitle: String? = null,
    val servings: Int = 4,
    val isCompleted: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_lists", indices = [Index("status"), Index("createdAt")])
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(ShoppingListEntity::class, ["id"], ["shoppingListId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(IngredientEntity::class, ["id"], ["ingredientId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("shoppingListId"), Index("ingredientId"), Index("isChecked")]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shoppingListId: Long,
    val ingredientId: String? = null,
    val name: String,
    val category: String = "OTHER",
    val amount: Double,
    val unit: String,
    val estimatedPrice: Double? = null,
    val actualPrice: Double? = null,
    val isChecked: Boolean = false,
    val source: String = "MANUAL",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "favorite_recipes",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)]
)
data class FavoriteRecipeEntity(
    @PrimaryKey val recipeId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_recipes",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)]
)
data class UserRecipeEntity(
    @PrimaryKey val recipeId: String,
    val personalNote: String = "",
    val originalRecipeId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_preferences")
data class FoodPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val vegetarian: Boolean = false,
    val noMeat: Boolean = false,
    val lowCost: Boolean = false,
    val quickMeals: Boolean = false,
    val lowSalt: Boolean = false,
    val dairyFree: Boolean = false,
    val glutenFree: Boolean = false,
    val dislikedIngredients: String = "",
    val allergies: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cooking_timers",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("recipeId"), Index("endAt")]
)
data class CookingTimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String? = null,
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val endAt: Long,
    val isRunning: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_leftovers", indices = [Index("suggestedUseBy")])
data class FoodLeftoverEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val recipeId: String? = null,
    val preparedDate: Long,
    val portions: Int = 1,
    val suggestedUseBy: Long,
    val note: String = "",
    val isConsumed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ingredient_prices")
data class IngredientPriceEntity(
    @PrimaryKey val ingredientId: String,
    val pricePerKg: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recipe_categories")
data class RecipeCategoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val accent: String,
    val icon: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "cooking_tips")
data class CookingTipEntity(
    @PrimaryKey val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val content: String,
    val safety: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<RecipeIngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val steps: List<RecipeStepEntity>
) {
    fun orderedIngredients(): List<RecipeIngredientEntity> = ingredients.sortedBy { it.sortOrder }
    fun orderedSteps(): List<RecipeStepEntity> = steps.sortedBy { it.stepNumber }
}

data class InventoryWithIngredient(
    @Embedded val item: InventoryEntity,
    @Relation(parentColumn = "ingredientId", entityColumn = "id")
    val ingredient: IngredientEntity?
)

data class MealPlanWithRecipe(
    @Embedded val item: MealPlanItemEntity,
    @Relation(parentColumn = "recipeId", entityColumn = "id")
    val recipe: RecipeEntity?
)
