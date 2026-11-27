package ir.ashpazyar.app.ui.screens.cost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.repository.CostRepository
import ir.ashpazyar.app.data.repository.PriceEntry
import ir.ashpazyar.app.data.repository.RecipeRepository
import ir.ashpazyar.app.domain.model.CostBreakdown
import ir.ashpazyar.app.domain.model.Recipe
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CostUiState(
    val loading: Boolean = true,
    val prices: List<PriceEntry> = emptyList(),
    val filtered: List<PriceEntry> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipe: Recipe? = null,
    val servings: Int = 4,
    val breakdown: CostBreakdown? = null,
    val query: String = "",
    val knownPriceCount: Int = 0
)

@HiltViewModel
class CostViewModel @Inject constructor(
    private val costRepository: CostRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedId = MutableStateFlow<String?>(null)
    private val servings = MutableStateFlow(4)
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private data class Base(
        val prices: List<PriceEntry>,
        val recipes: List<Recipe>,
        val selectedId: String?,
        val servings: Int,
        val query: String
    )

    val uiState: StateFlow<CostUiState> = combine(
        costRepository.prices,
        recipeRepository.recipes,
        combine(selectedId, servings, query) { id, s, q -> Triple(id, s, q) }
    ) { prices, recipes, (id, s, q) ->
        Base(prices, recipes, id, s, q)
    }.mapLatest { base ->
        val selected = base.recipes.firstOrNull { it.id == base.selectedId }
        val breakdown = if (selected != null) {
            runCatching {
                costRepository.calculate(selected, base.servings, recipeRepository.getIngredientMap())
            }.getOrNull()
        } else null
        val tokens = PersianText.normalize(base.query).split(" ").filter { it.isNotBlank() }
        val filtered = base.prices.filter { entry ->
            tokens.isEmpty() || tokens.all {
                PersianText.normalize(entry.ingredient.name + " " + entry.ingredient.categoryTitle).contains(it)
            }
        }.sortedWith(compareByDescending<PriceEntry> { it.pricePerKg != null }.thenBy { it.ingredient.name })
        CostUiState(
            loading = false,
            prices = base.prices,
            filtered = filtered,
            recipes = base.recipes.sortedBy { it.editorialOrder },
            selectedRecipe = selected,
            servings = base.servings,
            breakdown = breakdown,
            query = base.query,
            knownPriceCount = base.prices.count { it.pricePerKg != null }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CostUiState())

    fun setQuery(value: String) { query.value = value }
    fun selectRecipe(id: String) { selectedId.value = id }
    fun setServings(value: Int) { servings.value = value.coerceIn(1, 30) }

    fun savePrice(entry: PriceEntry, text: String) {
        val value = PersianText.toLatinDigits(text).replace("٫", ".").replace(",", "").trim().toDoubleOrNull()
        if (text.isNotBlank() && (value == null || value < 0)) {
            viewModelScope.launch { _events.emit("قیمت معتبر وارد کنید") }
            return
        }
        viewModelScope.launch {
            runCatching { costRepository.setPrice(entry.ingredient.id, value?.takeIf { it > 0 }) }
                .onSuccess { _events.emit("قیمت " + entry.ingredient.name + " ذخیره شد") }
                .onFailure { _events.emit("ذخیره قیمت ناموفق بود") }
        }
    }
}
