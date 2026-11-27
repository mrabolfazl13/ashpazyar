package ir.ashpazyar.app.ui.screens.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.repository.ShoppingRepository
import ir.ashpazyar.app.domain.model.ShoppingItem
import ir.ashpazyar.app.domain.model.ShoppingList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingHistoryUiState(
    val loading: Boolean = true,
    val history: List<ShoppingList> = emptyList(),
    val selected: ShoppingList? = null,
    val items: List<ShoppingItem> = emptyList(),
    val loadingItems: Boolean = false
) {
    val selectedTotal: Double get() = items.sumOf { it.actualPrice ?: it.estimatedPrice ?: 0.0 }
}

@HiltViewModel
class ShoppingHistoryViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {
    private val selected = MutableStateFlow<ShoppingList?>(null)
    private val items = MutableStateFlow<List<ShoppingItem>>(emptyList())
    private val loadingItems = MutableStateFlow(false)

    private data class PairState(val selected: ShoppingList?, val items: List<ShoppingItem>, val loading: Boolean)

    val uiState: StateFlow<ShoppingHistoryUiState> = combine(
        repository.history,
        combine(selected, items, loadingItems) { s, i, l -> PairState(s, i, l) }
    ) { history, detail ->
        ShoppingHistoryUiState(
            loading = false,
            history = history,
            selected = detail.selected,
            items = detail.items,
            loadingItems = detail.loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingHistoryUiState())

    fun open(list: ShoppingList) {
        selected.value = list
        items.value = emptyList()
        loadingItems.value = true
        viewModelScope.launch {
            items.value = runCatching { repository.historyItems(list.id) }.getOrDefault(emptyList())
            loadingItems.value = false
        }
    }

    fun close() {
        selected.value = null
        items.value = emptyList()
    }

    fun shareText(): String = repository.shareText(uiState.value.items)
}

fun formatHistoryDate(epochMillis: Long): String =
    ir.ashpazyar.app.core.util.JalaliCalendar.format(epochMillis)
