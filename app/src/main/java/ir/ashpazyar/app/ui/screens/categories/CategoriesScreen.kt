package ir.ashpazyar.app.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onSearch: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AshpaTopBar(
            title = "دسته‌بندی‌ها",
            onBack = onBack,
            action = {
                AshpaIconButton(onClick = onSearch) {
                    Icon(Icons.Rounded.Search, "جست‌وجو", tint = palette.ink)
                }
            }
        )
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("از کجا شروع کنیم؟", style = MaterialTheme.typography.displaySmall, color = palette.ink)
            Spacer(Modifier.height(5.dp))
            Text(
                "دسته‌ای را انتخاب کنید؛ از آشپزی ایرانی و یک‌قابلمه‌ای‌ها تا شیرینی، نوشیدنی و میز صبحانه.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted
            )
        }
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.categories, key = { it.category.id }) { item ->
                CategoryGridCard(item, onClick = { onOpenCategory(item.category.id) })
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(palette.forest)
                        .clickable(onClick = onSearch)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Rounded.Search, null, tint = palette.peach, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.height(30.dp))
                    Text("جست‌وجوی آزاد", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFFBF2))
                    Text("با نام غذا یا ماده اولیه", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD8E2D6))
                }
            }
            if (state.categories.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = "دسته‌بندی‌ها آماده نیست",
                        description = "داده‌های اولیه در حال بارگذاری‌اند. کمی بعد دوباره سر بزنید.",
                        icon = Icons.Rounded.GridView
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryGridCard(item: CategoryItem, onClick: () -> Unit) {
    val palette = AshpaTheme.palette
    val accent = item.sample?.heroStyle ?: "CREAM"
    val colors = when (accent.uppercase()) {
        "PEACH" -> listOf(Color(0xFFF4D3B7), Color(0xFFE5A97E))
        "TERRACOTTA" -> listOf(Color(0xFFD88A63), Color(0xFFB85A39))
        "SAGE" -> listOf(Color(0xFFD3E0C8), Color(0xFFA9C09B))
        "DARK" -> listOf(Color(0xFF304A39), Color(0xFF182A1F))
        else -> listOf(Color(0xFFF1E5CE), Color(0xFFDCC7A5))
    }
    val dark = accent.uppercase() == "DARK" || accent.uppercase() == "TERRACOTTA"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.category.icon.ifBlank { "🍲" }, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.weight(1f))
            Text(
                PersianText.faDigits(item.recipeCount.toString()) + " دستور",
                style = MaterialTheme.typography.labelSmall,
                color = if (dark) Color(0xFFFFEDD7) else palette.forest
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            item.category.title,
            style = MaterialTheme.typography.headlineSmall,
            color = if (dark) Color(0xFFFFFBF2) else palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(
            item.category.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = if (dark) Color(0xCCFFF9EE) else palette.muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
