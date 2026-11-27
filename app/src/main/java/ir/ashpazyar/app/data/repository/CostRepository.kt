package ir.ashpazyar.app.data.repository

import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.IngredientPriceEntity
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.domain.engine.CostEngine
import ir.ashpazyar.app.domain.model.CostBreakdown
import ir.ashpazyar.app.domain.model.Ingredient
import ir.ashpazyar.app.domain.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class PriceEntry(val ingredient: Ingredient, val pricePerKg: Double?)

@Singleton
class CostRepository @Inject constructor(
    private val database: AshpazyarDatabase,
    private val costEngine: CostEngine
) {
    val prices: Flow<List<PriceEntry>> = combine(
        database.ingredientDao().observeAll(),
        database.ingredientDao().observePrices()
    ) { ingredients, prices ->
        val map = prices.associateBy { it.ingredientId }
        ingredients.map { PriceEntry(it.toDomain(), map[it.id]?.pricePerKg) }
    }

    suspend fun setPrice(ingredientId: String, pricePerKg: Double?) {
        database.ingredientDao().upsertPrice(IngredientPriceEntity(ingredientId, pricePerKg, System.currentTimeMillis()))
    }

    suspend fun getPrices(): Map<String, Double> {
        return database.ingredientDao().getPrices()
            .mapNotNull { price -> price.pricePerKg?.let { price.ingredientId to it } }
            .toMap()
    }

    suspend fun calculate(recipe: Recipe, servings: Int, ingredients: Map<String, Ingredient>): CostBreakdown {
        return costEngine.calculate(recipe, servings, ingredients, getPrices())
    }
}
