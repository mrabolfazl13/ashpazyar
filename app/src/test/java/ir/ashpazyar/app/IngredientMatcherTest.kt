package ir.ashpazyar.app

import ir.ashpazyar.app.core.util.BaseQuantity
import ir.ashpazyar.app.core.util.Dimension
import ir.ashpazyar.app.domain.engine.IngredientMatcher
import ir.ashpazyar.app.domain.engine.RecipeScaler
import ir.ashpazyar.app.domain.engine.SubstituteRule
import ir.ashpazyar.app.domain.model.Unit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientMatcherTest {
    private val matcher = IngredientMatcher()
    private val rice = Fixtures.ingredient("rice", "برنج")
    private val sugar = Fixtures.ingredient("sugar", "شکر")
    private val brownSugar = Fixtures.ingredient("brown-sugar", "شکر قهوه‌ای")

    private fun pantry(amount: Double) = mapOf("rice" to listOf(BaseQuantity(amount, Dimension.MASS)))

    @Test fun thirtyPantryCoverageScenarios() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("rice", 100.0, name = "برنج")))
        val map = mapOf("rice" to rice)
        var checked = 0
        for (step in 0..29) {
            val grams = step * 5.0
            val result = matcher.match(recipe, pantry(grams), map, emptyList())
            val coverage = (grams / 100.0).coerceIn(0.0, 1.0)
            val expected = Math.round(coverage * 100.0).toInt()
            assertEquals("پوشش مرحله " + step, expected, result.matchPercent)
            assertEquals(100, result.totalRequired)
            assertEquals(grams >= 100.0, result.fullyAvailable)
            assertEquals(if (grams >= 100.0) 1 else 0, result.availableCount)
            checked++
        }
        assertEquals(30, checked)
    }

    @Test fun missingAmountIsRemaining() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("rice", 100.0, name = "برنج")))
        val result = matcher.match(recipe, pantry(40.0), mapOf("rice" to rice), emptyList())
        assertEquals(1, result.missingCount)
        assertEquals(40.0, result.missing.first().availableAmount, 0.0001)
        assertEquals(60.0, 100.0 - result.missing.first().availableAmount, 0.0001)
    }

    @Test fun emptyPantryMeansZero() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("rice", 100.0)))
        val result = matcher.match(recipe, emptyMap(), mapOf("rice" to rice), emptyList())
        assertEquals(0, result.matchPercent)
        assertFalse(result.fullyAvailable)
        assertEquals(1, result.missingCount)
    }

    @Test fun optionalIngredientIsIgnored() {
        val recipe = Fixtures.recipe(
            ingredients = listOf(
                Fixtures.line("rice", 100.0, name = "برنج"),
                Fixtures.line("sugar", 50.0, name = "شکر", optional = true)
            )
        )
        val result = matcher.match(recipe, pantry(100.0), mapOf("rice" to rice, "sugar" to sugar), emptyList())
        assertEquals(1, result.totalRequired)
        assertEquals(100, result.matchPercent)
        assertTrue(result.fullyAvailable)
    }

    @Test fun multipleInventoryLotsAreSummed() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("rice", 100.0, name = "برنج")))
        val lots = mapOf("rice" to listOf(BaseQuantity(30.0, Dimension.MASS), BaseQuantity(70.0, Dimension.MASS)))
        val result = matcher.match(recipe, lots, mapOf("rice" to rice), emptyList())
        assertEquals(100, result.matchPercent)
    }

    @Test fun substitutionFillsMissingIngredient() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("sugar", 100.0, name = "شکر")))
        val rules = listOf(
            SubstituteRule(
                ingredientId = "sugar",
                substituteIngredientId = "brown-sugar",
                ratio = 1.0,
                note = "شکر قهوه‌ای"
            )
        )
        val stock = mapOf("brown-sugar" to listOf(BaseQuantity(120.0, Dimension.MASS)))
        val result = matcher.match(recipe, stock, mapOf("sugar" to sugar, "brown-sugar" to brownSugar), rules)
        assertEquals(100, result.matchPercent)
        assertEquals(1, result.substitutesUsed.size)
        assertEquals("شکر قهوه‌ای", result.substitutesUsed.first().substituteName)
    }

    @Test fun insufficientSubstituteIsNotUsed() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("sugar", 100.0, name = "شکر")))
        val rules = listOf(SubstituteRule("sugar", "brown-sugar", ratio = 2.0))
        val stock = mapOf("brown-sugar" to listOf(BaseQuantity(100.0, Dimension.MASS)))
        val result = matcher.match(recipe, stock, mapOf("sugar" to sugar, "brown-sugar" to brownSugar), rules)
        assertTrue(result.substitutesUsed.isEmpty())
        assertFalse(result.fullyAvailable)
    }

    @Test fun recipeScalingAtDoubleServings() {
        val recipe = Fixtures.recipe(
            servings = 4,
            ingredients = listOf(Fixtures.line("rice", 200.0, name = "برنج"))
        )
        val scaled = RecipeScaler.scale(recipe, 8)
        assertEquals(400.0, scaled.first().amount, 0.0001)
    }

    @Test fun recipeScalingRoundsPieces() {
        val recipe = Fixtures.recipe(
            servings = 2,
            ingredients = listOf(Fixtures.line("eggs", 1.0, Unit.PIECE, name = "تخم‌مرغ"))
        )
        val scaled = RecipeScaler.scale(recipe, 3)
        assertEquals(1.5, scaled.first().amount, 0.0001)
    }

    @Test fun zeroServingsDoesNotCrash() {
        val recipe = Fixtures.recipe(servings = 0)
        assertEquals(1, RecipeScaler.scale(recipe, 0).size)
    }
}
