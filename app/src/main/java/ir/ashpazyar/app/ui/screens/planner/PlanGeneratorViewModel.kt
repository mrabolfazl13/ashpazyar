package ir.ashpazyar.app.ui.screens.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.repository.*
import ir.ashpazyar.app.domain.model.PlanGenerationInput
import ir.ashpazyar.app.domain.model.PlanProposal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanGeneratorUiState(
    val weekStart: Long = JalaliCalendar.startOfWeek().toEpochDay(),
    val familySize: Int = 4,
    val budgetText: String = "",
    val maxMinutes: Int? = 45,
    val includeBreakfast: Boolean = false,
    val includeLunch: Boolean = true,
    val includeDinner: Boolean = true,
    val variety: Boolean = true,
    val noMeat: Boolean = false,
    val vegetarian: Boolean = false,
    val lowCost: Boolean = false,
    val generating: Boolean = false,
    val saving: Boolean = false,
    val proposal: PlanProposal? = null,
    val error: String? = null
)

@HiltViewModel
class PlanGeneratorViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val kitchenRepository: KitchenRepository,
    private val preferencesRepository: PreferencesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val state = MutableStateFlow(PlanGeneratorUiState())
    val uiState = state
    private val _saved = MutableSharedFlow<Boolean>()
    val saved = _saved.asSharedFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val prefs = preferencesRepository.get()
            state.value = state.value.copy(
                familySize = settings.familySize,
                budgetText = if (settings.weeklyBudget > 0) settings.weeklyBudget.toString() else "",
                vegetarian = prefs.vegetarian || prefs.noMeat,
                noMeat = prefs.noMeat,
                lowCost = prefs.lowCost
            )
        }
    }

    fun shiftWeek(delta: Int) {
        state.value = state.value.copy(
            weekStart = state.value.weekStart + delta * 7,
            proposal = null
        )
    }

    fun setFamilySize(value: Int) { state.value = state.value.copy(familySize = value.coerceIn(1, 20), proposal = null) }
    fun setBudget(value: String) { state.value = state.value.copy(budgetText = value.filter { it.isDigit() || it == '٫' }, proposal = null) }
    fun setMaxMinutes(value: Int?) { state.value = state.value.copy(maxMinutes = value, proposal = null) }
    fun toggleBreakfast() { state.value = state.value.copy(includeBreakfast = !state.value.includeBreakfast, proposal = null) }
    fun toggleLunch() {
        if (state.value.includeLunch && !state.value.includeDinner) return
        state.value = state.value.copy(includeLunch = !state.value.includeLunch, proposal = null)
    }
    fun toggleDinner() {
        if (state.value.includeDinner && !state.value.includeLunch) return
        state.value = state.value.copy(includeDinner = !state.value.includeDinner, proposal = null)
    }
    fun toggleVariety() { state.value = state.value.copy(variety = !state.value.variety, proposal = null) }
    fun toggleNoMeat() { state.value = state.value.copy(noMeat = !state.value.noMeat, proposal = null) }
    fun toggleVegetarian() { state.value = state.value.copy(vegetarian = !state.value.vegetarian, proposal = null) }
    fun toggleLowCost() { state.value = state.value.copy(lowCost = !state.value.lowCost, proposal = null) }

    fun generate() {
        val current = state.value
        state.value = current.copy(generating = true, error = null, proposal = null)
        viewModelScope.launch {
            runCatching {
                val prefs = preferencesRepository.get()
                val inventory = kitchenRepository.getAllInventory()
                val budget = PersianText.toLatinDigits(current.budgetText).replace("٫", ".").replace(",", "").toDoubleOrNull()?.toLong() ?: 0L
                mealPlanRepository.generatePlan(
                    PlanGenerationInput(
                        startDate = current.weekStart,
                        familySize = current.familySize,
                        budget = budget,
                        maxMinutes = current.maxMinutes,
                        includeBreakfast = current.includeBreakfast,
                        includeLunch = current.includeLunch,
                        includeDinner = current.includeDinner,
                        variety = current.variety,
                        noMeat = current.noMeat,
                        vegetarian = current.vegetarian,
                        lowCost = current.lowCost,
                        pantryIngredientIds = inventory.filter { it.amount > 0 }.map { it.ingredientId }.toSet(),
                        dislikedIngredients = prefs.dislikedIngredients.toSet(),
                        allergens = prefs.allergies.toSet()
                    )
                )
            }.onSuccess { proposal ->
                state.value = state.value.copy(
                    generating = false,
                    proposal = proposal,
                    error = if (proposal.entries.isEmpty()) proposal.notes.firstOrNull() else null
                )
            }.onFailure {
                state.value = state.value.copy(generating = false, error = it.message ?: "ساخت برنامه ناموفق بود")
            }
        }
    }

    fun save() {
        val proposal = state.value.proposal ?: return
        if (proposal.entries.isEmpty()) return
        state.value = state.value.copy(saving = true)
        viewModelScope.launch {
            runCatching { mealPlanRepository.saveProposal(state.value.weekStart, proposal) }
                .onSuccess {
                    state.value = state.value.copy(saving = false)
                    _saved.emit(true)
                }
                .onFailure {
                    state.value = state.value.copy(saving = false, error = it.message ?: "ذخیره برنامه ناموفق بود")
                }
        }
    }
}
