package ir.ashpazyar.app.domain.engine

import ir.ashpazyar.app.core.util.BaseQuantity
import ir.ashpazyar.app.core.util.Dimension
import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

data class SubstituteRule(
    val ingredientId: String,
    val substituteIngredientId: String,
    val recipeId: String? = null,
    val ratio: Double = 1.0,
    val note: String = "",
    val contextTag: String = ""
)

@Singleton
class IngredientMatcher @Inject constructor() {

    fun pantryFromInventory(inventory: List<InventoryItem>): Map<String, List<BaseQuantity>> {
        val result = mutableMapOf<String, MutableList<BaseQuantity>>()
        for (item in inventory) {
            if (item.amount <= 0.0) continue
            val base = UnitConverter.toBase(item.amount, item.unit, item.ingredient?.profile) ?: continue
            val list = result.getOrPut(item.ingredientId) { mutableListOf() }
            val existingIndex = list.indexOfFirst { it.dimension == base.dimension }
            if (existingIndex >= 0) list[existingIndex] = list[existingIndex] + base
            else list += base
        }
        return result
    }

    fun match(
        recipe: Recipe,
        pantry: Map<String, List<BaseQuantity>>,
        ingredientsById: Map<String, Ingredient>,
        substitutes: List<SubstituteRule>
    ): MatchResult {
        val required = recipe.ingredients.filter { !it.optional }
        var coverageSum = 0.0
        var availableCount = 0
        var missingCount = 0
        val missing = mutableListOf<MissingIngredient>()
        val usedSubstitutes = mutableListOf<SubstituteUse>()

        for (line in required) {
            val ingredient = ingredientsById[line.ingredientId]
            val wanted = UnitConverter.toBase(line.amount, line.unit, ingredient?.profile)
            if (wanted == null || wanted.amount <= 0.0) {
                missingCount++
                missing += MissingIngredient(
                    line.ingredientId, line.ingredientName, line.amount, 0.0, line.unit, false,
                    UnitConverter.pretty(line.amount, line.unit, ingredient?.profile)
                )
                continue
            }

            val available = pantry[line.ingredientId].orEmpty()
                .filter { it.dimension == wanted.dimension }
                .sumOf { it.amount }
            var coverage = (available / wanted.amount).coerceIn(0.0, 1.0)
            var selectedSubstitute: SubstituteRule? = null
            var substituteBase: BaseQuantity? = null

            if (coverage < 0.995) {
                val applicable = substitutes.filter { rule ->
                    rule.ingredientId == line.ingredientId &&
                        (rule.recipeId == recipe.id || (rule.contextTag.isNotBlank() &&
                            (recipe.tags + recipe.categoryIds).any { it.contains(rule.contextTag, ignoreCase = true) }))
                }
                for (rule in applicable) {
                    val substituteProfile = ingredientsById[rule.substituteIngredientId]?.profile
                    val substituteAvailable = pantry[rule.substituteIngredientId].orEmpty()
                        .firstOrNull { it.dimension == wanted.dimension }
                        ?: continue
                    val requiredSubstitute = BaseQuantity(wanted.amount * rule.ratio, wanted.dimension)
                    if (substituteAvailable.amount >= requiredSubstitute.amount) {
                        coverage = 1.0
                        selectedSubstitute = rule
                        substituteBase = requiredSubstitute
                        break
                    }
                    val partial = (substituteAvailable.amount / requiredSubstitute.amount).coerceIn(0.0, 1.0)
                    if (partial > coverage) coverage = partial
                }
            }

            coverageSum += coverage
            if (coverage >= 0.995) {
                availableCount++
                if (selectedSubstitute != null) {
                    usedSubstitutes += SubstituteUse(
                        originalName = line.ingredientName,
                        substituteName = ingredientsById[selectedSubstitute.substituteIngredientId]?.name
                            ?: selectedSubstitute.substituteIngredientId,
                        note = selectedSubstitute.note
                    )
                }
            } else {
                missingCount++
                val missingAmount = (wanted.amount * (1.0 - coverage)).coerceAtLeast(0.0)
                val missingQuantity = BaseQuantity(missingAmount, wanted.dimension)
                missing += MissingIngredient(
                    ingredientId = line.ingredientId,
                    name = line.ingredientName,
                    requiredAmount = line.amount,
                    availableAmount = min(available, wanted.amount),
                    unit = line.unit,
                    optional = false,
                    suggestedAmountLabel = UnitConverter.bestDisplay(missingQuantity, ingredient?.profile)
                )
            }
        }

        val total = required.size
        val percent = if (total == 0) 100 else Math.round((coverageSum / total) * 100.0).toInt().coerceIn(0, 100)
        val scaledPercent = if (total > 0 && availableCount == total) 100 else percent
        return MatchResult(
            recipe = recipe,
            matchPercent = scaledPercent,
            availableCount = availableCount,
            missingCount = missingCount,
            totalRequired = total,
            missing = missing,
            substitutesUsed = usedSubstitutes,
            fullyAvailable = total == 0 || availableCount == total
        )
    }

    fun missingOnly(result: MatchResult): List<MissingIngredient> = result.missing.filter { !it.optional }
}
