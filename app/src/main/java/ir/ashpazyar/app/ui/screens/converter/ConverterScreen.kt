package ir.ashpazyar.app.ui.screens.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.Ingredient
import ir.ashpazyar.app.domain.model.IngredientUnit
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    onBack: () -> Unit,
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    var showIngredientPicker by remember { mutableStateOf(false) }
    var unitPicker by remember { mutableStateOf<UnitPicker?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        AshpaTopBar(title = "مبدل واحد", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("پیمانه یا گرم؟", style = MaterialTheme.typography.displaySmall, color = palette.ink)
            Text(
                "مقدار را وارد کنید و واحدها را تغییر دهید. برای تبدیل پیمانه به گرم، ماده اولیه را هم انتخاب کنید.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted
            )
            Spacer(Modifier.height(16.dp))
            AshpaCard(Modifier.fillMaxWidth(), corner = 24.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showIngredientPicker = true }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Spa, null, tint = palette.terracotta, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("ماده اولیه", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                            Text(
                                state.selected?.name ?: "بدون ماده خاص (تبدیل حجمی)",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.ink
                            )
                        }
                        if (state.selected != null) {
                            Text(
                                "پاک کردن",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.terracotta,
                                modifier = Modifier.clickable { viewModel.selectIngredient(null) }.padding(6.dp)
                            )
                        } else {
                            Icon(Icons.Rounded.ChevronLeft, null, tint = palette.muted)
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    AshpaTextField(
                        value = state.amount,
                        onValueChange = viewModel::setAmount,
                        label = "مقدار",
                        placeholder = "مثلاً ۲",
                        keyboardType = KeyboardType.Decimal
                    )
                    Spacer(Modifier.height(13.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        UnitBox(
                            title = "از",
                            unit = state.fromUnit,
                            modifier = Modifier.weight(1f),
                            onClick = { unitPicker = UnitPicker.FROM }
                        )
                        Box(Modifier.padding(top = 22.dp)) {
                            Icon(Icons.Rounded.SwapHoriz, "تبدیل", tint = palette.terracotta)
                        }
                        UnitBox(
                            title = "به",
                            unit = state.toUnit,
                            modifier = Modifier.weight(1f),
                            onClick = { unitPicker = UnitPicker.TO }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(palette.forest).padding(16.dp)
                    ) {
                        Column {
                            Text("نتیجه", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD6DFD1))
                            Text(
                                if (state.result.isNotBlank()) state.result else "—",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFFFFFBF2)
                            )
                            if (state.error != null) {
                                Text(state.error!!, style = MaterialTheme.typography.labelSmall, color = Color(0xFFF1C7A4))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (state.selected != null) {
                AshpaCard(Modifier.fillMaxWidth(), corner = 20.dp) {
                    Column(Modifier.padding(15.dp)) {
                        Text("مرجع این ماده", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                        Spacer(Modifier.height(7.dp))
                        if (state.selected?.cupWeightGrams != null) {
                            ReferenceLine("یک پیمانه", PersianText.number(state.selected!!.cupWeightGrams!!) + " گرم")
                        }
                        if (state.selected?.pieceWeightGrams != null) {
                            ReferenceLine("یک عدد", PersianText.number(state.selected!!.pieceWeightGrams!!) + " گرم")
                        }
                        if (state.selected?.cupWeightGrams == null && state.selected?.pieceWeightGrams == null) {
                            Text(
                                "برای این ماده وزن مرجع ثبت نشده است؛ تبدیل فقط در محدوده حجم یا تعداد انجام می‌شود.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted
                            )
                        }
                    }
                }
            } else {
                AshpaCard(Modifier.fillMaxWidth(), corner = 20.dp) {
                    Column(Modifier.padding(15.dp)) {
                        Text("تبدیل‌های پرکاربرد", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                        Spacer(Modifier.height(7.dp))
                        ReferenceLine("۱ پیمانه", "۲۴۰ میلی‌لیتر")
                        ReferenceLine("۱ قاشق غذاخوری", "۱۵ میلی‌لیتر")
                        ReferenceLine("۱ قاشق چای‌خوری", "۵ میلی‌لیتر")
                        ReferenceLine("۱ لیتر", "۱۰۰۰ میلی‌لیتر")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "برای تبدیل دقیق پیمانه به گرم، ماده اولیه را انتخاب کنید.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.muted
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            InlineNotice(
                "این تبدیل‌ها تقریبی‌اند. برای شیرینی‌پزی دقیق، همیشه از ترازو استفاده کنید.",
                tone = "warm"
            )
            Spacer(Modifier.height(26.dp))
        }
    }

    if (showIngredientPicker) {
        IngredientPickerSheet(
            ingredients = state.ingredients,
            query = state.query,
            onQuery = viewModel::setQuery,
            onDismiss = { showIngredientPicker = false },
            onPick = {
                viewModel.selectIngredient(it)
                showIngredientPicker = false
            }
        )
    }
    if (unitPicker != null) {
        UnitPickerSheet(
            title = if (unitPicker == UnitPicker.FROM) "واحد ورودی" else "واحد خروجی",
            units = if (unitPicker == UnitPicker.FROM) state.fromUnits else state.toUnits,
            selected = if (unitPicker == UnitPicker.FROM) state.fromUnit else state.toUnit,
            onDismiss = { unitPicker = null },
            onPick = {
                if (unitPicker == UnitPicker.FROM) viewModel.setFrom(it) else viewModel.setTo(it)
                unitPicker = null
            }
        )
    }
}

private enum class UnitPicker { FROM, TO }

@Composable
private fun UnitBox(title: String, unit: IngredientUnit, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = AshpaTheme.palette
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = palette.muted)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.surface)
                .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(unit.label, style = MaterialTheme.typography.titleSmall, color = palette.ink, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, null, tint = palette.muted)
        }
    }
}

@Composable
private fun ReferenceLine(label: String, value: String) {
    val palette = AshpaTheme.palette
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = palette.muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = palette.ink)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientPickerSheet(
    ingredients: List<Ingredient>,
    query: String,
    onQuery: (String) -> Unit,
    onDismiss: () -> Unit,
    onPick: (Ingredient) -> Unit
) {
    val palette = AshpaTheme.palette
    val filtered = remember(query, ingredients) {
        val q = PersianText.normalize(query)
        if (q.isBlank()) ingredients.take(80)
        else ingredients.filter { PersianText.normalize(it.name).contains(q) }.take(80)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("انتخاب ماده اولیه", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Spacer(Modifier.height(10.dp))
            AshpaSearchField(query, onQuery, "جست‌وجوی ماده…")
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
                                ingredient.categoryTitle + " · واحد پیش‌فرض " + ingredient.defaultUnit.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.muted
                            )
                        }
                        if (ingredient.cupWeightGrams != null || ingredient.pieceWeightGrams != null) {
                            Icon(Icons.Rounded.Scale, null, tint = palette.sage, modifier = Modifier.size(18.dp))
                        }
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
private fun UnitPickerSheet(
    title: String,
    units: List<IngredientUnit>,
    selected: IngredientUnit,
    onDismiss: () -> Unit,
    onPick: (IngredientUnit) -> Unit
) {
    val palette = AshpaTheme.palette
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 430.dp)) {
                items(units, key = { it.name }) { unit ->
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
