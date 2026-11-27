package ir.ashpazyar.app.ui.screens.recipeeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.repository.RecipeRepository
import ir.ashpazyar.app.domain.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorLine(
    val key: Long,
    val ingredientId: String,
    val name: String,
    val amount: String,
    val unit: Unit,
    val note: String = "",
    val optional: Boolean = false
)

data class EditorStep(
    val key: Long,
    val title: String,
    val instruction: String,
    val timer: String = ""
)

data class RecipeEditorUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val editingId: String? = null,
    val title: String = "",
    val description: String = "",
    val servings: Int = 4,
    val prepMinutes: Int = 15,
    val cookMinutes: Int = 30,
    val difficulty: Difficulty = Difficulty.EASY,
    val categoryIds: Set<String> = emptySet(),
    val tags: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val ingredientOptions: List<Ingredient> = emptyList(),
    val lines: List<EditorLine> = emptyList(),
    val steps: List<EditorStep> = listOf(EditorStep(1, "", "")),
    val tips: List<String> = emptyList(),
    val newTip: String = "",
    val error: String? = null
)

@HiltViewModel
class RecipeEditorViewModel @Inject constructor(
    private val repository: RecipeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val editArgument: String? = savedStateHandle.get<String>("recipeId")?.takeIf { it.isNotBlank() }
    private val state = MutableStateFlow(RecipeEditorUiState())
    val uiState: StateFlow<RecipeEditorUiState> = state
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()
    private var nextKey = 100L

    init {
        viewModelScope.launch {
            val categories = repository.getCategoriesOnce()
            val ingredients = repository.getIngredients()
            val source = editArgument?.let { repository.getRecipe(it) }
            val editable = source?.takeIf { it.isUserCreated }
            state.update {
                if (editable == null && source != null) {
                    it.copy(
                        loading = false,
                        categories = categories,
                        ingredientOptions = ingredients,
                        title = source.title + " (کپی)",
                        description = source.description,
                        servings = source.servings,
                        prepMinutes = source.prepMinutes,
                        cookMinutes = source.cookMinutes,
                        difficulty = source.difficulty,
                        categoryIds = source.categoryIds.toSet(),
                        tags = source.tags,
                        lines = source.ingredients.map { line ->
                            EditorLine(nextKey++, line.ingredientId, line.ingredientName, formatAmount(line.amount), line.unit, line.note, line.optional)
                        },
                        steps = source.steps.map { step ->
                            EditorStep(nextKey++, step.title, step.instruction, step.timerSeconds?.let { (it / 60).toString() } ?: "")
                        }.ifEmpty { listOf(EditorStep(nextKey++, "", "")) },
                        tips = source.tips
                    )
                } else {
                    it.copy(
                        loading = false,
                        categories = categories,
                        ingredientOptions = ingredients,
                        editingId = editable?.id,
                        title = editable?.title.orEmpty(),
                        description = editable?.description.orEmpty(),
                        servings = editable?.servings ?: 4,
                        prepMinutes = editable?.prepMinutes ?: 15,
                        cookMinutes = editable?.cookMinutes ?: 30,
                        difficulty = editable?.difficulty ?: Difficulty.EASY,
                        categoryIds = editable?.categoryIds?.toSet().orEmpty(),
                        tags = editable?.tags.orEmpty(),
                        lines = editable?.ingredients?.map { line ->
                            EditorLine(nextKey++, line.ingredientId, line.ingredientName, formatAmount(line.amount), line.unit, line.note, line.optional)
                        }.orEmpty(),
                        steps = editable?.steps?.map { step ->
                            EditorStep(nextKey++, step.title, step.instruction, step.timerSeconds?.let { (it / 60).toString() } ?: "")
                        }?.ifEmpty { listOf(EditorStep(nextKey++, "", "")) } ?: listOf(EditorStep(nextKey++, "", "")),
                        tips = editable?.tips.orEmpty()
                    )
                }
            }
        }
    }

    private fun formatAmount(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().replace(".", "٫")
    }

    fun setTitle(v: String) = state.update { it.copy(title = v, error = null) }
    fun setDescription(v: String) = state.update { it.copy(description = v) }
    fun setServings(v: Int) = state.update { it.copy(servings = v.coerceIn(1, 30)) }
    fun setPrep(v: Int) = state.update { it.copy(prepMinutes = v.coerceIn(0, 600)) }
    fun setCook(v: Int) = state.update { it.copy(cookMinutes = v.coerceIn(0, 600)) }
    fun setDifficulty(v: Difficulty) = state.update { it.copy(difficulty = v) }
    fun toggleCategory(id: String) = state.update {
        it.copy(categoryIds = if (id in it.categoryIds) it.categoryIds - id else it.categoryIds + id)
    }

    fun addIngredient(ingredient: Ingredient) {
        state.update { current ->
            if (current.lines.any { it.ingredientId == ingredient.id }) current.copy(error = "این ماده قبلاً اضافه شده است")
            else current.copy(
                lines = current.lines + EditorLine(nextKey++, ingredient.id, ingredient.name, "1", ingredient.defaultUnit),
                error = null
            )
        }
    }

    fun updateLine(key: Long, transform: (EditorLine) -> EditorLine) = state.update { current ->
        current.copy(lines = current.lines.map { if (it.key == key) transform(it) else it })
    }

    fun removeLine(key: Long) = state.update { current -> current.copy(lines = current.lines.filterNot { it.key == key }) }

    fun addStep() = state.update { current ->
        current.copy(steps = current.steps + EditorStep(nextKey++, "", ""))
    }

    fun updateStep(key: Long, transform: (EditorStep) -> EditorStep) = state.update { current ->
        current.copy(steps = current.steps.map { if (it.key == key) transform(it) else it })
    }

    fun removeStep(key: Long) = state.update { current ->
        if (current.steps.size <= 1) current
        else current.copy(steps = current.steps.filterNot { it.key == key })
    }

    fun moveStep(key: Long, up: Boolean) = state.update { current ->
        val list = current.steps.toMutableList()
        val index = list.indexOfFirst { it.key == key }
        if (index < 0) return@update current
        val target = if (up) index - 1 else index + 1
        if (target !in list.indices) return@update current
        val item = list.removeAt(index)
        list.add(target, item)
        current.copy(steps = list)
    }

    fun setNewTip(value: String) = state.update { it.copy(newTip = value) }

    fun addTip() = state.update { current ->
        val tip = current.newTip.trim()
        if (tip.length < 2) current.copy(error = "نکته را کامل‌تر بنویسید")
        else current.copy(tips = current.tips + tip, newTip = "", error = null)
    }

    fun removeTip(index: Int) = state.update { current ->
        current.copy(tips = current.tips.filterIndexed { i, _ -> i != index })
    }

    fun save() {
        val current = state.value
        if (current.saving) return
        val title = current.title.trim()
        if (title.length < 2) {
            state.update { it.copy(error = "نام دستور باید حداقل دو حرف باشد") }
            return
        }
        if (current.lines.isEmpty()) {
            state.update { it.copy(error = "حداقل یک ماده اولیه اضافه کنید") }
            return
        }
        val ingredients = current.lines.mapNotNull { line ->
            val amount = PersianText.toLatinDigits(line.amount).replace("٫", ".").replace(",", "").toDoubleOrNull()
            if (amount == null || amount <= 0) null
            else DraftIngredient(line.ingredientId, line.name, amount, line.unit, line.note.trim(), line.optional)
        }
        if (ingredients.size != current.lines.size) {
            state.update { it.copy(error = "مقدار همه مواد را با عدد مثبت کامل کنید") }
            return
        }
        val steps = current.steps.mapNotNull { step ->
            if (step.instruction.isBlank()) null
            else DraftStep(
                title = step.title.trim().ifBlank { "مرحله" },
                instruction = step.instruction.trim(),
                timerMinutes = PersianText.toLatinDigits(step.timer).toIntOrNull()?.takeIf { it > 0 }
            )
        }
        if (steps.isEmpty()) {
            state.update { it.copy(error = "حداقل یک مرحله با توضیح بنویسید") }
            return
        }
        val draft = RecipeDraft(
            id = current.editingId,
            title = title,
            description = current.description.trim().ifBlank { "دستور خانگی " + title },
            categoryIds = current.categoryIds.ifEmpty { setOf("family") },
            tags = current.tags,
            servings = current.servings,
            prepMinutes = current.prepMinutes,
            cookMinutes = current.cookMinutes,
            difficulty = current.difficulty,
            ingredients = ingredients,
            steps = steps,
            tips = current.tips
        )
        state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.saveUserRecipe(draft) }
                .onSuccess { recipe ->
                    state.update { it.copy(saving = false) }
                    _events.emit("دستور ذخیره شد")
                    _events.emit("saved:" + recipe.id)
                }
                .onFailure { error ->
                    state.update { it.copy(saving = false, error = error.message ?: "ذخیره دستور ناموفق بود") }
                }
        }
    }
}
