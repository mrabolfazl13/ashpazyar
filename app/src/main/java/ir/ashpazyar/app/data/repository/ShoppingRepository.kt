package ir.ashpazyar.app.data.repository

import androidx.room.withTransaction
import ir.ashpazyar.app.core.util.BaseQuantity
import ir.ashpazyar.app.core.util.Dimension
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.ShoppingListEntity
import ir.ashpazyar.app.data.local.entity.ShoppingListItemEntity
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.domain.model.ShoppingBuildResult
import ir.ashpazyar.app.domain.engine.ShoppingEngine
import ir.ashpazyar.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class ShoppingBundle(val list: ShoppingList?, val items: List<ShoppingItem>)

@Singleton
class ShoppingRepository @Inject constructor(
    private val database: AshpazyarDatabase,
    private val shoppingEngine: ShoppingEngine,
    private val kitchenRepository: KitchenRepository,
    private val recipeRepository: RecipeRepository,
    private val mealPlanRepository: MealPlanRepository
) {
    private val dao = database.shoppingDao()

    val activeList: Flow<ShoppingList?> = dao.observeActiveList().map { it?.toDomain() }

    val items: Flow<List<ShoppingItem>> = dao.observeActiveList().flatMapLatest { list ->
        if (list == null) flowOf(emptyList()) else dao.observeItems(list.id).map { rows -> rows.map { it.toDomain() } }
    }

    val bundle: Flow<ShoppingBundle> = combine(activeList, items) { list, items -> ShoppingBundle(list, items) }

    val history: Flow<List<ShoppingList>> = dao.observeHistory().map { list -> list.map { it.toDomain() } }

    suspend fun ensureActiveList(): ShoppingListEntity {
        return dao.getActiveList() ?: run {
            val id = dao.insertList(ShoppingListEntity(title = "فهرست خرید خانه"))
            dao.getList(id) ?: error("ساخت فهرست خرید ناموفق بود")
        }
    }

    suspend fun addManualItem(
        name: String,
        amount: Double,
        unit: IngredientUnit,
        category: String = "سایر",
        ingredientId: String? = null,
        note: String = ""
    ) {
        require(name.trim().isNotEmpty()) { "نام کالا لازم است" }
        val list = ensureActiveList()
        val existing = dao.getItems(list.id).firstOrNull {
            it.isChecked.not() && it.name.trim() == name.trim() && it.unit == unit.name
        }
        if (existing != null) {
            dao.updateItem(existing.copy(amount = existing.amount + amount, note = note.ifBlank { existing.note }))
        } else {
            dao.insertItem(
                ShoppingListItemEntity(
                    shoppingListId = list.id,
                    ingredientId = ingredientId,
                    name = name.trim(),
                    category = category,
                    amount = amount,
                    unit = unit.name,
                    source = "MANUAL",
                    note = note
                )
            )
        }
    }

    suspend fun addFromRecipe(recipe: Recipe, servings: Int): ShoppingBuildResult {
        val inventory = kitchenRepository.getAllInventory()
        val ingredients = recipeRepository.getIngredientMap()
        val build = shoppingEngine.buildFromRecipe(recipe, servings, inventory, ingredients)
        addAggregates(build.items, "RECIPE")
        return build
    }

    suspend fun addFromMealPlan(startDay: Long, endDay: Long): ShoppingBuildResult {
        val entries = mealPlanRepository.getRangeDetailed(startDay, endDay)
        val inventory = kitchenRepository.getAllInventory()
        val ingredients = recipeRepository.getIngredientMap()
        val build = shoppingEngine.buildMissingItems(entries, inventory, ingredients)
        addAggregates(build.items, "MEAL_PLAN")
        return build
    }

    private suspend fun addAggregates(aggregates: List<ShoppingAggregate>, source: String) {
        val list = ensureActiveList()
        val prices = recipeRepository.getIngredientMap().mapValues { it.value.referencePricePerKg ?: 0.0 }
        database.withTransaction {
            val existing = dao.getItems(list.id).toMutableList()
            for (item in aggregates) {
                val ingredient = recipeRepository.getIngredients().firstOrNull { it.id == item.ingredientId }
                val profile = ingredient?.profile
                val newBase = UnitConverter.toBase(item.amount, item.unit, profile)
                val match = existing.firstOrNull { row ->
                    row.isChecked.not() && row.ingredientId == item.ingredientId &&
                        (row.unit == item.unit.name || runCatching {
                            val oldBase = UnitConverter.toBase(row.amount, IngredientUnit.from(row.unit), profile)
                            oldBase != null && newBase != null && oldBase.dimension == newBase.dimension
                        }.getOrDefault(false))
                }
                val estimate = prices[item.ingredientId]?.takeIf { it > 0 }?.let { price ->
                    val kg = when (newBase?.dimension) {
                        Dimension.MASS -> (newBase?.amount ?: 0.0) / 1000.0
                        Dimension.VOLUME -> (newBase?.amount ?: 0.0) / 1000.0
                        else -> null
                    }
                    if (kg != null) kg * price else null
                }
                if (match == null) {
                    dao.insertItem(
                        ShoppingListItemEntity(
                            shoppingListId = list.id,
                            ingredientId = item.ingredientId,
                            name = item.name,
                            category = item.category,
                            amount = item.amount,
                            unit = item.unit.name,
                            estimatedPrice = estimate,
                            source = source
                        )
                    )
                } else {
                    val merged = if (match.unit == item.unit.name) {
                        match.amount + item.amount
                    } else {
                        val oldBase = UnitConverter.toBase(match.amount, IngredientUnit.from(match.unit), profile)
                        if (oldBase != null && newBase != null && oldBase.dimension == newBase.dimension) {
                            val sum = BaseQuantity(oldBase.amount + newBase.amount, newBase.dimension)
                            UnitConverter.fromBase(sum, IngredientUnit.from(match.unit), profile) ?: match.amount
                        } else match.amount
                    }
                    dao.updateItem(match.copy(amount = merged, estimatedPrice = (match.estimatedPrice ?: 0.0) + (estimate ?: 0.0)))
                }
            }
        }
    }

    suspend fun toggleItem(id: Long, checked: Boolean) = dao.setChecked(id, checked)

    suspend fun deleteItem(id: Long) = dao.deleteItem(id)

    suspend fun setActualPrice(id: Long, price: Double?) {
        val item = dao.getItems(ensureActiveList().id).firstOrNull { it.id == id } ?: return
        dao.updateItem(item.copy(actualPrice = price))
    }

    suspend fun archiveCurrent(): Long? {
        val list = dao.getActiveList() ?: return null
        dao.archiveList(list.id)
        dao.insertList(ShoppingListEntity(title = "فهرست خرید " + PersianText.faDigits(list.id.toString())))
        return list.id
    }

    suspend fun historyItems(listId: Long): List<ShoppingItem> =
        dao.getItems(listId).map { it.toDomain() }

    suspend fun toggleHistoryUnchecked(listId: Long) {
        // Histories are immutable by design; kept only for read-only review.
    }

    fun shareText(items: List<ShoppingItem>): String {
        return buildString {
            appendLine("🛒 فهرست خرید قاشق")
            appendLine()
            items.groupBy { it.category }.forEach { (category, rows) ->
                appendLine("• " + category)
                rows.forEach { item ->
                    val check = if (item.isChecked) "✓" else "☐"
                    appendLine("  " + check + " " + item.name + " — " + UnitConverter.pretty(item.amount, item.unit))
                }
            }
            appendLine()
            appendLine("ساخته‌شده با قاشق")
        }
    }
}
