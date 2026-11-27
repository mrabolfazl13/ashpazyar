package ir.ashpazyar.app.ui.screens.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.data.repository.MealPlanRepository
import ir.ashpazyar.app.data.repository.RecipeRepository
import ir.ashpazyar.app.data.repository.SettingsRepository
import ir.ashpazyar.app.domain.model.MealPlanEntry
import ir.ashpazyar.app.domain.model.MealType
import ir.ashpazyar.app.domain.model.Recipe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlannerUiState(
    val loading: Boolean = true,
    val weekStart: Long = JalaliCalendar.startOfWeek().toEpochDay(),
    val selectedDay: Long = JalaliCalendar.todayEpochDay(),
    val entries: List<MealPlanEntry> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val familySize: Int = 4,
    val plannedCount: Int = 0,
    val completedCount: Int = 0
) {
    fun entryFor(date: Long, meal: MealType): MealPlanEntry? = entries.firstOrNull { it.date == date && it.mealType == meal }
    fun dayEntries(date: Long): List<MealPlanEntry> = entries.filter { it.date == date }.sortedBy { it.mealType.ordinal }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val repository: MealPlanRepository,
    recipeRepository: RecipeRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val weekStart = MutableStateFlow(JalaliCalendar.startOfWeek().toEpochDay())
    private val selectedDay = MutableStateFlow(JalaliCalendar.todayEpochDay())
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()
    private val familySize = settingsRepository.settings.map { it.familySize }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 4)

    private val entries: Flow<List<MealPlanEntry>> = weekStart.flatMapLatest { start ->
        repository.observeRange(start, start + 6)
    }

    val uiState: StateFlow<PlannerUiState> = combine(
        weekStart,
        selectedDay,
        entries,
        recipeRepository.recipes
    ) { start, day, list, recipes ->
        PlannerUiState(
            loading = false,
            weekStart = start,
            selectedDay = day,
            entries = list,
            recipes = recipes.sortedBy { it.editorialOrder },
            familySize = 4,
            plannedCount = list.size,
            completedCount = list.count { it.isCompleted }
        )
    }.combine(familySize) { state, size ->
        state.copy(familySize = size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannerUiState())

    fun previousWeek() {
        weekStart.value -= 7
        selectedDay.value = weekStart.value
    }

    fun nextWeek() {
        weekStart.value += 7
        selectedDay.value = weekStart.value
    }

    fun selectDay(day: Long) { selectedDay.value = day }

    fun assign(day: Long, mealType: MealType, recipe: Recipe) {
        viewModelScope.launch {
            runCatching { repository.setMeal(day, mealType, recipe, familySize.value) }
                .onSuccess { _events.emit("به برنامه اضافه شد") }
                .onFailure { _events.emit("افزودن به برنامه ناموفق بود") }
        }
    }

    fun assignCustom(day: Long, mealType: MealType, title: String) {
        if (title.trim().length < 2) {
            viewModelScope.launch { _events.emit("نام وعده را کامل بنویسید") }
            return
        }
        viewModelScope.launch {
            runCatching { repository.setCustomMeal(day, mealType, title, familySize.value) }
                .onSuccess { _events.emit("وعده دلخواه ثبت شد") }
                .onFailure { _events.emit("ثبت وعده ناموفق بود") }
        }
    }

    fun remove(entry: MealPlanEntry) {
        viewModelScope.launch {
            runCatching { repository.remove(entry.id) }
                .onSuccess { _events.emit("از برنامه حذف شد") }
                .onFailure { _events.emit("حذف ناموفق بود") }
        }
    }

    fun toggleCompleted(entry: MealPlanEntry) {
        viewModelScope.launch { repository.setCompleted(entry.id, !entry.isCompleted) }
    }

    fun clearWeek() {
        viewModelScope.launch {
            runCatching { repository.clearWeek(weekStart.value) }
                .onSuccess { _events.emit("برنامه این هفته پاک شد") }
                .onFailure { _events.emit("پاک کردن ناموفق بود") }
        }
    }
}
