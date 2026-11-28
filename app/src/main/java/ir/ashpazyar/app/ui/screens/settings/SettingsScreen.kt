package ir.ashpazyar.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenTimers: () -> Unit,
    onOpenTips: () -> Unit,
    onOpenConverter: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenUserRecipes: () -> Unit,
    onOpenCost: () -> Unit,
    onOpenLeftovers: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    var budget by remember { mutableStateOf("") }
    var budgetInitialized by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(settings.weeklyBudget) {
        if (!budgetInitialized) {
            budget = if (settings.weeklyBudget > 0) settings.weeklyBudget.toString() else ""
            budgetInitialized = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
            AshpaTopBar(title = "تنظیمات", onBack = onBack)
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("آشپزخانه، به سلیقه شما", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                Text("ظاهر، یادآوری‌ها و ابزارهای شخصی قاشق", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                Spacer(Modifier.height(16.dp))
                AshpaCard(Modifier.fillMaxWidth(), corner = 22.dp) {
                    Column(Modifier.padding(15.dp)) {
                        Text("پوسته برنامه", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        Spacer(Modifier.height(9.dp))
                        val themeIndex = when (settings.themeMode) {
                            "LIGHT" -> 1
                            "DARK" -> 2
                            else -> 0
                        }
                        SegmentedOptions(
                            options = listOf("سیستم", "روشن", "تیره"),
                            selectedIndex = themeIndex,
                            onSelect = { viewModel.setTheme(listOf("SYSTEM", "LIGHT", "DARK")[it]) }
                        )
                        Spacer(Modifier.height(9.dp))
                        Text(
                            "پوسته تیره برای آشپزخانه‌های کم‌نور و پخت شبانه مناسب است.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.muted
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                AshpaCard(Modifier.fillMaxWidth(), corner = 22.dp) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("خانوار", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                            Spacer(Modifier.weight(1f))
                            QuantityStepper(settings.familySize, viewModel::setFamilySize, min = 1, max = 20)
                        }
                        Text("مقدار مواد دستورها بر اساس این تعداد پیشنهاد می‌شود.", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                        Spacer(Modifier.height(13.dp))
                        Text("بودجه هفتگی (تومان)", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                        Spacer(Modifier.height(7.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                AshpaTextField(
                                    budget,
                                    { budget = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                                    "مثلاً ۲۵۰۰۰۰۰",
                                    keyboardType = KeyboardType.Number
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            AshpaIconButton(onClick = { viewModel.setBudget(budget) }, filled = true) {
                                Icon(Icons.Rounded.Check, "ذخیره", tint = palette.peach)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                AshpaCard(Modifier.fillMaxWidth(), corner = 22.dp) {
                    Column(Modifier.padding(15.dp)) {
                        Text("یادآوری‌ها", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        AshpaSwitchRow("یادآوری وعده‌ها", "هر روز صبح برنامه غذایی را یادآوری کن", settings.mealReminders, viewModel::setMealReminders)
                        AshpaSwitchRow("هشدار انقضای مواد", "مواد نزدیک انقضا را به موقع اطلاع بده", settings.expiryNotifications, viewModel::setExpiryNotifications)
                        Spacer(Modifier.height(8.dp))
                        Text("چند روز قبل از انقضا هشدار بگیرید؟", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                        Spacer(Modifier.height(7.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(listOf(1, 3, 7, 14)) { days ->
                                Pill(
                                    PersianText.faDigits(days.toString()) + " روز",
                                    settings.expirationWarningDays == days,
                                    onClick = { viewModel.setWarningDays(days) }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("ابزارها و تنظیمات", style = MaterialTheme.typography.titleLarge, color = palette.ink)
                Spacer(Modifier.height(8.dp))
                AshpaCard(Modifier.fillMaxWidth(), corner = 22.dp) {
                    Column {
                        SettingsLink("سلیقه و رژیم غذایی", "حساسیت‌ها و مواد نامطلوب", Icons.Rounded.Eco, onOpenPreferences)
                        SettingsDivider()
                        SettingsLink("دستورهای من", "دستورهای شخصی و کپی‌شده", Icons.Rounded.MenuBook, onOpenUserRecipes)
                        SettingsDivider()
                        SettingsLink("تایمرهای آشپزی", "تایمرهای فعال و پایان‌یافته", Icons.Rounded.Timer, onOpenTimers)
                        SettingsDivider()
                        SettingsLink("هزینه غذا", "قیمت مواد و هزینه هر پرس", Icons.Rounded.Payments, onOpenCost)
                        SettingsDivider()
                        SettingsLink("باقی‌مانده‌ها", "مدیریت غذاهای اضافه", Icons.Rounded.Eco, onOpenLeftovers)
                        SettingsDivider()
                        SettingsLink("نکته‌های آشپزی", "دانش پایه آشپزخانه", Icons.Rounded.Lightbulb, onOpenTips)
                        SettingsDivider()
                        SettingsLink("مبدل واحد", "گرم، پیمانه و قاشق", Icons.Rounded.SwapHoriz, onOpenConverter)
                        SettingsDivider()
                        SettingsLink("پشتیبان‌گیری و بازیابی", "فایل پشتیبان آفلاین بسازید", Icons.Rounded.Backup, onOpenBackup)
                    }
                }
                Spacer(Modifier.height(12.dp))
                AshpaCard(Modifier.fillMaxWidth(), corner = 22.dp) {
                    Column {
                        SettingsLink("درباره قاشق", "نسخه، حریم خصوصی و مجوزها", Icons.Rounded.Info, onOpenAbout)
                        SettingsDivider()
                        SettingsLink("راهنمای شروع", "دوباره معرفی اولیه را ببینید", Icons.Rounded.Explore, { showReset = true })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "قاشق ۱.۲.۲ · بدون اینترنت، بدون حساب، بدون تبلیغ",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    modifier = Modifier.padding(bottom = 26.dp)
                )
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
    }

    if (showReset) {
        ConfirmDialog(
            title = "نمایش دوباره راهنما",
            message = "صفحه معرفی اولیه دوباره نمایش داده می‌شود. داده‌های شما حذف نمی‌شوند.",
            confirmLabel = "نمایش راهنما",
            onConfirm = {
                showReset = false
                viewModel.resetOnboarding()
            },
            onDismiss = { showReset = false }
        )
    }
}

@Composable
private fun SettingsLink(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val palette = AshpaTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(palette.sageSoft),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = palette.forest, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = palette.muted)
        }
        Icon(Icons.Rounded.ChevronLeft, null, tint = palette.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsDivider() {
    val palette = AshpaTheme.palette
    Box(Modifier.padding(start = 62.dp).fillMaxWidth().height(1.dp).background(palette.line.copy(alpha = 0.45f)))
}
