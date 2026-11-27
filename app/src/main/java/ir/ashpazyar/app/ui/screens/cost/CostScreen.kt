package ir.ashpazyar.app.ui.screens.cost

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.repository.PriceEntry
import ir.ashpazyar.app.domain.model.Recipe
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun CostScreen(
    onBack: () -> Unit,
    viewModel: CostViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<PriceEntry?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                Column(Modifier.statusBarsPadding()) {
                    AshpaTopBar(title = "هزینه غذا", onBack = onBack)
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text("خرج آشپزخانه", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                        Text(
                            "قیمت مواد را یک‌بار ثبت کنید تا هزینه هر دستور و هر پرس را ببینید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted
                        )
                        Spacer(Modifier.height(15.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.forest).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Payments, null, tint = Color(0xFFE6B88C), modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    PersianText.faDigits(state.knownPriceCount.toString()) + " ماده قیمت‌دار",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFFFFBF2)
                                )
                                Text("قیمت‌ها فقط روی این دستگاه ذخیره می‌شوند", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD6DFD1))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        SegmentedOptions(
                            options = listOf("قیمت مواد", "هزینه دستور"),
                            selectedIndex = tab,
                            onSelect = { tab = it }
                        )
                        Spacer(Modifier.height(15.dp))
                    }
                }
            }
            if (tab == 0) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        AshpaSearchField(state.query, viewModel::setQuery, "جست‌وجوی ماده…")
                        Spacer(Modifier.height(9.dp))
                        Text(
                            "برای شروع، قیمت هر کیلو یا واحد را وارد کنید. اگر قیمتی ثبت نشود، از قیمت مرجع داخلی استفاده می‌شود.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.muted
                        )
                    }
                }
                items(state.filtered, key = { it.ingredient.id }) { entry ->
                    PriceRow(entry, onClick = { editing = entry })
                }
            } else {
                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        RecipePickerButton(state.selectedRecipe, onClick = { showPicker = true })
                        if (state.selectedRecipe != null) {
                            Spacer(Modifier.height(9.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("تعداد پرس", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                                Spacer(Modifier.weight(1f))
                                QuantityStepper(state.servings, viewModel::setServings, min = 1, max = 30)
                            }
                            Spacer(Modifier.height(14.dp))
                            CostBreakdownCard(state)
                        } else {
                            Spacer(Modifier.height(10.dp))
                            InlineNotice("یک دستور را انتخاب کنید تا هزینه مواد آن را با قیمت‌های شما محاسبه کنیم.")
                        }
                    }
                }
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
    }

    editing?.let { entry ->
        PriceEditSheet(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { text ->
                viewModel.savePrice(entry, text)
                editing = null
            }
        )
    }
    if (showPicker) {
        RecipePickerSheet(
            recipes = state.recipes,
            onDismiss = { showPicker = false },
            onPick = {
                viewModel.selectRecipe(it.id)
                showPicker = false
            }
        )
    }
}

@Composable
private fun PriceRow(entry: PriceEntry, onClick: () -> Unit) {
    val palette = AshpaTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(palette.surface),
            contentAlignment = Alignment.Center
        ) { Text(entry.ingredient.name.take(1), style = MaterialTheme.typography.titleSmall, color = palette.forest) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.ingredient.name, style = MaterialTheme.typography.bodyLarge, color = palette.ink)
            Text(entry.ingredient.categoryTitle, style = MaterialTheme.typography.labelSmall, color = palette.muted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (entry.pricePerKg != null) PersianText.faDigits(entry.pricePerKg.toLong().grouped()) + " ت" else "ثبت قیمت",
                style = MaterialTheme.typography.labelMedium,
                color = if (entry.pricePerKg != null) palette.ink else palette.terracotta
            )
            if (entry.pricePerKg != null) {
                Text("هر کیلو", style = MaterialTheme.typography.labelSmall, color = palette.muted)
            }
        }
        Icon(Icons.Rounded.ChevronLeft, null, tint = palette.muted, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun RecipePickerButton(recipe: Recipe?, onClick: () -> Unit) {
    val palette = AshpaTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(palette.surface).clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (recipe != null) {
            FoodArt(recipe.heroStyle, recipe.id.hashCode(), Modifier.size(50.dp), corner = 13.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(recipe.title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
                Text(recipe.difficulty.label + " · " + PersianText.faDigits(recipe.totalMinutes.toString()) + " دقیقه", style = MaterialTheme.typography.labelSmall, color = palette.muted)
            }
            Text("تغییر", style = MaterialTheme.typography.labelSmall, color = palette.terracotta)
        } else {
            Icon(Icons.Rounded.RestaurantMenu, null, tint = palette.terracotta)
            Spacer(Modifier.width(9.dp))
            Text("انتخاب دستور", style = MaterialTheme.typography.bodyLarge, color = palette.ink)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronLeft, null, tint = palette.muted)
        }
    }
}

