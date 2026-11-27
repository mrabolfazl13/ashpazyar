package ir.ashpazyar.app

import ir.ashpazyar.app.domain.engine.MealPlanGenerator
import ir.ashpazyar.app.domain.engine.RecipeFilter
import ir.ashpazyar.app.domain.engine.RecipeFilterState
import ir.ashpazyar.app.domain.model.BudgetLevel
import ir.ashpazyar.app.domain.model.MealType
import ir.ashpazyar.app.domain.model.PlanGenerationInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerAndFilterTest {
    private val generator = MealPlanGenerator(ir.ashpazyar.app.domain.engine.IngredientMatcher())
    private val recipes = (1..30).map { index ->
        Fixtures.recipe(
            id = "r-" + index,
            title = "دستور " + index,
            categories = if (index % 2 == 0) listOf("breakfast") else listOf("main"),
            vegetarian = index % 3 == 0,
            budget = if (index % 4 == 0) BudgetLevel.BUDGET else BudgetLevel.MID,
            order = index
        )
    }

    private fun input(
        breakfast: Boolean = true,
        lunch: Boolean = true,
        dinner: Boolean = true,
        vegetarian: Boolean = false,
        lowCost: Boolean = false,
        maxMinutes: Int? = null
    ) = PlanGenerationInput(
        startDate = 20000,
        familySize = 4,
        budget = 5000000,
        maxMinutes = maxMinutes,
        includeBreakfast = breakfast,
        includeLunch = lunch,
        includeDinner = dinner,
        variety = true,
        noMeat = vegetarian,
        vegetarian = vegetarian,
        lowCost = lowCost,
        pantryIngredientIds = setOf("rice"),
        dislikedIngredients = emptySet(),
        allergens = emptySet()
    )

    @Test fun threeMealsForSevenDays() {
        val proposal = generator.generate(
            recipes,
            input(),
            mapOf("rice" to Fixtures.ingredient("rice"))
        )
        assertEquals(21, proposal.entries.size)
        assertEquals(7, proposal.entries.map { it.date }.distinct().size)
        assertEquals(setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER), proposal.entries.map { it.mealType }.toSet())
    }

    @Test fun vegetarianOnlyCandidates() {
        val proposal = generator.generate(
            recipes,
            input(breakfast = false, vegetarian = true),
            mapOf("rice" to Fixtures.ingredient("rice"))
        )
        assertTrue(proposal.entries.isNotEmpty())
        assertTrue(proposal.entries.all { it.recipe.isVegetarian })
    }

    @Test fun maxMinutesFilterApplied() {
        val quick = listOf(Fixtures.recipe(id = "quick", prep = 5, cook = 10))
        val slow = listOf(Fixtures.recipe(id = "slow", prep = 40, cook = 60))
        val result = generator.generate(
            quick + slow,
            input(maxMinutes = 30),
            emptyMap()
        )
        assertTrue(result.entries.all { it.recipe.totalMinutes <= 30 })
    }

    @Test fun emptyCandidatePoolReturnsNote() {
        val result = generator.generate(
            emptyList(),
            input(),
            emptyMap()
        )
        assertTrue(result.entries.isEmpty())
        assertTrue(result.notes.isNotEmpty())
    }

    @Test fun budgetRecipesPreferredWhenLowCost() {
        val cheap = listOf(Fixtures.recipe(id = "cheap", budget = BudgetLevel.BUDGET))
        val result = generator.generate(cheap, input(lowCost = true), emptyMap())
        assertEquals("cheap", result.entries.first().recipe.id)
    }

    @Test fun deterministicForSameSeed() {
        val first = generator.generate(recipes, input(), emptyMap())
        val second = generator.generate(recipes, input(), emptyMap())
        assertEquals(first.entries.map { it.recipe.id }, second.entries.map { it.recipe.id })
    }

    @Test fun filterByQueryAndCategory() {
        val all = listOf(
            Fixtures.recipe(id = "a", title = "خورش قورمه سبزی", categories = listOf("stew")),
            Fixtures.recipe(id = "b", title = "سالاد فصل", categories = listOf("salad"))
        )
        val result = RecipeFilter.apply(all, RecipeFilterState(query = "قورمه", categoryId = "stew"))
        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test fun persianNormalizationMatchesZwnj() {
        val recipe = Fixtures.recipe(title = "گوجه‌فرنگی پلو")
        val result = RecipeFilter.apply(listOf(recipe), RecipeFilterState(query = "گوجه فرنگی"))
        assertEquals(1, result.size)
    }

    @Test fun arabicYehAndKafNormalized() {
        val recipe = Fixtures.recipe(title = "کیک شکلاتی")
        val result = RecipeFilter.apply(listOf(recipe), RecipeFilterState(query = "كيك"))
        assertEquals(1, result.size)
    }

    @Test fun quickFilter() {
        val quick = Fixtures.recipe(id = "q", prep = 5, cook = 10)
        val slow = Fixtures.recipe(id = "s", prep = 30, cook = 60)
        val result = RecipeFilter.apply(listOf(quick, slow), RecipeFilterState(onlyQuick = true))
        assertEquals(listOf("q"), result.map { it.id })
    }

    @Test fun availableFilterRequiresAllRequired() {
        val recipe = Fixtures.recipe(
            ingredients = listOf(
                Fixtures.line("rice", 100.0),
                Fixtures.line("milk", 100.0, optional = true)
            )
        )
        val result = RecipeFilter.apply(
            listOf(recipe),
            RecipeFilterState(onlyAvailable = true, availableIngredientIds = setOf("rice"))
        )
        assertEquals(1, result.size)
    }

    @Test fun searchOnIngredientName() {
        val recipe = Fixtures.recipe(
            title = "غذای خانگی",
            ingredients = listOf(Fixtures.line("saffron", 1.0, name = "زعفران"))
        )
        val result = RecipeFilter.apply(listOf(recipe), RecipeFilterState(query = "زعفران"))
        assertEquals(1, result.size)
    }
}
