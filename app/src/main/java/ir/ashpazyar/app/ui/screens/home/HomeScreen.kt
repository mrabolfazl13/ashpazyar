package ir.ashpazyar.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.ClockFormat
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.MealType
import ir.ashpazyar.app.domain.model.Recommendation
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.navigation.Routes
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun HomeScreen(
    onOpenRecipe: (String) -> Unit,
    onOpenRecipes: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenSmart: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenShopping: () -> Unit,
    onOpenTimers: () -> Unit,
    onOpenFavorites: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val context = LocalContext.current
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted = it
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(palette.forest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ق", style = MaterialTheme.typography.headlineMedium, color = palette.peach)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("قاشق", style = MaterialTheme.typography.titleLarge, color = palette.ink)
                        Text("همراه روزهای آشپزی شما", style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                    AshpaIconButton(onClick = onOpenTimers) {
                        Icon(Icons.Rounded.Timer, "تایمرها", tint = palette.ink)
                    }
                    Spacer(Modifier.width(8.dp))
                    AshpaIconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Tune, "تنظیمات", tint = palette.ink)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(state.greeting, style = MaterialTheme.typography.headlineLarge, color = palette.ink)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WbSunny, null, tint = palette.terracotta, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(state.dateLabel, style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    Text(" • ", color = palette.muted)
                    Text(state.seasonLabel, style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.surface)
                        .clickable(onClick = onOpenSearch)
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = palette.muted, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("جست‌وجوی دستور، ماده اولیه، طعم…", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                }
                if (!notificationsGranted && Build.VERSION.SDK_INT >= 33) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.peach.copy(alpha = 0.42f))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = palette.terracotta, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "برای یادآوری تایمرها و وعده‌ها اجازه اعلان بدهید",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "فعال‌سازی",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.terracotta,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                                .padding(6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeStat("دستور", state.recipeCount, Icons.Rounded.MenuBook, Modifier.weight(1f))
                    HomeStat("در آشپزخانه", state.pantryCount, Icons.Rounded.Kitchen, Modifier.weight(1f))
                    HomeStat(
                        "نیاز به بررسی",
                        state.expiringCount,
                        Icons.Rounded.HourglassBottom,
                        Modifier.weight(1f),
                        warning = state.expiringCount > 0
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = if (state.todayMeals.isEmpty()) "امروز چه بپزیم؟" else "برنامه امروز",
                    caption = if (state.todayMeals.isEmpty()) "پیشنهادهای متناسب با مواد آشپزخانه شما" else "وعده‌های امروز را ببینید",
                    modifier = Modifier.weight(1f)
                )
                if (state.todayMeals.isEmpty()) {
                    Text(
                        "برنامه هفته",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.terracotta,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.peach.copy(alpha = 0.35f))
                            .clickable(onClick = onOpenPlanner)
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
            }
        }

        if (state.todayMeals.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    state.todayMeals.forEach { meal ->
                        TodayMealRow(meal, onClick = {
                            meal.recipe?.let { onOpenRecipe(it.id) } ?: onOpenPlanner()
                        })
                    }
                }
            }
        } else if (state.recommendations.isNotEmpty()) {
            item {
                Spacer(Modifier.height(10.dp))
                FeaturedRecommendation(
                    recommendation = state.recommendations.first(),
                    isFavorite = state.recommendations.first().recipe.id in state.favorites,
                    onOpen = { onOpenRecipe(state.recommendations.first().recipe.id) }
                )
            }
            if (state.recommendations.size > 1) {
                item {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.recommendations.drop(1), key = { it.recipe.id }) { recommendation ->
                            val recipe = recommendation.recipe
                            Column(
                                modifier = Modifier
                                    .width(214.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(palette.surface)
                                    .clickable { onOpenRecipe(recipe.id) }
                            ) {
                                FoodArt(
                                    style = recipe.heroStyle,
                                    seed = recipe.id.hashCode(),
                                    modifier = Modifier.fillMaxWidth().height(92.dp),
                                    corner = 20.dp
                                )
                                Column(Modifier.padding(11.dp)) {
                                    Text(recipe.title, style = MaterialTheme.typography.titleSmall, color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(3.dp))
                                    Text(recommendation.reasons.firstOrNull() ?: "پیشنهاد قاشق", style = MaterialTheme.typography.bodySmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader(
                title = "سریع و بی‌دردسر",
                caption = "برای روزهایی که وقت کم است",
                actionLabel = "همه دستورها",
                onAction = onOpenRecipes,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.quickRecipes, key = { it.id }) { recipe ->
                    CompactRecipeCard(
                        recipe = recipe,
                        onClick = { onOpenRecipe(recipe.id) },
                        isFavorite = recipe.id in state.favorites
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            SmartKitchenBanner(
                expiring = state.expiringCount,
                pantryCount = state.pantryCount,
                onClick = onOpenSmart,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            Spacer(Modifier.height(26.dp))
            SectionHeader(
                title = "دسته‌بندی‌ها",
                caption = "از آشپزی ایرانی تا شیرینی و نوشیدنی",
                actionLabel = "همه",
                onAction = onOpenCategories,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.categories, key = { it.id }) { category ->
                    CategoryTile(category, onClick = { onOpenCategory(category.id) })
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction("فهرست خرید", "خرید هوشمند", Icons.Rounded.ShoppingBasket, onOpenShopping, Modifier.weight(1f))
                QuickAction("محبوب‌ها", "دستورهای نشان‌شده", Icons.Rounded.Favorite, onOpenFavorites, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HomeStat(label: String, value: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, warning: Boolean = false) {
    val palette = AshpaTheme.palette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Icon(icon, null, tint = if (warning) palette.terracotta else palette.sage, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(9.dp))
        Text(
            PersianText.faDigits(value.toString()),
            style = MaterialTheme.typography.headlineSmall,
            color = palette.ink
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.muted, maxLines = 1)
    }
}

@Composable
private fun FeaturedRecommendation(recommendation: Recommendation, isFavorite: Boolean, onOpen: () -> Unit) {
    val palette = AshpaTheme.palette
    val recipe = recommendation.recipe
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(270.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onOpen)
    ) {
        FoodArt(style = recipe.heroStyle, seed = recipe.id.hashCode(), modifier = Modifier.fillMaxSize(), corner = 28.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x22000000), Color(0xDD14271D)),
                        startY = 120f
                    )
                )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp).fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "پیشنهاد قاشق",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFE6CA),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x663C5A45))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                )
                Spacer(Modifier.width(8.dp))
                if (recommendation.match?.fullyAvailable == true) {
                    Text(
                        "همه مواد موجود",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD8E9C8)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(recipe.title, style = MaterialTheme.typography.headlineMedium, color = Color(0xFFFFF9EE))
            Spacer(Modifier.height(5.dp))
            Text(
                ClockFormat.duration(recipe.totalMinutes) + " • " + recipe.difficulty.label + " • " + recipe.servings.toString() + " نفر",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE8DCC9)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                recipe.tags.take(3).forEach { tag ->
                    Text(
                        tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFF2DF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x334F6B57))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
        if (isFavorite) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).size(34.dp).clip(CircleShape).background(Color(0xCCFFF9EE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Favorite, null, tint = palette.terracotta, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TodayMealRow(meal: ir.ashpazyar.app.domain.model.MealPlanEntry, onClick: () -> Unit) {
    val palette = AshpaTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        meal.recipe?.let {
            FoodArt(it.heroStyle, it.id.hashCode(), Modifier.size(58.dp), corner = 14.dp)
        } ?: Box(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(palette.sageSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Restaurant, null, tint = palette.forest)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when (meal.mealType) {
                    MealType.BREAKFAST -> "صبحانه"
                    MealType.LUNCH -> "ناهار"
                    MealType.DINNER -> "شام"
                    MealType.SNACK -> "میان‌وعده"
                },
                style = MaterialTheme.typography.labelSmall,
                color = palette.terracotta
            )
            Text(meal.title, style = MaterialTheme.typography.titleMedium, color = palette.ink, maxLines = 1)
        }
        Icon(Icons.Rounded.ChevronLeft, null, tint = palette.muted)
    }
}

@Composable
private fun SmartKitchenBanner(expiring: Int, pantryCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.forest)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Kitchen, null, tint = Color(0xFFFFE6CA), modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text("آشپزخانه هوشمند", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFF9EE))
            Text(
                if (expiring > 0) PersianText.faDigits(expiring.toString()) + " ماده نیاز به بررسی دارد" else "با مواد موجود، دستور مناسب پیدا کنید",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD6DFD1)
            )
        }
        Icon(Icons.Rounded.ArrowBack, null, tint = palette.peach)
    }
}

@Composable
private fun CategoryTile(category: ir.ashpazyar.app.domain.model.Category, onClick: () -> Unit) {
    val palette = AshpaTheme.palette
    Column(
        modifier = Modifier
            .width(126.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .clickable(onClick = onClick)
            .padding(13.dp)
    ) {
        Text(category.icon.ifBlank { "🍲" }, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(category.title, style = MaterialTheme.typography.titleSmall, color = palette.ink, maxLines = 1)
        Text(category.subtitle, style = MaterialTheme.typography.labelSmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .clickable(onClick = onClick)
            .padding(15.dp)
    ) {
        Icon(icon, null, tint = palette.terracotta, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = palette.muted)
    }
}
