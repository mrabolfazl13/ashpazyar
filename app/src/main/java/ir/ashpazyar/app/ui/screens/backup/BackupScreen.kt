package ir.ashpazyar.app.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.data.repository.BackupResult
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.restore(uri)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
            AshpaTopBar(title = "پشتیبان‌گیری", onBack = onBack)
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("آشپزخانه‌تان را همراه ببرید", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                Text(
                    "همه چیز روی همین دستگاه است. یک فایل پشتیبان بسازید و در جای امن نگه دارید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.forest).padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = Color(0xFFE6B88C), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("حریم خصوصی کامل", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFFBF2))
                        Text(
                            "فایل پشتیبان فقط با انتخاب شما ساخته می‌شود و هیچ‌جا آپلود نمی‌شود.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD6DFD1)
                        )
                    }
                }
                Spacer(Modifier.height(15.dp))
                BackupActionCard(
                    icon = Icons.Rounded.FileUpload,
                    title = "ساخت فایل پشتیبان",
                    description = "دستورهای شخصی، انبار، برنامه غذایی، خرید، باقی‌مانده‌ها، هزینه‌ها و تنظیمات را در یک فایل JSON ذخیره می‌کند.",
                    buttonLabel = "ذخیره فایل پشتیبان",
                    loading = state.exporting,
                    onClick = { exportLauncher.launch(viewModel.suggestedName()) }
                )
                Spacer(Modifier.height(12.dp))
                BackupActionCard(
                    icon = Icons.Rounded.Restore,
                    title = "بازیابی از فایل",
                    description = "داده‌های فایل پشتیبان جایگزین داده‌های فعلی می‌شود. دستورهای داخلی دست‌نخورده می‌مانند.",
                    buttonLabel = "انتخاب فایل پشتیبان",
                    loading = state.restoring,
                    destructive = true,
                    onClick = { showRestoreConfirm = true }
                )
                if (state.lastResult != null) {
                    Spacer(Modifier.height(14.dp))
                    ResultCard(state.lastResult!!, state.lastAction == "export")
                }
                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    InlineNotice(state.error!!, tone = "warm")
                }
                Spacer(Modifier.height(16.dp))
                InlineNotice(
                    "پیشنهاد می‌کنیم پیش از بازیابی، از وضعیت فعلی هم یک فایل پشتیبان بسازید. بازیابی روی همین دستگاه انجام می‌شود و به اینترنت نیاز ندارد."
                )
                Spacer(Modifier.height(28.dp))
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
    }

    if (showRestoreConfirm) {
        ConfirmDialog(
            title = "بازیابی داده‌ها",
            message = "داده‌های فعلی انبار، برنامه، خرید و باقی‌مانده‌ها با محتوای فایل جایگزین می‌شوند. مطمئن هستید؟",
            confirmLabel = "انتخاب فایل",
            destructive = true,
            onConfirm = {
                showRestoreConfirm = false
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            },
            onDismiss = { showRestoreConfirm = false }
        )
    }
}

@Composable
private fun BackupActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    loading: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val palette = AshpaTheme.palette
    AshpaCard(Modifier.fillMaxWidth(), corner = 22.dp) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(if (destructive) palette.peach.copy(alpha = 0.4f) else palette.sageSoft),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = if (destructive) palette.terracotta else palette.forest, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.ink)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = palette.muted)
            Spacer(Modifier.height(12.dp))
            if (destructive) {
                SecondaryButton(buttonLabel, onClick = onClick, icon = Icons.Rounded.Restore, enabled = !loading)
            } else {
                PrimaryButton(buttonLabel, onClick = onClick, icon = Icons.Rounded.FileUpload, loading = loading, enabled = !loading)
            }
        }
    }
}

@Composable
private fun ResultCard(result: BackupResult, exported: Boolean) {
    val palette = AshpaTheme.palette
    AshpaCard(Modifier.fillMaxWidth(), corner = 22.dp, color = palette.sageSoft) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = palette.success)
                Spacer(Modifier.width(7.dp))
                Text(if (exported) "پشتیبان ساخته شد" else "بازیابی انجام شد", style = MaterialTheme.typography.titleMedium, color = palette.ink)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                PersianText.faDigits(result.recipes.toString()) + " دستور شخصی · " +
                    PersianText.faDigits(result.inventoryItems.toString()) + " ماده · " +
                    PersianText.faDigits(result.mealPlanItems.toString()) + " وعده",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink
            )
            Text(
                PersianText.faDigits(result.shoppingItems.toString()) + " قلم خرید · " +
                    PersianText.faDigits(result.leftovers.toString()) + " باقی‌مانده",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
        }
    }
}
