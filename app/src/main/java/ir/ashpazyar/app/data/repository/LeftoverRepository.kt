package ir.ashpazyar.app.data.repository

import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.FoodLeftoverEntity
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.domain.model.Leftover
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeftoverRepository @Inject constructor(
    private val database: AshpazyarDatabase
) {
    val leftovers: Flow<List<Leftover>> = database.leftoverDao().observeAll()
        .map { list -> list.map { it.toDomain() } }

    suspend fun add(title: String, recipeId: String?, preparedDate: Long, portions: Int, useBy: Long, note: String) {
        require(title.trim().isNotEmpty()) { "نام غذا لازم است" }
        database.leftoverDao().insert(
            FoodLeftoverEntity(
                title = title.trim(),
                recipeId = recipeId,
                preparedDate = preparedDate,
                portions = portions.coerceAtLeast(1),
                suggestedUseBy = useBy,
                note = note
            )
        )
    }

    suspend fun markConsumed(id: Long) {
        database.leftoverDao().observeAll().first().firstOrNull { it.id == id }?.let {
            database.leftoverDao().update(it.copy(isConsumed = true))
        }
    }

    suspend fun delete(id: Long) = database.leftoverDao().delete(id)
}
