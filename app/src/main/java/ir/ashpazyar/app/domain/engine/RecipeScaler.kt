package ir.ashpazyar.app.domain.engine

import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.domain.model.Recipe
import ir.ashpazyar.app.domain.model.RecipeIngredient
import ir.ashpazyar.app.domain.model.IngredientUnit

data class ScaledIngredient(
    val source: RecipeIngredient,
    val amount: Double,
    val unit: IngredientUnit,
    val amountLabel: String
)

object RecipeScaler {
    fun scale(recipe: Recipe, targetServings: Int): List<ScaledIngredient> {
        val base = recipe.servings.coerceAtLeast(1)
        val target = targetServings.coerceAtLeast(1)
        val factor = target.toDouble() / base.toDouble()
        val profile = ir.ashpazyar.app.domain.model.IngredientProfile(
            id = "", name = "", defaultUnit = IngredientUnit.GRAM
        )
        return recipe.ingredients.map { ingredient ->
            val scaled = ingredient.amount * factor
            val rounded = roundSmart(scaled, ingredient.unit)
            ScaledIngredient(
                source = ingredient,
                amount = rounded,
                unit = ingredient.unit,
                amountLabel = if (rounded <= 0.0) "به میزان لازم" else UnitConverter.pretty(rounded, ingredient.unit, profile)
            )
        }
    }

    fun scaleAmount(recipe: Recipe, targetServings: Int, line: RecipeIngredient): Double {
        val base = recipe.servings.coerceAtLeast(1)
        return line.amount * (targetServings.coerceAtLeast(1).toDouble() / base.toDouble())
    }

    private fun roundSmart(value: Double, unit: IngredientUnit): Double {
        return when (unit) {
            IngredientUnit.PIECE, IngredientUnit.CLOVE, IngredientUnit.SLICE, IngredientUnit.CAN, IngredientUnit.BUNCH -> {
                if (value >= 4) Math.round(value).toDouble()
                else Math.round(value * 2.0) / 2.0
            }
            IngredientUnit.TEASPOON, IngredientUnit.TABLESPOON, IngredientUnit.PINCH -> Math.round(value * 4.0) / 4.0
            IngredientUnit.GRAM, IngredientUnit.MILLILITER -> if (value >= 100) Math.round(value / 5.0) * 5.0
            else Math.round(value * 10.0) / 10.0
            IngredientUnit.KILOGRAM, IngredientUnit.LITER -> Math.round(value * 100.0) / 100.0
            IngredientUnit.CUP -> Math.round(value * 4.0) / 4.0
        }
    }
}
