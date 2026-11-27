package ir.ashpazyar.app.ui.screens.cooking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.RecipeRepository
import ir.ashpazyar.app.data.repository.TimerRepository
import ir.ashpazyar.app.domain.engine.CookingStepFlow
import ir.ashpazyar.app.domain.model.Recipe
import ir.ashpazyar.app.domain.model.RecipeStep
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CookingUiState(
    val loading: Boolean = true,
    val recipe: Recipe? = null,
    val step: RecipeStep? = null,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val isLast: Boolean = false,
    val completedSteps: Set<Int> = emptySet(),
    val completedCount: Int = 0,
    val stepCompleted: Boolean = false,
    val allCompleted: Boolean = false,
    val progress: Float = 0f
)

@HiltViewModel
class CookingModeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    recipeRepository: RecipeRepository,
    private val timerRepository: TimerRepository
) : ViewModel() {
    private val recipeId: String = savedStateHandle.get<String>("recipeId").orEmpty()
    private val stepIndex = MutableStateFlow(
        savedStateHandle.get<Int>(KEY_STEP_INDEX)?.coerceAtLeast(0) ?: 0
    )
    private val completedSteps = MutableStateFlow(
        savedStateHandle.get<IntArray>(KEY_COMPLETED_STEPS)?.toSet().orEmpty()
    )
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    val uiState: StateFlow<CookingUiState> = combine(
        recipeRepository.observeRecipe(recipeId),
        stepIndex,
        completedSteps
    ) { recipe, index, completed ->
        val steps = recipe?.steps.orEmpty()
        val safeIndex = index.coerceIn(0, (steps.size - 1).coerceAtLeast(0))
        val validCompleted = CookingStepFlow.normalizedCompleted(steps.size, completed)
        CookingUiState(
            loading = false,
            recipe = recipe,
            step = steps.getOrNull(safeIndex),
            stepIndex = safeIndex,
            totalSteps = steps.size,
            isLast = steps.isNotEmpty() && safeIndex == steps.lastIndex,
            completedSteps = validCompleted,
            completedCount = validCompleted.size,
            stepCompleted = safeIndex in validCompleted,
            allCompleted = CookingStepFlow.allCompleted(steps.size, validCompleted),
            progress = CookingStepFlow.progress(steps.size, validCompleted)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CookingUiState())

    fun completeCurrentStep() {
        val state = uiState.value
        if (state.step == null) return
        setCompletedSteps(completedSteps.value + state.stepIndex)
    }

    fun toggleCurrentStep() {
        toggleStep(uiState.value.stepIndex)
    }

    fun toggleStep(index: Int) {
        val state = uiState.value
        if (state.step == null || index !in 0 until state.totalSteps) return
        val completed = completedSteps.value
        setCompletedSteps(if (index in completed) completed - index else completed + index)
    }

    fun next() {
        val state = uiState.value
        val target = CookingStepFlow.nextIndex(state.totalSteps, state.stepIndex, state.completedSteps)
        if (target != null) {
            setStepIndex(target)
        } else if (!state.stepCompleted && state.stepIndex < state.totalSteps - 1) {
            viewModelScope.launch {
                _messages.emit("برای رفتن به مرحله بعد، ابتدا این مرحله را کامل کنید")
            }
        }
    }

    fun previous() {
        val state = uiState.value
        if (state.stepIndex > 0) setStepIndex(state.stepIndex - 1)
    }

    private fun setStepIndex(index: Int) {
        stepIndex.value = index
        savedStateHandle[KEY_STEP_INDEX] = index
    }

    private fun setCompletedSteps(completed: Set<Int>) {
        completedSteps.value = completed
        savedStateHandle[KEY_COMPLETED_STEPS] = completed.sorted().toIntArray()
    }

    fun startStepTimer() {
        val state = uiState.value
        val step = state.step ?: return
        val seconds = step.timerSeconds ?: return
        viewModelScope.launch {
            timerRepository.startTimer(
                recipeId = state.recipe?.id,
                label = if (step.title.isBlank()) "مرحله " + (state.stepIndex + 1) else step.title,
                seconds = seconds
            )
            _messages.emit("تایمر شروع شد؛ پایان آن را به شما خبر می‌دهیم")
        }
    }

    private companion object {
        const val KEY_STEP_INDEX = "cooking_step_index"
        const val KEY_COMPLETED_STEPS = "cooking_completed_steps"
    }
}
