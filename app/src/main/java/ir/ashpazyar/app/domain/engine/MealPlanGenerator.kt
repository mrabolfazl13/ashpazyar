package ir.ashpazyar.app.domain.engine

import ir.ashpazyar.app.domain.model.*
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanGenerator @Inject constructor(
    private val matcher: IngredientMatcher
) {
    fun generate(
        recipes: List<Recipe>,
        input: PlanGenerationInput,
        ingredientsById: Map<String, Ingredient>,
        substitutes: List<SubstituteRule> = emptyList()
    ): PlanProposal {
        val dislike = input.dislikedIngredients.map { it.trim() }.filter { it.isNotEmpty() }
        val allergy = input.allergens.map { it.lowercase() }.toSet()
        val candidates = recipes.filter { recipe ->
            !recipe.isUserCreated || recipe.ingredients.isNotEmpty()
        }.filter { recipe ->
            (input.maxMinutes == null || recipe.totalMinutes <= input.maxMinutes) &&
                (!input.noMeat || recipe.isVegetarian) &&
                (!input.vegetarian || recipe.isVegetarian) &&
                (!input.lowCost || recipe.isEconomical || recipe.budgetLevel != BudgetLevel.PREMIUM) &&
                recipe.allergens.none { it.lowercase() in allergy } &&
                recipe.ingredients.none { line -> dislike.any { line.ingredientName.contains(it) } }
        }
        if (candidates.isEmpty()) {
            return PlanProposal(emptyList(), listOf("با این محدودیت‌ها دستور مناسبی پیدا نشد. یکی از فیلترها را تغییر دهید."), 0)
        }

        val mealTypes = buildList {
            if (input.includeBreakfast) add(MealType.BREAKFAST)
            if (input.includeLunch) add(MealType.LUNCH)
            if (input.includeDinner) add(MealType.DINNER)
        }.ifEmpty { listOf(MealType.LUNCH, MealType.DINNER) }

        val random = Random(input.startDate * 31 + input.familySize)
        val used = mutableSetOf<String>()
        val selected = mutableListOf<ProposedMeal>()
        val notes = mutableListOf<String>()
        val start = input.startDate
        val totalSlots = 7 * mealTypes.size
        var matchSum = 0
        var matchCount = 0

        for (dayOffset in 0..6) {
            for (mealType in mealTypes) {
                val pool = candidates.filter { recipe ->
                    when (mealType) {
                        MealType.BREAKFAST -> recipe.categoryIds.contains("breakfast") || recipe.isQuick
                        MealType.SNACK -> false
                        else -> !recipe.categoryIds.contains("dessert") && !recipe.categoryIds.contains("drink")
                    }
                }.ifEmpty { candidates }
                val freshPool = pool.filter { it.id !in used }.ifEmpty { pool }
                val scored = freshPool.map { recipe ->
                    var score = random.nextInt(0, 18)
                    if (input.variety && recipe.id !in used) score += 22
                    if (recipe.isFamilyFriendly) score += 6
                    if (input.pantryIngredientIds.isNotEmpty()) {
                        val portion = recipe.ingredients.count { it.ingredientId in input.pantryIngredientIds }
                        score += (portion * 12) / recipe.ingredients.size.coerceAtLeast(1)
                    }
                    if (input.budget > 0 && recipe.budgetLevel == BudgetLevel.BUDGET) score += 8
                    if (input.lowCost && recipe.isEconomical) score += 8
                    if (recipe.season == Season.ALL) score += 2
                    if (mealType == MealType.BREAKFAST && recipe.categoryIds.contains("breakfast")) score += 20
                    if (input.maxMinutes != null && recipe.totalMinutes <= input.maxMinutes) score += 4
                    score to recipe
                }.sortedByDescending { it.first }
                val chosen = scored.first().second
                used += chosen.id
                selected += ProposedMeal(start + dayOffset, mealType, chosen, input.familySize)
                if (input.pantryIngredientIds.isNotEmpty()) {
                    val result = matcher.match(
                        chosen,
                        input.pantryIngredientIds.associateWith { id ->
                            listOf(ir.ashpazyar.app.core.util.BaseQuantity(1.0, ir.ashpazyar.app.core.util.Dimension.MASS))
                        },
                        ingredientsById,
                        substitutes
                    )
                    matchSum += result.matchPercent
                    matchCount++
                }
            }
        }
        notes += "برنامه بر اساس دستورهای داخلی و محدودیت‌های انتخابی شما ساخته شد."
        if (used.size < totalSlots) notes += "برای حفظ تنوع، تعداد محدودی دستور با شرایط شما وجود داشت."
        if (input.budget > 0) notes += "کنترل بودجه بر اساس سطح هزینه دستورها انجام شده است؛ قیمت‌های دقیق را در بخش هزینه غذا ثبت کنید."
        return PlanProposal(
            entries = selected,
            notes = notes,
            estimatedMatch = if (matchCount > 0) matchSum / matchCount else 0
        )
    }
}
