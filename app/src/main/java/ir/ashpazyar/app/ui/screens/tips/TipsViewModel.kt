package ir.ashpazyar.app.ui.screens.tips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.RecipeRepository
import ir.ashpazyar.app.domain.model.CookingTip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TipsUiState(
    val loading: Boolean = true,
    val tips: List<CookingTip> = emptyList(),
    val visible: List<CookingTip> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val query: String = ""
)

@HiltViewModel
class TipsViewModel @Inject constructor(
    repository: RecipeRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TipsUiState> = combine(repository.tips, query, category) { tips, q, cat ->
        val normalized = ir.ashpazyar.app.core.util.PersianText.normalize(q.trim())
        val visible = tips.filter { tip ->
            (cat == null || tip.category == cat) &&
                (normalized.isBlank() || ir.ashpazyar.app.core.util.PersianText.normalize(tip.title + " " + tip.summary + " " + tip.content).contains(normalized))
        }.sortedBy { it.title }
        TipsUiState(
            loading = false,
            tips = tips,
            visible = visible,
            categories = tips.map { it.category }.distinct().sorted(),
            selectedCategory = cat,
            query = q
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TipsUiState())

    fun setQuery(value: String) { query.value = value }
    fun setCategory(value: String?) { category.value = value }
}

data class TipDetailUiState(val loading: Boolean = true, val tip: CookingTip? = null)

@HiltViewModel
class TipDetailViewModel @Inject constructor(
    repository: RecipeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val tipId: String = savedStateHandle.get<String>("tipId").orEmpty()

    val uiState: StateFlow<TipDetailUiState> = repository.tips
        .combine(MutableStateFlow(tipId)) { tips, id ->
            TipDetailUiState(false, tips.firstOrNull { it.id == id })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TipDetailUiState())
}
