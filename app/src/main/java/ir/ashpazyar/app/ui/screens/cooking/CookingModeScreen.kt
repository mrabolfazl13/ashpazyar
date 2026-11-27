package ir.ashpazyar.app.ui.screens.cooking

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ashpazyar.app.core.util.ClockFormat
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.RecipeStep
import ir.ashpazyar.app.ui.components.AshpaSnackbarHost
import ir.ashpazyar.app.ui.components.ConfirmDialog
import ir.ashpazyar.app.ui.components.EmptyState

private val CookingCanvas = Color(0xFF13241A)
private val CookingPanelTop = Color(0xFF294331)
private val CookingPanelBottom = Color(0xFF1C3124)
private val CookingSurface = Color(0xFF1A2E21)
private val CookingInk = Color(0xFFF7F0E2)
private val CookingMuted = Color(0xFFC6CDBE)
private val CookingFaint = Color(0xFF96A18F)
private val CookingAccent = Color(0xFFE0A578)
private val CookingAccentSoft = Color(0xFFE6B88C)
private val CookingSage = Color(0xFFA8C995)
private val CookingDark = Color(0xFF213323)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    onExit: () -> Unit,
    onOpenTimers: () -> Unit,
    viewModel: CookingModeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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

    Box(
        Modifier
            .fillMaxSize()
            .background(CookingCanvas)
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CookingIconButton(onClick = { showExit = true }) {
                    Icon(Icons.Rounded.Close, "خروج", tint = CookingInk)
                }
                Text(
                    "حالت پخت",
                    style = MaterialTheme.typography.labelLarge,
                    color = CookingAccentSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                CookingIconButton(onClick = onOpenTimers) {
                    Icon(Icons.Rounded.Timer, "تایمرها", tint = CookingInk)
                }
            }
            if (state.totalSteps > 0) {
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        state.recipe?.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = CookingInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "مرحله " + PersianText.faDigits((state.stepIndex + 1).toString()) + " از " + PersianText.faDigits(state.totalSteps.toString()),
                        style = MaterialTheme.typography.labelMedium,
                        color = CookingAccentSoft
                    )
                }
                Spacer(Modifier.height(12.dp))
                StepProgressRail(
                    totalSteps = state.totalSteps,
                    currentIndex = state.stepIndex,
                    completedSteps = state.completedSteps
                )
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (state.allCompleted) "همه مراحل کامل شد"
                        else PersianText.faDigits(state.completedCount.toString()) + " از " + PersianText.faDigits(state.totalSteps.toString()) + " مرحله انجام شده",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.allCompleted) CookingSage else CookingFaint
                    )
                    if (state.allCompleted) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.CheckCircle, null, tint = CookingSage, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            val step = state.step
            if (step != null) {
                AnimatedContent(
                    targetState = state.stepIndex,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically(tween(340)) { it / 8 } + fadeIn(tween(280)))
                                .togetherWith(slideOutVertically(tween(220)) { -it / 12 } + fadeOut(tween(160)))
                        } else {
                            (slideInVertically(tween(340)) { -it / 8 } + fadeIn(tween(280)))
                                .togetherWith(slideOutVertically(tween(220)) { it / 12 } + fadeOut(tween(160)))
                        }
                    },
                    label = "cooking-step"
                ) { index ->
                    val visibleStep = state.recipe?.steps?.getOrNull(index)
                    if (visibleStep != null) {
                        StepPanel(
                            step = visibleStep,
                            index = index,
                            isLast = index == state.totalSteps - 1,
                            completed = index in state.completedSteps,
                            onToggleCompletion = { viewModel.toggleStep(index) },
                            onStartTimer = viewModel::startStepTimer
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                CookingActions(
                    stepCompleted = state.stepCompleted,
                    isLast = state.isLast,
                    canGoBack = state.stepIndex > 0,
                    onMaterials = { showIngredients = true },
                    onPrevious = viewModel::previous,
                    onPrimary = {
                        when {
                            !state.stepCompleted -> viewModel.completeCurrentStep()
                            state.isLast -> onExit()
                            else -> viewModel.next()
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))
            } else if (!state.loading) {
                EmptyState("مرحله‌ای ثبت نشده", "این دستور هنوز مرحله‌بندی نشده است.", actionLabel = "بازگشت", onAction = onExit)
            }
        }
        AshpaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 78.dp))
    }

    if (showExit) {
        ConfirmDialog(
            title = "از حالت پخت خارج شوید؟",
            message = if (state.completedCount > 0) {
                "علامت مراحل این نشست پاک می‌شود؛ تایمرهای فعال در پس‌زمینه ادامه می‌دهند."
            } else {
                "تایمرهای فعال در پس‌زمینه ادامه می‌دهند و به شما اطلاع می‌دهند."
            },
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
            containerColor = CookingSurface,
            contentColor = CookingInk
        ) {
            Column(Modifier.padding(horizontal = 22.dp).navigationBarsPadding()) {
                Text("مواد لازم", style = MaterialTheme.typography.headlineSmall, color = CookingInk)
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(state.recipe!!.ingredients, key = { it.ingredientId + it.unit.name }) { line ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(line.ingredientName, style = MaterialTheme.typography.bodyLarge, color = CookingInk, modifier = Modifier.weight(1f))
                            Text(
                                PersianText.faDigits(if (line.amount % 1.0 == 0.0) line.amount.toInt().toString() else String.format("%.1f", line.amount)) + " " + line.unit.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = CookingAccentSoft
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StepProgressRail(
    totalSteps: Int,
    currentIndex: Int,
    completedSteps: Set<Int>
) {
    if (totalSteps <= 12) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(8.dp)
        ) {
            repeat(totalSteps) { index ->
                val isCompleted = index in completedSteps
                val isCurrent = index == currentIndex
                val height by animateDpAsState(if (isCurrent) 8.dp else 5.dp, label = "step-rail-height")
                val color by animateColorAsState(
                    when {
                        isCurrent && !isCompleted -> CookingAccent
                        isCompleted -> CookingSage
                        else -> Color(0x33FFFFFF)
                    },
                    label = "step-rail-color"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(height)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }
    } else {
        LinearProgressIndicator(
            progress = { completedSteps.size.toFloat() / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
            color = CookingSage,
            trackColor = Color(0x33FFFFFF)
        )
    }
}

@Composable
private fun StepPanel(
    step: RecipeStep,
    index: Int,
    isLast: Boolean,
    completed: Boolean,
    onToggleCompletion: () -> Unit,
    onStartTimer: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (completed) CookingSage.copy(alpha = 0.55f) else Color(0x26E6B88C),
        label = "step-border"
    )
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.verticalGradient(listOf(CookingPanelTop, CookingPanelBottom)))
            .border(1.dp, borderColor, RoundedCornerShape(30.dp))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepNumberBadge(index = index, completed = completed)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "مرحله " + PersianText.faDigits((index + 1).toString()),
                        style = MaterialTheme.typography.labelMedium,
                        color = CookingAccentSoft
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (step.timerSeconds != null) "این قدم زمان‌دار است" else "با آرامش، قدم به قدم",
                        style = MaterialTheme.typography.bodySmall,
                        color = CookingFaint
                    )
                }
                if (completed) {
                    Icon(Icons.Rounded.CheckCircle, "این مرحله کامل شد", tint = CookingSage, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                step.title.ifBlank { "قدم " + PersianText.faDigits((index + 1).toString()) },
                style = MaterialTheme.typography.headlineLarge,
                color = CookingInk
            )
            Spacer(Modifier.height(12.dp))
            Text(
                step.instruction,
                style = MaterialTheme.typography.headlineSmall,
                color = CookingMuted,
                lineHeight = 34.sp
            )
            if (step.timerSeconds != null) {
                Spacer(Modifier.height(22.dp))
                TimerCallout(seconds = step.timerSeconds, onStartTimer = onStartTimer)
            }
            Spacer(Modifier.height(22.dp))
            HorizontalDivider(color = Color(0x22FFFFFF))
            Spacer(Modifier.height(18.dp))
            CompletionToggle(
                completed = completed,
                isLast = isLast,
                onToggle = onToggleCompletion
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (isLast) "تا این مرحله را تأیید نکنید، دکمه «پایان پخت» فعال نمی‌شود."
                else "تا این مرحله را تأیید نکنید، دکمه «مرحله بعد» فعال نمی‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = CookingFaint
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StepNumberBadge(index: Int, completed: Boolean) {
    val background by animateColorAsState(if (completed) CookingSage else CookingAccent, label = "step-badge")
    Box(
        modifier = Modifier.size(54.dp).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center
    ) {
        if (completed) {
            Icon(Icons.Rounded.Check, "تکمیل شده", tint = CookingDark, modifier = Modifier.size(28.dp))
        } else {
            Text(
                PersianText.faDigits((index + 1).toString()),
                style = MaterialTheme.typography.headlineSmall,
                color = CookingDark,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimerCallout(seconds: Int, onStartTimer: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CookingSurface)
            .border(1.dp, Color(0x33E6B88C), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(CookingAccent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Timer, null, tint = CookingAccentSoft, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("زمان پیشنهادی این مرحله", style = MaterialTheme.typography.titleSmall, color = CookingInk)
            Spacer(Modifier.height(2.dp))
            Text(timerLabel(seconds), style = MaterialTheme.typography.bodySmall, color = CookingMuted)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onStartTimer,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CookingAccent, contentColor = CookingDark),
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 10.dp)
        ) { Text("شروع", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun CompletionToggle(
    completed: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit
) {
    val fill by animateColorAsState(
        if (completed) Color(0xFF33513C) else CookingSurface,
        label = "completion-fill"
    )
    val border by animateColorAsState(
        if (completed) CookingSage.copy(alpha = 0.65f) else Color(0x33FFFFFF),
        label = "completion-border"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .toggleable(value = completed, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(29.dp)
                .clip(CircleShape)
                .background(if (completed) CookingSage else Color.Transparent)
                .border(1.5.dp, if (completed) CookingSage else Color(0x88E6B88C), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Rounded.Check, null, tint = CookingDark, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (completed) "این مرحله کامل شد" else "این مرحله را انجام دادم",
                style = MaterialTheme.typography.titleSmall,
                color = if (completed) CookingSage else CookingInk
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (completed) {
                    if (isLast) "برای پایان پخت، دکمه پایین را بزنید" else "حالا می‌توانید به مرحله بعد بروید"
                } else {
                    "برای فعال شدن مرحله بعد، این بخش را لمس کنید"
                },
                style = MaterialTheme.typography.bodySmall,
                color = CookingFaint
            )
        }
    }
}

@Composable
private fun CookingActions(
    stepCompleted: Boolean,
    isLast: Boolean,
    canGoBack: Boolean,
    onMaterials: () -> Unit,
    onPrevious: () -> Unit,
    onPrimary: () -> Unit
) {
    val primaryLabel = when {
        !stepCompleted -> "این مرحله انجام شد"
        isLast -> "پایان پخت"
        else -> "مرحله بعد"
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onMaterials,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x446E8067)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CookingInk),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(Icons.Rounded.ShoppingBasket, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text("مواد", style = MaterialTheme.typography.labelMedium)
        }
        if (canGoBack) {
            OutlinedButton(
                onClick = onPrevious,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0x446E8067)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CookingInk),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Rounded.ChevronRight, "مرحله قبلی", modifier = Modifier.size(22.dp))
            }
        }
        Button(
            onClick = onPrimary,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CookingAccent, contentColor = CookingDark),
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier.weight(1f).height(56.dp)
        ) {
            Text(
                primaryLabel,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                when {
                    !stepCompleted -> Icons.Rounded.Check
                    isLast -> Icons.Rounded.Check
                    else -> Icons.Rounded.ChevronLeft
                },
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun CookingIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0x1FFFFFFF))
            .border(1.dp, Color(0x22FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

private fun timerLabel(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return when {
        minutes == 0 -> PersianText.faDigits(seconds.toString()) + " ثانیه"
        remainder == 0 -> ClockFormat.duration(minutes)
        else -> ClockFormat.duration(minutes) + " و " + PersianText.faDigits(remainder.toString()) + " ثانیه"
    }
}
