package ir.ashpazyar.app.data.repository

import androidx.room.withTransaction
import ir.ashpazyar.app.core.util.BaseQuantity
import ir.ashpazyar.app.core.util.Dimension
import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.InventoryEntity
import ir.ashpazyar.app.data.local.entity.InventoryTransactionEntity
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

data class ConsumeResult(
    val groupId: String?,
    val consumed: List<String>,
    val insufficient: List<MissingIngredient>,
    val usedNothing: Boolean
)

@Singleton
class KitchenRepository @Inject constructor(
    private val database: AshpazyarDatabase
) {
    private val kitchenDao = database.kitchenDao()
    private val ingredientDao = database.ingredientDao()

    val inventory: Flow<List<InventoryItem>> = kitchenDao.observeInventory()
        .map { list -> list.map { it.toDomain() } }

    val transactions: Flow<List<InventoryTransaction>> = kitchenDao.observeTransactions()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getAllInventory(): List<InventoryItem> = kitchenDao.getInventory().map { it.toDomain() }

    suspend fun addInventory(
        ingredientId: String,
        amount: Double,
        unit: IngredientUnit,
        location: StorageLocation,
        purchaseDate: Long?,
        expiryDate: Long?,
        note: String,
        pricePaid: Double?
    ): Long {
        require(amount > 0.0) { "مقدار باید بزرگ‌تر از صفر باشد" }
        val ingredient = ingredientDao.getById(ingredientId) ?: error("ماده اولیه پیدا نشد")
        require(UnitConverter.toBase(amount, unit, ingredient.toDomain().profile) != null) { "واحد نامعتبر است" }
        return kitchenDao.insertInventory(
            InventoryEntity(
                ingredientId = ingredientId,
                amount = amount,
                unit = unit.name,
                location = location.name,
                purchaseDate = purchaseDate,
                expiryDate = expiryDate,
                note = note.trim(),
                pricePaid = pricePaid,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateInventory(item: InventoryItem) {
        require(item.amount >= 0.0) { "مقدار نمی‌تواند منفی باشد" }
        kitchenDao.updateInventory(
            InventoryEntity(
                id = item.id,
                ingredientId = item.ingredientId,
                amount = item.amount,
                unit = item.unit.name,
                location = item.location.name,
                purchaseDate = item.purchaseDate,
                expiryDate = item.expiryDate,
                note = item.note,
                pricePaid = item.pricePaid,
                createdAt = item.createdAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteInventory(id: Long) {
        kitchenDao.deleteInventoryById(id)
    }

    suspend fun consumeForRecipe(
        recipe: Recipe,
        targetServings: Int,
        reason: String = "پخت " + recipe.title
    ): ConsumeResult {
        val ingredients = ingredientDao.getAll().associateBy { it.id }
        val inventory = kitchenDao.getInventory()
        val groupId = UUID.randomUUID().toString()
        val consumed = mutableListOf<String>()
        val insufficient = mutableListOf<MissingIngredient>()
        val transactions = mutableListOf<InventoryTransactionEntity>()
        val factor = targetServings.coerceAtLeast(1).toDouble() / recipe.servings.coerceAtLeast(1).toDouble()

        database.withTransaction {
            for (line in recipe.ingredients.filter { !it.optional }) {
                val ingredient = ingredients[line.ingredientId] ?: continue
                val profile = ingredient.toDomain().profile
                val wanted = UnitConverter.toBase(line.amount * factor, line.unit, profile) ?: continue
                var remaining = wanted.amount
                val candidates = inventory.filter { it.item.ingredientId == line.ingredientId }
                    .sortedBy { it.item.expiryDate ?: Long.MAX_VALUE }
                for (candidate in candidates) {
                    if (remaining <= 0.000001) break
                    val available = UnitConverter.toBase(candidate.item.amount, IngredientUnit.from(candidate.item.unit), profile)
                        ?: continue
                    if (available.dimension != wanted.dimension) continue
                    val taken = min(remaining, available.amount)
                    if (taken <= 0.0) continue
                    val newBase = available.amount - taken
                    val newAmount = UnitConverter.fromBase(BaseQuantity(newBase, available.dimension), IngredientUnit.from(candidate.item.unit), profile)
                        ?: continue
                    kitchenDao.updateAmount(candidate.item.id, newAmount.coerceAtLeast(0.0), candidate.item.unit)
                    transactions += InventoryTransactionEntity(
                        groupId = groupId,
                        inventoryId = candidate.item.id,
                        ingredientId = line.ingredientId,
                        ingredientNameSnapshot = ingredient.name,
                        deltaBaseAmount = -taken,
                        dimension = available.dimension.name,
                        unitSnapshot = candidate.item.unit,
                        reason = reason,
                        referenceType = "RECIPE",
                        referenceId = recipe.id
                    )
                    remaining -= taken
                    if (ingredient.name !in consumed) consumed += ingredient.name
                }
                if (remaining > 0.0001) {
                    val missingQuantity = BaseQuantity(remaining, wanted.dimension)
                    insufficient += MissingIngredient(
                        ingredientId = line.ingredientId,
                        name = ingredient.name,
                        requiredAmount = line.amount * factor,
                        availableAmount = line.amount * factor - UnitConverter.fromBase(missingQuantity, line.unit, profile).orZero(),
                        unit = line.unit,
                        optional = false,
                        suggestedAmountLabel = UnitConverter.bestDisplay(missingQuantity, profile)
                    )
                }
            }
            if (transactions.isNotEmpty()) kitchenDao.insertTransactions(transactions)
        }
        return ConsumeResult(
            groupId = groupId.takeIf { transactions.isNotEmpty() },
            consumed = consumed,
            insufficient = insufficient,
            usedNothing = transactions.isEmpty()
        )
    }

    suspend fun undoConsumption(groupId: String): Boolean {
        var restored = false
        database.withTransaction {
            val items = kitchenDao.getUndoableByGroup(groupId)
            if (items.isEmpty()) return@withTransaction
            val inventory = kitchenDao.getInventory().associateBy { it.item.id }
            for (transaction in items) {
                val row = inventory[transaction.inventoryId] ?: continue
                val profile = ingredientDao.getById(row.item.ingredientId)?.toDomain()?.profile
                val current = UnitConverter.toBase(row.item.amount, IngredientUnit.from(row.item.unit), profile) ?: continue
                if (current.dimension.name != transaction.dimension) continue
                val restoredBase = BaseQuantity(current.amount - transaction.deltaBaseAmount, current.dimension)
                val restoredAmount = UnitConverter.fromBase(restoredBase, IngredientUnit.from(row.item.unit), profile) ?: continue
                kitchenDao.updateAmount(row.item.id, restoredAmount, row.item.unit)
                restored = true
            }
            kitchenDao.markGroupUndone(groupId)
        }
        return restored
    }

    suspend fun inventoryCountAtRisk(warningDays: Int): Int =
        inventory.first().count {
            val status = it.status(warningDays = warningDays)
            status == InventoryStatus.NEAR_EXPIRY || status == InventoryStatus.EXPIRED
        }

    private fun Double?.orZero(): Double = this ?: 0.0
}
