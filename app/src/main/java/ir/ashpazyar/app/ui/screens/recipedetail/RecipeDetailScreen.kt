package ir.ashpazyar.app.ui.screens.recipedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.ClockFormat
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.engine.RecipeScaler
import ir.ashpazyar.app.domain.model.*
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onStartCooking: (String) -> Unit,
    onEditRecipe: (String) -> Unit,
    onDeleted: () -> Unit,
    onOpenCategory: (String) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    var showPlanSheet by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event.deleted) {
                onDeleted()
            } else {
                val result = snackbar.showSnackbar(
                    message = event.message,
                    actionLabel = if (event.undoGroupId != null) "بازگرداندن" else null,
                    withDismissAction = false,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed && event.undoGroupId != null) {
                    viewModel.undoConsume(event.undoGroupId)
                }
            }
        }
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingState() }
        return
    }
    val recipe = state.recipe
    if (recipe == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AshpaTopBar(title = "دستور", onBack = onBack)
            EmptyState("دستور پیدا نشد", "ممکن است حذف شده باشد.", icon = Icons.Rounded.MenuBook)
        }
        return
    }
    val scaled = RecipeScaler.scale(recipe, state.servings)
    val missingIds = state.match?.missing?.map { it.ingredientId }?.toSet().orEmpty()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                item {
                    Box(Modifier.fillMaxWidth().height(330.dp)) {
                        FoodArt(
                            style = recipe.heroStyle,
                            seed = recipe.id.hashCode(),
                            modifier = Modifier.fillMaxSize(),
                            corner = 0.dp
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    listOf(Color(0x99000000), Color.Transparent, Color(0xCC14271D)),
                                    startY = 0f
                                )
                            )
                        )
                        Row(
                            modifier = Modifier.statusBarsPadding().padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AshpaIconButton(onClick = onBack, filled = false) {
                                Icon(Icons.Rounded.ArrowForward, "بازگشت", tint = palette.ink)
                            }
                            Spacer(Modifier.weight(1f))
                            AshpaIconButton(onClick = viewModel::toggleFavorite) {
                                Icon(
                                    if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    "محبوب",
                                    tint = if (state.isFavorite) palette.terracotta else palette.ink
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box {
                                AshpaIconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Rounded.MoreHoriz, "بیشتر", tint = palette.ink)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    containerColor = palette.surface
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("کپی دستور") },
                                        onClick = { showMenu = false; viewModel.duplicateRecipe() }
                                    )
                                    if (recipe.isUserCreated) {
                                        DropdownMenuItem(
                                            text = { Text("ویرایش دستور") },
                                            onClick = { showMenu = false; onEditRecipe(recipe.id) }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("مواد مصرف شد") },
                                        onClick = { showMenu = false; viewModel.consumeIngredients() }
                                    )
                                    if (recipe.isUserCreated) {
                                        DropdownMenuItem(
                                            text = { Text("حذف دستور", color = palette.danger) },
                                            onClick = { showMenu = false; showDelete = true }
                                        )
                                    }
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                recipe.categoryIds.take(3).forEach { category ->
                                    Text(
                                        text = categoryLabel(category),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFF2DF),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0x554F6B57))
                                            .clickable { onOpenCategory(category) }
                                            .padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(recipe.title, style = MaterialTheme.typography.displaySmall, color = Color(0xFFFFFBF2))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                recipe.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE8DCC9),
                                maxLines = 2
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetaChip(Icons.Rounded.Schedule, ClockFormat.duration(recipe.totalMinutes))
                        MetaChip(Icons.Rounded.LocalFireDepartment, recipe.difficulty.label)
                        MetaChip(Icons.Rounded.People, PersianText.faDigits(recipe.servings.toString()) + " نفر")
                        if (recipe.calories != null) {
                            MetaChip(Icons.Rounded.Bolt, PersianText.faDigits(recipe.calories.toString()) + " کالری")
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionHeader(
                                title = "مواد لازم",
                                caption = if (state.match?.fullyAvailable == true) "همه مواد اصلی موجود است" else "برای " + PersianText.faDigits(state.servings.toString()) + " نفر",
                                modifier = Modifier.weight(1f)
                            )
                            QuantityStepper(
                                value = state.servings,
                                onChange = viewModel::setServings,
                                min = 1,
                                max = 20
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        if (state.match != null && !state.match!!.fullyAvailable) {
                            InlineNotice(
                                "با موجودی فعلی، " + PersianText.faDigits(state.match!!.matchPercent.toString()) + "٪ مواد را دارید. مواد کم را می‌توانید یک‌جا به فهرست خرید بفرستید.",
                                tone = "warm"
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        AshpaCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 15.dp, vertical = 6.dp)) {
                                scaled.forEachIndexed { index, item ->
                                    val line = item.source
                                    val missing = line.ingredientId in missingIds
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(8.dp).clip(CircleShape)
                                                .background(
                                                    when {
                                                        line.optional -> palette.muted.copy(alpha = 0.4f)
                                                        missing -> palette.terracotta
                                                        else -> palette.success
                                                    }
                                                )
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                line.ingredientName + if (line.optional) " (اختیاری)" else "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.ink
                                            )
                                            if (line.note.isNotBlank()) {
                                                Text(line.note, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                                            }
                                        }
                                        Text(
                                            item.amountLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (missing) palette.terracotta else palette.ink,
                                            textDecoration = if (missing) TextDecoration.None else TextDecoration.None
                                        )
                                    }
                                    if (index != scaled.lastIndex) HorizontalDivider(color = palette.line.copy(alpha = 0.55f))
                                }
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.match?.missing?.isNotEmpty() == true) {
                                SecondaryButton(
                                    text = "افزودن مواد کم به خرید",
                                    onClick = viewModel::addMissingToShopping,
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Rounded.AddShoppingCart
                                )
                            }
                            SecondaryButton(
                                text = "مصرف از آشپزخانه",
                                onClick = viewModel::consumeIngredients,
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Kitchen
                            )
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(26.dp))
                    SectionHeader(
                        title = "مراحل پخت",
                        caption = "گام‌به‌گام، بدون عجله",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }
                itemsIndexed(recipe.steps, key = { _, step -> step.number }) { index, step ->
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(palette.sageSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                PersianText.faDigits((index + 1).toString()),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.forest
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f).padding(top = 3.dp)) {
                            if (step.title.isNotBlank()) {
                                Text(step.title, style = MaterialTheme.typography.titleMedium, color = palette.ink)
                            }
                            Text(step.instruction, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                            if (step.timerSeconds != null) {
                                Spacer(Modifier.height(5.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Timer, null, tint = palette.terracotta, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        ClockFormat.duration(step.timerSeconds / 60),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.terracotta
                                    )
                                }
                            }
                        }
                    }
                }
                if (recipe.tips.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(22.dp))
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            SectionHeader(title = "نکته‌های آشپز", caption = "برای نتیجه بهتر")
                            Spacer(Modifier.height(8.dp))
                            recipe.tips.forEach { tip ->
                                Row(Modifier.padding(vertical = 5.dp)) {
                                    Text("•", color = palette.terracotta)
                                    Spacer(Modifier.width(7.dp))
                                    Text(tip, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                                }
                            }
                        }
                    }
                }
                if (recipe.allergens.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(18.dp))
                        InlineNotice(
                            "این دستور شامل: " + recipe.allergens.joinToString("، ") { allergenLabel(it) } + " است.",
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
            Surface(color = palette.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AshpaIconButton(onClick = { showPlanSheet = true }, filled = true) {
                        Icon(Icons.Rounded.CalendarMonth, "افزودن به برنامه", tint = palette.peach)
                    }
                    PrimaryButton(
                        text = "شروع پخت",
                        onClick = { onStartCooking(recipe.id) },
                        icon = Icons.Rounded.PlayArrow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 86.dp))
    }

    if (showPlanSheet) {
        AddToPlanSheet(
            onDismiss = { showPlanSheet = false },
            onPick = { date, meal ->
                viewModel.addToPlan(date, meal)
                showPlanSheet = false
            }
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "حذف دستور",
            message = "این دستور برای همیشه حذف می‌شود. موجودی و سابقه مصرف دست‌نخورده می‌ماند.",
            confirmLabel = "حذف کن",
            destructive = true,
            onConfirm = {
                showDelete = false
                viewModel.deleteRecipe()
            },
            onDismiss = { showDelete = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlanSheet(onDismiss: () -> Unit, onPick: (Long, MealType) -> Unit) {
    val palette = AshpaTheme.palette
    val today = JalaliCalendar.todayEpochDay()
    var selectedDay by remember { mutableStateOf(today) }
    var meal by remember { mutableStateOf(MealType.DINNER) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.ink
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("افزودن به برنامه غذایی", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Spacer(Modifier.height(5.dp))
            Text("روز و وعده را انتخاب کنید", style = MaterialTheme.typography.bodySmall, color = palette.muted)
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(7) { offset ->
                    val day = today + offset
                    Pill(
                        JalaliCalendar.relativeDayLabel(day),
                        selected = selectedDay == day,
                        onClick = { selectedDay = day }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealType.entries.filter { it != MealType.SNACK }.forEach { type ->
                    Pill(type.label, selected = meal == type, onClick = { meal = type })
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton("افزودن به برنامه", onClick = { onPick(selectedDay, meal) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun categoryLabel(id: String): String = when (id) {
    "iranian" -> "ایرانی"
    "quick" -> "سریع"
    "economical" -> "اقتصادی"
    "vegetarian" -> "گیاهی"
    "international" -> "جهانی"
    "stew" -> "خورش"
    "rice" -> "برنج"
    "kabab" -> "کباب"
    "breakfast" -> "صبحانه"
    "appetizer" -> "پیش‌غذا"
    "salad" -> "سالاد"
    "soup" -> "سوپ"
    "dessert" -> "دسر"
    "drink" -> "نوشیدنی"
    "family" -> "خانوادگی"
    else -> id
}

private fun allergenLabel(key: String): String = when (key.lowercase()) {
    "gluten" -> "گلوتن"
    "dairy" -> "لبنیات"
    "egg" -> "تخم‌مرغ"
    "nuts" -> "خشکبار"
    "sesame" -> "کنجد"
    "fish" -> "ماهی"
    "shellfish" -> "صدف"
    "soy" -> "سویا"
    "celery" -> "کرفس"
    else -> key
}
