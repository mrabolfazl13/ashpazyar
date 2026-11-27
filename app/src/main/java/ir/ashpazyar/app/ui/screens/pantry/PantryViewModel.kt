package ir.ashpazyar.app.ui.screens.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.data.repository.KitchenRepository
import ir.ashpazyar.app.data.repository.SettingsRepository
import ir.ashpazyar.app.domain.model.InventoryItem
import ir.ashpazyar.app.domain.model.InventoryStatus
import ir.ashpazyar.app.domain.model.StorageLocation
import ir.ashpazyar.app.domain.model.Unit
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PantryUiState(
    val loading: Boolean = true,
    val items: List<InventoryItem> = emptyList(),
    val filtered: List<InventoryItem> = emptyList(),
    val locations: List<StorageLocation> = emptyList(),
    val query: String = "",
    val selectedLocation: StorageLocation? = null,
    val freshCount: Int = 0,
    val riskCount: Int = 0,
    val totalValue: Double = 0.0,
    val warningDays: Int = 3
)

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val kitchenRepository: KitchenRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val location = MutableStateFlow<StorageLocation?>(null)
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private data class Base(val items: List<InventoryItem>, val warningDays: Int)

    val uiState: StateFlow<PantryUiState> = combine(
        kitchenRepository.inventory,
        settingsRepository.settings
    ) { items, settings -> Base(items, settings.expirationWarningDays) }
        .combine(query) { base, q -> base to q }
        .combine(location) { (base, q), loc -> Triple(base, q, loc) }
        .map { (base, q, loc) ->
            val tokens = PersianText.normalize(q).split(" ").filter { it.isNotBlank() }
            val filtered = base.items.filter { item ->
                (loc == null || item.location == loc) &&
                    (tokens.isEmpty() || tokens.all {
                        PersianText.normalize(item.displayName + " " + item.note + " " + item.location.label).contains(it)
                    })
            }.sortedWith(
                compareBy<InventoryItem> { statusRank(it.status(warningDays = base.warningDays)) }
                    .thenBy { it.displayName }
            )
            PantryUiState(
                loading = false,
                items = base.items,
                filtered = filtered,
                locations = base.items.map { it.location }.distinct().sortedBy { it.ordinal },
                query = q,
                selectedLocation = loc,
                freshCount = base.items.count { it.status(warningDays = base.warningDays) == InventoryStatus.FRESH },
                riskCount = base.items.count {
                    val status = it.status(warningDays = base.warningDays)
                    status == InventoryStatus.NEAR_EXPIRY || status == InventoryStatus.EXPIRED
                },
                totalValue = base.items.sumOf { it.pricePaid ?: 0.0 },
                warningDays = base.warningDays
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PantryUiState())

    fun setQuery(value: String) { query.value = value }
    fun setLocation(value: StorageLocation?) { location.value = value }

    fun updateAmount(item: InventoryItem, amountText: String, unit: Unit) {
        val amount = amountText.toPersianDoubleOrNull()
        if (amount == null || amount < 0) {
            emit("مقدار معتبر وارد کنید")
            return
        }
        if (UnitConverter.toBase(amount, unit, item.ingredient?.profile) == null) {
            emit("این واحد برای " + item.displayName + " قابل استفاده نیست")
            return
        }
        viewModelScope.launch {
            runCatching { kitchenRepository.updateInventory(item.copy(amount = amount, unit = unit)) }
                .onSuccess { emit("موجودی " + item.displayName + " به‌روز شد") }
                .onFailure { emit(it.message ?: "به‌روزرسانی ناموفق بود") }
        }
    }

    fun delete(item: InventoryItem) {
        viewModelScope.launch {
            runCatching { kitchenRepository.deleteInventory(item.id) }
                .onSuccess { emit(item.displayName + " از انبار حذف شد") }
                .onFailure { emit("حذف ناموفق بود") }
        }
    }

    private fun emit(message: String) { viewModelScope.launch { _events.emit(message) } }

    private fun statusRank(status: InventoryStatus): Int = when (status) {
        InventoryStatus.EXPIRED -> 0
        InventoryStatus.NEAR_EXPIRY -> 1
        InventoryStatus.FRESH -> 2
        InventoryStatus.NO_DATE -> 3
    }

    private fun String.toPersianDoubleOrNull(): Double? {
        val normalized = PersianText.toLatinDigits(this).replace("٫", ".").replace(",", "").trim()
        return normalized.toDoubleOrNull()
    }
}
