package ir.ashpazyar.app.ui.screens.tips

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
import ir.ashpazyar.app.domain.model.CookingTip
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun TipsScreen(
    onBack: () -> Unit,
    onOpenTip: (String) -> Unit,
    viewModel: TipsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette

    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            Column(Modifier.statusBarsPadding()) {
                AshpaTopBar(title = "نکته‌های آشپزی", onBack = onBack)
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("دانش آشپزخانه", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                    Text(
                        "نکته‌های کوتاه و کاربردی برای پخت بهتر، نگهداری درست و آشپزخانه امن‌تر.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.muted
                    )
                    Spacer(Modifier.height(14.dp))
                    AshpaSearchField(state.query, viewModel::setQuery, "جست‌وجوی نکته…")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        item { Pill("همه", state.selectedCategory == null, onClick = { viewModel.setCategory(null) }) }
                        items(state.categories) { category ->
                            Pill(category, state.selectedCategory == category, onClick = { viewModel.setCategory(category) })
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                }
            }
        }
        if (state.visible.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    title = "نکته‌ای پیدا نشد",
                    description = "عبارت دیگری را جست‌وجو کنید یا دسته را تغییر دهید.",
                    icon = Icons.Rounded.Lightbulb
                )
            }
        } else {
            if (state.selectedCategory == null && state.query.isBlank() && state.visible.isNotEmpty()) {
                item {
                    val featured = state.visible.first()
                    FeaturedTip(featured, onClick = { onOpenTip(featured.id) })
                }
            }
            items(state.visible, key = { it.id }) { tip ->
                TipCard(tip, onClick = { onOpenTip(tip.id) }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp))
            }
        }
    }
}

@Composable
private fun FeaturedTip(tip: CookingTip, onClick: () -> Unit) {
    val palette = AshpaTheme.palette
    Box(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp).fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)).background(palette.forest).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF335544)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Lightbulb, null, tint = Color(0xFFE6B88C), modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "نکته برگزیده · " + tip.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD6DFD1)
                )
                Spacer(Modifier.height(3.dp))
                Text(tip.title, style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFFBF2))
                Text(
                    tip.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD6DFD1),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Rounded.ChevronLeft, null, tint = Color(0xFFE6B88C))
        }
    }
}

@Composable
private fun TipCard(tip: CookingTip, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    AshpaCard(modifier = modifier.fillMaxWidth(), corner = 20.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(palette.sageSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (tip.safety) Icons.Rounded.HealthAndSafety else Icons.Rounded.Lightbulb,
                    null,
                    tint = palette.forest,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(tip.title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
                Text(tip.summary, style = MaterialTheme.typography.labelSmall, color = palette.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(tip.category, style = MaterialTheme.typography.labelSmall, color = palette.terracotta)
            }
            Icon(Icons.Rounded.ChevronLeft, null, tint = palette.muted, modifier = Modifier.size(18.dp))
        }
    }
}
