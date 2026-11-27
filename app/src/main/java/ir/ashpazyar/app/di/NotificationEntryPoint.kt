package ir.ashpazyar.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.ashpazyar.app.data.local.dao.KitchenDao
import ir.ashpazyar.app.data.local.dao.MealPlanDao
import ir.ashpazyar.app.data.local.dao.TimerDao
import ir.ashpazyar.app.data.repository.SettingsRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationEntryPoint {
    fun kitchenDao(): KitchenDao
    fun mealPlanDao(): MealPlanDao
    fun timerDao(): TimerDao
    fun settingsRepository(): SettingsRepository
}
