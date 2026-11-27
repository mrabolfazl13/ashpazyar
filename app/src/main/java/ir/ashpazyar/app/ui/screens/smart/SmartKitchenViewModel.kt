package ir.ashpazyar.app.ui.screens.smart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.BaseQuantity
import ir.ashpazyar.app.data.repository.*
import ir.ashpazyar.app.domain.engine.IngredientMatcher
import ir.ashpazyar.app.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class KitchenFilter { ALL, FULL, ONE_MISSING, FEW_MISSING }

data class SmartKitchenUiState(
    val loading: Boolean = true,
    val matches: List<MatchResult> = emptyList(),
    val totalRecipes: Int = 0,
    val cookableCount: Int = 0,
    val pantryCount: Int = 0,
    val expiringCount: Int = 0,
    val favorites: Set<String> = emptySet(),
    val filter: KitchenFilter = KitchenFilter.ALL,
    val query: String = "",
    val lastMessage: String? = null
)

@HiltViewModel
class SmartKitchenViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val kitchenRepository: KitchenRepository,
    private val shoppingRepository: ShoppingRepository,
    private val settingsRepository: SettingsRepository,
    private val matcher: IngredientMatcher
) : ViewModel() {
    private val filter = MutableStateFlow(KitchenFilter.ALL)
    private val query = MutableStateFlow("")
    private val messages = MutableSharedFlow<String>()
    val events = messages.asSharedFlow()

    private data class Base(
        val recipes: List<Recipe>,
        val inventory: List<InventoryItem>,
        val ingredients: List<Ingredient>,
        val favorites: Set<String>,
        val substitutes: List<ir.ashpazyar.app.domain.engine.SubstituteRule>,
        val warningDays: Int
    )

    private val base = combine(
        recipeRepository.recipes,
        kitchenRepository.inventory,
        recipeRepository.ingredientsById,
        recipeRepository.favorites
    ) { recipes, inventory, ingredients, favorites ->
        Base(recipes, inventory, ingredients.values.toList(), favorites, emptyList(), 3)
    }.combine(settingsRepository.settings) { data, settings ->
        data.copy(warningDays = settings.expirationWarningDays)
    }

    private val withSubs = base.map { data ->
        data.copy(substitutes = recipeRepository.getSubstituteRules())
    }

    val uiState: StateFlow<SmartKitchenUiState> = combine(
        withSubs,
        filter,
        query
    ) { data, filter, query ->
        val pantry = matcher.pantryFromInventory(data.inventory)
        val ingredientMap = data.ingredients.associateBy { it.id }
        val tokens = ir.ashpazyar.app.core.util.PersianText.normalize(query).split(" ").filter { it.isNotBlank() }
        val matches = data.recipes.map { recipe ->
            matcher.match(recipe, pantry, ingredientMap, data.substitutes)
        }.filter { result ->
            if (tokens.isNotEmpty()) {
                val text = ir.ashpazyar.app.core.util.PersianText.normalize(
                    result.recipe.title + " " + result.recipe.ingredients.joinToString(" ") { it.ingredientName } + " " + result.recipe.tags.joinToString(" ")
                )
                if (!tokens.all { text.contains(it) }) return@filter false
            }
            when (filter) {
                KitchenFilter.ALL -> true
                KitchenFilter.FULL -> result.fullyAvailable
                KitchenFilter.ONE_MISSING -> result.missingCount <= 1
                KitchenFilter.FEW_MISSING -> result.missingCount <= 3
            }
        }.sortedWith(
            compareByDescending<MatchResult> { it.fullyAvailable }
                .thenByDescending { it.matchPercent }
                .thenBy { it.missingCount }
                .thenBy { it.recipe.editorialOrder }
        )
        val expiring = data.inventory.count {
            val status = it.status(data.warningDays)
            status == InventoryStatus.NEAR_EXPIRY || status == InventoryStatus.EXPIRED
        }
        SmartKitchenUiState(
            loading = false,
            matches = matches,
            totalRecipes = data.recipes.size,
            cookableCount = data.recipes.count { recipe ->
                matcher.match(recipe, pantry, ingredientMap, data.substitutes).fullyAvailable
            },
            pantryCount = data.inventory.count { it.amount > 0 },
            expiringCount = expiring,
            favorites = data.favorites,
            filter = filter,
            query = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmartKitchenUiState())

    fun setFilter(value: KitchenFilter) { filter.value = value }
    fun setQuery(value: String) { query.value = value }

    fun addMissingToShopping(recipeId: String) {
        viewModelScope.launch {
            val recipe = recipeRepository.getRecipe(recipeId) ?: return@launch
            runCatching { shoppingRepository.addFromRecipe(recipe, recipe.servings) }
                .onSuccess { result ->
                    messages.emit(
                        if (result.items.isEmpty()) "همه مواد این دستور موجود است"
                        else ir.ashpazyar.app.core.util.PersianText.faDigits(result.items.size.toString()) + " ماده به فهرست خرید اضافه شد"
                    )
                }
                .onFailure { messages.emit(it.message ?: "افزودن به خرید ناموفق بود") }
        }
    }
}
