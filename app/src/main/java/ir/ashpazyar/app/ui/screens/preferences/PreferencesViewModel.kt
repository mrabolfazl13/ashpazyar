package ir.ashpazyar.app.ui.screens.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.PreferencesRepository
import ir.ashpazyar.app.domain.model.FoodPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreferencesUiState(
    val loading: Boolean = true,
    val preferences: FoodPreferences = FoodPreferences()
)

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val repository: PreferencesRepository
) : ViewModel() {
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<PreferencesUiState> = repository.preferences
        .map { PreferencesUiState(false, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PreferencesUiState())

    private fun update(transform: (FoodPreferences) -> FoodPreferences, message: String = "") {
        val current = uiState.value.preferences
        viewModelScope.launch {
            runCatching { repository.save(transform(current)) }
                .onSuccess { if (message.isNotBlank()) _events.emit(message) }
                .onFailure { _events.emit(it.message ?: "ذخیره تنظیمات ناموفق بود") }
        }
    }

    fun setVegetarian(value: Boolean) = update({ it.copy(vegetarian = value, noMeat = if (value) true else it.noMeat) })
    fun setNoMeat(value: Boolean) = update({ it.copy(noMeat = value, vegetarian = if (!value) false else it.vegetarian) })
    fun setLowCost(value: Boolean) = update({ it.copy(lowCost = value) })
    fun setQuick(value: Boolean) = update({ it.copy(quickMeals = value) })
    fun setLowSalt(value: Boolean) = update({ it.copy(lowSalt = value) })
    fun setDairyFree(value: Boolean) = update({ it.copy(dairyFree = value) })
    fun setGlutenFree(value: Boolean) = update({ it.copy(glutenFree = value) })

    fun toggleAllergy(key: String) = update({ prefs ->
        if (key in prefs.allergies) prefs.copy(allergies = prefs.allergies - key)
        else prefs.copy(allergies = prefs.allergies + key)
    })

    fun addDislike(name: String) {
        val clean = name.trim()
        if (clean.length < 2) {
            viewModelScope.launch { _events.emit("نام ماده را کامل بنویسید") }
            return
        }
        if (clean in uiState.value.preferences.dislikedIngredients) return
        update({ it.copy(dislikedIngredients = it.dislikedIngredients + clean) }, "به مواد نامطلوب اضافه شد")
    }

    fun removeDislike(name: String) = update({
        it.copy(dislikedIngredients = it.dislikedIngredients - name)
    })
}
