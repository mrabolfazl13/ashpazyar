package ir.ashpazyar.app.ui.screens.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.data.repository.IngredientRepository
import ir.ashpazyar.app.data.repository.KitchenRepository
import ir.ashpazyar.app.domain.model.Ingredient
import ir.ashpazyar.app.domain.model.StorageLocation
import ir.ashpazyar.app.domain.model.IngredientUnit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddPantryUiState(
    val query: String = "",
    val suggestions: List<Ingredient> = emptyList(),
    val selected: Ingredient? = null,
    val amount: String = "",
    val unit: IngredientUnit = IngredientUnit.GRAM,
    val location: StorageLocation = StorageLocation.PANTRY,
    val expiryDays: Int? = 14,
    val note: String = "",
    val price: String = "",
    val error: String? = null,
    val saving: Boolean = false
)

@HiltViewModel
class AddPantryViewModel @Inject constructor(
    private val ingredientRepository: IngredientRepository,
    private val kitchenRepository: KitchenRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selected = MutableStateFlow<Ingredient?>(null)
    private val amount = MutableStateFlow("")
    private val unit = MutableStateFlow(IngredientUnit.GRAM)
    private val location = MutableStateFlow(StorageLocation.PANTRY)
    private val expiryDays = MutableStateFlow<Int?>(14)
    private val note = MutableStateFlow("")
    private val price = MutableStateFlow("")
    private val error = MutableStateFlow<String?>(null)
    private val saving = MutableStateFlow(false)
    private val _saved = MutableSharedFlow<Boolean>()
    val saved = _saved.asSharedFlow()

    val uiState: StateFlow<AddPantryUiState> = combine(
        query,
        ingredientRepository.ingredients
    ) { rawQuery, ingredients ->
        val normalized = PersianText.normalize(rawQuery)
        val filtered = if (normalized.isBlank()) ingredients.sortedBy { it.name }.take(24)
        else ingredients.filter {
            PersianText.normalize(it.name).contains(normalized) ||
                it.nameEn.contains(rawQuery, ignoreCase = true)
        }.sortedBy {
            val index = PersianText.normalize(it.name).indexOf(normalized)
            if (index < 0) 500 else index
        }.take(30)
        filtered
    }.combine(selected) { list, ingredient -> list to ingredient }
        .combine(amount) { (list, ingredient), value -> Triple(list, ingredient, value) }
        .combine(unit) { (list, ingredient, value), u -> Quad(list, ingredient, value, u) }
        .combine(location) { data, loc -> data.copy(location = loc) }
        .combine(expiryDays) { data, days -> data.copy(expiryDays = days) }
        .combine(note) { data, n -> data.copy(note = n) }
        .combine(price) { data, p -> data.copy(price = p) }
        .combine(error) { data, e -> data.copy(error = e) }
        .combine(saving) { data, s -> data.copy(saving = s) }
        .combine(query) { data, q ->
            AddPantryUiState(
                query = q,
                suggestions = data.suggestions,
                selected = data.selected,
                amount = data.amount,
                unit = data.unit,
                location = data.location,
                expiryDays = data.expiryDays,
                note = data.note,
                price = data.price,
                error = data.error,
                saving = data.saving
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddPantryUiState())

    private data class Quad(
        val suggestions: List<Ingredient>,
        val selected: Ingredient?,
        val amount: String,
        val unit: IngredientUnit,
        val location: StorageLocation = StorageLocation.PANTRY,
        val expiryDays: Int? = 14,
        val note: String = "",
        val price: String = "",
        val error: String? = null,
        val saving: Boolean = false
    )

    fun setQuery(value: String) {
        query.value = value
        if (selected.value != null && value != selected.value?.name) selected.value = null
    }

    fun select(ingredient: Ingredient) {
        selected.value = ingredient
        unit.value = ingredient.defaultUnit
        error.value = null
        query.value = ingredient.name
    }

    fun clearSelection() {
        selected.value = null
        query.value = ""
        amount.value = ""
        unit.value = IngredientUnit.GRAM
        error.value = null
    }

    fun setAmount(value: String) { amount.value = value; error.value = null }
    fun setUnit(value: IngredientUnit) { unit.value = value }
    fun setLocation(value: StorageLocation) { location.value = value }
    fun setExpiryDays(value: Int?) { expiryDays.value = value }
    fun setNote(value: String) { note.value = value }
    fun setPrice(value: String) { price.value = value }

    fun save() {
        val ingredient = selected.value ?: run {
            error.value = "یک ماده اولیه انتخاب کنید"
            return
        }
        val parsedAmount = PersianText.toLatinDigits(amount.value).replace("٫", ".").replace(",", "").trim().toDoubleOrNull()
        if (parsedAmount == null || parsedAmount <= 0.0) {
            error.value = "مقدار باید عددی بزرگ‌تر از صفر باشد"
            return
        }
        if (UnitConverter.toBase(parsedAmount, unit.value, ingredient.profile) == null) {
            error.value = "این واحد برای " + ingredient.name + " قابل استفاده نیست"
            return
        }
        val parsedPrice = PersianText.toLatinDigits(price.value).replace("٫", ".").replace(",", "").trim().toDoubleOrNull()
        saving.value = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            runCatching {
                kitchenRepository.addInventory(
                    ingredientId = ingredient.id,
                    amount = parsedAmount,
                    unit = unit.value,
                    location = location.value,
                    purchaseDate = now,
                    expiryDate = expiryDays.value?.let { now + it * 86_400_000L },
                    note = note.value,
                    pricePaid = parsedPrice?.takeIf { it > 0 }
                )
            }.onSuccess {
                _saved.emit(true)
            }.onFailure {
                error.value = it.message ?: "افزودن به انبار ناموفق بود"
            }
            saving.value = false
        }
    }
}
