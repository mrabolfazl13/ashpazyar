package ir.ashpazyar.app.data.repository

import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.CookingTimerEntity
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.domain.model.CookingTimer
import ir.ashpazyar.app.notifications.TimerAlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val database: AshpazyarDatabase,
    private val alarmScheduler: TimerAlarmScheduler
) {
    val runningTimers: Flow<List<CookingTimer>> = database.timerDao().observeRunning()
        .map { list -> list.map { it.toDomain() } }

    suspend fun startTimer(recipeId: String?, label: String, seconds: Int): Long {
        val safe = seconds.coerceIn(1, 24 * 60 * 60)
        val now = System.currentTimeMillis()
        val id = database.timerDao().upsert(
            CookingTimerEntity(
                recipeId = recipeId,
                label = label.ifBlank { "تایمر آشپزی" },
                totalSeconds = safe,
                remainingSeconds = safe,
                endAt = now + safe * 1000L,
                isRunning = true
            )
        )
        alarmScheduler.schedule(id, label, now + safe * 1000L, recipeId)
        return id
    }

    suspend fun stopTimer(id: Long) {
        database.timerDao().stop(id)
        alarmScheduler.cancel(id)
    }

    suspend fun deleteTimer(id: Long) {
        database.timerDao().delete(id)
        alarmScheduler.cancel(id)
    }

    suspend fun restoreAlarms() {
        database.timerDao().getRunning().forEach { timer ->
            alarmScheduler.schedule(timer.id, timer.label, timer.endAt, timer.recipeId)
        }
    }
}
