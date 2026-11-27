package ir.ashpazyar.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.ashpazyar.app.data.local.AshpazyarDatabase
import ir.ashpazyar.app.data.local.entity.*
import ir.ashpazyar.app.data.toDomain
import ir.ashpazyar.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class BackupResult(
    val recipes: Int,
    val inventoryItems: Int,
    val mealPlanItems: Int,
    val shoppingItems: Int,
    val leftovers: Int
)

class BackupException(message: String) : Exception(message)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AshpazyarDatabase,
    private val recipeRepository: RecipeRepository,
    private val kitchenRepository: KitchenRepository,
    private val preferencesRepository: PreferencesRepository,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun exportTo(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val userRecipes = recipeRepository.userRecipes.first()
        val inventory = kitchenRepository.inventory.first()
        val mealRows = database.mealPlanDao().getAll()
        val activeList = database.shoppingDao().getActiveList()
        val shoppingRows = activeList?.let { database.shoppingDao().getItems(it.id) }.orEmpty()
        val leftovers = database.leftoverDao().getAll()
        val favorites = database.favoriteDao().getIds()
        val prices = database.ingredientDao().getPrices()
        val preferences = preferencesRepository.get()
        val settings = settingsRepository.settings.first()

        val envelope = BackupEnvelope(
            exportedAt = System.currentTimeMillis(),
            userRecipes = userRecipes.map { recipe ->
                BackupRecipe(
                    id = recipe.id,
                    title = recipe.title,
                    description = recipe.description,
                    servings = recipe.servings,
                    prepMinutes = recipe.prepMinutes,
                    cookMinutes = recipe.cookMinutes,
                    difficulty = recipe.difficulty.name,
                    categoryIds = recipe.categoryIds,
                    tags = recipe.tags,
                    ingredients = recipe.ingredients.map {
                        BackupRecipeIngredient(it.ingredientId, it.amount, it.unit.name, it.note, it.optional)
                    },
                    steps = recipe.steps.map { BackupStep(it.title, it.instruction, it.timerSeconds) },
                    tips = recipe.tips
                )
            },
            inventory = inventory.map {
                BackupInventory(
                    ingredientId = it.ingredientId,
                    amount = it.amount,
                    unit = it.unit.name,
                    location = it.location.name,
                    expiryDate = it.expiryDate,
                    note = it.note,
                    pricePaid = it.pricePaid
                )
            },
            mealPlan = mealRows.map {
                BackupMeal(it.planDate, it.mealType, it.recipeId, it.customTitle, it.servings)
            },
            shoppingItems = shoppingRows.map {
                BackupShoppingItem(
                    name = it.name,
                    ingredientId = it.ingredientId,
                    category = it.category,
                    amount = it.amount,
                    unit = it.unit,
                    estimatedPrice = it.estimatedPrice,
                    actualPrice = it.actualPrice,
                    isChecked = it.isChecked
                )
            },
            preferences = BackupPreferences(
                vegetarian = preferences.vegetarian,
                noMeat = preferences.noMeat,
                lowCost = preferences.lowCost,
                quickMeals = preferences.quickMeals,
                lowSalt = preferences.lowSalt,
                dairyFree = preferences.dairyFree,
                glutenFree = preferences.glutenFree,
                dislikedIngredients = preferences.dislikedIngredients,
                allergies = preferences.allergies
            ),
            favorites = favorites,
            prices = prices.map { BackupPrice(it.ingredientId, it.pricePerKg) },
            leftovers = leftovers.map {
                BackupLeftover(it.title, it.recipeId, it.preparedDate, it.portions, it.suggestedUseBy, it.note, it.isConsumed)
            },
            settings = BackupSettings(
                themeMode = settings.themeMode,
                expirationWarningDays = settings.expirationWarningDays,
                familySize = settings.familySize,
                weeklyBudget = settings.weeklyBudget,
                mealReminders = settings.mealReminders,
                expiryNotifications = settings.expiryNotifications
            )
        )
        val content = json.encodeToString(BackupEnvelope.serializer(), envelope)
        val output = context.contentResolver.openOutputStream(uri)
            ?: throw BackupException("نمی‌توان فایل پشتیبان را نوشت")
        output.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        BackupResult(
            recipes = envelope.userRecipes.size,
            inventoryItems = envelope.inventory.size,
            mealPlanItems = envelope.mealPlan.size,
            shoppingItems = envelope.shoppingItems.size,
            leftovers = envelope.leftovers.size
        )
    }

    suspend fun importFrom(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            if (bytes.size > 15 * 1024 * 1024) throw BackupException("فایل پشتیبان بیش از حد بزرگ است")
            bytes.toString(Charsets.UTF_8)
        } ?: throw BackupException("فایل پشتیبان خوانده نشد")
        val envelope = try {
            json.decodeFromString(BackupEnvelope.serializer(), text)
        } catch (error: SerializationException) {
            throw BackupException("ساختار فایل پشتیبان معتبر نیست")
        } catch (error: IllegalArgumentException) {
            throw BackupException("ساختار فایل پشتیبان معتبر نیست")
        }
        if (envelope.format != "ashpazyar-backup" || envelope.version !in 1..1) {
            throw BackupException("این فایل پشتیبان با نسخه فعلی آشپزیار سازگار نیست")
        }
        val ingredients = database.ingredientDao().getAll().associateBy { it.id }
        val allRecipeIds = database.recipeDao().getAllWithDetails()
            .filter { !it.recipe.isUserCreated }.map { it.recipe.id }.toSet()

        database.withTransaction {
            database.recipeDao().clearUserRecipes()
            database.favoriteDao().clear()
        }

        var importedRecipes = 0
        for (backup in envelope.userRecipes) {
            if (backup.title.isBlank() || backup.ingredients.isEmpty()) continue
            val draft = RecipeDraft(
                id = backup.id.takeIf { it.startsWith("user-") },
                title = backup.title,
                description = backup.description,
                categoryIds = backup.categoryIds.toSet(),
                tags = backup.tags,
                servings = backup.servings,
                prepMinutes = backup.prepMinutes,
                cookMinutes = backup.cookMinutes,
                difficulty = Difficulty.from(backup.difficulty),
                ingredients = backup.ingredients.filter { ingredients.containsKey(it.ingredientId) }.map {
                    DraftIngredient(
                        ingredientId = it.ingredientId,
                        ingredientName = ingredients[it.ingredientId]?.name.orEmpty(),
                        amount = it.amount,
                        unit = IngredientUnit.from(it.unit),
                        note = it.note,
                        optional = it.optional
                    )
                },
                steps = backup.steps.map { DraftStep(it.title, it.instruction, it.timerSeconds?.div(60)) },
                tips = backup.tips
            )
            if (draft.ingredients.isEmpty()) continue
            recipeRepository.saveUserRecipe(draft)
            importedRecipes++
        }

        val userRecipeIds = database.recipeDao().getAllWithDetails()
            .filter { it.recipe.isUserCreated }.map { it.recipe.id }.toSet()
        val validRecipeIds = allRecipeIds + userRecipeIds

        database.withTransaction {
            database.kitchenDao().clearAllInventory()
            for (item in envelope.inventory) {
                if (!ingredients.containsKey(item.ingredientId)) continue
                if (item.amount <= 0) continue
                database.kitchenDao().insertInventory(
                    InventoryEntity(
                        ingredientId = item.ingredientId,
                        amount = item.amount,
                        unit = IngredientUnit.from(item.unit).name,
                        location = StorageLocation.from(item.location).name,
                        expiryDate = item.expiryDate,
                        note = item.note,
                        pricePaid = item.pricePaid
                    )
                )
            }

            database.mealPlanDao().clearAll()
            for (meal in envelope.mealPlan) {
                database.mealPlanDao().upsert(
                    MealPlanItemEntity(
                        planDate = meal.date,
                        mealType = MealType.from(meal.mealType).name,
                        recipeId = meal.recipeId?.takeIf { it in validRecipeIds },
                        customTitle = meal.customTitle,
                        servings = meal.servings.coerceIn(1, 30)
                    )
                )
            }

            database.shoppingDao().clearAllItems()
            val activeList = database.shoppingDao().getActiveList()
                ?: ShoppingListEntity(title = "فهرست خرید خانه").let {
                    val id = database.shoppingDao().insertList(it)
                    ShoppingListEntity(id = id, title = it.title)
                }
            for (item in envelope.shoppingItems) {
                database.shoppingDao().insertItem(
                    ShoppingListItemEntity(
                        shoppingListId = activeList.id,
                        ingredientId = item.ingredientId?.takeIf { ingredients.containsKey(it) },
                        name = item.name,
                        category = item.category.ifBlank { "سایر" },
                        amount = item.amount.coerceAtLeast(0.0),
                        unit = IngredientUnit.from(item.unit).name,
                        estimatedPrice = item.estimatedPrice,
                        actualPrice = item.actualPrice,
                        isChecked = item.isChecked
                    )
                )
            }

            database.leftoverDao().clearAll()
            for (item in envelope.leftovers) {
                database.leftoverDao().insert(
                    FoodLeftoverEntity(
                        title = item.title,
                        recipeId = item.recipeId?.takeIf { it in validRecipeIds },
                        preparedDate = item.preparedDate,
                        portions = item.portions.coerceAtLeast(1),
                        suggestedUseBy = item.suggestedUseBy,
                        note = item.note,
                        isConsumed = item.isConsumed
                    )
                )
            }

            for (favorite in envelope.favorites) {
                if (favorite in validRecipeIds) database.favoriteDao().add(FavoriteRecipeEntity(favorite))
            }
            for (price in envelope.prices) {
                if (ingredients.containsKey(price.ingredientId)) {
                    database.ingredientDao().upsertPrice(
                        IngredientPriceEntity(price.ingredientId, price.pricePerKg)
                    )
                }
            }
        }

        preferencesRepository.save(
            FoodPreferences(
                vegetarian = envelope.preferences.vegetarian,
                noMeat = envelope.preferences.noMeat,
                lowCost = envelope.preferences.lowCost,
                quickMeals = envelope.preferences.quickMeals,
                lowSalt = envelope.preferences.lowSalt,
                dairyFree = envelope.preferences.dairyFree,
                glutenFree = envelope.preferences.glutenFree,
                dislikedIngredients = envelope.preferences.dislikedIngredients,
                allergies = envelope.preferences.allergies
            )
        )
        settingsRepository.setThemeMode(envelope.settings.themeMode)
        settingsRepository.setWarningDays(envelope.settings.expirationWarningDays)
        settingsRepository.setFamilySize(envelope.settings.familySize)
        settingsRepository.setWeeklyBudget(envelope.settings.weeklyBudget)
        settingsRepository.setMealReminders(envelope.settings.mealReminders)
        settingsRepository.setExpiryNotifications(envelope.settings.expiryNotifications)

        BackupResult(
            recipes = importedRecipes,
            inventoryItems = envelope.inventory.size,
            mealPlanItems = envelope.mealPlan.size,
            shoppingItems = envelope.shoppingItems.size,
            leftovers = envelope.leftovers.size
        )
    }

    fun suggestedFileName(): String {
        val date = ir.ashpazyar.app.core.util.JalaliCalendar.today()
        return "ashpazyar-backup-" + date.year + "-" + date.month.toString().padStart(2, '0') + "-" + date.day.toString().padStart(2, '0') + ".json"
    }
}
