package ir.ashpazyar.app.data.seed

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.toDetailString
import ir.ashpazyar.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.util.zip.GZIPInputStream
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
        val catalogIngredients = readGzipAsset<List<SeedIngredient>>("seed/catalog_ingredients.json.gz")
        val substituteFile = readOptionalAsset<SeedSubstituteFile>("seed/substitutes.json")

        val allIngredients = ingredients + catalogIngredients
        val ingredientMap = allIngredients.associateBy { it.id }
        val aliasSeen = mutableSetOf<String>()

        database.withTransaction {
            database.ingredientDao().insertAll(allIngredients.map { seed ->
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
            allIngredients.forEach { seed ->
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
                seed.toRecipeEntity(index, ingredientMap)
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
                        timerSeconds = step.timerSeconds?.takeIf { it > 0 },
                        ingredientRefs = step.ingredientRefs.toDetailString(),
                        equipment = step.equipment.toDetailString(),
                        temperatureC = step.temperatureC,
                        heatLevel = step.heatLevel,
                        successIndicators = step.successIndicators.toDetailString(),
                        commonMistakes = step.commonMistakes.toDetailString(),
                        recoveryInstructions = step.recoveryInstructions,
                        safetyNotes = step.safetyNotes.toDetailString()
                    )
                }
            }
            if (recipeSteps.isNotEmpty()) database.recipeDao().insertSteps(recipeSteps)

            var catalogCount = 0
            var catalogOrder = 10_000
            val recipeBatch = ArrayList<RecipeEntity>(200)
            val catalogIngredientBatch = ArrayList<RecipeIngredientEntity>(2_000)

            suspend fun flushCatalogBatch() {
                if (recipeBatch.isNotEmpty()) {
                    database.recipeDao().insertRecipes(recipeBatch)
                    recipeBatch.clear()
                }
                if (catalogIngredientBatch.isNotEmpty()) {
                    database.recipeDao().insertIngredients(catalogIngredientBatch)
                    catalogIngredientBatch.clear()
                }
            }

            GZIPInputStream(context.assets.open("seed/catalog_recipes.jsonl.gz")).bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val seed = json.decodeFromString<SeedRecipe>(line)
                    val entity = seed.toRecipeEntity(catalogOrder, ingredientMap) ?: continue
                    catalogOrder++
                    recipeBatch += entity
                    seed.ingredients.forEachIndexed { order, item ->
                        if (!ingredientMap.containsKey(item.ingredientId)) return@forEachIndexed
                        catalogIngredientBatch += RecipeIngredientEntity(
                            recipeId = seed.id,
                            ingredientId = item.ingredientId,
                            amount = item.amount,
                            unit = item.unit,
                            note = item.note,
                            optional = item.optional,
                            sortOrder = order
                        )
                    }
                    catalogCount++
                    if (recipeBatch.size >= 200) flushCatalogBatch()
                }
            }
            flushCatalogBatch()
            check(catalogCount == 5_790) { "Full catalog seed expected 5790 recipes, loaded $catalogCount" }

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

    private fun SeedRecipe.toRecipeEntity(
        index: Int,
        ingredientMap: Map<String, SeedIngredient>
    ): RecipeEntity? {
        if (id.isBlank() || ingredients.isEmpty()) return null
        val ingredientNames = ingredients.mapNotNull { ingredientMap[it.ingredientId]?.name }
        val searchable = buildString {
            append(title).append(' ').append(titleEn ?: "").append(' ')
            append(description).append(' ')
            append(tags.joinToString(" ")).append(' ')
            append(categoryIds.joinToString(" ")).append(' ')
            append(ingredientNames.joinToString(" "))
        }
        return RecipeEntity(
            id = id,
            title = title,
            titleEn = titleEn,
            description = description,
            categoryIds = categoryIds.joinToString("|"),
            tags = tags.joinToString("|"),
            servings = servings.coerceAtLeast(1),
            prepMinutes = prepMinutes.coerceAtLeast(0),
            cookMinutes = cookMinutes.coerceAtLeast(0),
            difficulty = difficulty,
            calories = calories,
            season = season,
            budgetLevel = budgetLevel,
            heroStyle = heroStyle,
            isVegetarian = isVegetarian,
            isVegan = isVegan,
            isGlutenFree = isGlutenFree,
            isDairyFree = isDairyFree,
            isFamilyFriendly = isFamilyFriendly,
            allergens = allergens.joinToString("|"),
            tips = tips.joinToString("\u001F"),
            searchText = PersianText.normalize(searchable),
            isUserCreated = false,
            editorialOrder = index,
            region = region,
            history = history,
            resultDescription = resultDescription,
            restMinutes = restMinutes.coerceAtLeast(0),
            equipment = equipment.toDetailString(),
            ovenTemperatureC = ovenTemperatureC,
            prepGuide = prepGuide.toDetailString(),
            servingGuide = servingGuide.toDetailString(),
            storageGuide = storageGuide,
            troubleshooting = troubleshooting.toDetailString(),
            sourceName = sourceName,
            sourceUrl = sourceUrl,
            license = license,
            validationStatus = validationStatus
        )
    }

    private inline fun <reified T> readGzipAsset(name: String): T {
        val content = GZIPInputStream(context.assets.open(name)).bufferedReader().use { it.readText() }
        return json.decodeFromString(content)
    }

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
        const val SEED_VERSION = "4"
    }
}
