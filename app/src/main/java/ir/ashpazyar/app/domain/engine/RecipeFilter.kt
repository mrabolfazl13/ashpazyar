package ir.ashpazyar.app.domain.engine

import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.*

data class RecipeFilterState(
    val query: String = "",
    val categoryId: String? = null,
    val maxMinutes: Int? = null,
    val difficulty: Difficulty? = null,
    val budgetLevel: BudgetLevel? = null,
    val onlyVegetarian: Boolean = false,
    val onlyQuick: Boolean = false,
    val onlyEconomical: Boolean = false,
    val onlyFamily: Boolean = false,
    val onlyAvailable: Boolean = false,
    val availableIngredientIds: Set<String> = emptySet()
)

object RecipeFilter {
    fun apply(recipes: List<Recipe>, state: RecipeFilterState): List<Recipe> {
        val tokens = PersianText.normalize(state.query).split(" ").filter { it.isNotBlank() }
        return recipes.filter { recipe ->
            if (state.categoryId != null && state.categoryId !in recipe.categoryIds) return@filter false
            if (state.maxMinutes != null && recipe.totalMinutes > state.maxMinutes) return@filter false
            if (state.difficulty != null && recipe.difficulty != state.difficulty) return@filter false
            if (state.budgetLevel != null && recipe.budgetLevel != state.budgetLevel) return@filter false
            if (state.onlyVegetarian && !recipe.isVegetarian) return@filter false
            if (state.onlyQuick && !recipe.isQuick) return@filter false
            if (state.onlyEconomical && !recipe.isEconomical) return@filter false
            if (state.onlyFamily && !recipe.isFamilyFriendly) return@filter false
            if (state.onlyAvailable && state.availableIngredientIds.isNotEmpty()) {
                val required = recipe.ingredients.filter { !it.optional }
                if (required.any { it.ingredientId !in state.availableIngredientIds }) return@filter false
            }
            if (tokens.isNotEmpty()) {
                val haystack = PersianText.normalize(
                    recipe.title + " " + (recipe.titleEn ?: "") + " " + recipe.description + " " +
                        recipe.tags.joinToString(" ") + " " + recipe.categoryIds.joinToString(" ") + " " +
                        recipe.ingredients.joinToString(" ") { it.ingredientName }
                )
                if (!tokens.all { haystack.contains(it) }) return@filter false
            }
            true
        }.sortedWith(compareBy<Recipe> { it.editorialOrder }.thenBy { it.title })
    }
}
