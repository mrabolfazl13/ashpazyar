package ir.ashpazyar.app.data.repository

import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.IngredientAliasEntity
import ir.ashpazyar.app.data.local.entity.IngredientEntity
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.domain.model.Ingredient
import ir.ashpazyar.app.domain.model.IngredientUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngredientRepository @Inject constructor(
    private val database: AshpazyarDatabase
) {
    private val dao = database.ingredientDao()

    val ingredients: Flow<List<Ingredient>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Ingredient> = dao.getAll().map { it.toDomain() }

    suspend fun resolve(raw: String): Ingredient? {
        val normalized = PersianText.normalize(raw)
        if (normalized.isBlank()) return null
        dao.findByAlias(normalized)?.let { return it.toDomain() }
        val candidates = dao.searchByName(normalized)
        return candidates.firstOrNull()?.toDomain()
    }

    suspend fun search(query: String, limit: Int = 30): List<Ingredient> {
        val normalized = PersianText.normalize(query)
        val all = getAll()
        if (normalized.isBlank()) return all.sortedBy { it.name }.take(limit)
        val aliases = dao.getAllAliases().groupBy { it.ingredientId }
        return all.asSequence()
            .filter { ingredient ->
                PersianText.normalize(ingredient.name).contains(normalized) ||
                    ingredient.nameEn.contains(query, ignoreCase = true) ||
                    aliases[ingredient.id].orEmpty().any { it.aliasNormalized.contains(normalized) }
            }
            .sortedBy { ingredient ->
                val index = PersianText.normalize(ingredient.name).indexOf(normalized)
                if (index < 0) 500 else index
            }
            .take(limit)
            .toList()
    }

    suspend fun createCustom(name: String, categoryTitle: String, defaultUnit: IngredientUnit): Ingredient {
        require(name.trim().length >= 2) { "نام ماده اولیه کوتاه است" }
        val normalized = PersianText.normalize(name)
        dao.findByAlias(normalized)?.let { return it.toDomain() }
        val id = "custom-" + UUID.randomUUID().toString().take(8)
        val entity = IngredientEntity(
            id = id,
            name = name.trim(),
            category = "custom",
            categoryTitle = categoryTitle.ifBlank { "سایر" },
            defaultUnit = defaultUnit.name,
            nameNormalized = normalized,
            referencePricePerKg = null
        )
        dao.insertAll(listOf(entity))
        dao.insertAliases(
            listOf(
                IngredientAliasEntity(
                    ingredientId = id,
                    alias = name.trim(),
                    aliasNormalized = normalized
                )
            )
        )
        return entity.toDomain()
    }

    suspend fun byId(id: String): Ingredient? = dao.getById(id)?.toDomain()

    suspend fun getRecipeIngredientNames(recipeId: String): List<String> {
        return database.recipeDao().getWithDetails(recipeId)?.ingredients.orEmpty().mapNotNull { line ->
            dao.getById(line.ingredientId)?.name
        }
    }
}
