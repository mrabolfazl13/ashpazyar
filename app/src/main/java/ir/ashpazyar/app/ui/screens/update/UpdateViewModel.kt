package ir.ashpazyar.app.ui.screens.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.ashpazyar.app.BuildConfig
import ir.ashpazyar.app.data.update.BazaarLauncher
import ir.ashpazyar.app.data.update.UpdateCheckResult
import ir.ashpazyar.app.data.update.UpdateRepository
import ir.ashpazyar.app.domain.model.UpdateManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UpdatePhase { IDLE, CHECKING, AVAILABLE, FORCED, UP_TO_DATE, ERROR }

data class UpdateUiState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val manifest: UpdateManifest? = null,
    val mandatory: Boolean = false,
    val manual: Boolean = false,
    val message: String? = null,
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UpdateRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = mutableState.asStateFlow()
    private var checkJob: kotlinx.coroutines.Job? = null

    init {
        check(manual = false)
    }

    fun check(manual: Boolean) {
        if (checkJob?.isActive == true) return
        if (mutableState.value.phase == UpdatePhase.CHECKING) return
        val previous = mutableState.value
        val forced = previous.phase == UpdatePhase.FORCED && previous.manifest != null
        mutableState.update {
            it.copy(
                phase = if (forced) UpdatePhase.FORCED else UpdatePhase.CHECKING,
                manual = manual,
                message = if (forced) "در حال بررسی دوباره…" else null
            )
        }
        checkJob = viewModelScope.launch {
            when (val result = repository.check(manual)) {
                UpdateCheckResult.Disabled -> mutableState.update { it.copy(phase = UpdatePhase.IDLE) }
                UpdateCheckResult.Dismissed -> mutableState.update { it.copy(phase = UpdatePhase.IDLE) }
                UpdateCheckResult.UpToDate -> mutableState.update {
                    it.copy(
                        phase = if (forced) UpdatePhase.IDLE else if (manual) UpdatePhase.UP_TO_DATE else UpdatePhase.IDLE,
                        manifest = null,
                        mandatory = false,
                        message = null
                    )
                }
                is UpdateCheckResult.Available -> mutableState.update {
                    it.copy(
                        phase = if (result.mandatory) UpdatePhase.FORCED else UpdatePhase.AVAILABLE,
                        manifest = result.manifest,
                        mandatory = result.mandatory,
                        message = null
                    )
                }
                is UpdateCheckResult.Failed -> mutableState.update {
                    it.copy(
                        phase = when {
                            forced -> UpdatePhase.FORCED
                            manual -> UpdatePhase.ERROR
                            else -> UpdatePhase.IDLE
                        },
                        message = result.message
                    )
                }
            }
        }
    }

    fun dismissOptional() {
        val current = mutableState.value
        val manifest = current.manifest ?: return
        if (current.mandatory) return
        repository.dismiss(manifest.latestVersion)
        mutableState.update { it.copy(phase = UpdatePhase.IDLE, manifest = null, message = null) }
    }

    fun openBazaar() {
        val opened = BazaarLauncher.open(context, context.packageName)
        if (!opened) {
            mutableState.update {
                it.copy(message = "بازار روی این دستگاه نصب نیست یا باز نشد. برنامه بازار را از فروشگاه رسمی نصب کنید.")
            }
        }
    }

    fun closeMessage() {
        mutableState.update {
            if (it.phase == UpdatePhase.ERROR || it.phase == UpdatePhase.UP_TO_DATE) {
                it.copy(phase = UpdatePhase.IDLE, message = null)
            } else {
                it.copy(message = null)
            }
        }
    }
}
