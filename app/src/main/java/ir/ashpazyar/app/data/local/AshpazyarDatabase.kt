package ir.ashpazyar.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.ashpazyar.app.data.local.dao.*
import ir.ashpazyar.app.data.local.entity.*

@Database(
    entities = [
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        RecipeStepEntity::class,
        IngredientEntity::class,
        IngredientAliasEntity::class,
        IngredientSubstituteEntity::class,
        InventoryEntity::class,
        InventoryTransactionEntity::class,
        MealPlanItemEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        FavoriteRecipeEntity::class,
        UserRecipeEntity::class,
        FoodPreferenceEntity::class,
        CookingTimerEntity::class,
        FoodLeftoverEntity::class,
        IngredientPriceEntity::class,
        RecipeCategoryEntity::class,
        CookingTipEntity::class,
        AppMetaEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AshpazyarDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun kitchenDao(): KitchenDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tipDao(): TipDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun timerDao(): TimerDao
    abstract fun leftoverDao(): LeftoverDao
    abstract fun userRecipeDao(): UserRecipeDao
    abstract fun metaDao(): MetaDao
}
