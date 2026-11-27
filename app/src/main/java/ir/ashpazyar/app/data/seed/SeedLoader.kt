package ir.ashpazyar.app.data.seed

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AshpazyarDatabase
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    suspend fun seedIfNeeded(force: Boolean = false) = withContext(Dispatchers.IO) {
        val meta = database.metaDao().get("seed_version")
        val recipeCount = database.recipeDao().count()
        val ingredientCount = database.ingredientDao().count()
        if (!force && meta == SEED_VERSION && recipeCount > 0 && ingredientCount > 0) return@withContext

        val ingredients = readAsset<List<SeedIngredient>>("seed/ingredients.json")
        val categories = readAsset<List<SeedCategory>>("seed/categories.json")
        val tips = readAsset<List<SeedTip>>("seed/tips.json")
        val recipeFile = readAsset<SeedRecipeFile>("seed/recipes.json")
        val substituteFile = readOptionalAsset<SeedSubstituteFile>("seed/substitutes.json")

        val ingredientMap = ingredients.associateBy { it.id }
        val aliasSeen = mutableSetOf<String>()

        database.withTransaction {
            database.ingredientDao().insertAll(ingredients.map { seed ->
                IngredientEntity(
                    id = seed.id,
                    name = seed.name,
                    nameEn = seed.nameEn,
                    category = seed.category,
                    categoryTitle = seed.categoryTitle,
                    defaultUnit = seed.defaultUnit,
                    nameNormalized = PersianText.normalize(seed.name),
                    cupWeightGrams = seed.cupWeightGrams,
                    pieceWeightGrams = seed.pieceWeightGrams,
                    referencePricePerKg = seed.referencePricePerKg
                )
            })

            val aliases = mutableListOf<IngredientAliasEntity>()
            fun addAlias(ingredientId: String, raw: String) {
                val normalized = PersianText.normalize(raw)
                if (normalized.isBlank() || !aliasSeen.add(normalized)) return
                aliases += IngredientAliasEntity(ingredientId = ingredientId, alias = raw, aliasNormalized = normalized)
            }
            ingredients.forEach { seed ->
                addAlias(seed.id, seed.name)
                seed.aliases.forEach { addAlias(seed.id, it) }
            }
            database.ingredientDao().insertAliases(aliases)

            val substitutes = substituteFile?.substitutes.orEmpty().filter {
                ingredientMap.containsKey(it.ingredientId) && ingredientMap.containsKey(it.substituteIngredientId)
            }
            database.ingredientDao().clearGlobalSubstitutes()
            if (substitutes.isNotEmpty()) {
                database.ingredientDao().insertSubstitutes(substitutes.map {
                    IngredientSubstituteEntity(
                        ingredientId = it.ingredientId,
                        substituteIngredientId = it.substituteIngredientId,
                        recipeId = it.recipeId,
                        ratio = it.ratio,
                        note = it.note,
                        contextTag = it.contextTag
                    )
                })
            }

            database.categoryDao().insertAll(categories.mapIndexed { index, category ->
                RecipeCategoryEntity(category.id, category.title, category.subtitle, category.accent, category.icon, index)
            })
            database.tipDao().insertAll(tips.mapIndexed { index, tip ->
                CookingTipEntity(tip.id, tip.category, tip.title, tip.summary, tip.content, tip.safety, index)
            })

            val recipes = recipeFile.recipes.mapIndexedNotNull { index, seed ->
                if (seed.id.isBlank() || seed.ingredients.isEmpty()) return@mapIndexedNotNull null
                val ingredientNames = seed.ingredients.mapNotNull { ingredientMap[it.ingredientId]?.name }
                val searchable = buildString {
                    append(seed.title).append(' ').append(seed.titleEn ?: "").append(' ')
                    append(seed.description).append(' ')
                    append(seed.tags.joinToString(" ")).append(' ')
                    append(seed.categoryIds.joinToString(" ")).append(' ')
                    append(ingredientNames.joinToString(" "))
                }
                RecipeEntity(
                    id = seed.id,
                    title = seed.title,
                    titleEn = seed.titleEn,
                    description = seed.description,
                    categoryIds = seed.categoryIds.joinToString("|"),
                    tags = seed.tags.joinToString("|"),
                    servings = seed.servings.coerceAtLeast(1),
                    prepMinutes = seed.prepMinutes.coerceAtLeast(0),
                    cookMinutes = seed.cookMinutes.coerceAtLeast(0),
                    difficulty = seed.difficulty,
                    calories = seed.calories,
                    season = seed.season,
                    budgetLevel = seed.budgetLevel,
                    heroStyle = seed.heroStyle,
                    isVegetarian = seed.isVegetarian,
                    isVegan = seed.isVegan,
                    isGlutenFree = seed.isGlutenFree,
                    isDairyFree = seed.isDairyFree,
                    isFamilyFriendly = seed.isFamilyFriendly,
                    allergens = seed.allergens.joinToString("|"),
                    tips = seed.tips.joinToString("\u001F"),
                    searchText = PersianText.normalize(searchable),
                    isUserCreated = false,
                    editorialOrder = index
                )
            }
            database.recipeDao().insertRecipes(recipes)

            val recipeIngredients = recipeFile.recipes.flatMap { seed ->
                seed.ingredients.mapIndexedNotNull { order, item ->
                    if (!ingredientMap.containsKey(item.ingredientId)) return@mapIndexedNotNull null
                    RecipeIngredientEntity(
                        recipeId = seed.id,
                        ingredientId = item.ingredientId,
                        amount = item.amount,
                        unit = item.unit,
                        note = item.note,
                        optional = item.optional,
                        sortOrder = order
                    )
                }
            }
            if (recipeIngredients.isNotEmpty()) database.recipeDao().insertIngredients(recipeIngredients)

            val recipeSteps = recipeFile.recipes.flatMap { seed ->
                seed.steps.mapIndexedNotNull { order, step ->
                    if (step.instruction.isBlank()) return@mapIndexedNotNull null
                    RecipeStepEntity(
                        recipeId = seed.id,
                        stepNumber = order + 1,
                        title = step.title,
                        instruction = step.instruction,
                        timerSeconds = step.timerSeconds?.takeIf { it > 0 }
                    )
                }
            }
            if (recipeSteps.isNotEmpty()) database.recipeDao().insertSteps(recipeSteps)

            if (database.preferenceDao().get() == null) {
                database.preferenceDao().save(FoodPreferenceEntity())
            }
            if (database.shoppingDao().getActiveList() == null) {
                database.shoppingDao().insertList(ShoppingListEntity(title = "فهرست خرید خانه"))
            }
            database.metaDao().put(AppMetaEntity("seed_version", SEED_VERSION))
        }
    }

    suspend fun reseedRecipes() = seedIfNeeded(force = true)

    private inline fun <reified T> readAsset(name: String): T {
        val content = context.assets.open(name).bufferedReader().use { it.readText() }
        return json.decodeFromString(content)
    }

    private inline fun <reified T> readOptionalAsset(name: String): T? {
        return runCatching {
            val content = context.assets.open(name).bufferedReader().use { it.readText() }
            json.decodeFromString<T>(content)
        }.getOrNull()
    }

    companion object {
        const val SEED_VERSION = "1"
    }
}
