package ir.ashpazyar.app.data.repository

import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.FoodPreferenceEntity
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.data.toEntity
import ir.ashpazyar.app.domain.model.FoodPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val database: AshpazyarDatabase
) {
    val preferences: Flow<FoodPreferences> = database.preferenceDao().observe().map { it?.toDomain() ?: FoodPreferences() }

    suspend fun save(preferences: FoodPreferences) {
        database.preferenceDao().save(preferences.toEntity())
    }

    suspend fun get(): FoodPreferences = database.preferenceDao().get()?.toDomain() ?: FoodPreferences()
}
