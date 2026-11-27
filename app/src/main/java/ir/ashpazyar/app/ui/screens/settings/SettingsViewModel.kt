package ir.ashpazyar.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.SettingsRepository
import ir.ashpazyar.app.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: UserSettings = UserSettings()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = repository.settings
        .map { SettingsUiState(false, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: String) {
        viewModelScope.launch {
            runCatching { repository.setThemeMode(mode) }
                .onFailure { _events.emit("تغییر پوسته ناموفق بود") }
        }
    }

    fun setFamilySize(value: Int) {
        viewModelScope.launch {
            runCatching { repository.setFamilySize(value) }
                .onFailure { _events.emit("ذخیره تعداد نفرات ناموفق بود") }
        }
    }

    fun setWarningDays(value: Int) {
        viewModelScope.launch {
            runCatching { repository.setWarningDays(value) }
                .onFailure { _events.emit("ذخیره هشدار انقضا ناموفق بود") }
        }
    }

    fun setBudget(text: String) {
        val value = ir.ashpazyar.app.core.util.PersianText.toLatinDigits(text)
            .replace(",", "").replace("٫", ".").trim().toLongOrNull() ?: 0L
        viewModelScope.launch {
            runCatching { repository.setWeeklyBudget(value) }
                .onSuccess { _events.emit("بودجه هفتگی ذخیره شد") }
                .onFailure { _events.emit("ذخیره بودجه ناموفق بود") }
        }
    }

    fun setMealReminders(value: Boolean) {
        viewModelScope.launch { repository.setMealReminders(value) }
    }

    fun setExpiryNotifications(value: Boolean) {
        viewModelScope.launch { repository.setExpiryNotifications(value) }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            repository.resetOnboarding()
            _events.emit("راهنمای شروع دوباره فعال شد")
        }
    }
}
