package ir.ashpazyar.app.ui.screens.pantry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.core.util.UnitConverter
import ir.ashpazyar.app.domain.model.Ingredient
import ir.ashpazyar.app.domain.model.StorageLocation
import ir.ashpazyar.app.domain.model.Unit
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun AddPantryScreen(
    onBack: () -> Unit,
    viewModel: AddPantryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    var savedFlag by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saved.collect {
            savedFlag = true
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AshpaTopBar(title = "افزودن به انبار", onBack = onBack)
        if (state.selected == null) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("چه خریده‌اید؟", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                Text("ماده اولیه را انتخاب کنید تا واحد مناسبش را پیشنهاد بدهیم.", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                Spacer(Modifier.height(14.dp))
                AshpaSearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "جست‌وجوی ماده اولیه…"
                )
                Spacer(Modifier.height(14.dp))
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 26.dp)) {
                items(state.suggestions, key = { it.id }) { ingredient ->
                    IngredientRow(ingredient, onClick = { viewModel.select(ingredient) })
                }
                if (state.suggestions.isEmpty()) {
                    item {
                        EmptyState(
                            title = "ماده‌ای پیدا نشد",
                            description = "نام دیگری را امتحان کنید. می‌توانید بعداً از دستورساز، ماده سفارشی بسازید.",
                            icon = Icons.Rounded.Search
                        )
                    }
                }
            }
        } else {
            val ingredient = state.selected!!
            LazyColumn(contentPadding = PaddingValues(bottom = 26.dp)) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(palette.sageSoft).padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(palette.surface),
                                contentAlignment = Alignment.Center
                            ) { Text(ingredient.name.take(1), style = MaterialTheme.typography.titleLarge, color = palette.forest) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ingredient.name, style = MaterialTheme.typography.titleMedium, color = palette.ink)
                                Text(ingredient.categoryTitle + " · واحد پیشنهادی: " + ingredient.defaultUnit.label, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                            }
                            Text(
                                "تغییر",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.terracotta,
                                modifier = Modifier.clickable(onClick = viewModel::clearSelection).padding(8.dp)
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("مقدار", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        Spacer(Modifier.height(7.dp))
                        AshpaTextField(
                            value = state.amount,
                            onValueChange = viewModel::setAmount,
                            label = "چقدر دارید؟",
                            placeholder = "مثلاً ۵۰۰",
                            keyboardType = KeyboardType.Decimal,
                            error = state.error
                        )
                        Spacer(Modifier.height(9.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(unitOptions(ingredient)) { u -> Pill(u.label, state.unit == u, onClick = { viewModel.setUnit(u) }) }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("محل نگهداری", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(StorageLocation.entries) { loc ->
                                Pill(loc.label, state.location == loc, onClick = { viewModel.setLocation(loc) })
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("تاریخ انقضا", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            item { Pill("۳ روز", state.expiryDays == 3, onClick = { viewModel.setExpiryDays(3) }) }
                            item { Pill("۱ هفته", state.expiryDays == 7, onClick = { viewModel.setExpiryDays(7) }) }
                            item { Pill("۲ هفته", state.expiryDays == 14, onClick = { viewModel.setExpiryDays(14) }) }
                            item { Pill("۱ ماه", state.expiryDays == 30, onClick = { viewModel.setExpiryDays(30) }) }
                            item { Pill("۳ ماه", state.expiryDays == 90, onClick = { viewModel.setExpiryDays(90) }) }
                            item { Pill("بدون تاریخ", state.expiryDays == null, onClick = { viewModel.setExpiryDays(null) }) }
                        }
                        Spacer(Modifier.height(18.dp))
                        AshpaTextField(
                            value = state.price,
                            onValueChange = viewModel::setPrice,
                            label = "قیمت پرداختی (اختیاری، تومان)",
                            placeholder = "مثلاً ۹۵۰۰۰",
                            keyboardType = KeyboardType.Number
                        )
                        Spacer(Modifier.height(10.dp))
                        AshpaTextField(
                            value = state.note,
                            onValueChange = viewModel::setNote,
                            label = "یادداشت (اختیاری)",
                            placeholder = "مثلاً برای خورشِ مهمان",
                            singleLine = false,
                            minLines = 2
                        )
                        Spacer(Modifier.height(10.dp))
                        InlineNotice("تاریخ انقضا فقط برای یادآوری است؛ همیشه خودِ ماده را بررسی کنید.")
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            "افزودن به انبار",
                            onClick = viewModel::save,
                            icon = Icons.Rounded.Add,
                            loading = state.saving,
                            enabled = !state.saving && !savedFlag
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

private fun unitOptions(ingredient: Ingredient): List<Unit> {
    val defaults = mutableListOf(ingredient.defaultUnit)
    val candidates = when (ingredient.defaultUnit) {
        Unit.KILOGRAM, Unit.GRAM -> listOf(Unit.GRAM, Unit.KILOGRAM, Unit.PIECE)
        Unit.LITER, Unit.MILLILITER -> listOf(Unit.MILLILITER, Unit.LITER, Unit.CUP)
        Unit.PIECE, Unit.CLOVE, Unit.BUNCH, Unit.SLICE -> listOf(Unit.PIECE, Unit.GRAM, Unit.BUNCH)
        else -> listOf(Unit.GRAM, Unit.PIECE, Unit.CUP, Unit.TABLESPOON)
    }
    candidates.forEach { if (it !in defaults) defaults += it }
    return defaults.distinct().filter { UnitConverter.toBase(1.0, it, ingredient.profile) != null }
}

@Composable
private fun IngredientRow(ingredient: Ingredient, onClick: () -> Unit) {
    val palette = AshpaTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(palette.surface),
            contentAlignment = Alignment.Center
        ) { Text(ingredient.name.take(1), style = MaterialTheme.typography.titleMedium, color = palette.forest) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(ingredient.name, style = MaterialTheme.typography.titleSmall, color = palette.ink)
            Text(ingredient.categoryTitle, style = MaterialTheme.typography.labelSmall, color = palette.muted)
        }
        Text(ingredient.defaultUnit.label, style = MaterialTheme.typography.labelSmall, color = palette.muted)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Rounded.ChevronLeft, null, tint = palette.muted, modifier = Modifier.size(17.dp))
    }
}
