package ir.ashpazyar.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationScheduler.scheduleExpiryCheck(context)
            NotificationScheduler.scheduleNextMealReminder(context)
        }
    }
}
