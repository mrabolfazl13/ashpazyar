package ir.ashpazyar.app.ui.screens.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.repository.ShoppingRepository
import ir.ashpazyar.app.domain.model.ShoppingItem
import ir.ashpazyar.app.domain.model.ShoppingList
import ir.ashpazyar.app.domain.model.IngredientUnit
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ShoppingFilter { ALL, REMAINING, DONE }

data class ShoppingGroup(val category: String, val items: List<ShoppingItem>)

data class ShoppingUiState(
    val loading: Boolean = true,
    val list: ShoppingList? = null,
    val items: List<ShoppingItem> = emptyList(),
    val groups: List<ShoppingGroup> = emptyList(),
    val filter: ShoppingFilter = ShoppingFilter.ALL,
    val history: List<ShoppingList> = emptyList(),
    val checkedCount: Int = 0,
    val totalCount: Int = 0,
    val estimatedTotal: Double = 0.0,
    val actualTotal: Double = 0.0
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {
    private val filter = MutableStateFlow(ShoppingFilter.ALL)
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<ShoppingUiState> = combine(
        repository.bundle,
        filter,
        repository.history
    ) { bundle, filter, history ->
        val visible = bundle.items.filter {
            when (filter) {
                ShoppingFilter.ALL -> true
                ShoppingFilter.REMAINING -> !it.isChecked
                ShoppingFilter.DONE -> it.isChecked
            }
        }
        val groups = visible.groupBy { it.category.ifBlank { "سایر" } }
            .toSortedMap(compareBy<String> { categoryOrder(it) }.thenBy { it })
            .map { (category, items) -> ShoppingGroup(category, items.sortedBy { it.name }) }
        ShoppingUiState(
            loading = false,
            list = bundle.list,
            items = bundle.items,
            groups = groups,
            filter = filter,
            history = history,
            checkedCount = bundle.items.count { it.isChecked },
            totalCount = bundle.items.size,
            estimatedTotal = bundle.items.sumOf { it.linePrice ?: 0.0 },
            actualTotal = bundle.items.sumOf { it.actualPrice ?: 0.0 }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    fun setFilter(value: ShoppingFilter) { filter.value = value }

    fun toggle(item: ShoppingItem) {
        viewModelScope.launch { repository.toggleItem(item.id, !item.isChecked) }
    }

    fun delete(item: ShoppingItem) {
        viewModelScope.launch { repository.deleteItem(item.id) }
    }

    fun addManual(name: String, amountText: String, unit: IngredientUnit, category: String) {
        val amount = PersianText.toLatinDigits(amountText).replace("٫", ".").replace(",", "").trim().toDoubleOrNull()
        if (name.trim().length < 2 || amount == null || amount <= 0) {
            viewModelScope.launch { _events.emit("نام و مقدار معتبر وارد کنید") }
            return
        }
        viewModelScope.launch {
            runCatching { repository.addManualItem(name, amount, unit, category.ifBlank { "سایر" }) }
                .onSuccess { _events.emit("به فهرست خرید اضافه شد") }
                .onFailure { _events.emit(it.message ?: "افزودن ناموفق بود") }
        }
    }

    fun setPrice(item: ShoppingItem, text: String) {
        val value = PersianText.toLatinDigits(text).replace("٫", ".").replace(",", "").trim().toDoubleOrNull()
        viewModelScope.launch {
            runCatching { repository.setActualPrice(item.id, value?.takeIf { it >= 0.0 }) }
                .onFailure { _events.emit("ثبت قیمت ناموفق بود") }
        }
    }

    fun addFromThisWeek() {
        val start = JalaliCalendar.startOfWeek().toEpochDay()
        viewModelScope.launch {
            runCatching { repository.addFromMealPlan(start, start + 6) }
                .onSuccess { result ->
                    _events.emit(
                        if (result.items.isEmpty()) "برای وعده‌های این هفته چیز کم نیست"
                        else PersianText.faDigits(result.items.size.toString()) + " قلم به فهرست اضافه شد"
                    )
                }
                .onFailure { _events.emit(it.message ?: "ساخت فهرست از برنامه ناموفق بود") }
        }
    }

    fun archive() {
        viewModelScope.launch {
            runCatching { repository.archiveCurrent() }
                .onSuccess { _events.emit(if (it != null) "فهرست بایگانی شد" else "فهرستی برای بایگانی نیست") }
                .onFailure { _events.emit("بایگانی ناموفق بود") }
        }
    }

    fun shareText(): String = repository.shareText(uiState.value.items)

    private fun categoryOrder(category: String): Int = when (category) {
        "سبزیجات" -> 0
        "میوه" -> 1
        "گوشت و مرغ" -> 2
        "ماهی و آبزیان" -> 3
        "لبنیات" -> 4
        "حبوبات" -> 5
        "غلات" -> 6
        "ادویه" -> 7
        "روغن" -> 8
        "خشکبار" -> 9
        "نوشیدنی" -> 10
        else -> 20
    }
}
