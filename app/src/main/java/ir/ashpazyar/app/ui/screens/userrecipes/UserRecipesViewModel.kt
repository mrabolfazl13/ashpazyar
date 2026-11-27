package ir.ashpazyar.app.ui.screens.userrecipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.RecipeRepository
import ir.ashpazyar.app.domain.model.Recipe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserRecipesUiState(
    val loading: Boolean = true,
    val recipes: List<Recipe> = emptyList(),
    val favorites: Set<String> = emptySet()
)

@HiltViewModel
class UserRecipesViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    val uiState: StateFlow<UserRecipesUiState> = combine(
        repository.userRecipes,
        repository.favorites
    ) { recipes, favorites ->
        UserRecipesUiState(false, recipes, favorites)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserRecipesUiState())

    fun toggleFavorite(id: String) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteUserRecipe(id) }
                .onSuccess { _messages.emit("دستور حذف شد") }
                .onFailure { _messages.emit(it.message ?: "حذف دستور ناموفق بود") }
        }
    }
}
