package ir.ashpazyar.app.domain.engine

import ir.ashpazyar.app.core.util.BaseQuantity
import ir.ashpazyar.app.core.util.Dimension
import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingEngine @Inject constructor() {

    fun buildMissingItems(
        entries: List<MealPlanEntry>,
        inventory: List<InventoryItem>,
        ingredientsById: Map<String, Ingredient>
    ): ShoppingBuildResult {
        val demand = mutableMapOf<String, MutableMap<Dimension, Double>>()
        val sources = mutableMapOf<String, MutableSet<String>>()
        val incompatible = mutableSetOf<String>()
        var covered = 0
        var totalAggregates = 0

        for (entry in entries) {
            val recipe = entry.recipe ?: continue
            val factor = entry.servings.toDouble() / recipe.servings.coerceAtLeast(1).toDouble()
            for (line in recipe.ingredients) {
                if (line.optional) continue
                val profile = ingredientsById[line.ingredientId]?.profile
                val base = UnitConverter.toBase(line.amount * factor, line.unit, profile) ?: continue
                val byDimension = demand.getOrPut(line.ingredientId) { mutableMapOf() }
                byDimension[base.dimension] = (byDimension[base.dimension] ?: 0.0) + base.amount
                sources.getOrPut(line.ingredientId) { mutableSetOf() }.add(recipe.id)
            }
        }

        val pantry = mutableMapOf<String, MutableMap<Dimension, Double>>()
        for (item in inventory) {
            if (item.amount <= 0.0) continue
            val base = UnitConverter.toBase(item.amount, item.unit, item.ingredient?.profile) ?: continue
            pantry.getOrPut(item.ingredientId) { mutableMapOf() }[base.dimension] =
                (pantry[item.ingredientId]?.get(base.dimension) ?: 0.0) + base.amount
        }

        val result = mutableListOf<ShoppingAggregate>()
        for ((ingredientId, dimensions) in demand) {
            val ingredient = ingredientsById[ingredientId]
            val profile = ingredient?.profile
            for ((dimension, needed) in dimensions) {
                totalAggregates += 1
                val have = pantry[ingredientId]?.get(dimension) ?: 0.0
                val deficit = needed - have
                if (deficit <= 0.0001) {
                    covered += 1
                    continue
                }
                if (needed > 0 && have > 0 && have < needed) covered += 0
                val quantity = BaseQuantity(deficit, dimension)
                val unit = UnitConverter.shoppingUnit(quantity, profile)
                result += ShoppingAggregate(
                    ingredientId = ingredientId,
                    name = ingredient?.name ?: ingredientId,
                    category = ingredient?.categoryTitle ?: "سایر",
                    amount = UnitConverter.fromBase(quantity, unit, profile) ?: deficit,
                    unit = unit,
                    sourceRecipeIds = sources[ingredientId].orEmpty()
                )
            }
        }

        return ShoppingBuildResult(
            items = result.sortedWith(compareBy({ it.category }, { it.name })),
            skippedIncompatible = incompatible.toList(),
            totalNeededCount = totalAggregates,
            alreadyCoveredCount = covered
        )
    }

    fun buildFromRecipe(
        recipe: Recipe,
        servings: Int,
        inventory: List<InventoryItem>,
        ingredientsById: Map<String, Ingredient>
    ): ShoppingBuildResult {
        val entry = MealPlanEntry(
            id = 0,
            date = 0,
            mealType = MealType.LUNCH,
            recipe = recipe,
            recipeId = recipe.id,
            customTitle = null,
            servings = servings,
            isCompleted = false,
            note = ""
        )
        return buildMissingItems(listOf(entry), inventory, ingredientsById)
    }
}
