package ir.ashpazyar.app.domain.engine

import ir.ashpazyar.app.core.util.BaseQuantity
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

data class RecommenderContext(
    val preferences: FoodPreferences = FoodPreferences(),
    val pantry: Map<String, List<BaseQuantity>> = emptyMap(),
    val ingredientsById: Map<String, Ingredient> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val substitutes: List<SubstituteRule> = emptyList(),
    val now: Long = System.currentTimeMillis()
)

@Singleton
class RecommendationEngine @Inject constructor(
    private val matcher: IngredientMatcher
) {
    fun recommend(
        recipes: List<Recipe>,
        context: RecommenderContext,
        limit: Int = 10,
        requireAllIngredients: Boolean = false
    ): List<Recommendation> {
        val currentSeason = currentSeason()
        return recipes.asSequence()
            .filter { isAllowed(it, context.preferences) }
            .map { recipe ->
                val match = if (context.pantry.isNotEmpty()) {
                    matcher.match(recipe, context.pantry, context.ingredientsById, context.substitutes)
                } else null
                val reasons = mutableListOf<String>()
                var score = 35

                if (match != null) {
                    score += (match.matchPercent * 45) / 100
                    if (match.fullyAvailable) {
                        score += 12
                        reasons += "همه مواد اصلی در آشپزخانه موجود است"
                    } else if (match.missingCount <= 2) {
                        reasons += "فقط " + match.missingCount + " ماده کم دارد"
                    }
                }
                if (recipe.season == currentSeason || recipe.season == Season.ALL) {
                    score += 4
                    if (recipe.season == currentSeason) reasons += "مناسب فصل " + currentSeason.label
                }
                if (context.preferences.quickMeals && recipe.isQuick) {
                    score += 5
                    reasons += "کمتر از ۳۰ دقیقه آماده می‌شود"
                }
                if (context.preferences.lowCost && recipe.isEconomical) {
                    score += 5
                    reasons += "در سطح هزینه اقتصادی"
                }
                if (context.favorites.contains(recipe.id)) {
                    score += 8
                    reasons += "از دستورهای محبوب شما"
                }
                if (recipe.isFamilyFriendly) score += 2
                if (recipe.calories != null && recipe.isVegetarian) score += 1
                if (reasons.isEmpty()) {
                    reasons += when {
                        recipe.isQuick -> "انتخابی سریع برای یک روز شلوغ"
                        recipe.isEconomical -> "طعم خوب با هزینه متعادل"
                        else -> "پیشنهاد منتخب چاشیکا"
                    }
                }
                Recommendation(recipe, score.coerceIn(0, 100), reasons.take(3), match)
            }
            .filter { !requireAllIngredients || (it.match?.fullyAvailable == true) }
            .sortedWith(compareByDescending<Recommendation> { it.score }.thenBy { it.recipe.editorialOrder })
            .take(limit)
            .toList()
    }

    fun isAllowed(recipe: Recipe, preferences: FoodPreferences): Boolean {
        if (preferences.vegetarian && !recipe.isVegetarian) return false
        if (preferences.noMeat && (!recipe.isVegetarian || recipe.ingredients.any {
                it.ingredientName.contains("مرغ") || it.ingredientName.contains("گوشت") ||
                    it.ingredientName.contains("ماهی") || it.ingredientName.contains("میگو")
            })) return false
        if (preferences.dairyFree && !recipe.isDairyFree) return false
        if (preferences.glutenFree && !recipe.isGlutenFree) return false
        val allergyKeys = preferences.allergies.map { it.lowercase() }.toSet()
        if (recipe.allergens.any { it.lowercase() in allergyKeys }) return false
        val disliked = preferences.dislikedIngredients.map { it.trim() }.filter { it.isNotEmpty() }
        if (recipe.ingredients.any { line -> disliked.any { line.ingredientName.contains(it) } }) return false
        return true
    }

    fun reasonFor(recipe: Recipe, match: MatchResult?): String {
        return when {
            match?.fullyAvailable == true -> "با مواد موجود شما قابل پخت است"
            match != null && match.missingCount <= 2 -> "فقط " + match.missingCount + " ماده کم دارد"
            recipe.isQuick -> "در کمتر از ۳۰ دقیقه آماده می‌شود"
            recipe.isEconomical -> "انتخاب اقتصادی برای وعده امروز"
            else -> "پیشنهاد چاشیکا برای تنوع سفره"
        }
    }

    private fun currentSeason(): Season {
        val month = JalaliCalendar.today().month
        return when (month) {
            1, 2, 3 -> Season.SPRING
            4, 5, 6 -> Season.SUMMER
            7, 8, 9 -> Season.AUTUMN
            else -> Season.WINTER
        }
    }
}
