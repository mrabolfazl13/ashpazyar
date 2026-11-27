package ir.ashpazyar.app.ui.screens.shopping

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.ShoppingItem
import ir.ashpazyar.app.domain.model.Unit
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    onOpenHistory: () -> Unit,
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }
    var showArchive by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("فهرست خرید", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                            Text("چیزی از قلم نیفتد", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                        }
                        AshpaIconButton(onClick = onOpenHistory) {
                            Icon(Icons.Rounded.History, "تاریخچه", tint = palette.ink)
                        }
                        Spacer(Modifier.width(8.dp))
                        AshpaIconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, viewModel.shareText())
                            }
                            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری فهرست"))
                        }) {
                            Icon(Icons.Rounded.Share, "اشتراک‌گذاری", tint = palette.ink)
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    ShoppingHero(state)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(
                            "افزودن از برنامه هفته",
                            onClick = viewModel::addFromThisWeek,
                            icon = Icons.Rounded.CalendarMonth,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            "کالای دستی",
                            onClick = { showAdd = true },
                            icon = Icons.Rounded.Add,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Pill("همه", state.filter == ShoppingFilter.ALL, onClick = { viewModel.setFilter(ShoppingFilter.ALL) }) }
                    item { Pill("باقی‌مانده", state.filter == ShoppingFilter.REMAINING, onClick = { viewModel.setFilter(ShoppingFilter.REMAINING) }) }
                    item { Pill("خریداری‌شده", state.filter == ShoppingFilter.DONE, onClick = { viewModel.setFilter(ShoppingFilter.DONE) }) }
                }
            }
            if (state.items.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = "فهرست خرید خالی است",
                        description = "از برنامه هفته، دستورها یا مواد کم یک دستور، فهرست را بسازید. می‌توانید کالای دستی هم اضافه کنید.",
                        actionLabel = "کالای دستی",
                        onAction = { showAdd = true },
                        icon = Icons.Rounded.ShoppingBasket
                    )
                }
            } else {
                state.groups.forEach { group ->
                    item(key = "header-" + group.category) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(group.category, style = MaterialTheme.typography.titleMedium, color = palette.ink)
                            Spacer(Modifier.weight(1f))
                            Text(
                                PersianText.faDigits(group.items.size.toString()) + " قلم",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.muted
                            )
                        }
                    }
                    items(group.items, key = { it.id }) { item ->
                        ShoppingRow(
                            item = item,
                            onToggle = { viewModel.toggle(item) },
                            onClick = { editing = item },
                            onDelete = { viewModel.delete(item) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 3.dp)
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(18.dp))
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        SecondaryButton(
                            "پایان خرید و بایگانی فهرست",
                            onClick = { showArchive = true },
                            icon = Icons.Rounded.Archive
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "با بایگانی، این فهرست در تاریخچه می‌ماند و فهرست تازه‌ای شروع می‌شود.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.muted
                        )
                    }
                }
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
    }

    if (showAdd) {
        AddShoppingItemSheet(
            onDismiss = { showAdd = false },
            onAdd = { name, amount, unit, category ->
                viewModel.addManual(name, amount, unit, category)
                showAdd = false
            }
        )
    }
    editing?.let { item ->
        PriceSheet(
            item = item,
            onDismiss = { editing = null },
            onSave = { text ->
                viewModel.setPrice(item, text)
                editing = null
            }
        )
    }
    if (showArchive) {
        ConfirmDialog(
            title = "پایان خرید",
            message = "این فهرست بایگانی می‌شود و یک فهرست تازه شروع می‌شود. می‌توانید بعداً از تاریخچه ببینید.",
            confirmLabel = "بایگانی کن",
            onConfirm = { viewModel.archive(); showArchive = false },
            onDismiss = { showArchive = false }
        )
    }
}

@Composable
private fun ShoppingHero(state: ShoppingUiState) {
    val palette = AshpaTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.forest).padding(17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                PersianText.faDigits(state.checkedCount.toString()) + " از " + PersianText.faDigits(state.totalCount.toString()),
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFFFBF2)
            )
            Text("قلم خریداری شده", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD6DFD1))
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (state.totalCount == 0) 0f else state.checkedCount.toFloat() / state.totalCount },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                color = Color(0xFFE0A578),
                trackColor = Color(0x33FFFFFF)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("برآورد", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD6DFD1))
            Text(
                PersianText.faDigits(state.estimatedTotal.toLong().grouped()) + " ت",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFFBF2)
            )
            if (state.actualTotal > 0) {
                Text(
                    "خرید " + PersianText.faDigits(state.actualTotal.toLong().grouped()) + " ت",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE6B88C)
                )
            }
        }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AshpaTheme.palette
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.surface).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape)
                .background(if (item.isChecked) palette.success else Color.Transparent)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (item.isChecked) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
                "وضعیت",
                tint = if (item.isChecked) Color.White else palette.muted,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f).clickable(onClick = onClick)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.isChecked) palette.muted else palette.ink,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                PersianText.faDigits(if (item.amount % 1.0 == 0.0) item.amount.toInt().toString() else String.format("%.1f", item.amount)) + " " + item.unit.label +
                    if (item.actualPrice != null) " · " + PersianText.faDigits(item.actualPrice.toLong().grouped()) + " ت" else "",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted
            )
        }
        Icon(
            Icons.Rounded.Close,
            "حذف",
            tint = palette.muted,
            modifier = Modifier.size(17.dp).clickable(onClick = onDelete)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddShoppingItemSheet(onDismiss: () -> Unit, onAdd: (String, String, Unit, String) -> Unit) {
    val palette = AshpaTheme.palette
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(Unit.PIECE) }
    var category by remember { mutableStateOf("سایر") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("کالای دستی", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Spacer(Modifier.height(12.dp))
            AshpaTextField(name, { name = it }, "نام کالا", placeholder = "مثلاً زعفران")
            Spacer(Modifier.height(9.dp))
            AshpaTextField(amount, { amount = it }, "مقدار", placeholder = "مثلاً ۱۰", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf(Unit.PIECE, Unit.GRAM, Unit.KILOGRAM, Unit.LITER, Unit.MILLILITER, Unit.BUNCH, Unit.CAN)) { u ->
                    Pill(u.label, unit == u, onClick = { unit = u })
                }
            }
            Spacer(Modifier.height(9.dp))
            AshpaTextField(category, { category = it }, "دسته (اختیاری)", placeholder = "مثلاً ادویه")
            Spacer(Modifier.height(14.dp))
            PrimaryButton("افزودن", onClick = { onAdd(name, amount, unit, category) }, icon = Icons.Rounded.Add)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceSheet(item: ShoppingItem, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val palette = AshpaTheme.palette
    var price by remember(item.id) { mutableStateOf(item.actualPrice?.toLong()?.toString().orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(item.name, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Text("قیمت واقعی پرداختی را ثبت کنید تا هزینه خانه دقیق‌تر شود.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
            Spacer(Modifier.height(12.dp))
            AshpaTextField(price, { price = it }, "قیمت (تومان)", placeholder = "اختیاری", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(13.dp))
            PrimaryButton("ثبت قیمت", onClick = { onSave(price) }, icon = Icons.Rounded.Payments)
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun Long.grouped(): String = this.toString().reversed().chunked(3).joinToString(",").reversed()
