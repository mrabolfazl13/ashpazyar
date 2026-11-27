package ir.ashpazyar.app.data.repository

import androidx.room.withTransaction
import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.MealPlanItemEntity
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.domain.engine.MealPlanGenerator
import ir.ashpazyar.app.domain.engine.PlanGenerationInput
import ir.ashpazyar.app.domain.engine.PlanProposal
import ir.ashpazyar.app.domain.engine.SubstituteRule
import ir.ashpazyar.app.domain.model.MealPlanEntry
import ir.ashpazyar.app.domain.model.MealType
import ir.ashpazyar.app.domain.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanRepository @Inject constructor(
    private val database: AshpazyarDatabase,
    private val recipeRepository: RecipeRepository,
    private val generator: MealPlanGenerator
) {
    private val dao = database.mealPlanDao()

    fun observeRange(startDay: Long, endDay: Long): Flow<List<MealPlanEntry>> {
        return combine(dao.observeRange(startDay, endDay), recipeRepository.recipes) { rows, recipes ->
            val recipeMap = recipes.associateBy { it.id }
            rows.map { row ->
                MealPlanEntry(
                    id = row.item.id,
                    date = row.item.planDate,
                    mealType = MealType.from(row.item.mealType),
                    recipe = row.item.recipeId?.let { recipeMap[it] },
                    recipeId = row.item.recipeId,
                    customTitle = row.item.customTitle,
                    servings = row.item.servings,
                    isCompleted = row.item.isCompleted,
                    note = row.item.note
                )
            }
        }
    }

    suspend fun getRangeDetailed(startDay: Long, endDay: Long): List<MealPlanEntry> {
        val rows = dao.getRange(startDay, endDay)
        val recipes = recipeRepository.getAllRecipes().associateBy { it.id }
        return rows.map { row ->
            MealPlanEntry(
                id = row.item.id,
                date = row.item.planDate,
                mealType = MealType.from(row.item.mealType),
                recipe = row.item.recipeId?.let { recipes[it] },
                recipeId = row.item.recipeId,
                customTitle = row.item.customTitle,
                servings = row.item.servings,
                isCompleted = row.item.isCompleted,
                note = row.item.note
            )
        }
    }

    suspend fun setMeal(date: Long, mealType: MealType, recipe: Recipe, servings: Int) {
        val existing = dao.getDay(date).firstOrNull { it.mealType == mealType.name }
        dao.upsert(
            MealPlanItemEntity(
                id = existing?.id ?: 0,
                planDate = date,
                mealType = mealType.name,
                recipeId = recipe.id,
                servings = servings.coerceAtLeast(1),
                isCompleted = existing?.isCompleted ?: false,
                note = existing?.note.orEmpty()
            )
        )
    }

    suspend fun setCustomMeal(date: Long, mealType: MealType, title: String, servings: Int) {
        val existing = dao.getDay(date).firstOrNull { it.mealType == mealType.name }
        dao.upsert(
            MealPlanItemEntity(
                id = existing?.id ?: 0,
                planDate = date,
                mealType = mealType.name,
                customTitle = title.trim(),
                servings = servings.coerceAtLeast(1)
            )
        )
    }

    suspend fun remove(id: Long) = dao.deleteById(id)

    suspend fun setCompleted(id: Long, completed: Boolean) = dao.setCompleted(id, completed)

    suspend fun copyDay(from: Long, to: Long, replace: Boolean = true) {
        database.withTransaction {
            if (replace) dao.clearRange(to, to)
            val source = dao.getDay(from)
            val existingTargets = dao.getDay(to).associateBy { it.mealType }.toMutableMap()
            for (item in source) {
                val target = existingTargets[item.mealType]
                if (target != null) {
                    dao.upsert(target.copy(recipeId = item.recipeId, customTitle = item.customTitle, servings = item.servings, note = item.note))
                } else {
                    dao.upsert(item.copy(id = 0, planDate = to))
                }
            }
        }
    }

    suspend fun move(id: Long, newDate: Long, newMeal: MealType) {
        database.withTransaction {
            val target = dao.getDay(newDate).firstOrNull { it.mealType == newMeal.name && it.id != id }
            if (target != null) dao.deleteById(target.id)
            dao.moveSlot(id, newDate, newMeal.name)
        }
    }

    suspend fun clearWeek(startDay: Long) = dao.clearRange(startDay, startDay + 6)

    suspend fun generatePlan(input: PlanGenerationInput): PlanProposal {
        val recipes = recipeRepository.getAllRecipes()
        val ingredients = recipeRepository.getIngredientMap()
        val substitutes = recipeRepository.getSubstituteRules()
        return generator.generate(recipes, input, ingredients, substitutes)
    }

    suspend fun saveProposal(startDay: Long, proposal: PlanProposal) {
        database.withTransaction {
            dao.clearRange(startDay, startDay + 6)
            dao.upsertAll(proposal.entries.map { meal ->
                MealPlanItemEntity(
                    planDate = meal.date,
                    mealType = meal.mealType.name,
                    recipeId = meal.recipe.id,
                    servings = meal.servings
                )
            })
        }
    }
}
