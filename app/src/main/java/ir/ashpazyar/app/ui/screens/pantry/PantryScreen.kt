package ir.ashpazyar.app.ui.screens.pantry

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
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.*
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun PantryScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpenShopping: () -> Unit,
    onOpenSmart: () -> Unit,
    viewModel: PantryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<InventoryItem?>(null) }
    var deleteTarget by remember { mutableStateOf<InventoryItem?>(null) }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                Column(Modifier.statusBarsPadding()) {
                    AshpaTopBar(
                        title = "انبار آشپزخانه",
                        onBack = onBack,
                        action = {
                            AshpaIconButton(onClick = onAdd) {
                                Icon(Icons.Rounded.Add, "افزودن", tint = palette.ink)
                            }
                        }
                    )
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text("چه داریم؟", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                        Text(
                            "موجودی را دقیق نگه دارید تا آشپزیار دقیق‌تر پیشنهاد بدهد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted
                        )
                        Spacer(Modifier.height(15.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            StatBox("موجود", PersianText.faDigits(state.items.size.toString()), Modifier.weight(1f))
                            StatBox("در خطر", PersianText.faDigits(state.riskCount.toString()), Modifier.weight(1f), palette.terracotta)
                            StatBox(
                                "ارزش",
                                PersianText.faDigits(state.totalValue.toLong().grouped()) + " ت",
                                Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        AshpaSearchField(
                            value = state.query,
                            onValueChange = viewModel::setQuery,
                            placeholder = "جست‌وجو در مواد موجود…"
                        )
                        Spacer(Modifier.height(11.dp))
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Pill("همه", state.selectedLocation == null, onClick = { viewModel.setLocation(null) }) }
                        items(state.locations, key = { it.name }) { loc ->
                            Pill(loc.label, state.selectedLocation == loc, onClick = { viewModel.setLocation(loc) })
                        }
                    }
                    Spacer(Modifier.height(11.dp))
                    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("خرید", onClick = onOpenShopping, icon = Icons.Rounded.ShoppingBasket, modifier = Modifier.weight(1f))
                        SecondaryButton("پیشنهاد پخت", onClick = onOpenSmart, icon = Icons.Rounded.AutoAwesome, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(13.dp))
                }
            }
            if (state.filtered.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = if (state.items.isEmpty()) "انبار خالی است" else "چیزی با این جست‌وجو پیدا نشد",
                        description = if (state.items.isEmpty()) "موادی که در خانه دارید را اضافه کنید تا پیشنهادهای واقعی‌تر بسازیم." else "فیلترها یا عبارت جست‌وجو را تغییر دهید.",
                        actionLabel = if (state.items.isEmpty()) "افزودن ماده" else null,
                        onAction = if (state.items.isEmpty()) onAdd else null,
                        icon = Icons.Rounded.Kitchen
                    )
                }
            } else {
                items(state.filtered, key = { it.id }) { item ->
                    PantryRow(
                        item = item,
                        warningDays = state.warningDays,
                        onClick = { editing = item },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
    }

    editing?.let { item ->
        EditInventorySheet(
            item = item,
            onDismiss = { editing = null },
            onSave = { amount, unit ->
                viewModel.updateAmount(item, amount, unit)
                editing = null
            },
            onDelete = {
                editing = null
                deleteTarget = item
            }
        )
    }
    deleteTarget?.let { item ->
        ConfirmDialog(
            title = "حذف از انبار",
            message = item.displayName + " از موجودی حذف می‌شود. سابقه مصرف باقی می‌ماند.",
            confirmLabel = "حذف",
            destructive = true,
            onConfirm = {
                viewModel.delete(item)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    val palette = AshpaTheme.palette
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(palette.surface).padding(12.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor ?: palette.ink)
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.muted)
    }
}

@Composable
private fun PantryRow(item: InventoryItem, warningDays: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    val status = item.status(warningDays = warningDays)
    val statusColor = when (status) {
        InventoryStatus.EXPIRED -> palette.danger
        InventoryStatus.NEAR_EXPIRY -> palette.terracotta
        InventoryStatus.FRESH -> palette.success
        InventoryStatus.NO_DATE -> palette.muted
    }
    AshpaCard(modifier = modifier.fillMaxWidth(), corner = 19.dp, onClick = onClick) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(palette.sageSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(item.displayName.take(1), style = MaterialTheme.typography.titleLarge, color = palette.forest)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(item.displayName, style = MaterialTheme.typography.titleMedium, color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.location.label, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                    Text(" · ", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                    Text(
                        PersianText.faDigits(formatAmount(item.amount)) + " " + item.unit.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted
                    )
                    if (item.expiryDate != null) {
                        Text(" · ", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                        Text(
                            JalaliCalendar.format(item.expiryDate, withWeekday = false),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
                if (item.note.isNotBlank()) {
                    Text(item.note, style = MaterialTheme.typography.labelSmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(status.label, style = MaterialTheme.typography.labelSmall, color = statusColor)
                Spacer(Modifier.height(4.dp))
                Icon(Icons.Rounded.ChevronLeft, null, tint = palette.muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditInventorySheet(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onSave: (String, IngredientUnit) -> Unit,
    onDelete: () -> Unit
) {
    val palette = AshpaTheme.palette
    var amount by remember(item.id) { mutableStateOf(formatAmountRaw(item.amount)) }
    var unit by remember(item.id) { mutableStateOf(item.unit) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(item.displayName, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Text("ویرایش موجودی", style = MaterialTheme.typography.bodySmall, color = palette.muted)
            Spacer(Modifier.height(14.dp))
            AshpaTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "مقدار",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            )
            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf(IngredientUnit.GRAM, IngredientUnit.KILOGRAM, IngredientUnit.PIECE, IngredientUnit.MILLILITER, IngredientUnit.LITER, IngredientUnit.CUP, IngredientUnit.TABLESPOON, IngredientUnit.CLOVE, IngredientUnit.BUNCH)) { u ->
                    Pill(u.label, unit == u, onClick = { unit = u })
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("ذخیره تغییرات", onClick = { onSave(amount, unit) })
            Spacer(Modifier.height(6.dp))
            Text(
                "حذف از انبار",
                style = MaterialTheme.typography.labelMedium,
                color = palette.danger,
                modifier = Modifier.clickable(onClick = onDelete).padding(vertical = 12.dp)
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun formatAmount(value: Double): String =
    PersianText.faDigits(if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value))

private fun formatAmountRaw(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)

private fun Long.grouped(): String {
    val text = this.toString()
    return text.reversed().chunked(3).joinToString(",").reversed()
}
