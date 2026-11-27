package ir.ashpazyar.app.data.local.dao

import androidx.room.*
import ir.ashpazyar.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Transaction
    @Query("SELECT * FROM meal_plan_items WHERE planDate BETWEEN :startDay AND :endDay ORDER BY planDate ASC, CASE mealType WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 WHEN 'DINNER' THEN 2 ELSE 3 END")
    fun observeRange(startDay: Long, endDay: Long): Flow<List<MealPlanWithRecipe>>

    @Transaction
    @Query("SELECT * FROM meal_plan_items WHERE planDate BETWEEN :startDay AND :endDay ORDER BY planDate ASC")
    suspend fun getRange(startDay: Long, endDay: Long): List<MealPlanWithRecipe>

    @Query("SELECT * FROM meal_plan_items WHERE planDate = :day")
    suspend fun getDay(day: Long): List<MealPlanItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MealPlanItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MealPlanItemEntity>)

    @Query("DELETE FROM meal_plan_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM meal_plan_items WHERE planDate BETWEEN :startDay AND :endDay")
    suspend fun clearRange(startDay: Long, endDay: Long)

    @Query("UPDATE meal_plan_items SET planDate = :newDate, mealType = :newMeal WHERE id = :id")
    suspend fun moveSlot(id: Long, newDate: Long, newMeal: String)

    @Query("UPDATE meal_plan_items SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("SELECT * FROM meal_plan_items ORDER BY planDate ASC, mealType ASC")
    suspend fun getAll(): List<MealPlanItemEntity>

    @Query("DELETE FROM meal_plan_items")
    suspend fun clearAll()
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_lists ORDER BY CASE status WHEN 'ACTIVE' THEN 0 ELSE 1 END, createdAt DESC")
    fun observeLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE status = 'ACTIVE' ORDER BY createdAt DESC LIMIT 1")
    fun observeActiveList(): Flow<ShoppingListEntity?>

    @Query("SELECT * FROM shopping_lists WHERE status = 'ACTIVE' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveList(): ShoppingListEntity?

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    suspend fun getList(id: Long): ShoppingListEntity?

    @Insert
    suspend fun insertList(list: ShoppingListEntity): Long

    @Update
    suspend fun updateList(list: ShoppingListEntity)

    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :listId ORDER BY isChecked ASC, category ASC, createdAt DESC")
    fun observeItems(listId: Long): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :listId")
    suspend fun getItems(listId: Long): List<ShoppingListItemEntity>

    @Insert
    suspend fun insertItem(item: ShoppingListItemEntity): Long

    @Insert
    suspend fun insertItems(items: List<ShoppingListItemEntity>)

    @Update
    suspend fun updateItem(item: ShoppingListItemEntity)

    @Query("UPDATE shopping_list_items SET isChecked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM shopping_list_items WHERE shoppingListId = :listId")
    suspend fun clearItems(listId: Long)

    @Query("UPDATE shopping_lists SET status = 'ARCHIVED', updatedAt = :now WHERE id = :id")
    suspend fun archiveList(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM shopping_lists WHERE status = 'ARCHIVED' ORDER BY createdAt DESC")
    fun observeHistory(): Flow<List<ShoppingListEntity>>

    @Query("DELETE FROM shopping_list_items")
    suspend fun clearAllItems()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_recipes")
    fun observeAll(): Flow<List<FavoriteRecipeEntity>>

    @Query("SELECT recipeId FROM favorite_recipes")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT recipeId FROM favorite_recipes")
    suspend fun getIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(item: FavoriteRecipeEntity)

    @Query("DELETE FROM favorite_recipes WHERE recipeId = :recipeId")
    suspend fun remove(recipeId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_recipes WHERE recipeId = :recipeId)")
    suspend fun isFavorite(recipeId: String): Boolean

    @Query("DELETE FROM favorite_recipes")
    suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM recipe_categories ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<RecipeCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecipeCategoryEntity>)
}

@Dao
interface TipDao {
    @Query("SELECT * FROM cooking_tips ORDER BY sortOrder ASC, title ASC")
    fun observeAll(): Flow<List<CookingTipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CookingTipEntity>)
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM food_preferences WHERE id = 1")
    fun observe(): Flow<FoodPreferenceEntity?>

    @Query("SELECT * FROM food_preferences WHERE id = 1")
    suspend fun get(): FoodPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(item: FoodPreferenceEntity)
}

@Dao
interface TimerDao {
    @Query("SELECT * FROM cooking_timers WHERE isRunning = 1 ORDER BY endAt ASC")
    fun observeRunning(): Flow<List<CookingTimerEntity>>

    @Query("SELECT * FROM cooking_timers WHERE isRunning = 1 ORDER BY endAt ASC")
    suspend fun getRunning(): List<CookingTimerEntity>

    @Query("SELECT * FROM cooking_timers WHERE id = :id")
    suspend fun getById(id: Long): CookingTimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(timer: CookingTimerEntity): Long

    @Query("UPDATE cooking_timers SET isRunning = 0 WHERE id = :id")
    suspend fun stop(id: Long)

    @Query("DELETE FROM cooking_timers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM cooking_timers WHERE isRunning = 0")
    suspend fun clearFinished()
}

@Dao
interface LeftoverDao {
    @Query("SELECT * FROM food_leftovers ORDER BY isConsumed ASC, suggestedUseBy ASC")
    fun observeAll(): Flow<List<FoodLeftoverEntity>>

    @Query("SELECT * FROM food_leftovers WHERE isConsumed = 0 ORDER BY suggestedUseBy ASC")
    suspend fun getActive(): List<FoodLeftoverEntity>

    @Insert
    suspend fun insert(item: FoodLeftoverEntity): Long

    @Update
    suspend fun update(item: FoodLeftoverEntity)

    @Query("DELETE FROM food_leftovers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM food_leftovers")
    suspend fun getAll(): List<FoodLeftoverEntity>

    @Query("DELETE FROM food_leftovers")
    suspend fun clearAll()
}

@Dao
interface UserRecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: UserRecipeEntity)

    @Query("SELECT * FROM user_recipes WHERE recipeId = :recipeId")
    suspend fun get(recipeId: String): UserRecipeEntity?

    @Query("DELETE FROM user_recipes WHERE recipeId = :recipeId")
    suspend fun delete(recipeId: String)
}

@Dao
interface MetaDao {
    @Query("SELECT value FROM app_meta WHERE \"key\" = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(meta: AppMetaEntity)

    @Query("DELETE FROM app_meta")
    suspend fun clear()
}
