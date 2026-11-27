package ir.ashpazyar.app.ui.screens.timers

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.ClockFormat
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.CookingTimer
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun TimersScreen(
    onBack: () -> Unit,
    viewModel: TimersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    var customMinutes by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 26.dp)) {
            item {
                Column(Modifier.statusBarsPadding()) {
                    AshpaTopBar(title = "تایمرهای آشپزی", onBack = onBack)
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text("هیچ‌چیز نسوزد", style = MaterialTheme.typography.displaySmall, color = palette.ink)
                        Text(
                            "تایمر بسازید؛ حتی اگر از برنامه بیرون بروید، در زمان مقرر خبر می‌دهیم.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.forest).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Timer, null, tint = Color(0xFFE6B88C), modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (state.timers.isEmpty()) "تایمر فعالی نیست" else PersianText.faDigits(state.timers.size.toString()) + " تایمر در جریان",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFFFFBF2)
                                )
                                Text("می‌توانید هم‌زمان چند تایمر داشته باشید", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD6DFD1))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("تایمر سریع", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                        Spacer(Modifier.height(7.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(3, 5, 10, 15, 30, 45)) { minutes ->
                                Pill(
                                    PersianText.faDigits(minutes.toString()) + " دقیقه",
                                    onClick = { viewModel.start(minutes) }
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Box(Modifier.weight(1f)) {
                                AshpaTextField(
                                    value = customMinutes,
                                    onValueChange = { customMinutes = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                                    label = "دقیقه دلخواه",
                                    placeholder = "مثلاً ۲۵",
                                    keyboardType = KeyboardType.Number
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            PrimaryButton(
                                "شروع",
                                onClick = {
                                    val minutes = PersianText.latinDigits(customMinutes).toIntOrNull() ?: 0
                                    if (minutes > 0) {
                                        viewModel.start(minutes)
                                        customMinutes = ""
                                    }
                                },
                                icon = Icons.Rounded.PlayArrow,
                                modifier = Modifier.weight(0.55f),
                                enabled = customMinutes.isNotBlank()
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        SectionHeader(title = "در جریان", caption = if (state.timers.isEmpty()) "تایمری فعال نیست" else "به ترتیب نزدیک‌ترین پایان")
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            if (state.timers.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = "آشپزخانه آرام است",
                        description = "یک تایمر سریع بسازید یا از داخل مراحل پخت، تایمر همان مرحله را شروع کنید.",
                        icon = Icons.Rounded.Timer
                    )
                }
            } else {
                items(state.timers, key = { it.timer.id }) { display ->
                    TimerCard(
                        display = display,
                        onStop = { viewModel.stop(display.timer) },
                        onDelete = { viewModel.delete(display.timer) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
    }
}

@Composable
private fun TimerCard(display: TimerDisplay, onStop: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    AshpaCard(modifier = modifier.fillMaxWidth(), corner = 22.dp) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                CircularProgressIndicator(
                    progress = { display.progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 4.dp,
                    color = palette.terracotta,
                    trackColor = palette.surfaceHigh
                )
                Icon(Icons.Rounded.Timer, null, tint = palette.terracotta, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(display.timer.label, style = MaterialTheme.typography.titleSmall, color = palette.ink)
                Text(
                    ClockFormat.countdown(display.remainingSeconds),
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.ink
                )
                Text(
                    "از " + ClockFormat.duration(display.timer.totalSeconds / 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                AshpaIconButton(onClick = onStop) { Icon(Icons.Rounded.Pause, "توقف", tint = palette.ink) }
                Text(
                    "حذف",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.danger,
                    modifier = Modifier.clickable(onClick = onDelete).padding(7.dp)
                )
            }
        }
    }
}
