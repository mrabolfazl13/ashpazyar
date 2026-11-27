package ir.ashpazyar.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.di.NotificationEntryPoint
import ir.ashpazyar.app.domain.model.InventoryStatus
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.notifications.NotificationHelper
import kotlinx.coroutines.flow.first

class ExpiryCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val entry = EntryPointAccessors.fromApplication(appContext, NotificationEntryPoint::class.java)
        val settings = runCatching { entry.settingsRepository().settings.first() }.getOrNull()
            ?: ir.ashpazyar.app.domain.model.UserSettings()
        if (!settings.expiryNotifications) return Result.success()
        val inventory = entry.kitchenDao().getInventory().map { it.toDomain() }
        val expired = inventory.filter { it.status(settings.expirationWarningDays) == InventoryStatus.EXPIRED }
        val soon = inventory.filter { it.status(settings.expirationWarningDays) == InventoryStatus.NEAR_EXPIRY }
        if (expired.isNotEmpty() || soon.isNotEmpty()) {
            val names = (expired + soon).take(3).joinToString("، ") { it.displayName }
            NotificationHelper.showExpiry(appContext, expired.size, soon.size, names)
        }
        return Result.success()
    }
}
