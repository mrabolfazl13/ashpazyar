package ir.ashpazyar.app.domain.engine

import ir.ashpazyar.app.core.util.Dimension
import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CostEngine @Inject constructor() {
    fun calculate(
        recipe: Recipe,
        servings: Int,
        ingredientsById: Map<String, Ingredient>,
        prices: Map<String, Double>
    ): CostBreakdown {
        val factor = servings.toDouble() / recipe.servings.coerceAtLeast(1).toDouble()
        val lines = recipe.ingredients.mapNotNull { line ->
            val ingredient = ingredientsById[line.ingredientId] ?: return@mapNotNull null
            val pricePerKg = prices[ingredient.id] ?: ingredient.referencePricePerKg ?: return@mapNotNull null
            if (pricePerKg <= 0.0) return@mapNotNull null
            val base = UnitConverter.toBase(line.amount * factor, line.unit, ingredient.profile)
                ?: return@mapNotNull null
            val kilograms = when (base.dimension) {
                Dimension.MASS -> base.amount / 1000.0
                Dimension.VOLUME -> base.amount / 1000.0
                Dimension.COUNT -> {
                    val pieceWeight = ingredient.pieceWeightGrams ?: return@mapNotNull null
                    base.amount * pieceWeight / 1000.0
                }
            }
            CostLine(
                ingredientName = ingredient.name,
                amountLabel = UnitConverter.pretty(line.amount * factor, line.unit, ingredient.profile),
                cost = kilograms * pricePerKg
            )
        }.sortedByDescending { it.cost }
        val total = lines.sumOf { it.cost }
        return CostBreakdown(
            recipeTitle = recipe.title,
            servings = servings,
            lines = lines,
            total = total,
            perServing = if (servings > 0) total / servings else 0.0
        )
    }
}
