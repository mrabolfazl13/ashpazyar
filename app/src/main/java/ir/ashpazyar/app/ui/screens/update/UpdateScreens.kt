package ir.ashpazyar.app.ui.screens.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.ui.components.PrimaryButton
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun UpdateOverlay(
    state: UpdateUiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenBazaar: () -> Unit,
    onCloseMessage: () -> Unit
) {
    when (state.phase) {
        UpdatePhase.FORCED -> ForcedUpdateScreen(state, onRetry, onOpenBazaar)
        UpdatePhase.AVAILABLE -> OptionalUpdateDialog(state, onDismiss, onOpenBazaar, onCloseMessage)
        UpdatePhase.CHECKING -> if (state.manual) CheckingDialog()
        UpdatePhase.UP_TO_DATE -> if (state.manual) InfoDialog(
            title = "قاشق به‌روز است",
            message = "نسخه ${PersianText.faDigits(state.currentVersion)} آخرین نسخه منتشرشده در کانال ${channelLabel()} است.",
            onDismiss = onCloseMessage
        )
        UpdatePhase.ERROR -> if (state.manual) InfoDialog(
            title = "بررسی به‌روزرسانی ممکن نشد",
            message = state.message ?: "اتصال اینترنت را بررسی کنید و دوباره تلاش کنید.",
            confirmLabel = "تلاش دوباره",
            onConfirm = onRetry,
            onDismiss = onCloseMessage
        )
        UpdatePhase.IDLE -> Unit
    }
}

@Composable
private fun ForcedUpdateScreen(
    state: UpdateUiState,
    onRetry: () -> Unit,
    onOpenBazaar: () -> Unit
) {
    val palette = AshpaTheme.palette
    val manifest = state.manifest
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(92.dp).background(palette.forest, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = palette.peach, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text("به‌روزرسانی الزامی", style = MaterialTheme.typography.displaySmall, color = palette.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "برای ادامه استفاده از قاشق، نسخه تازه را از بازار نصب کنید.",
            style = MaterialTheme.typography.bodyLarge,
            color = palette.muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        AshpaUpdateCard {
            Text("نسخه فعلی: ${PersianText.faDigits(state.currentVersion)}", style = MaterialTheme.typography.bodyMedium, color = palette.ink)
            Spacer(Modifier.height(5.dp))
            Text("نسخه لازم: ${PersianText.faDigits(manifest?.latestVersion ?: "—")}", style = MaterialTheme.typography.bodyMedium, color = palette.ink)
            if (manifest != null && manifest.releaseNotes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("تغییرات این نسخه", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                Spacer(Modifier.height(4.dp))
                Text(manifest.releaseNotes, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            }
        }
        if (state.message != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.message, style = MaterialTheme.typography.bodySmall, color = palette.danger, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            text = "رفتن به بازار",
            onClick = onOpenBazaar,
            icon = Icons.Rounded.Storefront,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "دانلود و نصب فقط در برنامه بازار انجام می‌شود. پس از نصب، قاشق را دوباره باز کنید.",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            textAlign = TextAlign.Center
        )
        if (state.message != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("بررسی دوباره", color = palette.terracotta) }
        }
    }
}

@Composable
private fun OptionalUpdateDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onOpenBazaar: () -> Unit,
    onCloseMessage: () -> Unit
) {
    val manifest = state.manifest ?: return
    val palette = AshpaTheme.palette
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SystemUpdate, null, tint = palette.terracotta) },
        title = { Text("نسخه تازه قاشق آماده است") },
        text = {
            Column {
                Text(
                    "نسخه ${PersianText.faDigits(manifest.latestVersion)} در بازار منتشر شده است. به‌روزرسانی اختیاری است و می‌توانید بعداً انجام دهید.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (manifest.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("تغییرات این نسخه", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(3.dp))
                    Text(manifest.releaseNotes, style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
                if (state.message != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(state.message, style = MaterialTheme.typography.bodySmall, color = palette.danger)
                    TextButton(onClick = onCloseMessage) { Text("بستن پیام") }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "دانلود و نصب فقط از برنامه بازار انجام می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenBazaar) { Text("رفتن به بازار") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بعداً") }
        }
    )
}

@Composable
private fun CheckingDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("بررسی به‌روزرسانی") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("در حال بررسی نسخه در GitHub…")
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun InfoDialog(
    title: String,
    message: String,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm ?: onDismiss) { Text(confirmLabel ?: "باشه") }
        },
        dismissButton = if (confirmLabel != null) {
            { TextButton(onClick = onDismiss) { Text("بستن") } }
        } else null
    )
}

@Composable
private fun AshpaUpdateCard(content: @Composable ColumnScope.() -> Unit) {
    val palette = AshpaTheme.palette
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = palette.surface
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

private fun channelLabel(): String = when (ir.ashpazyar.app.BuildConfig.UPDATE_CHANNEL) {
    "beta" -> "بتا"
    "dev" -> "توسعه"
    else -> "پایدار"
}