@Composable
private fun CostBreakdownCard(state: CostUiState) {
    val palette = AshpaTheme.palette
    val breakdown = state.breakdown
    if (breakdown == null) {
        InlineNotice("برای این دستور قیمتی ثبت نشده است. از تب قیمت مواد، چند ماده را قیمت‌گذاری کنید.", tone = "warm")
        return
    }
    AshpaCard(Modifier.fillMaxWidth(), corner = 22.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("هزینه مواد", style = MaterialTheme.typography.labelMedium, color = palette.muted)
                    Text(
                        PersianText.faDigits(breakdown.total.toLong().grouped()) + " تومان",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.ink
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("هر پرس", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                    Text(
                        PersianText.faDigits(breakdown.perServing.toLong().grouped()) + " ت",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.terracotta
                    )
                }
            }
            if (breakdown.lines.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("قیمت هیچ‌یک از مواد این دستور ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
            } else {
                Spacer(Modifier.height(12.dp))
                breakdown.lines.forEachIndexed { index, line ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(line.ingredientName, style = MaterialTheme.typography.bodyMedium, color = palette.ink, modifier = Modifier.weight(1f))
                        Text(line.amountLabel, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            PersianText.faDigits(line.cost.toLong().grouped()) + " ت",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.ink
                        )
                    }
                    if (index != breakdown.lines.lastIndex) HorizontalDivider(color = palette.line.copy(alpha = 0.4f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "این عدد فقط مواد غذایی است و هزینه گاز، برق و زمان پخت را شامل نمی‌شود.",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceEditSheet(entry: PriceEntry, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val palette = AshpaTheme.palette
    var price by remember(entry.ingredient.id) {
        mutableStateOf(entry.pricePerKg?.toLong()?.toString().orEmpty())
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(entry.ingredient.name, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Text("قیمت هر کیلوگرم یا لیتر را وارد کنید. خالی گذاشتن، قیمت را پاک می‌کند.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
            Spacer(Modifier.height(12.dp))
            AshpaTextField(price, { price = it }, "قیمت (تومان)", placeholder = "مثلاً ۱۸۰۰۰۰", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(14.dp))
            PrimaryButton("ذخیره", onClick = { onSave(price) }, icon = Icons.Rounded.Save)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipePickerSheet(recipes: List<Recipe>, onDismiss: () -> Unit, onPick: (Recipe) -> Unit) {
    val palette = AshpaTheme.palette
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, recipes) {
        if (query.isBlank()) recipes.take(50)
        else recipes.filter { it.title.contains(query, ignoreCase = true) }.take(50)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface, contentColor = palette.ink) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("انتخاب دستور", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            Spacer(Modifier.height(10.dp))
            AshpaSearchField(query, { query = it }, "جست‌وجوی دستور…")
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 430.dp)) {
                items(filtered, key = { it.id }) { recipe ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(recipe) }.padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FoodArt(recipe.heroStyle, recipe.id.hashCode(), Modifier.size(42.dp), corner = 11.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(recipe.title, style = MaterialTheme.typography.titleSmall, color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(recipe.difficulty.label + " · " + PersianText.faDigits(recipe.totalMinutes.toString()) + " دقیقه", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                        }
                        Icon(Icons.Rounded.ChevronLeft, null, tint = palette.muted)
                    }
                    HorizontalDivider(color = palette.line.copy(alpha = 0.4f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun Long.grouped(): String = this.toString().reversed().chunked(3).joinToString(",").reversed()
