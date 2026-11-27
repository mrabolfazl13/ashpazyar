package ir.ashpazyar.app.ui.screens.tips

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
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun TipDetailScreen(
    onBack: () -> Unit,
    viewModel: TipDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val tip = state.tip

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        AshpaTopBar(title = "نکته آشپزی", onBack = onBack)
        if (tip == null) {
            if (state.loading) LoadingState()
            else EmptyState(
                title = "نکته پیدا نشد",
                description = "این نکته حذف شده یا در دسترس نیست.",
                icon = Icons.Rounded.Lightbulb
            )
            return@Column
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(tip.category, style = MaterialTheme.typography.labelMedium, color = palette.terracotta)
            Spacer(Modifier.height(5.dp))
            Text(tip.title, style = MaterialTheme.typography.displaySmall, color = palette.ink)
            Spacer(Modifier.height(7.dp))
            Text(tip.summary, style = MaterialTheme.typography.titleSmall, color = palette.muted)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.forest).padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = Color(0xFFE6B88C), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(11.dp))
                    Text(
                        tip.summary,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFFBF2)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            tip.content.split("\n").filter { it.isNotBlank() }.forEach { paragraph ->
                Text(
                    paragraph.trim(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.ink,
                    modifier = Modifier.padding(bottom = 11.dp)
                )
            }
            if (tip.safety) {
                Spacer(Modifier.height(5.dp))
                InlineNotice(
                    "این نکته به ایمنی غذا مربوط است. برای اطمینان، دمای پخت و نگهداری را جدی بگیرید.",
                    tone = "warm"
                )
            }
            Spacer(Modifier.height(20.dp))
            AshpaCard(Modifier.fillMaxWidth(), corner = 20.dp) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MenuBook, null, tint = palette.terracotta)
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text("در دستورها به کار ببرید", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                        Text("این دانش را در آشپزی روزمره تمرین کنید.", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
