package ir.ashpazyar.app.ui.screens.recipedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.data.repository.*
import ir.ashpazyar.app.domain.engine.IngredientMatcher
import ir.ashpazyar.app.domain.engine.SubstituteRule
import ir.ashpazyar.app.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeDetailUiState(
    val loading: Boolean = true,
    val recipe: Recipe? = null,
    val isFavorite: Boolean = false,
    val servings: Int = 4,
    val match: MatchResult? = null,
    val notFound: Boolean = false
)

data class RecipeEvent(
    val message: String,
    val undoGroupId: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val kitchenRepository: KitchenRepository,
    private val shoppingRepository: ShoppingRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val settingsRepository: SettingsRepository,
    private val matcher: IngredientMatcher
) : ViewModel() {

    val recipeId: String = savedStateHandle.get<String>("recipeId").orEmpty()
    private val servings = MutableStateFlow(4)
    private val substitutes = MutableStateFlow<List<SubstituteRule>>(emptyList())
    private val _events = MutableSharedFlow<RecipeEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch { substitutes.value = recipeRepository.getSubstituteRules() }
        viewModelScope.launch {
            settingsRepository.settings.first().let { servings.value = it.familySize }
        }
    }

    private data class Base(
        val recipe: Recipe?,
        val favorite: Boolean,
        val inventory: List<InventoryItem>,
        val ingredients: List<Ingredient>,
        val prefs: FoodPreferences
    )

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        recipeRepository.observeRecipe(recipeId),
        recipeRepository.favorites,
        kitchenRepository.inventory,
        recipeRepository.ingredientsById,
        recipeRepository.tips
    ) { recipe, favorites, inventory, ingredients, _ ->
        Base(
            recipe = recipe,
            favorite = recipe != null && recipe.id in favorites,
            inventory = inventory,
            ingredients = ingredients.values.toList(),
            prefs = FoodPreferences()
        )
    }.combine(servings) { base, servingCount -> base to servingCount }
        .combine(substitutes) { (base, servingCount), substitutes ->
            val recipe = base.recipe
            if (recipe == null) {
                RecipeDetailUiState(loading = false, notFound = true)
            } else {
                val pantry = matcher.pantryFromInventory(base.inventory)
                val match = matcher.match(
                    recipe,
                    pantry,
                    base.ingredients.associateBy { it.id },
                    substitutes
                )
                RecipeDetailUiState(
                    loading = false,
                    recipe = recipe,
                    isFavorite = base.favorite,
                    servings = servingCount,
                    match = match
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState())

    fun setServings(value: Int) { servings.value = value.coerceIn(1, 30) }

    fun toggleFavorite() {
        viewModelScope.launch {
            val added = recipeRepository.toggleFavorite(recipeId)
            _events.emit(RecipeEvent(if (added) "به محبوب‌ها اضافه شد" else "از محبوب‌ها حذف شد"))
        }
    }

    fun addMissingToShopping() {
        val state = uiState.value
        val recipe = state.recipe ?: return
        viewModelScope.launch {
            runCatching { shoppingRepository.addFromRecipe(recipe, state.servings) }
                .onSuccess { build ->
                    val message = if (build.items.isEmpty()) "همه مواد در آشپزخانه موجود است؛ چیزی به خرید اضافه نشد"
                    else PersianTextCount(build.items.size, "ماده") + " به فهرست خرید اضافه شد"
                    _events.emit(RecipeEvent(message))
                }
                .onFailure { _events.emit(RecipeEvent(it.message ?: "افزودن به خرید ناموفق بود")) }
        }
    }

    fun consumeIngredients() {
        val state = uiState.value
        val recipe = state.recipe ?: return
        viewModelScope.launch {
            val result = kitchenRepository.consumeForRecipe(recipe, state.servings)
            when {
                result.usedNothing -> _events.emit(RecipeEvent("موادی از آشپزخانه برای کسر پیدا نشد"))
                result.insufficient.isNotEmpty() -> _events.emit(
                    RecipeEvent(
                        "مواد موجود کسر شد؛ " + result.insufficient.size + " ماده کافی نبود",
                        undoGroupId = result.groupId
                    )
                )
                else -> _events.emit(RecipeEvent("مواد مصرف‌شده از موجودی آشپزخانه کسر شد", undoGroupId = result.groupId))
            }
        }
    }

    fun undoConsume(groupId: String) {
        viewModelScope.launch {
            val restored = kitchenRepository.undoConsumption(groupId)
            _events.emit(RecipeEvent(if (restored) "مواد به موجودی برگشت" else "بازگردانی ممکن نبود"))
        }
    }

    fun addToPlan(date: Long, mealType: MealType) {
        val recipe = uiState.value.recipe ?: return
        viewModelScope.launch {
            runCatching { mealPlanRepository.setMeal(date, mealType, recipe, servings.value) }
                .onSuccess { _events.emit(RecipeEvent("به برنامه " + mealType.label + " اضافه شد")) }
                .onFailure { _events.emit(RecipeEvent("افزودن به برنامه ناموفق بود")) }
        }
    }

    fun deleteRecipe() {
        val recipe = uiState.value.recipe ?: return
        if (!recipe.isUserCreated) return
        viewModelScope.launch {
            runCatching { recipeRepository.deleteUserRecipe(recipe.id) }
                .onSuccess { _events.emit(RecipeEvent("دستور حذف شد", deleted = true)) }
                .onFailure { _events.emit(RecipeEvent("حذف ناموفق بود")) }
        }
    }

    fun duplicateRecipe() {
        viewModelScope.launch {
            runCatching { recipeRepository.duplicateRecipe(recipeId) }
                .onSuccess { _events.emit(RecipeEvent("یک کپی از دستور ساخته شد")) }
                .onFailure { _events.emit(RecipeEvent("ساخت کپی ناموفق بود")) }
        }
    }

    private fun PersianTextCount(count: Int, label: String): String =
        ir.ashpazyar.app.core.util.PersianText.faDigits(count.toString()) + " " + label
}
