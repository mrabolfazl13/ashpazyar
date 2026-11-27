package ir.ashpazyar.app.ui.screens.leftovers

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.Leftover
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeftoversScreen(
    onBack: () -> Unit,
    viewModel: LeftoversViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Leftover?>(null) }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                Column(Modifier.statusBarsPadding()) {
                    AshpaTopBar(
                        title = "باقی‌مانده‌ها",
                        onBack = onBack,
                        action = {
                            AshpaIconButton(onClick = { showAdd = true }) {
                                Icon(Icons.Rounded.Add, "افزودن", tint = palette.ink)
                            }
                        }
                    )
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text("غذا را هدر ندهیم", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                        Text(
                            "هر ظرف باقی‌مانده را ثبت کنید تا پیش از خراب شدن، یادتان بیندازیم.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted
                        )
                        Spacer(Modifier.height(15.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(palette.surface).padding(12.dp)) {
                                Column {
                                    Text(PersianText.faDigits(state.activeCount.toString()), style = MaterialTheme.typography.titleLarge, color = palette.ink)
                                    Text("باقی‌مانده فعال", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                                }
                            }
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (state.expiredCount > 0) palette.peach.copy(alpha = 0.45f) else palette.surface).padding(12.dp)) {
                                Column {
                                    Text(PersianText.faDigits(state.expiredCount.toString()), style = MaterialTheme.typography.titleLarge, color = if (state.expiredCount > 0) palette.terracotta else palette.ink)
                                    Text("از مهلت گذشته", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                                }
                            }
                        }
                        Spacer(Modifier.height(13.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("فعال", state.filter == LeftoverFilter.ACTIVE, onClick = { viewModel.setFilter(LeftoverFilter.ACTIVE) })
                            Pill("همه", state.filter == LeftoverFilter.ALL, onClick = { viewModel.setFilter(LeftoverFilter.ALL) })
                            Pill("مصرف‌شده", state.filter == LeftoverFilter.CONSUMED, onClick = { viewModel.setFilter(LeftoverFilter.CONSUMED) })
                            Spacer(Modifier.weight(1f))
                            Text(
                                "افزودن",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.terracotta,
                                modifier = Modifier.clickable { showAdd = true }.padding(7.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
            if (state.visible.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = if (state.filter == LeftoverFilter.CONSUMED) "چیزی مصرف نشده" else "باقی‌مانده‌ای ثبت نشده",
                        description = "بعد از پخت، مقدار اضافه را ثبت کنید تا با یادآوری به‌موقع، دور ریخته نشود.",
                        actionLabel = "ثبت باقی‌مانده",
                        onAction = { showAdd = true },
                        icon = Icons.Rounded.Eco
                    )
                }
            } else {
                items(state.visible, key = { it.id }) { item ->
                    LeftoverCard(
                        item = item,
                        onConsume = { viewModel.consume(item) },
                        onDelete = { pendingDelete = item },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
    }

    if (showAdd) {
        AddLeftoverSheet(
            onDismiss = { showAdd = false },
            onAdd = { title, portions, days, note ->
                viewModel.add(title, portions, days, note)
                showAdd = false
            }
        )
    }
    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "حذف باقی‌مانده",
            message = item.title + " از فهرست حذف می‌شود.",
            confirmLabel = "حذف",
            destructive = true,
            onConfirm = {
                viewModel.delete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun LeftoverCard(item: Leftover, onConsume: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    val expired = item.isExpired() && !item.isConsumed
    AshpaCard(modifier = modifier.fillMaxWidth(), corner = 20.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape)
                    .background(if (expired) palette.peach.copy(alpha = 0.5f) else palette.sageSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (expired) Icons.Rounded.WarningAmber else Icons.Rounded.Eco,
                    null,
                    tint = if (expired) palette.terracotta else palette.forest
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink,
                    textDecoration = if (item.isConsumed) TextDecoration.LineThrough else null
                )
                Text(
                    PersianText.faDigits(item.portions.toString()) + " پرس · پخته‌شده " + JalaliCalendar.format(item.preparedDate, withWeekday = false),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted
                )
                Text(
                    if (item.isConsumed) "مصرف شده" else if (expired) "مهلت گذشته؛ با احتیاط بررسی کنید" else "تا " + JalaliCalendar.format(item.suggestedUseBy, withWeekday = false),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isConsumed) palette.muted else if (expired) palette.terracotta else palette.success
                )
                if (item.note.isNotBlank()) {
                    Text(item.note, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                }
            }
            if (!item.isConsumed) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    "مصرف شد",
                    tint = palette.success,
                    modifier = Modifier.size(23.dp).clickable(onClick = onConsume)
                )
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                Icons.Rounded.DeleteOutline,
                "حذف",
                tint = palette.muted,
                modifier = Modifier.size(20.dp).clickable(onClick = onDelete)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLeftoverSheet(onDismiss: () -> Unit, onAdd: (String, Int, Int, String) -> Unit) {
    val palette = AshpaTheme.palette
    var title by remember { mutableStateOf("") }
    var portions by remember { mutableIntStateOf(2) }
    var days by remember { mutableIntStateOf(2) }
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("ثبت باقی‌مانده", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Spacer(Modifier.height(12.dp))
            AshpaTextField(title, { title = it }, "نام غذا", placeholder = "مثلاً خورش قورمه‌سبزی")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("تعداد پرس", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                Spacer(Modifier.weight(1f))
                QuantityStepper(portions, { portions = it }, min = 1, max = 12)
            }
            Spacer(Modifier.height(10.dp))
            Text("تا چند روز قابل مصرف است؟", style = MaterialTheme.typography.titleSmall, color = palette.ink)
            Spacer(Modifier.height(7.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf(1, 2, 3, 5, 7)) { d -> Pill(PersianText.faDigits(d.toString()) + " روز", days == d, onClick = { days = d }) }
            }
            Spacer(Modifier.height(10.dp))
            AshpaTextField(note, { note = it }, "یادداشت (اختیاری)", placeholder = "مثلاً بدون پیاز", singleLine = false, minLines = 2)
            Spacer(Modifier.height(14.dp))
            PrimaryButton("ثبت", onClick = { onAdd(title, portions, days, note) }, icon = Icons.Rounded.Add)
            Spacer(Modifier.height(12.dp))
        }
    }
}
