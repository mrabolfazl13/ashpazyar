package ir.ashpazyar.app.ui.screens.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.RecipeRepository
import ir.ashpazyar.app.domain.engine.RecipeFilter
import ir.ashpazyar.app.domain.engine.RecipeFilterState
import ir.ashpazyar.app.domain.model.Category
import ir.ashpazyar.app.domain.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipesUiState(
    val loading: Boolean = true,
    val recipes: List<Recipe> = emptyList(),
    val allCount: Int = 0,
    val categories: List<Category> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val query: String = "",
    val categoryId: String? = null,
    val onlyQuick: Boolean = false,
    val onlyVegetarian: Boolean = false,
    val onlyEconomical: Boolean = false,
    val onlyFamily: Boolean = false
)

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val categoryId = MutableStateFlow<String?>(null)
    private val onlyQuick = MutableStateFlow(false)
    private val onlyVegetarian = MutableStateFlow(false)
    private val onlyEconomical = MutableStateFlow(false)
    private val onlyFamily = MutableStateFlow(false)

    private data class Filters(
        val query: String,
        val categoryId: String?,
        val quick: Boolean,
        val vegetarian: Boolean,
        val economical: Boolean,
        val family: Boolean
    )

    private val filters = combine(query, categoryId, onlyQuick, onlyVegetarian) { q, c, quick, veg ->
        Filters(q, c, quick, veg, false, false)
    }.let { first ->
        combine(first, onlyEconomical, onlyFamily) { base, eco, family ->
            base.copy(economical = eco, family = family)
        }
    }

    val uiState: StateFlow<RecipesUiState> = combine(
        repository.recipes,
        repository.categories,
        repository.favorites,
        filters
    ) { recipes, categories, favorites, filter ->
        val result = RecipeFilter.apply(
            recipes,
            RecipeFilterState(
                query = filter.query,
                categoryId = filter.categoryId,
                onlyQuick = filter.quick,
                onlyVegetarian = filter.vegetarian,
                onlyEconomical = filter.economical,
                onlyFamily = filter.family
            )
        )
        RecipesUiState(
            loading = false,
            recipes = result,
            allCount = recipes.size,
            categories = categories,
            favorites = favorites,
            query = filter.query,
            categoryId = filter.categoryId,
            onlyQuick = filter.quick,
            onlyVegetarian = filter.vegetarian,
            onlyEconomical = filter.economical,
            onlyFamily = filter.family
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState())

    fun setQuery(value: String) { query.value = value }
    fun setCategory(id: String?) { categoryId.value = if (categoryId.value == id) null else id }
    fun toggleQuick() { onlyQuick.value = !onlyQuick.value }
    fun toggleVegetarian() { onlyVegetarian.value = !onlyVegetarian.value }
    fun toggleEconomical() { onlyEconomical.value = !onlyEconomical.value }
    fun toggleFamily() { onlyFamily.value = !onlyFamily.value }
    fun clearFilters() {
        query.value = ""
        categoryId.value = null
        onlyQuick.value = false
        onlyVegetarian.value = false
        onlyEconomical.value = false
        onlyFamily.value = false
    }
    fun toggleFavorite(id: String) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }
}
