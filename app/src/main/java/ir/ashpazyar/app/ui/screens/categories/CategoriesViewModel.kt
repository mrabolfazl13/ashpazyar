package ir.ashpazyar.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.RecipeRepository
import ir.ashpazyar.app.domain.model.Category
import ir.ashpazyar.app.domain.model.Recipe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategoryItem(
    val category: Category,
    val recipeCount: Int,
    val sample: Recipe?
)

data class CategoriesUiState(
    val loading: Boolean = true,
    val categories: List<CategoryItem> = emptyList()
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    repository: RecipeRepository
) : ViewModel() {
    val uiState: StateFlow<CategoriesUiState> = combine(
        repository.categories,
        repository.recipes
    ) { categories, recipes ->
        CategoriesUiState(
            loading = false,
            categories = categories.map { category ->
                val matches = recipes.filter { category.id in it.categoryIds }
                CategoryItem(category, matches.size, matches.firstOrNull())
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())
}
