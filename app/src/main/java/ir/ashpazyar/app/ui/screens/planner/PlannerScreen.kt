package ir.ashpazyar.app.ui.screens.planner

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.MealPlanEntry
import ir.ashpazyar.app.domain.model.MealType
import ir.ashpazyar.app.domain.model.Recipe
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onOpenRecipe: (String) -> Unit,
    onOpenGenerator: () -> Unit,
    onOpenShopping: () -> Unit,
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    var showClear by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Pair<Long, MealType>?>(null) }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("برنامه غذایی", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                            Text("هفته‌ای بی‌دغدغه، با وعده‌های هماهنگ", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                        }
                        AshpaIconButton(onClick = onOpenShopping) {
                            Icon(Icons.Rounded.ShoppingBasket, "خرید", tint = palette.ink)
                        }
                        Spacer(Modifier.width(8.dp))
                        AshpaIconButton(onClick = onOpenGenerator) {
                            Icon(Icons.Rounded.AutoAwesome, "برنامه‌ساز", tint = palette.ink)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OverviewCard(
                        planned = state.plannedCount,
                        completed = state.completedCount,
                        onGenerate = onOpenGenerator
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AshpaIconButton(onClick = viewModel::previousWeek) {
                            Icon(Icons.Rounded.ChevronRight, "هفته قبل", tint = palette.ink)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            val start = JalaliCalendar.dateOfEpochDay(state.weekStart)
                            val end = start.plusDays(6)
                            Text(
                                JalaliCalendar.format(start) + " تا " + JalaliCalendar.short(JalaliCalendar.epochMillisOfEpochDay(state.weekStart + 6)),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.ink
                            )
                            Text(
                                if (state.weekStart == JalaliCalendar.startOfWeek().toEpochDay()) "هفته جاری" else "هفته انتخابی",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.terracotta
                            )
                        }
                        AshpaIconButton(onClick = viewModel::nextWeek) {
                            Icon(Icons.Rounded.ChevronLeft, "هفته بعد", tint = palette.ink)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    WeekDaySelector(
                        weekStart = state.weekStart,
                        selected = state.selectedDay,
                        onSelect = viewModel::selectDay
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            JalaliCalendar.relativeDayLabel(state.selectedDay),
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.ink
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            PersianText.faDigits(state.dayEntries(state.selectedDay).size.toString()) + " وعده",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.muted
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "پاک کردن هفته",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.danger,
                            modifier = Modifier.clickable { showClear = true }.padding(6.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(MealType.entries) { meal ->
                val entry = state.entryFor(state.selectedDay, meal)
                MealSlot(
                    meal = meal,
                    entry = entry,
                    onAdd = { picker = state.selectedDay to meal },
                    onOpen = { entry?.recipe?.let { onOpenRecipe(it.id) } },
                    onToggle = { entry?.let(viewModel::toggleCompleted) },
                    onRemove = { entry?.let(viewModel::remove) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }
            item {
                Spacer(Modifier.height(18.dp))
                InlineNotice(
                    "وعده‌ها را می‌توانید از کتاب دستورها یا برنامه‌ساز اضافه کنید. برنامه‌ساز با محدودیت‌های شما یک هفته کامل می‌چیند.",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
    }

    if (showClear) {
        ConfirmDialog(
            title = "پاک کردن برنامه هفته",
            message = "همه وعده‌های این هفته حذف می‌شوند. این کار قابل بازگشت نیست.",
            confirmLabel = "پاک کن",
            destructive = true,
            onConfirm = { viewModel.clearWeek(); showClear = false },
            onDismiss = { showClear = false }
        )
    }
    picker?.let { (day, meal) ->
        MealPickerSheet(
            recipes = state.recipes,
            meal = meal,
            onDismiss = { picker = null },
            onPick = { recipe ->
                viewModel.assign(day, meal, recipe)
                picker = null
            },
            onCustom = { title ->
                viewModel.assignCustom(day, meal, title)
                picker = null
            }
        )
    }
}

@Composable
private fun OverviewCard(planned: Int, completed: Int, onGenerate: () -> Unit) {
    val palette = AshpaTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.forest).padding(17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                PersianText.faDigits(planned.toString()) + " وعده این هفته",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFFBF2)
            )
            Text(
                PersianText.faDigits(completed.toString()) + " وعده انجام شده",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD6DFD1)
            )
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF))
                    .clickable(onClick = onGenerate).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFFF4C7A0), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("چیدن خودکار هفته", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFFBF2))
            }
        }
        Box(
            modifier = Modifier.size(62.dp).clip(CircleShape).background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center
        ) { Text("🗓", style = MaterialTheme.typography.headlineMedium) }
    }
}

