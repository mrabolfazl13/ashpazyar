package ir.ashpazyar.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.data.repository.*
import ir.ashpazyar.app.domain.engine.IngredientMatcher
import ir.ashpazyar.app.domain.engine.RecommendationEngine
import ir.ashpazyar.app.domain.engine.RecommenderContext
import ir.ashpazyar.app.domain.model.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val greeting: String = "",
    val dateLabel: String = "",
    val seasonLabel: String = "",
    val recipeCount: Int = 0,
    val pantryCount: Int = 0,
    val expiringCount: Int = 0,
    val recommendations: List<Recommendation> = emptyList(),
    val quickRecipes: List<Recipe> = emptyList(),
    val categories: List<Category> = emptyList(),
    val todayMeals: List<MealPlanEntry> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val userName: String = "آشپز عزیز"
)

private data class HomeBase(
    val recipes: List<Recipe>,
    val categories: List<Category>,
    val favorites: Set<String>,
    val inventory: List<InventoryItem>,
    val preferences: FoodPreferences
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    recipeRepository: RecipeRepository,
    ingredientRepository: IngredientRepository,
    kitchenRepository: KitchenRepository,
    mealPlanRepository: MealPlanRepository,
    preferencesRepository: PreferencesRepository,
    private val settingsRepository: SettingsRepository,
    private val recommendationEngine: RecommendationEngine,
    private val matcher: IngredientMatcher
) : ViewModel() {

    private val base = combine(
        recipeRepository.recipes,
        recipeRepository.categories,
        recipeRepository.favorites,
        kitchenRepository.inventory,
        preferencesRepository.preferences
    ) { recipes, categories, favorites, inventory, preferences ->
        HomeBase(recipes, categories, favorites, inventory, preferences)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        base,
        mealPlanRepository.observeRange(JalaliCalendar.todayEpochDay(), JalaliCalendar.todayEpochDay()),
        settingsRepository.settings,
        ingredientRepository.ingredients,
        recipeRepository.tips
    ) { base, todayMeals, settings, ingredients, _ ->
        val ingredientMap = ingredients.associateBy { it.id }
        val pantry = matcher.pantryFromInventory(base.inventory)
        val substitutes = recipeRepository.getSubstituteRules()
        val recommendations = recommendationEngine.recommend(
            recipes = base.recipes,
            context = RecommenderContext(
                preferences = base.preferences,
                pantry = pantry,
                ingredientsById = ingredientMap,
                favorites = base.favorites,
                substitutes = substitutes
            ),
            limit = 6
        )
        val hour = LocalTime.now().hour
        val greeting = when {
            hour in 5..11 -> "صبح بخیر، وقت یک صبحانه خوب است"
            hour in 12..16 -> "ظهر بخیر، آشپزخانه منتظر شماست"
            hour in 17..21 -> "عصر بخیر، شام چه داریم؟"
            else -> "شب بخیر، برای فردا برنامه بریزید"
        }
        val month = JalaliCalendar.today().month
        val season = when (month) {
            1, 2, 3 -> "بهار"
            4, 5, 6 -> "تابستان"
            7, 8, 9 -> "پاییز"
            else -> "زمستان"
        }
        val expiring = base.inventory.count {
            val status = it.status(warningDays = settings.expirationWarningDays)
            status == InventoryStatus.NEAR_EXPIRY || status == InventoryStatus.EXPIRED
        }
        HomeUiState(
            loading = false,
            greeting = greeting,
            dateLabel = JalaliCalendar.format(System.currentTimeMillis(), withWeekday = true),
            seasonLabel = season,
            recipeCount = base.recipes.size,
            pantryCount = base.inventory.count { it.amount > 0 },
            expiringCount = expiring,
            recommendations = recommendations,
            quickRecipes = base.recipes.filter { it.isQuick }
                .sortedWith(compareByDescending<Recipe> { it.isFamilyFriendly }.thenBy { it.editorialOrder })
                .take(8),
            categories = base.categories,
            todayMeals = todayMeals.sortedBy { it.mealType.ordinal },
            favorites = base.favorites
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
