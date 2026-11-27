package ir.ashpazyar.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import ir.ashpazyar.app.di.NotificationEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TIMER_FINISHED) return
        val id = intent.getLongExtra("timer_id", -1L)
        val label = intent.getStringExtra("timer_label") ?: "تایمر آشپزی"
        if (id <= 0L) return
        NotificationHelper.showTimer(context, id, label)
        val pendingResult = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, NotificationEntryPoint::class.java)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { entryPoint.timerDao().stop(id) }
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_TIMER_FINISHED = "ir.ashpazyar.app.TIMER_FINISHED"
    }
}
