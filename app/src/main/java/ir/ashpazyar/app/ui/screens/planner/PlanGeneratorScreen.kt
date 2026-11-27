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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.ProposedMeal
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun PlanGeneratorScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: PlanGeneratorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.saved.collect {
            saved = true
            onSaved()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AshpaTopBar(title = "برنامه‌ساز هفته", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("هفته را بسپارید به ما", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                    Text(
                        "با توجه به نفرات، زمان، بودجه و مواد موجود، هفت روز وعده می‌چینیم. بعد از دیدن پیش‌نمایش، ذخیره کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.muted
                    )
                    Spacer(Modifier.height(15.dp))
                    WeekNav(
                        weekStart = state.weekStart,
                        onPrevious = { viewModel.shiftWeek(-1) },
                        onNext = { viewModel.shiftWeek(1) }
                    )
                    Spacer(Modifier.height(18.dp))
                    SettingsSection(title = "برای چند نفر؟") {
                        QuantityStepper(state.familySize, viewModel::setFamilySize, min = 1, max = 20)
                    }
                    Spacer(Modifier.height(16.dp))
                    SettingsSection(title = "حداکثر زمان هر غذا") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Pill("بدون محدودیت", state.maxMinutes == null, onClick = { viewModel.setMaxMinutes(null) }) }
                            item { Pill("۲۰ دقیقه", state.maxMinutes == 20, onClick = { viewModel.setMaxMinutes(20) }) }
                            item { Pill("۳۰ دقیقه", state.maxMinutes == 30, onClick = { viewModel.setMaxMinutes(30) }) }
                            item { Pill("۴۵ دقیقه", state.maxMinutes == 45, onClick = { viewModel.setMaxMinutes(45) }) }
                            item { Pill("۹۰ دقیقه", state.maxMinutes == 90, onClick = { viewModel.setMaxMinutes(90) }) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    SettingsSection(title = "وعده‌ها") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Pill("صبحانه", state.includeBreakfast, onClick = viewModel::toggleBreakfast) }
                            item { Pill("ناهار", state.includeLunch, onClick = viewModel::toggleLunch) }
                            item { Pill("شام", state.includeDinner, onClick = viewModel::toggleDinner) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    SettingsSection(title = "بودجه هفتگی (اختیاری)") {
                        AshpaTextField(
                            value = state.budgetText,
                            onValueChange = viewModel::setBudget,
                            label = "تومان",
                            placeholder = "مثلاً ۲۵۰۰۰۰۰",
                            keyboardType = KeyboardType.Number
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("اگر خالی بگذارید، سطح اقتصادی دستورها ملاک است.", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                    }
                    Spacer(Modifier.height(8.dp))
                    AshpaSwitchRow("تنوع حداکثری", "تا حد امکان دستور تکراری در هفته نباشد", state.variety) { viewModel.toggleVariety() }
                    AshpaSwitchRow("بدون گوشت", "دستورهای گیاهی یا بدون گوشت", state.noMeat) { viewModel.toggleNoMeat() }
                    AshpaSwitchRow("گیاهی", "فقط دستورهای کاملاً گیاهی", state.vegetarian) { viewModel.toggleVegetarian() }
                    AshpaSwitchRow("اقتصادی", "در اولویت قرار دادن گزینه‌های کم‌هزینه", state.lowCost) { viewModel.toggleLowCost() }
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(
                        text = if (state.proposal == null) "چیدن برنامه" else "چیدن دوباره",
                        onClick = viewModel::generate,
                        icon = Icons.Rounded.AutoAwesome,
                        loading = state.generating
                    )
                    if (state.error != null) {
                        Spacer(Modifier.height(8.dp))
                        InlineNotice(state.error!!, tone = "warm")
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            state.proposal?.let { proposal ->
                if (proposal.entries.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("پیش‌نمایش هفته", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
                                Spacer(Modifier.weight(1f))
                                if (proposal.estimatedMatch > 0) {
                                    Text(
                                        PersianText.faDigits(proposal.estimatedMatch.toString()) + "٪ مواد موجود",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.success
                                    )
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                        }
                    }
                    val byDay = proposal.entries.groupBy { it.date }.toSortedMap()
                    byDay.forEach { (day, meals) ->
                        item(key = "day-$day") {
                            DayProposal(day, meals)
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            proposal.notes.forEach { note ->
                                Text("• " + note, style = MaterialTheme.typography.bodySmall, color = palette.muted, modifier = Modifier.padding(vertical = 2.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            PrimaryButton(
                                "ذخیره برنامه در تقویم",
                                onClick = viewModel::save,
                                icon = Icons.Rounded.Save,
                                loading = state.saving,
                                enabled = !saved
                            )
                        }
                    }
                }
            }
            if (state.proposal == null) {
                item {
                    Spacer(Modifier.height(12.dp))
                    InlineNotice(
                        "پیشنهادها فقط پیش‌نویس‌اند؛ تا وقتی ذخیره نکنید برنامه فعلی شما تغییری نمی‌کند.",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekNav(weekStart: Long, onPrevious: () -> Unit, onNext: () -> Unit) {
    val palette = AshpaTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(palette.surface).padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AshpaIconButton(onClick = onPrevious) { Icon(Icons.Rounded.ChevronRight, "هفته قبل", tint = palette.ink) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(JalaliCalendar.format(JalaliCalendar.dateOfEpochDay(weekStart)), style = MaterialTheme.typography.labelMedium, color = palette.ink)
            Text("شنبه تا جمعه", style = MaterialTheme.typography.labelSmall, color = palette.muted)
        }
        AshpaIconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronLeft, "هفته بعد", tint = palette.ink) }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val palette = AshpaTheme.palette
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.surface).padding(14.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
        Spacer(Modifier.height(9.dp))
        content()
    }
}

@Composable
private fun DayProposal(day: Long, meals: List<ProposedMeal>) {
    val palette = AshpaTheme.palette
    Column(Modifier.padding(horizontal = 20.dp, vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(palette.terracotta))
            Spacer(Modifier.width(6.dp))
            Text(
                JalaliCalendar.relativeDayLabel(day) + " · " + JalaliCalendar.format(JalaliCalendar.epochMillisOfEpochDay(day), withWeekday = false),
                style = MaterialTheme.typography.labelMedium,
                color = palette.ink
            )
        }
        Spacer(Modifier.height(6.dp))
        AshpaCard(modifier = Modifier.fillMaxWidth(), corner = 18.dp) {
            meals.sortedBy { it.mealType.ordinal }.forEachIndexed { index, meal ->
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        meal.mealType.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.terracotta,
                        modifier = Modifier.width(52.dp)
                    )
                    FoodArt(meal.recipe.heroStyle, meal.recipe.id.hashCode(), Modifier.size(42.dp), corner = 11.dp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(meal.recipe.title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
                        Text(
                            PersianText.faDigits(meal.recipe.totalMinutes.toString()) + " دقیقه · " + meal.recipe.difficulty.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.muted
                        )
                    }
                }
                if (index != meals.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line.copy(alpha = 0.4f)))
            }
        }
    }
}
