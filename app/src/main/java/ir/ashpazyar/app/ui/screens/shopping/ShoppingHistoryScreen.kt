package ir.ashpazyar.app.ui.screens.shopping

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.ShoppingList
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingHistoryScreen(
    onBack: () -> Unit,
    viewModel: ShoppingHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AshpaTopBar(title = "تاریخچه خرید", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("خریدهای گذشته", style = MaterialTheme.typography.displaySmall, color = palette.ink)
            Text("برای مرور هزینه‌ها یا خرید دوباره همان اقلام", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            Spacer(Modifier.height(15.dp))
        }
        if (state.history.isEmpty() && !state.loading) {
            EmptyState(
                title = "تاریخچه‌ای نیست",
                description = "وقتی فهرست خریدی را بایگانی کنید، اینجا نگه داشته می‌شود.",
                icon = Icons.Rounded.History
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(state.history, key = { it.id }) { list ->
                    AshpaCard(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth(),
                        corner = 19.dp,
                        onClick = { viewModel.open(list) }
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(palette.sageSoft),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.ReceiptLong, null, tint = palette.forest, modifier = Modifier.size(21.dp)) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(list.title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
                                Text(
                                    JalaliCalendar.format(list.createdAt, withWeekday = false),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.muted
                                )
                            }
                            Text(
                                if (list.status == "ARCHIVED") "بایگانی" else "فعال",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.terracotta
                            )
                            Icon(Icons.Rounded.ChevronLeft, null, tint = palette.muted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    state.selected?.let { list ->
        ModalBottomSheet(
            onDismissRequest = viewModel::close,
            containerColor = palette.surface,
            contentColor = palette.ink
        ) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(list.title, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
                        Text(JalaliCalendar.format(list.createdAt), style = MaterialTheme.typography.labelSmall, color = palette.muted)
                    }
                    AshpaIconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, viewModel.shareText())
                        }
                        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
                    }) { Icon(Icons.Rounded.Share, "اشتراک", tint = palette.ink) }
                }
                Spacer(Modifier.height(10.dp))
                if (state.loadingItems) {
                    LoadingState(label = "در حال خواندن فهرست…")
                } else {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(state.items, key = { it.id }) { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (item.isChecked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    null,
                                    tint = if (item.isChecked) palette.success else palette.muted,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.ink,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    PersianText.faDigits(if (item.amount % 1.0 == 0.0) item.amount.toInt().toString() else String.format("%.1f", item.amount)) + " " + item.unit.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.muted
                                )
                            }
                        }
                    }
                    if (state.items.isEmpty()) {
                        Text("این فهرست خالی بوده است.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("جمع تقریبی", style = MaterialTheme.typography.labelMedium, color = palette.muted)
                        Spacer(Modifier.weight(1f))
                        Text(
                            PersianText.faDigits(state.selectedTotal.toLong().grouped()) + " تومان",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.ink
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun Long.grouped(): String = this.toString().reversed().chunked(3).joinToString(",").reversed()
