package ir.ashpazyar.app.ui.screens.recipeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.Difficulty
import ir.ashpazyar.app.domain.model.Ingredient
import ir.ashpazyar.app.domain.model.IngredientUnit
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RecipeEditorScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: RecipeEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    var showIngredientPicker by remember { mutableStateOf(false) }
    var unitLineKey by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event.startsWith("saved:")) onSaved(event.removePrefix("saved:"))
            else snackbar.showSnackbar(event)
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AshpaTopBar(
                title = if (state.editingId == null) "دستور تازه" else "ویرایش دستور",
                onBack = onBack,
                action = {
                    Text(
                        "ذخیره",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (state.saving) palette.muted else palette.terracotta,
                        modifier = Modifier.clickable(enabled = !state.saving) { viewModel.save() }.padding(9.dp)
                    )
                }
            )
            if (state.loading) {
                LoadingState()
                return@Column
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text("از آشپزخانه خودتان بنویسید", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                Text(
                    "نام، مواد و مرحله‌ها را وارد کنید. دستور بعد از ذخیره در فهرست دستورها و دستورهای من دیده می‌شود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted
                )
                Spacer(Modifier.height(15.dp))
                EditorSection(title = "اطلاعات اصلی", icon = Icons.Rounded.EditNote) {
                    AshpaTextField(state.title, viewModel::setTitle, "نام دستور", placeholder = "مثلاً خورش بادمجان خانگی")
                    Spacer(Modifier.height(10.dp))
                    AshpaTextField(
                        state.description,
                        viewModel::setDescription,
                        "توضیح کوتاه",
                        placeholder = "طعم، بافت یا خاطره‌ای از این غذا…",
                        singleLine = false,
                        minLines = 2
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("تعداد پرس", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                        Spacer(Modifier.weight(1f))
                        QuantityStepper(state.servings, viewModel::setServings, min = 1, max = 30)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.weight(1f)) {
                            AshpaTextField(
                                PersianText.faDigits(state.prepMinutes.toString()),
                                { viewModel.setPrep(PersianText.toLatinDigits(it).toIntOrNull() ?: 0) },
                                "آماده‌سازی (دقیقه)",
                                keyboardType = KeyboardType.Number
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            AshpaTextField(
                                PersianText.faDigits(state.cookMinutes.toString()),
                                { viewModel.setCook(PersianText.toLatinDigits(it).toIntOrNull() ?: 0) },
                                "پخت (دقیقه)",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("سطح سختی", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Difficulty.entries.forEach { difficulty ->
                            Pill(difficulty.label, state.difficulty == difficulty, onClick = { viewModel.setDifficulty(difficulty) })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                EditorSection(title = "دسته‌بندی", icon = Icons.Rounded.Category) {
                    Text(
                        "حداقل یک دسته انتخاب کنید تا دستور در جای درست پیدا شود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        state.categories.forEach { category ->
                            Pill(
                                category.title,
                                category.id in state.categoryIds,
                                onClick = { viewModel.toggleCategory(category.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                EditorSection(title = "مواد اولیه", icon = Icons.Rounded.Kitchen) {
                    if (state.lines.isEmpty()) {
                        Text("هنوز ماده‌ای اضافه نشده است.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                    state.lines.forEachIndexed { index, line ->
                        EditorIngredientRow(
                            number = index + 1,
                            line = line,
                            onAmount = { value -> viewModel.updateLine(line.key) { it.copy(amount = value) } },
                            onNote = { value -> viewModel.updateLine(line.key) { it.copy(note = value) } },
                            onOptional = { value -> viewModel.updateLine(line.key) { it.copy(optional = value) } },
                            onUnit = { unitLineKey = line.key },
                            onRemove = { viewModel.removeLine(line.key) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    SecondaryButton("افزودن ماده اولیه", onClick = { showIngredientPicker = true }, icon = Icons.Rounded.Add)
                }
                Spacer(Modifier.height(12.dp))
                EditorSection(title = "مراحل پخت", icon = Icons.Rounded.FormatListNumbered) {
                    state.steps.forEachIndexed { index, step ->
                        EditorStepRow(
                            number = index + 1,
                            step = step,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.steps.lastIndex,
                            onTitle = { value -> viewModel.updateStep(step.key) { it.copy(title = value) } },
                            onInstruction = { value -> viewModel.updateStep(step.key) { it.copy(instruction = value) } },
                            onTimer = { value -> viewModel.updateStep(step.key) { it.copy(timer = value) } },
                            onUp = { viewModel.moveStep(step.key, true) },
                            onDown = { viewModel.moveStep(step.key, false) },
                            onRemove = { viewModel.removeStep(step.key) }
                        )
                        Spacer(Modifier.height(9.dp))
                    }
                    SecondaryButton("افزودن مرحله", onClick = viewModel::addStep, icon = Icons.Rounded.Add)
                }
                Spacer(Modifier.height(12.dp))
                EditorSection(title = "نکته‌های پخت", icon = Icons.Rounded.Lightbulb) {
                    if (state.tips.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            state.tips.forEachIndexed { index, tip ->
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(palette.surfaceHigh)
                                        .padding(start = 12.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tip, style = MaterialTheme.typography.labelSmall, color = palette.ink)
                                    Spacer(Modifier.width(5.dp))
                                    Icon(
                                        Icons.Rounded.Close,
                                        "حذف",
                                        tint = palette.muted,
                                        modifier = Modifier.size(14.dp).clickable { viewModel.removeTip(index) }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            AshpaTextField(state.newTip, viewModel::setNewTip, "نکته تازه", placeholder = "مثلاً حرارت را کم نگه دارید")
                        }
                        Spacer(Modifier.width(8.dp))
                        AshpaIconButton(onClick = viewModel::addTip, filled = true) {
                            Icon(Icons.Rounded.Add, "افزودن نکته", tint = palette.peach)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    if (state.editingId == null) "ذخیره دستور" else "ذخیره تغییرات",
                    onClick = viewModel::save,
                    icon = Icons.Rounded.Save,
                    loading = state.saving
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "این دستور فقط روی دستگاه شما ذخیره می‌شود و در فهرست عمومی آشپزیار قرار نمی‌گیرد.",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted
                )
                Spacer(Modifier.height(28.dp))
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }

    if (showIngredientPicker) {
        IngredientEditorPicker(
            ingredients = state.ingredientOptions,
            onDismiss = { showIngredientPicker = false },
            onPick = {
                viewModel.addIngredient(it)
                showIngredientPicker = false
            }
        )
    }
    unitLineKey?.let { key ->
        val line = state.lines.firstOrNull { it.key == key }
        if (line != null) {
            UnitEditorPicker(
                selected = line.unit,
                onDismiss = { unitLineKey = null },
                onPick = { unit ->
                    viewModel.updateLine(key) { it.copy(unit = unit) }
                    unitLineKey = null
                }
            )
        }
    }
}

@Composable
private fun EditorSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = AshpaTheme.palette
    AshpaCard(Modifier.fillMaxWidth(), corner = 22.dp) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = palette.terracotta, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = palette.ink)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EditorIngredientRow(
    number: Int,
    line: EditorLine,
    onAmount: (String) -> Unit,
    onNote: (String) -> Unit,
    onOptional: (Boolean) -> Unit,
    onUnit: () -> Unit,
    onRemove: () -> Unit
) {
    val palette = AshpaTheme.palette
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.surface.copy(alpha = 0.55f)).padding(11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(23.dp).clip(CircleShape).background(palette.forest),
                contentAlignment = Alignment.Center
            ) { Text(PersianText.faDigits(number.toString()), style = MaterialTheme.typography.labelSmall, color = palette.peach) }
            Spacer(Modifier.width(7.dp))
            Text(line.name, style = MaterialTheme.typography.titleSmall, color = palette.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.DeleteOutline, "حذف", tint = palette.muted, modifier = Modifier.size(18.dp).clickable(onClick = onRemove))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.weight(1f)) {
                AshpaTextField(line.amount, onAmount, "مقدار", keyboardType = KeyboardType.Decimal)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(13.dp)).background(palette.surfaceHigh)
                    .clickable(onClick = onUnit).padding(horizontal = 12.dp, vertical = 15.dp)
            ) { Text(line.unit.label, style = MaterialTheme.typography.labelMedium, color = palette.ink) }
        }
        Spacer(Modifier.height(6.dp))
        AshpaTextField(line.note, onNote, "توضیح ماده (اختیاری)", placeholder = "مثلاً خردشده", singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("اختیاری", style = MaterialTheme.typography.labelSmall, color = palette.muted, modifier = Modifier.weight(1f))
            Switch(checked = line.optional, onCheckedChange = onOptional, colors = SwitchDefaults.colors(checkedThumbColor = palette.forest, checkedTrackColor = palette.sage))
        }
    }
}

@Composable
private fun EditorStepRow(
    number: Int,
    step: EditorStep,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTitle: (String) -> Unit,
    onInstruction: (String) -> Unit,
    onTimer: (String) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit
) {
    val palette = AshpaTheme.palette
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.surface.copy(alpha = 0.55f)).padding(11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(25.dp).clip(CircleShape).background(palette.terracotta),
                contentAlignment = Alignment.Center
            ) { Text(PersianText.faDigits(number.toString()), style = MaterialTheme.typography.labelSmall, color = Color.White) }
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.KeyboardArrowUp, "بالا", tint = if (canMoveUp) palette.muted else palette.line, modifier = Modifier.size(21.dp).clickable(enabled = canMoveUp, onClick = onUp))
            Icon(Icons.Rounded.KeyboardArrowDown, "پایین", tint = if (canMoveDown) palette.muted else palette.line, modifier = Modifier.size(21.dp).clickable(enabled = canMoveDown, onClick = onDown))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.DeleteOutline, "حذف", tint = palette.muted, modifier = Modifier.size(18.dp).clickable(onClick = onRemove))
        }
        Spacer(Modifier.height(7.dp))
        AshpaTextField(step.title, onTitle, "عنوان مرحله", placeholder = "مثلاً تفت دادن پیاز")
        Spacer(Modifier.height(7.dp))
        AshpaTextField(step.instruction, onInstruction, "توضیح مرحله", placeholder = "چه کاری، با چه حرارتی و تا چه زمانی؟", singleLine = false, minLines = 2)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.width(175.dp)) {
            AshpaTextField(step.timer, onTimer, "تایمر (دقیقه، اختیاری)", keyboardType = KeyboardType.Number)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientEditorPicker(
    ingredients: List<Ingredient>,
    onDismiss: () -> Unit,
    onPick: (Ingredient) -> Unit
) {
    val palette = AshpaTheme.palette
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, ingredients) {
        val q = PersianText.normalize(query)
        if (q.isBlank()) ingredients.take(80)
        else ingredients.filter { PersianText.normalize(it.name).contains(q) || it.nameEn.contains(query, true) }.take(80)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("افزودن ماده اولیه", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Spacer(Modifier.height(10.dp))
            AshpaSearchField(query, { query = it }, "جست‌وجوی ماده…")
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 430.dp)) {
                items(filtered, key = { it.id }) { ingredient ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(ingredient) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(ingredient.name, style = MaterialTheme.typography.titleSmall, color = palette.ink)
                            Text(
                                ingredient.categoryTitle + " · " + ingredient.defaultUnit.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.muted
                            )
                        }
                        Icon(Icons.Rounded.Add, null, tint = palette.terracotta, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = palette.line.copy(alpha = 0.4f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitEditorPicker(selected: IngredientUnit, onDismiss: () -> Unit, onPick: (IngredientUnit) -> Unit) {
    val palette = AshpaTheme.palette
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("واحد اندازه‌گیری", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 430.dp)) {
                items(IngredientUnit.entries, key = { it.name }) { unit ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(unit) }.padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(unit.label, style = MaterialTheme.typography.titleSmall, color = palette.ink, modifier = Modifier.weight(1f))
                        Text(unit.symbol, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                        if (unit == selected) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.Check, null, tint = palette.terracotta, modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = palette.line.copy(alpha = 0.4f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
