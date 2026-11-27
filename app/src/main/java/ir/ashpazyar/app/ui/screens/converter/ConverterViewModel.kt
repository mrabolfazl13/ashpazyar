package ir.ashpazyar.app.ui.screens.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.BaseQuantity
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.data.repository.IngredientRepository
import ir.ashpazyar.app.domain.model.Ingredient
import ir.ashpazyar.app.domain.model.Unit
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ConverterUiState(
    val loading: Boolean = true,
    val ingredients: List<Ingredient> = emptyList(),
    val selected: Ingredient? = null,
    val query: String = "",
    val amount: String = "1",
    val fromUnit: Unit = Unit.CUP,
    val toUnit: Unit = Unit.MILLILITER,
    val fromUnits: List<Unit> = Unit.entries,
    val toUnits: List<Unit> = Unit.entries,
    val result: String = "",
    val error: String? = null
)

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val ingredientRepository: IngredientRepository
) : ViewModel() {
    private val selectedId = MutableStateFlow<String?>(null)
    private val amount = MutableStateFlow("1")
    private val fromUnit = MutableStateFlow(Unit.CUP)
    private val toUnit = MutableStateFlow(Unit.MILLILITER)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<ConverterUiState> = combine(
        ingredientRepository.ingredients,
        combine(selectedId, amount, fromUnit, toUnit, query) { id, a, f, t, q ->
            Selection(id, a, f, t, q)
        }
    ) { ingredients, selection ->
        val selected = ingredients.firstOrNull { it.id == selection.selectedId }
        val profile = selected?.profile
        val allUnits = Unit.entries
        val fromUnits = allUnits.filter { UnitConverter.toBase(1.0, it, profile) != null }
        val fromBase = UnitConverter.toBase(1.0, selection.from, profile)
        val toUnits = if (fromBase == null) allUnits
        else allUnits.filter { unit ->
            UnitConverter.fromBase(fromBase, unit, profile) != null
        }
        val numeric = PersianText.toLatinDigits(selection.amount).replace("٫", ".").replace(",", "").trim().toDoubleOrNull()
        val converted = if (numeric == null || numeric < 0) null
        else UnitConverter.convert(numeric, selection.from, selection.to, profile)
        val result = if (converted != null) UnitConverter.pretty(converted, selection.to, profile) else ""
        val error = when {
            numeric == null && selection.amount.isNotBlank() -> "عدد معتبر وارد کنید"
            numeric != null && numeric < 0 -> "عدد نمی‌تواند منفی باشد"
            numeric != null && converted == null -> "این دو واحد با هم قابل تبدیل نیستند"
            else -> null
        }
        ConverterUiState(
            loading = false,
            ingredients = ingredients.sortedBy { it.name },
            selected = selected,
            query = selection.query,
            amount = selection.amount,
            fromUnit = selection.from,
            toUnit = selection.to,
            fromUnits = fromUnits,
            toUnits = toUnits,
            result = result,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConverterUiState())

    private data class Selection(val selectedId: String?, val amount: String, val from: Unit, val to: Unit, val query: String)

    fun setAmount(value: String) {
        amount.value = value.filter { it.isDigit() || it == '.' || it == '/' || it == '٫' || it == ',' || it in '۰'..'۹' }
    }

    fun setFrom(unit: Unit) {
        fromUnit.value = unit
        val profile = uiState.value.selected?.profile
        val base = UnitConverter.toBase(1.0, unit, profile) ?: return
        if (UnitConverter.fromBase(base, toUnit.value, profile) == null) {
            val candidate = Unit.entries.firstOrNull { UnitConverter.fromBase(base, it, profile) != null }
            if (candidate != null) toUnit.value = candidate
        }
    }

    fun setTo(unit: Unit) { toUnit.value = unit }
    fun setQuery(value: String) { query.value = value }

    fun selectIngredient(ingredient: Ingredient?) {
        selectedId.value = ingredient?.id
        val profile = ingredient?.profile
        val defaultFrom = if (profile?.cupWeightGrams != null) Unit.CUP else Unit.GRAM
        fromUnit.value = defaultFrom
        val base = UnitConverter.toBase(1.0, defaultFrom, profile)
        toUnit.value = Unit.entries.firstOrNull { it != defaultFrom && base != null && UnitConverter.fromBase(base, it, profile) != null }
            ?: Unit.KILOGRAM
    }
}
