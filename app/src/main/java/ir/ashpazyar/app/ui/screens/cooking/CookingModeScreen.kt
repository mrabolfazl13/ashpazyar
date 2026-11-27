package ir.ashpazyar.app.ui.screens.cooking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.ClockFormat
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    onExit: () -> Unit,
    onOpenTimers: () -> Unit,
    viewModel: CookingModeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = AshpaTheme.palette
    val snackbar = remember { SnackbarHostState() }
    val view = LocalView.current
    var showExit by remember { mutableStateOf(false) }
    var showIngredients by remember { mutableStateOf(false) }

    DisposableEffect(view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    BackHandler(enabled = true) { showExit = true }

    val ink = Color(0xFFF7F0E2)
    val muted = Color(0xFFC6CDBE)
    val card = Color(0xFF263D2E)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF16271D))
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AshpaIconButton(onClick = { showExit = true }, filled = false) {
                    Icon(Icons.Rounded.Close, "خروج", tint = ink)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "حالت پخت",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE6B88C)
                )
                Spacer(Modifier.weight(1f))
                AshpaIconButton(onClick = onOpenTimers, filled = false) {
                    Icon(Icons.Rounded.Timer, "تایمرها", tint = ink)
                }
            }
            if (state.totalSteps > 0) {
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "مرحله " + PersianText.faDigits((state.stepIndex + 1).toString()) + " از " + PersianText.faDigits(state.totalSteps.toString()),
                        style = MaterialTheme.typography.labelMedium,
                        color = muted
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        state.recipe?.title.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE6B88C)
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (state.stepIndex + 1f) / state.totalSteps.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                    color = Color(0xFFE0A578),
                    trackColor = Color(0x33FFFFFF)
                )
            }
            Spacer(Modifier.height(26.dp))
            val step = state.step
            if (step != null) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Box(
                        modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFE0A578)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            PersianText.faDigits((state.stepIndex + 1).toString()),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF213323)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        step.title.ifBlank { "قدم " + PersianText.faDigits((state.stepIndex + 1).toString()) },
                        style = MaterialTheme.typography.headlineLarge,
                        color = ink
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        step.instruction,
                        style = MaterialTheme.typography.headlineSmall,
                        color = muted,
                        lineHeight = androidx.compose.ui.unit.TextUnit(34f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                    if (step.timerSeconds != null) {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(card)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Timer, null, tint = Color(0xFFE6B88C), modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text("این مرحله زمان‌دار است", style = MaterialTheme.typography.titleSmall, color = ink)
                                Text(ClockFormat.duration(step.timerSeconds / 60), style = MaterialTheme.typography.bodySmall, color = muted)
                            }
                            Button(
                                onClick = viewModel::startStepTimer,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0A578), contentColor = Color(0xFF213323))
                            ) { Text("شروع تایمر") }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "نکته: آشپزیار در طول پخت کنار شماست. اگر لازم شد، تایمرها را از بالای صفحه ببینید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF96A18F)
                    )
                    Spacer(Modifier.height(20.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showIngredients = true },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x446E8067)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ink),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Icon(Icons.Rounded.ShoppingBasket, null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("مواد")
                    }
                    if (state.stepIndex > 0) {
                        OutlinedButton(
                            onClick = viewModel::previous,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x446E8067)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ink),
                            modifier = Modifier.height(54.dp)
                        ) { Icon(Icons.Rounded.ChevronRight, "قبلی") }
                    }
                    Button(
                        onClick = {
                            if (state.isLast) onExit() else viewModel.next()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0A578), contentColor = Color(0xFF213323)),
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        Text(if (state.isLast) "پایان و بازگشت" else "مرحله بعد")
                        Spacer(Modifier.width(6.dp))
                        Icon(if (state.isLast) Icons.Rounded.Check else Icons.Rounded.ChevronLeft, null)
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else if (!state.loading) {
                EmptyState("مرحله‌ای ثبت نشده", "این دستور هنوز مرحله‌بندی نشده است.", actionLabel = "بازگشت", onAction = onExit)
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 78.dp))
    }

    if (showExit) {
        ConfirmDialog(
            title = "از حالت پخت خارج شوید؟",
            message = "تایمرهای فعال در پس‌زمینه ادامه می‌دهند و به شما اطلاع می‌دهند.",
            confirmLabel = "خروج",
            onConfirm = {
                showExit = false
                onExit()
            },
            onDismiss = { showExit = false }
        )
    }
    if (showIngredients && state.recipe != null) {
        ModalBottomSheet(
            onDismissRequest = { showIngredients = false },
            containerColor = Color(0xFF263D2E),
            contentColor = ink
        ) {
            Column(Modifier.padding(horizontal = 22.dp).navigationBarsPadding()) {
                Text("مواد لازم", style = MaterialTheme.typography.headlineSmall, color = ink)
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(state.recipe!!.ingredients, key = { it.ingredientId + it.unit.name }) { line ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(line.ingredientName, style = MaterialTheme.typography.bodyLarge, color = ink, modifier = Modifier.weight(1f))
                            Text(
                                PersianText.faDigits(if (line.amount % 1.0 == 0.0) line.amount.toInt().toString() else String.format("%.1f", line.amount)) + " " + line.unit.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFE6B88C)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
