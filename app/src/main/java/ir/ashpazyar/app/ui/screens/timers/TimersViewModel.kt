package ir.ashpazyar.app.ui.screens.timers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.TimerRepository
import ir.ashpazyar.app.domain.model.CookingTimer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerDisplay(val timer: CookingTimer, val remainingSeconds: Long, val progress: Float)

data class TimersUiState(
    val loading: Boolean = true,
    val timers: List<TimerDisplay> = emptyList()
)

@HiltViewModel
class TimersViewModel @Inject constructor(
    private val repository: TimerRepository
) : ViewModel() {
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    val uiState: StateFlow<TimersUiState> = combine(
        repository.runningTimers,
        ticker
    ) { timers, now ->
        TimersUiState(
            loading = false,
            timers = timers.map { timer ->
                val remaining = ((timer.endAt - now) / 1000L).coerceAtLeast(0)
                val total = timer.totalSeconds.coerceAtLeast(1)
                TimerDisplay(timer, remaining, (remaining.toFloat() / total).coerceIn(0f, 1f))
            }.sortedBy { it.remainingSeconds }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimersUiState())

    fun start(minutes: Int) {
        if (minutes <= 0) return
        viewModelScope.launch {
            runCatching { repository.startTimer(null, "تایمر " + minutes + " دقیقه", minutes * 60) }
                .onSuccess { _events.emit("تایمر " + minutes + " دقیقه‌ای شروع شد") }
                .onFailure { _events.emit("شروع تایمر ناموفق بود") }
        }
    }

    fun stop(timer: CookingTimer) {
        viewModelScope.launch {
            runCatching { repository.stopTimer(timer.id) }
                .onSuccess { _events.emit("تایمر متوقف شد") }
                .onFailure { _events.emit("توقف تایمر ناموفق بود") }
        }
    }

    fun delete(timer: CookingTimer) {
        viewModelScope.launch {
            runCatching { repository.deleteTimer(timer.id) }
                .onSuccess { _events.emit("تایمر حذف شد") }
                .onFailure { _events.emit("حذف تایمر ناموفق بود") }
        }
    }
}
