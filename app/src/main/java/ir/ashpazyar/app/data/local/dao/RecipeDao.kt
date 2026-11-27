package ir.ashpazyar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ir.ashpazyar.app.data.local.entity.*

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY isUserCreated ASC, editorialOrder ASC, title ASC")
    fun observeAllWithDetails(): kotlinx.coroutines.flow.Flow<List<RecipeWithDetails>>

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY isUserCreated ASC, editorialOrder ASC, title ASC")
    suspend fun getAllWithDetails(): List<RecipeWithDetails>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getWithDetails(id: String): RecipeWithDetails?

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipe(id: String): RecipeEntity?

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(items: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(items: List<RecipeIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(items: List<RecipeStepEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(item: RecipeEntity)

    @Update
    suspend fun updateRecipe(item: RecipeEntity)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: String)

    @Query("DELETE FROM recipe_steps WHERE recipeId = :recipeId")
    suspend fun deleteSteps(recipeId: String)

    @Query("DELETE FROM recipes WHERE id = :id AND isUserCreated = 1")
    suspend fun deleteUserRecipe(id: String)

    @Query("SELECT COUNT(*) FROM recipes WHERE isUserCreated = 1")
    fun observeUserRecipeCount(): kotlinx.coroutines.flow.Flow<Int>

    @Transaction
    @Query("SELECT * FROM recipes WHERE isUserCreated = 1 ORDER BY updatedAt DESC")
    fun observeUserRecipes(): kotlinx.coroutines.flow.Flow<List<RecipeWithDetails>>

    @Query("DELETE FROM recipes WHERE isUserCreated = 1")
    suspend fun clearUserRecipes()
}
