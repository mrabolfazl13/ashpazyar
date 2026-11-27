package ir.ashpazyar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ir.ashpazyar.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KitchenDao {
    @Transaction
    @Query("SELECT * FROM kitchen_inventory ORDER BY expiryDate IS NULL, expiryDate ASC, createdAt DESC")
    fun observeInventory(): Flow<List<InventoryWithIngredient>>

    @Transaction
    @Query("SELECT * FROM kitchen_inventory ORDER BY expiryDate IS NULL, expiryDate ASC")
    suspend fun getInventory(): List<InventoryWithIngredient>

    @Transaction
    @Query("SELECT * FROM kitchen_inventory WHERE id = :id")
    suspend fun getInventoryItem(id: Long): InventoryWithIngredient?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(item: InventoryEntity): Long

    @Update
    suspend fun updateInventory(item: InventoryEntity)

    @Query("UPDATE kitchen_inventory SET amount = :amount, unit = :unit, updatedAt = :now WHERE id = :id")
    suspend fun updateAmount(id: Long, amount: Double, unit: String, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteInventory(item: InventoryEntity)

    @Query("DELETE FROM kitchen_inventory WHERE id = :id")
    suspend fun deleteInventoryById(id: Long)

    @Insert
    suspend fun insertTransaction(item: InventoryTransactionEntity)

    @Insert
    suspend fun insertTransactions(items: List<InventoryTransactionEntity>)

    @Query("SELECT * FROM inventory_transactions ORDER BY createdAt DESC LIMIT 200")
    fun observeTransactions(): Flow<List<InventoryTransactionEntity>>

    @Query("SELECT * FROM inventory_transactions WHERE groupId = :groupId")
    suspend fun getTransactionsByGroup(groupId: String): List<InventoryTransactionEntity>

    @Query("SELECT * FROM inventory_transactions WHERE groupId = :groupId AND canUndo = 1")
    suspend fun getUndoableByGroup(groupId: String): List<InventoryTransactionEntity>

    @Query("UPDATE inventory_transactions SET canUndo = 0 WHERE groupId = :groupId")
    suspend fun markGroupUndone(groupId: String)

    @Query("DELETE FROM kitchen_inventory")
    suspend fun clearAllInventory()
}
