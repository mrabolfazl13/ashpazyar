package ir.ashpazyar.app

import ir.ashpazyar.app.domain.engine.ShoppingEngine
import ir.ashpazyar.app.domain.model.IngredientUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingEngineTest {
    private val engine = ShoppingEngine()
    private val rice = Fixtures.ingredient("rice", "برنج")
    private val milk = Fixtures.ingredient("milk", "شیر", unit = IngredientUnit.MILLILITER)
    private val eggs = Fixtures.ingredient("eggs", "تخم‌مرغ", unit = IngredientUnit.PIECE, pieceWeight = 50.0)

    @Test fun twentyIngredientsProduceTwentyDeficits() {
        val ingredients = (1..20).map { Fixtures.ingredient("item-" + it, "ماده " + it) }
        val recipe = Fixtures.recipe(
            ingredients = ingredients.map { Fixtures.line(it.id, 100.0, name = it.name) }
        )
        val inventory = ingredients.map { Fixtures.inventory(it, 30.0) }
        val result = engine.buildFromRecipe(recipe, 4, inventory, ingredients.associateBy { it.id })
        assertEquals(20, result.items.size)
        assertEquals(20, result.totalNeededCount)
        assertEquals(0, result.alreadyCoveredCount)
        result.items.forEach { assertEquals(70.0, it.amount, 0.0001) }
    }

    @Test fun fullyCoveredIngredientsAreSkipped() {
        val ingredients = (1..20).map { Fixtures.ingredient("item-" + it, "ماده " + it) }
        val recipe = Fixtures.recipe(
            ingredients = ingredients.map { Fixtures.line(it.id, 100.0, name = it.name) }
        )
        val inventory = ingredients.map { Fixtures.inventory(it, 200.0) }
        val result = engine.buildFromRecipe(recipe, 4, inventory, ingredients.associateBy { it.id })
        assertTrue(result.items.isEmpty())
        assertEquals(20, result.alreadyCoveredCount)
    }

    @Test fun duplicatesAcrossMealsAreAggregated() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("rice", 100.0, name = "برنج")))
        val result = engine.buildMissingItems(
            listOf(Fixtures.entry(recipe, date = 1), Fixtures.entry(recipe, date = 2)),
            emptyList(),
            mapOf("rice" to rice)
        )
        assertEquals(1, result.items.size)
        assertEquals(200.0, result.items.first().amount, 0.0001)
        assertEquals(setOf("recipe-1"), result.items.first().sourceRecipeIds)
    }

    @Test fun servingsScaleDemand() {
        val recipe = Fixtures.recipe(
            servings = 4,
            ingredients = listOf(Fixtures.line("rice", 100.0, name = "برنج"))
        )
        val result = engine.buildFromRecipe(recipe, 8, emptyList(), mapOf("rice" to rice))
        assertEquals(200.0, result.items.first().amount, 0.0001)
    }

    @Test fun inventoryPartiallyCoversDemand() {
        val recipe = Fixtures.recipe(
            ingredients = listOf(
                Fixtures.line("rice", 100.0, name = "برنج"),
                Fixtures.line("milk", 200.0, IngredientUnit.MILLILITER, name = "شیر")
            )
        )
        val inventory = listOf(Fixtures.inventory(rice, 60.0), Fixtures.inventory(milk, 50.0, IngredientUnit.MILLILITER))
        val result = engine.buildFromRecipe(
            recipe,
            4,
            inventory,
            mapOf("rice" to rice, "milk" to milk)
        )
        assertEquals(2, result.items.size)
        assertEquals(40.0, result.items.first { it.ingredientId == "rice" }.amount, 0.0001)
        assertEquals(150.0, result.items.first { it.ingredientId == "milk" }.amount, 0.0001)
    }

    @Test fun optionalIngredientIsNotShopped() {
        val recipe = Fixtures.recipe(
            ingredients = listOf(
                Fixtures.line("rice", 100.0, name = "برنج"),
                Fixtures.line("eggs", 2.0, IngredientUnit.PIECE, name = "تخم‌مرغ", optional = true)
            )
        )
        val result = engine.buildFromRecipe(
            recipe,
            4,
            emptyList(),
            mapOf("rice" to rice, "eggs" to eggs)
        )
        assertEquals(listOf("rice"), result.items.map { it.ingredientId })
    }

    @Test fun noInventoryStillBuildsFullList() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("eggs", 3.0, IngredientUnit.PIECE, name = "تخم‌مرغ")))
        val result = engine.buildFromRecipe(recipe, 4, emptyList(), mapOf("eggs" to eggs))
        assertEquals(1, result.items.size)
        assertEquals(3.0, result.items.first().amount, 0.0001)
        assertEquals(IngredientUnit.PIECE, result.items.first().unit)
    }

    @Test fun sourceRecipesArePreserved() {
        val recipe = Fixtures.recipe(id = "abc", ingredients = listOf(Fixtures.line("rice", 10.0, name = "برنج")))
        val result = engine.buildFromRecipe(recipe, 4, emptyList(), mapOf("rice" to rice))
        assertEquals("abc", result.items.first().sourceRecipeIds.first())
    }

    @Test fun outputIsSortedByCategoryThenName() {
        val a = Fixtures.ingredient("a", "آب").copy(categoryTitle = "ب")
        val b = Fixtures.ingredient("b", "نان").copy(categoryTitle = "الف")
        val recipe = Fixtures.recipe(
            ingredients = listOf(Fixtures.line("a", 10.0, name = "آب"), Fixtures.line("b", 10.0, name = "نان"))
        )
        val result = engine.buildFromRecipe(recipe, 4, emptyList(), mapOf("a" to a, "b" to b))
        assertEquals(listOf("b", "a"), result.items.map { it.ingredientId })
    }

    @Test fun zeroAmountInventoryIgnored() {
        val recipe = Fixtures.recipe(ingredients = listOf(Fixtures.line("rice", 100.0, name = "برنج")))
        val inventory = listOf(Fixtures.inventory(rice, 0.0))
        val result = engine.buildFromRecipe(recipe, 4, inventory, mapOf("rice" to rice))
        assertEquals(100.0, result.items.first().amount, 0.0001)
    }
}
