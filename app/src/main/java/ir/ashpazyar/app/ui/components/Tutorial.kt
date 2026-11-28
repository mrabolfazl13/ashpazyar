package ir.ashpazyar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ir.ashpazyar.app.domain.model.ValidationStatus
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun TutorialBulletSection(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.CheckCircle,
    tone: Color? = null
) {
    if (items.isEmpty()) return
    val palette = AshpaTheme.palette
    val accent = tone ?: palette.forest
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
        }
        Spacer(Modifier.height(7.dp))
        items.forEach { item ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.padding(top = 7.dp).size(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.7f))
                )
                Spacer(Modifier.width(8.dp))
                Text(item, style = MaterialTheme.typography.bodySmall, color = palette.muted)
            }
        }
    }
}

@Composable
fun TutorialParagraph(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    tone: Color? = null
) {
    if (text.isBlank()) return
    val palette = AshpaTheme.palette
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = tone ?: palette.forest, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
        }
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = palette.muted)
    }
}

@Composable
fun TutorialFactRow(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.muted, modifier = Modifier.width(98.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = palette.ink, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ValidationPill(status: ValidationStatus, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    val (label, color) = when (status) {
        ValidationStatus.VERIFIED -> "آموزش تأییدشده" to palette.success
        ValidationStatus.REVIEW_REQUIRED -> "پیش‌نویس؛ نیازمند بازبینی" to palette.warning
        ValidationStatus.REJECTED -> "ردشده" to palette.danger
    }
    Row(
        modifier = modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
