package ir.ashpazyar.app.ui.screens.smart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.MatchResult
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun SmartKitchenScreen(
    onOpenRecipe: (String) -> Unit,
    onOpenPantry: () -> Unit,
    onOpenShopping: () -> Unit,
    onOpenTimers: () -> Unit,
    viewModel: SmartKitchenViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("آشپزخانه هوشمند", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                            Text("با آنچه دارید، بهترین گزینه را پیدا کنید", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                        }
                        AshpaIconButton(onClick = onOpenTimers) {
                            Icon(Icons.Rounded.Timer, "تایمرها", tint = palette.ink)
                        }
                        Spacer(Modifier.width(8.dp))
                        AshpaIconButton(onClick = onOpenPantry) {
                            Icon(Icons.Rounded.Kitchen, "انبار", tint = palette.ink)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.forest).padding(17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                            Text("🍳", style = MaterialTheme.typography.headlineMedium)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                PersianText.faDigits(state.cookableCount.toString()) + " دستور آماده پخت",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFFFFBF2)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "از " + PersianText.faDigits(state.totalRecipes.toString()) + " دستور، با " + PersianText.faDigits(state.pantryCount.toString()) + " ماده در آشپزخانه",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6DFD1)
                            )
                        }
                    }
                    if (state.expiringCount > 0) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(palette.peach.copy(alpha = 0.4f))
                                .clickable(onClick = onOpenPantry)
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.HourglassBottom, null, tint = palette.terracotta, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                PersianText.faDigits(state.expiringCount.toString()) + " ماده نزدیک انقضا یا منقضی شده است",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Rounded.ChevronLeft, null, tint = palette.terracotta)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    AshpaSearchField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        placeholder = "جست‌وجو در دستورهای قابل پخت…"
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Pill("همه", state.filter == KitchenFilter.ALL, onClick = { viewModel.setFilter(KitchenFilter.ALL) }) }
                    item { Pill("همه مواد موجود", state.filter == KitchenFilter.FULL, onClick = { viewModel.setFilter(KitchenFilter.FULL) }) }
                    item { Pill("حداکثر ۱ کم", state.filter == KitchenFilter.ONE_MISSING, onClick = { viewModel.setFilter(KitchenFilter.ONE_MISSING) }) }
                    item { Pill("حداکثر ۳ کم", state.filter == KitchenFilter.FEW_MISSING, onClick = { viewModel.setFilter(KitchenFilter.FEW_MISSING) }) }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 13.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(PersianText.faDigits(state.matches.size.toString()) + " پیشنهاد", style = MaterialTheme.typography.labelMedium, color = palette.muted)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "بر اساس مواد اصلی",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.terracotta
                    )
                }
            }
            if (state.matches.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = "با این فیلتر چیزی پیدا نشد",
                        description = "فیلتر را بازتر کنید یا چند ماده به انبار اضافه کنید تا پیشنهادهای تازه بسازیم.",
                        actionLabel = "مدیریت انبار",
                        onAction = onOpenPantry,
                        icon = Icons.Rounded.Kitchen
                    )
                }
            } else {
                items(state.matches, key = { it.recipe.id }) { match ->
                    MatchCard(
                        match = match,
                        isFavorite = match.recipe.id in state.favorites,
                        onOpen = { onOpenRecipe(match.recipe.id) },
                        onAddMissing = { viewModel.addMissingToShopping(match.recipe.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp))
    }
}

@Composable
private fun MatchCard(
    match: MatchResult,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onAddMissing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AshpaTheme.palette
    val recipe = match.recipe
    AshpaCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.padding(11.dp)) {
            FoodArt(recipe.heroStyle, recipe.id.hashCode(), Modifier.size(width = 104.dp, height = 126.dp), corner = 17.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(recipe.title, style = MaterialTheme.typography.titleMedium, color = palette.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (isFavorite) Icon(Icons.Rounded.Favorite, null, tint = palette.terracotta, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (match.fullyAvailable) "همه مواد موجود" else PersianText.faDigits(match.missingCount.toString()) + " ماده کم",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (match.fullyAvailable) palette.success else palette.terracotta
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        PersianText.faDigits(match.matchPercent.toString()) + "٪",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (match.fullyAvailable) palette.success else palette.ink
                    )
                }
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { match.matchPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                    color = if (match.fullyAvailable) palette.success else palette.terracotta,
                    trackColor = palette.surfaceHigh
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    match.missing.take(2).joinToString("، ") { it.name }.ifBlank { "برای پخت آماده است" },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (match.missing.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(palette.sageSoft)
                            .clickable(onClick = onAddMissing)
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.AddShoppingCart, null, tint = palette.forest, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("مواد کم به خرید", style = MaterialTheme.typography.labelSmall, color = palette.forest)
                    }
                }
            }
        }
    }
}
