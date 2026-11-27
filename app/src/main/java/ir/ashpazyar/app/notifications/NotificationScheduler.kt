package ir.ashpazyar.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ir.ashpazyar.app.workers.DailyMealWorker
import ir.ashpazyar.app.workers.ExpiryCheckWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val EXPIRY_WORK = "ashpazyar_expiry_check"
    private const val MEAL_WORK = "ashpazyar_meal_reminder"

    fun scheduleExpiryCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EXPIRY_WORK, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    fun scheduleNextMealReminder(context: Context) {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var target = now.withHour(17).withMinute(30).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val delay = Duration.between(now, target).toMillis().coerceAtLeast(60_000)
        val request = OneTimeWorkRequestBuilder<DailyMealWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            MEAL_WORK, ExistingWorkPolicy.REPLACE, request
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(EXPIRY_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(MEAL_WORK)
    }
}
