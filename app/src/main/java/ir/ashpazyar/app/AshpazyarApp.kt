package ir.ashpazyar.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ir.ashpazyar.app.data.repository.TimerRepository
import ir.ashpazyar.app.notifications.NotificationHelper
import ir.ashpazyar.app.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AshpazyarApp : Application(), Configuration.Provider {

    @Inject
    lateinit var timerRepository: TimerRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        NotificationScheduler.scheduleExpiryCheck(this)
        NotificationScheduler.scheduleNextMealReminder(this)
        appScope.launch {
            runCatching { timerRepository.restoreAlarms() }
                .onFailure { Log.w("Ashpazyar", "Timer alarms could not be restored", it) }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
