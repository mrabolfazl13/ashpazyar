package ir.ashpazyar.app.data.repository

import androidx.room.withTransaction
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.*
import ir.ashpazyar.app.data.toDetailString
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.data.toListParts
import ir.ashpazyar.app.data.toStoredList
import ir.ashpazyar.app.domain.engine.IngredientMatcher
import ir.ashpazyar.app.domain.engine.RecipeFilter
import ir.ashpazyar.app.domain.engine.RecipeFilterState
import ir.ashpazyar.app.domain.engine.SubstituteRule
import ir.ashpazyar.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val database: AshpazyarDatabase,
    private val matcher: IngredientMatcher
) {
    private val recipeDao = database.recipeDao()
    private val ingredientDao = database.ingredientDao()
    private val favoriteDao = database.favoriteDao()
    private val metaDao = database.metaDao()

    val ingredientsById: Flow<Map<String, Ingredient>> = ingredientDao.observeAll()
        .map { list -> list.map { it.id to it.toDomain() }.toMap() }

    val recipes: Flow<List<Recipe>> = combine(
        recipeDao.observeAllWithDetails(),
        ingredientDao.observeAll()
    ) { recipeRows, ingredients ->
        val map = ingredients.associate { it.id to it.toDomain() }
        recipeRows.map { it.toDomain(map) }
    }

    val favorites: Flow<Set<String>> = favoriteDao.observeIds().map { it.toSet() }

    val categories: Flow<List<Category>> = database.categoryDao().observeAll().map { list -> list.map { it.toDomain() } }

    val tips: Flow<List<CookingTip>> = database.tipDao().observeAll().map { list -> list.map { it.toDomain() } }

    val userRecipes: Flow<List<Recipe>> = combine(
        recipeDao.observeUserRecipes(),
        ingredientDao.observeAll()
    ) { rows, ingredients ->
        val map = ingredients.associate { it.id to it.toDomain() }
        rows.map { it.toDomain(map) }
    }

    fun observeRecipe(id: String): Flow<Recipe?> = combine(recipes, ingredientsById) { list, _ ->
        list.firstOrNull { it.id == id }
    }

    suspend fun getRecipe(id: String): Recipe? {
        val row = recipeDao.getWithDetails(id) ?: return null
        val ingredients = ingredientDao.getAll().associate { it.id to it.toDomain() }
        return row.toDomain(ingredients)
    }

    suspend fun getAllRecipes(): List<Recipe> {
        val ingredients = ingredientDao.getAll().associate { it.id to it.toDomain() }
        return recipeDao.getAllWithDetails().map { it.toDomain(ingredients) }
    }

    suspend fun getIngredients(): List<Ingredient> = ingredientDao.getAll().map { it.toDomain() }

    suspend fun getIngredientMap(): Map<String, Ingredient> = ingredientDao.getAll().associate { it.id to it.toDomain() }

    fun search(state: RecipeFilterState): Flow<List<Recipe>> = recipes.map { RecipeFilter.apply(it, state) }

    suspend fun toggleFavorite(recipeId: String): Boolean {
        return database.withTransaction {
            val exists = favoriteDao.isFavorite(recipeId)
            if (exists) favoriteDao.remove(recipeId) else favoriteDao.add(FavoriteRecipeEntity(recipeId))
            !exists
        }
    }

    suspend fun addFavorite(recipeId: String) {
        favoriteDao.add(FavoriteRecipeEntity(recipeId))
    }

    suspend fun removeFavorite(recipeId: String) {
        favoriteDao.remove(recipeId)
    }

    suspend fun deleteUserRecipe(recipeId: String) {
        database.withTransaction {
            recipeDao.deleteUserRecipe(recipeId)
            database.userRecipeDao().delete(recipeId)
        }
    }

    suspend fun saveUserRecipe(draft: RecipeDraft): Recipe {
        require(draft.title.trim().length >= 2) { "نام دستور باید حداقل دو حرف باشد" }
        require(draft.ingredients.isNotEmpty()) { "حداقل یک ماده اولیه لازم است" }
        require(draft.steps.isNotEmpty()) { "حداقل یک مرحله پخت لازم است" }
        val ingredientMap = getIngredientMap()
        val id = draft.id?.takeIf { it.isNotBlank() } ?: "user-" + UUID.randomUUID().toString().take(8)
        val allIngredients = draft.ingredients.mapNotNull { line ->
            val ingredient = ingredientMap[line.ingredientId] ?: return@mapNotNull null
            RecipeIngredient(line.ingredientId, ingredient.name, line.amount, line.unit, line.note, line.optional)
        }
        val names = allIngredients.map { it.ingredientName }
        val flags = dietFlags(names)
        val categories = if (draft.categoryIds.isEmpty()) setOf("family") else draft.categoryIds
        val recipe = RecipeEntity(
            id = id,
            title = draft.title.trim(),
            titleEn = draft.titleEn.trim().ifBlank { null },
            description = draft.description.trim(),
            categoryIds = categories.joinToString("|"),
            tags = draft.tags.filter { it.isNotBlank() }.joinToString("|"),
            servings = draft.servings.coerceIn(1, 30),
            prepMinutes = draft.prepMinutes.coerceIn(0, 1440),
            cookMinutes = draft.cookMinutes.coerceIn(0, 1440),
            difficulty = draft.difficulty.name,
            calories = draft.calories,
            season = Season.ALL.name,
            budgetLevel = BudgetLevel.MEDIUM.name,
            heroStyle = "PEACH",
            isVegetarian = flags.vegetarian,
            isVegan = flags.vegan,
            isGlutenFree = flags.glutenFree,
            isDairyFree = flags.dairyFree,
            isFamilyFriendly = true,
            allergens = flags.allergens.joinToString("|"),
            tips = draft.tips.filter { it.isNotBlank() }.joinToString("\u001F"),
            searchText = PersianText.normalize(
                listOf(draft.title, draft.titleEn, draft.description, draft.tags.joinToString(" "), names.joinToString(" ")).joinToString(" ")
            ),
            isUserCreated = true,
            imageUri = draft.imageUri,
            editorialOrder = 5000,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.withTransaction {
            if (draft.id != null) {
                recipeDao.deleteIngredients(id)
                recipeDao.deleteSteps(id)
            }
            recipeDao.insertRecipe(recipe)
            recipeDao.insertIngredients(allIngredients.mapIndexed { index, line ->
                RecipeIngredientEntity(
                    recipeId = id, ingredientId = line.ingredientId, amount = line.amount,
                    unit = line.unit.name, note = line.note, optional = line.optional, sortOrder = index
                )
            })
            recipeDao.insertSteps(draft.steps.mapIndexed { index, step ->
                RecipeStepEntity(
                    recipeId = id, stepNumber = index + 1, title = step.title,
                    instruction = step.instruction, timerSeconds = step.timerMinutes?.times(60)?.takeIf { it > 0 }
                )
            })
            database.userRecipeDao().upsert(UserRecipeEntity(recipeId = id, updatedAt = System.currentTimeMillis()))
        }
        return getRecipe(id) ?: error("ذخیره دستور ناموفق بود")
    }

    suspend fun duplicateRecipe(recipeId: String): Recipe? {
        val source = getRecipe(recipeId) ?: return null
        val newId = "user-" + UUID.randomUUID().toString().take(8)
        val copy = RecipeEntity(
            id = newId,
            title = source.title + " (کپی)",
            titleEn = source.titleEn,
            description = source.description,
            categoryIds = source.categoryIds.toStoredList(),
            tags = source.tags.toStoredList(),
            servings = source.servings,
            prepMinutes = source.prepMinutes,
            cookMinutes = source.cookMinutes,
            difficulty = source.difficulty.name,
            calories = source.calories,
            season = source.season.name,
            budgetLevel = source.budgetLevel.name,
            heroStyle = source.heroStyle,
            isVegetarian = source.isVegetarian,
            isVegan = source.isVegan,
            isGlutenFree = source.isGlutenFree,
            isDairyFree = source.isDairyFree,
            isFamilyFriendly = source.isFamilyFriendly,
            allergens = source.allergens.toStoredList(),
            tips = source.tips.joinToString("\u001F"),
            searchText = PersianText.normalize(source.title + " " + source.description),
            isUserCreated = true,
            imageUri = source.imageUri,
            editorialOrder = 5000,
            region = source.region,
            history = source.history,
            resultDescription = source.resultDescription,
            restMinutes = source.restMinutes,
            equipment = source.equipment.toDetailString(),
            ovenTemperatureC = source.ovenTemperatureC,
            prepGuide = source.prepGuide.toDetailString(),
            servingGuide = source.servingGuide.toDetailString(),
            storageGuide = source.storageGuide,
            troubleshooting = source.troubleshooting.toDetailString(),
            sourceName = source.sourceName,
            sourceUrl = source.sourceUrl,
            license = source.license,
            validationStatus = source.validationStatus.name
        )
        database.withTransaction {
            recipeDao.insertRecipe(copy)
            recipeDao.insertIngredients(source.ingredients.mapIndexed { index, line ->
                RecipeIngredientEntity(
                    recipeId = newId, ingredientId = line.ingredientId, amount = line.amount,
                    unit = line.unit.name, note = line.note, optional = line.optional, sortOrder = index
                )
            })
            recipeDao.insertSteps(source.steps.mapIndexed { index, step ->
                RecipeStepEntity(
                    recipeId = newId, stepNumber = index + 1, title = step.title,
                    instruction = step.instruction, timerSeconds = step.timerSeconds,
                    ingredientRefs = step.ingredientRefs.toDetailString(),
                    equipment = step.equipment.toDetailString(),
                    temperatureC = step.temperatureC,
                    heatLevel = step.heatLevel,
                    successIndicators = step.successIndicators.toDetailString(),
                    commonMistakes = step.commonMistakes.toDetailString(),
                    recoveryInstructions = step.recoveryInstructions,
                    safetyNotes = step.safetyNotes.toDetailString()
                )
            })
            database.userRecipeDao().upsert(UserRecipeEntity(recipeId = newId))
        }
        return getRecipe(newId)
    }

    suspend fun getSubstituteRules(): List<SubstituteRule> = ingredientDao.getAllSubstitutes().map {
        SubstituteRule(it.ingredientId, it.substituteIngredientId, it.recipeId, it.ratio, it.note, it.contextTag)
    }

    suspend fun getCategoriesOnce(): List<Category> {
        return database.categoryDao().observeAll().first().map { it.toDomain() }
    }

    private data class DietFlags(
        val vegetarian: Boolean,
        val vegan: Boolean,
        val glutenFree: Boolean,
        val dairyFree: Boolean,
        val allergens: List<String>
    )

    private fun dietFlags(names: List<String>): DietFlags {
        val text = names.joinToString(" ")
        val meatWords = listOf("گوشت", "مرغ", "ماهی", "میگو", "بوقلمون", "سوسیس", "جگر", "تن ماهی", "ماهیچه")
        val dairyWords = listOf("شیر", "ماست", "کره", "پنیر", "خامه", "کشک", "دوغ")
        val glutenWords = listOf("آرد گندم", "نان", "ماکارونی", "اسپاگتی", "رشته", "جو", "بلغور", "سوخاری", "لازانیا", "پیتزا", "تورتیلا")
        val egg = names.any { it.contains("تخم‌مرغ") }
        val meat = meatWords.any { text.contains(it) }
        val dairy = dairyWords.any { text.contains(it) }
        val gluten = glutenWords.any { text.contains(it) }
        val nuts = names.any { name -> listOf("گردو", "بادام", "پسته", "فندق", "کنجد").any { name.contains(it) } }
        val allergens = buildList {
            if (gluten) add("gluten")
            if (dairy) add("dairy")
            if (egg) add("egg")
            if (nuts) add("nuts")
            if (names.any { it.contains("کنجد") }) add("sesame")
            if (names.any { it.contains("ماهی") || it.contains("میگو") }) add(if (text.contains("میگو")) "shellfish" else "fish")
            if (names.any { it.contains("سویا") }) add("soy")
            if (names.any { it.contains("کرفس") }) add("celery")
        }.distinct()
        return DietFlags(
            vegetarian = !meat,
            vegan = !meat && !dairy && !egg && !names.any { it.contains("عسل") },
            glutenFree = !gluten,
            dairyFree = !dairy,
            allergens = allergens
        )
    }
}
