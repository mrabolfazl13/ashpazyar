package ir.ashpazyar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.ashpazyar.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients ORDER BY categoryTitle ASC, name ASC")
    fun observeAll(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients ORDER BY categoryTitle ASC, name ASC")
    suspend fun getAll(): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getById(id: String): IngredientEntity?

    @Query("SELECT * FROM ingredients WHERE id = :id")
    fun observeById(id: String): Flow<IngredientEntity?>

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(items: List<IngredientAliasEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubstitutes(items: List<IngredientSubstituteEntity>)

    @Query("SELECT i.* FROM ingredients i INNER JOIN ingredient_aliases a ON i.id = a.ingredientId WHERE a.aliasNormalized = :normalized LIMIT 1")
    suspend fun findByAlias(normalized: String): IngredientEntity?

    @Query("SELECT * FROM ingredients WHERE nameNormalized LIKE '%' || :normalized || '%' ORDER BY name ASC LIMIT 20")
    suspend fun searchByName(normalized: String): List<IngredientEntity>

    @Query("SELECT * FROM ingredient_aliases")
    suspend fun getAllAliases(): List<IngredientAliasEntity>

    @Query("SELECT * FROM ingredient_substitutes")
    suspend fun getAllSubstitutes(): List<IngredientSubstituteEntity>

    @Query("DELETE FROM ingredient_substitutes WHERE recipeId IS NULL")
    suspend fun clearGlobalSubstitutes()

    @Query("SELECT * FROM ingredient_prices")
    fun observePrices(): Flow<List<IngredientPriceEntity>>

    @Query("SELECT * FROM ingredient_prices")
    suspend fun getPrices(): List<IngredientPriceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrice(price: IngredientPriceEntity)

    @Query("SELECT * FROM ingredient_prices WHERE ingredientId = :ingredientId")
    suspend fun getPrice(ingredientId: String): IngredientPriceEntity?
}
