package ir.ashpazyar.app.domain.engine

import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.domain.model.Recipe
import ir.ashpazyar.app.domain.model.RecipeIngredient
import ir.ashpazyar.app.domain.model.Unit

data class ScaledIngredient(
    val source: RecipeIngredient,
    val amount: Double,
    val unit: Unit,
    val amountLabel: String
)

object RecipeScaler {
    fun scale(recipe: Recipe, targetServings: Int): List<ScaledIngredient> {
        val base = recipe.servings.coerceAtLeast(1)
        val target = targetServings.coerceAtLeast(1)
        val factor = target.toDouble() / base.toDouble()
        val profile = ir.ashpazyar.app.domain.model.IngredientProfile(
            id = "", name = "", defaultUnit = Unit.GRAM
        )
        return recipe.ingredients.map { ingredient ->
            val scaled = ingredient.amount * factor
            val rounded = roundSmart(scaled, ingredient.unit)
            ScaledIngredient(
                source = ingredient,
                amount = rounded,
                unit = ingredient.unit,
                amountLabel = UnitConverter.pretty(rounded, ingredient.unit, profile)
            )
        }
    }

    fun scaleAmount(recipe: Recipe, targetServings: Int, line: RecipeIngredient): Double {
        val base = recipe.servings.coerceAtLeast(1)
        return line.amount * (targetServings.coerceAtLeast(1).toDouble() / base.toDouble())
    }

    private fun roundSmart(value: Double, unit: Unit): Double {
        return when (unit) {
            Unit.PIECE, Unit.CLOVE, Unit.SLICE, Unit.CAN, Unit.BUNCH -> {
                if (value >= 4) Math.round(value).toDouble()
                else Math.round(value * 2.0) / 2.0
            }
            Unit.TEASPOON, Unit.TABLESPOON, Unit.PINCH -> Math.round(value * 4.0) / 4.0
            Unit.GRAM, Unit.MILLILITER -> if (value >= 100) Math.round(value / 5.0) * 5.0
            else Math.round(value * 10.0) / 10.0
            Unit.KILOGRAM, Unit.LITER -> Math.round(value * 100.0) / 100.0
            Unit.CUP -> Math.round(value * 4.0) / 4.0
        }
    }
}
