package ir.ashpazyar.app.ui.screens.leftovers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.LeftoverRepository
import ir.ashpazyar.app.domain.model.Leftover
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LeftoverFilter { ACTIVE, ALL, CONSUMED }

data class LeftoversUiState(
    val loading: Boolean = true,
    val all: List<Leftover> = emptyList(),
    val visible: List<Leftover> = emptyList(),
    val filter: LeftoverFilter = LeftoverFilter.ACTIVE,
    val activeCount: Int = 0,
    val expiredCount: Int = 0
)

@HiltViewModel
class LeftoversViewModel @Inject constructor(
    private val repository: LeftoverRepository
) : ViewModel() {
    private val filter = MutableStateFlow(LeftoverFilter.ACTIVE)
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<LeftoversUiState> = combine(repository.leftovers, filter) { list, filter ->
        val sorted = list.sortedWith(compareBy<Leftover> { it.isConsumed }.thenBy { it.suggestedUseBy })
        val visible = when (filter) {
            LeftoverFilter.ALL -> sorted
            LeftoverFilter.ACTIVE -> sorted.filter { !it.isConsumed }
            LeftoverFilter.CONSUMED -> sorted.filter { it.isConsumed }
        }
        LeftoversUiState(
            loading = false,
            all = sorted,
            visible = visible,
            filter = filter,
            activeCount = list.count { !it.isConsumed },
            expiredCount = list.count { !it.isConsumed && it.isExpired() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeftoversUiState())

    fun setFilter(value: LeftoverFilter) { filter.value = value }

    fun add(title: String, portions: Int, useByDays: Int, note: String) {
        if (title.trim().length < 2) {
            viewModelScope.launch { _events.emit("نام غذا را کامل بنویسید") }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            runCatching {
                repository.add(
                    title = title,
                    recipeId = null,
                    preparedDate = now,
                    portions = portions,
                    useBy = now + useByDays * 86_400_000L,
                    note = note
                )
            }.onSuccess { _events.emit("به باقی‌مانده‌ها اضافه شد") }
                .onFailure { _events.emit(it.message ?: "ثبت ناموفق بود") }
        }
    }

    fun consume(item: Leftover) {
        viewModelScope.launch {
            runCatching { repository.markConsumed(item.id) }
                .onSuccess { _events.emit("مصرف شد") }
                .onFailure { _events.emit("به‌روزرسانی ناموفق بود") }
        }
    }

    fun delete(item: Leftover) {
        viewModelScope.launch {
            runCatching { repository.delete(item.id) }
                .onSuccess { _events.emit("حذف شد") }
                .onFailure { _events.emit("حذف ناموفق بود") }
        }
    }
}
