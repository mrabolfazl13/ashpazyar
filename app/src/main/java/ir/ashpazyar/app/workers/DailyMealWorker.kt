package ir.ashpazyar.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.di.NotificationEntryPoint
import ir.ashpazyar.app.domain.model.MealType
import ir.ashpazyar.app.notifications.NotificationHelper
import ir.ashpazyar.app.notifications.NotificationScheduler
import kotlinx.coroutines.flow.first

class DailyMealWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val entry = EntryPointAccessors.fromApplication(appContext, NotificationEntryPoint::class.java)
        val settings = runCatching { entry.settingsRepository().settings.first() }.getOrNull()
            ?: return Result.success()
        if (settings.mealReminders) {
            val today = JalaliCalendar.todayEpochDay()
            val rows = entry.mealPlanDao().getRange(today, today)
            val dinner = rows.firstOrNull { it.item.mealType == MealType.DINNER.name }
            val lunch = rows.firstOrNull { it.item.mealType == MealType.LUNCH.name }
            val selected = dinner ?: lunch
            if (selected != null) {
                val recipe = selected.recipe
                val title = recipe?.title ?: selected.item.customTitle ?: "وعده امروز"
                val label = if (selected.item.mealType == MealType.DINNER.name) "شام" else "ناهار"
                NotificationHelper.showMeal(appContext, label, title)
            }
        }
        NotificationScheduler.scheduleNextMealReminder(appContext)
        return Result.success()
    }
}