@Composable
private fun MealSlot(
    meal: MealType,
    entry: MealPlanEntry?,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AshpaTheme.palette
    AshpaCard(modifier = modifier.fillMaxWidth(), corner = 20.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    meal.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry != null) palette.terracotta else palette.muted
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier.size(7.dp).clip(CircleShape)
                        .background(if (entry != null) palette.terracotta else palette.line)
                )
            }
            if (entry == null) {
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).clickable(onClick = onAdd).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, null, tint = palette.muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("افزودن وعده", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                }
            } else {
                FoodArt(entry.recipe?.heroStyle ?: "CREAM", entry.title.hashCode(), Modifier.size(58.dp), corner = 14.dp)
                Spacer(Modifier.width(10.dp))
                Column(
                    Modifier.weight(1f).clickable(onClick = onOpen)
                ) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.ink,
                        textDecoration = if (entry.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        PersianText.faDigits(entry.servings.toString()) + " نفر" + if (entry.isCompleted) " · پخته شد" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted
                    )
                }
                Icon(
                    if (entry.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    "وضعیت",
                    tint = if (entry.isCompleted) palette.success else palette.muted,
                    modifier = Modifier.size(22.dp).clickable(onClick = onToggle)
                )
                Text(
                    "×",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.muted,
                    modifier = Modifier.clickable(onClick = onRemove).padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealPickerSheet(
    recipes: List<Recipe>,
    meal: MealType,
    onDismiss: () -> Unit,
    onPick: (Recipe) -> Unit,
    onCustom: (String) -> Unit
) {
    val palette = AshpaTheme.palette
    var query by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf("") }
    val filtered = remember(query, recipes) {
        if (query.isBlank()) recipes.take(40)
        else recipes.filter { it.title.contains(query, ignoreCase = true) || it.ingredients.any { line -> line.ingredientName.contains(query) } }.take(40)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("افزودن " + meal.label, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Spacer(Modifier.height(11.dp))
            AshpaSearchField(query, { query = it }, "جست‌وجوی دستور…")
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(filtered, key = { it.id }) { recipe ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(recipe) }.padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FoodArt(recipe.heroStyle, recipe.id.hashCode(), Modifier.size(44.dp), corner = 11.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(recipe.title, style = MaterialTheme.typography.titleSmall, color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                PersianText.faDigits(recipe.totalMinutes.toString()) + " دقیقه · " + recipe.difficulty.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.muted
                            )
                        }
                        Icon(Icons.Rounded.AddCircle, null, tint = palette.terracotta, modifier = Modifier.size(19.dp))
                    }
                    HorizontalDivider(color = palette.line.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("یا یک وعده دلخواه بنویسید", style = MaterialTheme.typography.labelMedium, color = palette.muted)
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    AshpaTextField(custom, { custom = it }, "نام وعده", placeholder = "مثلاً شام مهمان")
                }
                Spacer(Modifier.width(8.dp))
                AshpaIconButton(onClick = { if (custom.isNotBlank()) onCustom(custom) }, filled = true) {
                    Icon(Icons.Rounded.Check, "ثبت", tint = palette.peach)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
