package ir.ashpazyar.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ir.ashpazyar.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_completed")
        val darkTheme = booleanPreferencesKey("dark_theme")
        val themeMode = stringPreferencesKey("theme_mode")
        val warningDays = intPreferencesKey("expiration_warning_days")
        val familySize = intPreferencesKey("family_size")
        val weeklyBudget = longPreferencesKey("weekly_budget")
        val mealReminders = booleanPreferencesKey("meal_reminders")
        val expiryNotifications = booleanPreferencesKey("expiry_notifications")
        val measurementSystem = stringPreferencesKey("measurement_system")
    }

    val settings: Flow<UserSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error }
        .map { prefs ->
            UserSettings(
                onboardingCompleted = prefs[Keys.onboarding] ?: false,
                darkTheme = prefs[Keys.darkTheme] ?: false,
                themeMode = prefs[Keys.themeMode] ?: "SYSTEM",
                expirationWarningDays = (prefs[Keys.warningDays] ?: 3).coerceIn(1, 30),
                familySize = (prefs[Keys.familySize] ?: 4).coerceIn(1, 20),
                weeklyBudget = prefs[Keys.weeklyBudget] ?: 0L,
                mealReminders = prefs[Keys.mealReminders] ?: true,
                expiryNotifications = prefs[Keys.expiryNotifications] ?: true,
                measurementSystem = prefs[Keys.measurementSystem] ?: "METRIC"
            )
        }

    suspend fun setOnboardingCompleted(value: Boolean) {
        dataStore.edit { it[Keys.onboarding] = value }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit {
            it[Keys.themeMode] = mode
            it[Keys.darkTheme] = mode == "DARK"
        }
    }

    suspend fun setDarkTheme(value: Boolean) {
        dataStore.edit {
            it[Keys.darkTheme] = value
            it[Keys.themeMode] = if (value) "DARK" else "LIGHT"
        }
    }

    suspend fun setWarningDays(days: Int) {
        dataStore.edit { it[Keys.warningDays] = days.coerceIn(1, 30) }
    }

    suspend fun setFamilySize(size: Int) {
        dataStore.edit { it[Keys.familySize] = size.coerceIn(1, 20) }
    }

    suspend fun setWeeklyBudget(value: Long) {
        dataStore.edit { it[Keys.weeklyBudget] = value.coerceAtLeast(0) }
    }

    suspend fun setMealReminders(value: Boolean) {
        dataStore.edit { it[Keys.mealReminders] = value }
    }

    suspend fun setExpiryNotifications(value: Boolean) {
        dataStore.edit { it[Keys.expiryNotifications] = value }
    }

    suspend fun resetOnboarding() {
        dataStore.edit { it[Keys.onboarding] = false }
    }
}
