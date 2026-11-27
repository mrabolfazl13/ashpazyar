package ir.ashpazyar.app.ui.screens.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.BackupRepository
import ir.ashpazyar.app.data.repository.BackupResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val exporting: Boolean = false,
    val restoring: Boolean = false,
    val lastResult: BackupResult? = null,
    val lastAction: String? = null,
    val error: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository
) : ViewModel() {
    private val state = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = state
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    fun suggestedName(): String = repository.suggestedFileName()

    fun export(uri: Uri) {
        state.value = state.value.copy(exporting = true, error = null)
        viewModelScope.launch {
            runCatching { repository.exportTo(uri) }
                .onSuccess {
                    state.value = state.value.copy(exporting = false, lastResult = it, lastAction = "export")
                    _events.emit("پشتیبان با موفقیت ذخیره شد")
                }
                .onFailure {
                    state.value = state.value.copy(exporting = false, error = it.message ?: "ساخت پشتیبان ناموفق بود")
                }
        }
    }

    fun restore(uri: Uri) {
        state.value = state.value.copy(restoring = true, error = null)
        viewModelScope.launch {
            runCatching { repository.importFrom(uri) }
                .onSuccess {
                    state.value = state.value.copy(restoring = false, lastResult = it, lastAction = "restore")
                    _events.emit("بازیابی با موفقیت انجام شد")
                }
                .onFailure {
                    state.value = state.value.copy(restoring = false, error = it.message ?: "بازیابی ناموفق بود؛ فایل معتبر نیست")
                }
        }
    }
}
