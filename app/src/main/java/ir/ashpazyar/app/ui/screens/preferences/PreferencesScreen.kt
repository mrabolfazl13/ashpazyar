package ir.ashpazyar.app.ui.screens.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

private val allergenOptions = listOf(
    "gluten" to "گلوتن",
    "dairy" to "لبنیات",
    "egg" to "تخم‌مرغ",
    "nuts" to "خشکبار",
    "sesame" to "کنجد",
    "fish" to "ماهی",
    "shellfish" to "صدف",
    "soy" to "سویا",
    "celery" to "کرفس"
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PreferencesScreen(
    onBack: () -> Unit,
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val prefs = state.preferences
    val snackbar = remember { SnackbarHostState() }
    var newDislike by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            AshpaTopBar(title = "سلیقه و رژیم", onBack = onBack)
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("آشپزیار را بشناسید", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                Text(
                    "این تنظیمات روی پیشنهادها، برنامه‌ساز و فهرست خرید اثر می‌گذارد. هر زمان قابل تغییر است.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted
                )
                Spacer(Modifier.height(16.dp))
                PreferencesSection(title = "رژیم و سبک زندگی", icon = Icons.Rounded.Eco) {
                    AshpaSwitchRow("گیاهی", "دستورهای بدون گوشت، مرغ و ماهی", prefs.vegetarian, viewModel::setVegetarian)
                    AshpaSwitchRow("بدون گوشت", "می‌تواند شامل تخم‌مرغ و لبنیات باشد", prefs.noMeat, viewModel::setNoMeat)
                    AshpaSwitchRow("بدون لبنیات", "حذف شیر، ماست، پنیر و کره", prefs.dairyFree, viewModel::setDairyFree)
                    AshpaSwitchRow("بدون گلوتن", "حذف آرد گندم، نان و رشته", prefs.glutenFree, viewModel::setGlutenFree)
                }
                Spacer(Modifier.height(12.dp))
                PreferencesSection(title = "اولویت‌های روزمره", icon = Icons.Rounded.Tune) {
                    AshpaSwitchRow("کم‌هزینه", "دستورهایی با مواد اقتصادی", prefs.lowCost, viewModel::setLowCost)
                    AshpaSwitchRow("سریع", "آماده در ۳۰ دقیقه یا کمتر", prefs.quickMeals, viewModel::setQuick)
                    AshpaSwitchRow("کم‌نمک", "پیشنهاد غذاهای کم‌نمک", prefs.lowSalt, viewModel::setLowSalt)
                }
                Spacer(Modifier.height(12.dp))
                PreferencesSection(title = "حساسیت‌ها", icon = Icons.Rounded.WarningAmber) {
                    Text(
                        "اگر به ماده‌ای حساسیت دارید، آن را انتخاب کنید تا از پیشنهادها کنار گذاشته شود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted
                    )
                    Spacer(Modifier.height(9.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        allergenOptions.forEach { (key, label) ->
                            Pill(label, key in prefs.allergies, onClick = { viewModel.toggleAllergy(key) })
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "حساسیت شدید دارید؟ همیشه برچسب مواد و دستور را خودتان بررسی کنید.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.terracotta
                    )
                }
                Spacer(Modifier.height(12.dp))
                PreferencesSection(title = "مواد نامطلوب", icon = Icons.Rounded.Block) {
                    if (prefs.dislikedIngredients.isEmpty()) {
                        Text("موردی ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    } else {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            prefs.dislikedIngredients.forEach { ingredient ->
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(palette.surfaceHigh).padding(start = 12.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ingredient, style = MaterialTheme.typography.labelSmall, color = palette.ink)
                                    Spacer(Modifier.width(5.dp))
                                    Icon(
                                        Icons.Rounded.Close,
                                        "حذف",
                                        tint = palette.muted,
                                        modifier = Modifier.size(14.dp).clickable { viewModel.removeDislike(ingredient) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            AshpaTextField(newDislike, { newDislike = it }, "مثلاً بادمجان")
                        }
                        Spacer(Modifier.width(8.dp))
                        AshpaIconButton(onClick = {
                            viewModel.addDislike(newDislike)
                            newDislike = ""
                        }, filled = true) { Icon(Icons.Rounded.Add, "افزودن", tint = palette.peach) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                InlineNotice(
                    "این تنظیمات فقط برای شخصی‌سازی است و جای مشاوره پزشکی یا تغذیه را نمی‌گیرد.",
                    tone = "warm"
                )
                Spacer(Modifier.height(28.dp))
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
    }
}

@Composable
private fun PreferencesSection(
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
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}
