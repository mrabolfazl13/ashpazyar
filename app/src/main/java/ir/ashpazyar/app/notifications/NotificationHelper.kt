package ir.ashpazyar.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ir.ashpazyar.app.MainActivity
import ir.ashpazyar.app.R

object NotificationHelper {
    const val CHANNEL_TIMERS = "cooking_timers"
    const val CHANNEL_EXPIRY = "expiry_alerts"
    const val CHANNEL_MEALS = "meal_reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMERS, "تایمرهای آشپزی", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "پایان تایمرهای پخت"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_EXPIRY, "هشدار انقضای مواد", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "یادآوری مواد نزدیک به انقضا"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MEALS, "یادآوری وعده‌ها", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "یادآوری برنامه غذایی روز"
            }
        )
    }

    fun showTimer(context: Context, id: Long, label: String) {
        notify(
            context, CHANNEL_TIMERS, TIMER_NOTIFICATION_BASE + (id % 1000).toInt(),
            "تایمر «" + label + "» تمام شد",
            "غذای شما آماده بررسی است. چاشیکا کنار شماست.",
            highPriority = true
        )
    }

    fun showExpiry(context: Context, expired: Int, soon: Int, names: String) {
        val title = when {
            expired > 0 -> expired.toString() + " ماده منقضی‌شده در آشپزخانه"
            soon > 0 -> soon.toString() + " ماده نزدیک به انقضا"
            else -> "وضعیت مواد آشپزخانه"
        }
        val text = if (names.isBlank()) "برای جلوگیری از هدررفت، موجودی آشپزخانه را بررسی کنید."
        else "بررسی کنید: " + names
        notify(context, CHANNEL_EXPIRY, EXPIRY_NOTIFICATION_ID, title, text)
    }

    fun showMeal(context: Context, label: String, title: String) {
        notify(
            context, CHANNEL_MEALS, MEAL_NOTIFICATION_ID,
            "برنامه امروز: " + label,
            title + " در برنامه غذایی شماست. زمان آماده‌سازی را فراموش نکنید."
        )
    }

    private fun notify(
        context: Context,
        channel: String,
        id: Int,
        title: String,
        text: String,
        highPriority: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private const val TIMER_NOTIFICATION_BASE = 12000
    private const val EXPIRY_NOTIFICATION_ID = 11001
    private const val MEAL_NOTIFICATION_ID = 11002
}
