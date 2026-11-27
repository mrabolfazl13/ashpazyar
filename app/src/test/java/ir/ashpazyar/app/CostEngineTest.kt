package ir.ashpazyar.app

import ir.ashpazyar.app.domain.engine.CostEngine
import ir.ashpazyar.app.domain.model.Unit
import org.junit.Assert.assertEquals
import org.junit.Test

class CostEngineTest {
    private val engine = CostEngine()
    private val rice = Fixtures.ingredient("rice", "برنج", price = 100000.0)

    @Test fun massCost() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("rice", 200.0)))
        val result = engine.calculate(recipe, 4, mapOf("rice" to rice), emptyMap())
        assertEquals(20000.0, result.total, 0.001)
        assertEquals(5000.0, result.perServing, 0.001)
    }

    @Test fun priceOverrideWins() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("rice", 200.0)))
        val result = engine.calculate(recipe, 4, mapOf("rice" to rice), mapOf("rice" to 200000.0))
        assertEquals(40000.0, result.total, 0.001)
    }

    @Test fun servingsScaleCost() {
        val recipe = Fixtures.recipe(servings = 4, ingredients = listOf(Fixtures.line("rice", 200.0)))
        val result = engine.calculate(recipe, 8, mapOf("rice" to rice), emptyMap())
        assertEquals(40000.0, result.total, 0.001)
    }

    @Test fun pieceCostUsesPieceWeight() {
        val egg = Fixtures.ingredient("eggs", "تخم‌مرغ", unit = Unit.PIECE, pieceWeight = 50.0, price = 200000.0)
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("eggs", 2.0, Unit.PIECE)))
        val result = engine.calculate(recipe, 4, mapOf("eggs" to egg), emptyMap())
        assertEquals(20000.0, result.total, 0.001)
    }

    @Test fun unknownIngredientSkipped() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("unknown", 100.0)))
        val result = engine.calculate(recipe, 4, mapOf("rice" to rice), emptyMap())
        assertEquals(0.0, result.total, 0.001)
    }

    @Test fun zeroPriceSkipped() {
        val free = rice.copy(referencePricePerKg = 0.0)
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("rice", 200.0)))
        val result = engine.calculate(recipe, 4, mapOf("rice" to free), emptyMap())
        assertEquals(0.0, result.total, 0.001)
    }
}
