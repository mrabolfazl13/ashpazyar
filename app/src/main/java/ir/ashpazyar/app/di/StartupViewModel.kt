package ir.ashpazyar.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.SettingsRepository
import ir.ashpazyar.app.data.seed.SeedLoader
import ir.ashpazyar.app.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val seedLoader: SeedLoader,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _seeded = MutableStateFlow(false)
    val seeded: StateFlow<Boolean> = _seeded.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        seed()
    }

    fun seed() {
        viewModelScope.launch {
            _error.value = null
            runCatching { seedLoader.seedIfNeeded() }
                .onSuccess { _seeded.value = true }
                .onFailure {
                    _error.value = it.message ?: "آماده‌سازی داده‌های اولیه ناموفق بود"
                    _seeded.value = false
                }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }
}
