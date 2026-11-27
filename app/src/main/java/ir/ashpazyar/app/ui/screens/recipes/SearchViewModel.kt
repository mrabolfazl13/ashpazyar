package ir.ashpazyar.app.ui.screens.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.RecipeRepository
import ir.ashpazyar.app.domain.engine.RecipeFilter
import ir.ashpazyar.app.domain.engine.RecipeFilterState
import ir.ashpazyar.app.domain.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Recipe> = emptyList(),
    val suggestions: List<Recipe> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val quickOnly: Boolean = false,
    val vegetarianOnly: Boolean = false,
    val totalSearched: Int = 0
)

private data class SearchFlags(val query: String, val quick: Boolean, val vegetarian: Boolean)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val quick = MutableStateFlow(false)
    private val vegetarian = MutableStateFlow(false)

    private val flags = combine(query, quick, vegetarian) { q, quick, veg -> SearchFlags(q, quick, veg) }

    val uiState: StateFlow<SearchUiState> = combine(
        repository.recipes,
        repository.favorites,
        flags
    ) { recipes, favorites, f ->
        val filtered = RecipeFilter.apply(
            recipes,
            RecipeFilterState(query = f.query, onlyQuick = f.quick, onlyVegetarian = f.vegetarian)
        )
        SearchUiState(
            query = f.query,
            results = if (f.query.isBlank() && !f.quick && !f.vegetarian) emptyList() else filtered,
            suggestions = if (f.query.isBlank()) {
                recipes.sortedWith(compareByDescending<Recipe> { it.isFamilyFriendly }.thenBy { it.editorialOrder }).take(8)
            } else emptyList(),
            favorites = favorites,
            quickOnly = f.quick,
            vegetarianOnly = f.vegetarian,
            totalSearched = recipes.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(value: String) { query.value = value }
    fun toggleQuick() { quick.value = !quick.value }
    fun toggleVegetarian() { vegetarian.value = !vegetarian.value }
    fun toggleFavorite(id: String) { viewModelScope.launch { repository.toggleFavorite(id) } }
}
