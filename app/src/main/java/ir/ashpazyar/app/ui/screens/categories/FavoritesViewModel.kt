package ir.ashpazyar.app.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.RecipeRepository
import ir.ashpazyar.app.domain.model.Recipe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val loading: Boolean = true,
    val recipes: List<Recipe> = emptyList()
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val uiState: StateFlow<FavoritesUiState> = combine(
        repository.recipes,
        repository.favorites
    ) { recipes, favorites ->
        FavoritesUiState(false, recipes.filter { it.id in favorites })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    fun removeFavorite(id: String) {
        viewModelScope.launch { repository.removeFavorite(id) }
    }
}
