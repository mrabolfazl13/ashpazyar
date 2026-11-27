package ir.ashpazyar.app.ui.screens.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun RecipesScreen(
    onOpenRecipe: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    initialCategoryId: String? = null,
    categoryTitle: String? = null,
    viewModel: RecipesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    LaunchedEffect(initialCategoryId) {
        if (initialCategoryId != null) viewModel.setCategory(initialCategoryId)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(10.dp))
                if (onBack != null) {
                    AshpaTopBar(title = categoryTitle ?: "دستورها", onBack = onBack)
                } else {
                    ScreenHeader(
                        eyebrow = "کتاب آشپزی شما",
                        title = categoryTitle ?: "دستورها",
                        subtitle = if (state.allCount > 0) PersianText.faDigits(state.allCount.toString()) + " دستور آزموده‌شده، برای هر روز و هر ذائقه" else "در حال چیدن کتاب آشپزی…"
                    )
                }
                Spacer(Modifier.height(14.dp))
                AshpaSearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "جست‌وجو در نام، مواد یا طعم…"
                )
                Spacer(Modifier.height(13.dp))
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Pill("همه", selected = state.categoryId == null, onClick = { viewModel.setCategory(null) })
                }
                items(state.categories, key = { it.id }) { category ->
                    Pill(
                        category.title,
                        selected = state.categoryId == category.id,
                        onClick = { viewModel.setCategory(category.id) }
                    )
                }
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Pill("سریع", state.onlyQuick, onClick = viewModel::toggleQuick) }
                item { Pill("گیاهی", state.onlyVegetarian, onClick = viewModel::toggleVegetarian) }
                item { Pill("اقتصادی", state.onlyEconomical, onClick = viewModel::toggleEconomical) }
                item { Pill("خانوادگی", state.onlyFamily, onClick = viewModel::toggleFamily) }
                val active = state.onlyQuick || state.onlyVegetarian || state.onlyEconomical || state.onlyFamily || state.categoryId != null || state.query.isNotBlank()
                if (active) {
                    item {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(palette.surfaceHigh)
                                .clickable(onClick = viewModel::clearFilters)
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Close, null, tint = palette.muted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("پاک کردن", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    PersianText.faDigits(state.recipes.size.toString()) + " دستور",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.muted
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "مرتب‌شده برای شما",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.terracotta
                )
            }
        }
        if (state.recipes.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    title = "دستوری پیدا نشد",
                    description = "فیلترها را ساده‌تر کنید یا نام دیگری جست‌وجو کنید. شاید وقت آن رسیده یک دستور تازه بنویسید.",
                    actionLabel = "پاک کردن فیلترها",
                    onAction = viewModel::clearFilters,
                    icon = Icons.Rounded.MenuBook
                )
            }
        } else {
            items(state.recipes, key = { it.id }) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    isFavorite = recipe.id in state.favorites,
                    onClick = { onOpenRecipe(recipe.id) },
                    onFavoriteClick = { viewModel.toggleFavorite(recipe.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }
}
