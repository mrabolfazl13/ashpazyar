package ir.ashpazyar.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.ashpazyar.app.core.util.ClockFormat
import ir.ashpazyar.app.core.util.JalaliCalendar
import ir.ashpazyar.app.core.util.PersianText
import ir.ashpazyar.app.domain.model.Difficulty
import ir.ashpazyar.app.domain.model.Recipe
import ir.ashpazyar.app.ui.theme.AshpaTheme
import java.time.LocalDate

@Composable
fun AshpaBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val palette = AshpaTheme.palette
    Box(modifier = modifier.fillMaxSize().background(palette.background), content = content)
}

@Composable
fun AshpaCard(
    modifier: Modifier = Modifier,
    color: Color? = null,
    corner: androidx.compose.ui.unit.Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = AshpaTheme.palette
    val shape = RoundedCornerShape(corner)
    Surface(
        modifier = modifier.shadow(12.dp, shape, ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.08f)),
        shape = shape,
        color = color ?: palette.surface,
        tonalElevation = 0.dp,
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Column(content = content)
    }
}

@Composable
fun AshpaTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = AshpaTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            AshpaIconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowForward, contentDescription = "بازگشت", tint = palette.ink)
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = palette.ink,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        action?.invoke()
    }
}

@Composable
fun AshpaIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = AshpaTheme.palette
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (filled) palette.forest else palette.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun ScreenHeader(
    eyebrow: String? = null,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val palette = AshpaTheme.palette
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.terracotta,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(title, style = MaterialTheme.typography.displaySmall, color = palette.ink)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            }
        }
        action?.invoke()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val palette = AshpaTheme.palette
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.bodySmall, color = palette.muted)
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = palette.terracotta,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val palette = AshpaTheme.palette
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.forest,
            contentColor = if (palette.isDark) Color(0xFF102018) else Color(0xFFFFFBF2),
            disabledContainerColor = palette.forest.copy(alpha = 0.35f),
            disabledContentColor = palette.ink.copy(alpha = 0.55f)
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = palette.ink
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val palette = AshpaTheme.palette
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, palette.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.ink),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Pill(
    text: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val palette = AshpaTheme.palette
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) palette.forest else palette.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (selected) {
            if (palette.isDark) Color(0xFF102018) else Color(0xFFFFFBF2)
        } else palette.muted
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = palette.muted,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surfaceHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        maxLines = 1
    )
}

@Composable
fun AshpaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    error: String? = null,
    readOnly: Boolean = false
) {
    val palette = AshpaTheme.palette
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = palette.muted.copy(alpha = 0.7f)) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = palette.muted) } },
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(17.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.surface,
            unfocusedContainerColor = palette.surface,
            focusedBorderColor = palette.forest,
            unfocusedBorderColor = palette.line,
            focusedTextColor = palette.ink,
            unfocusedTextColor = palette.ink,
            cursorColor = palette.terracotta,
            focusedLabelColor = palette.forest,
            unfocusedLabelColor = palette.muted,
            errorBorderColor = palette.danger,
            errorTextColor = palette.danger
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        readOnly = readOnly,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun AshpaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    AshpaTextField(
        value = value,
        onValueChange = onValueChange,
        label = "",
        placeholder = placeholder,
        leadingIcon = Icons.Rounded.Search,
        modifier = modifier
    )
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    matchPercent: Int? = null,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null
) {
    val palette = AshpaTheme.palette
    AshpaCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FoodArt(
                style = recipe.heroStyle,
                seed = recipe.id.hashCode(),
                modifier = Modifier.size(width = 112.dp, height = 124.dp),
                corner = 18.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        recipe.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (onFavoriteClick != null) {
                        IconButton(onClick = onFavoriteClick, modifier = Modifier.size(34.dp)) {
                            Icon(
                                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isFavorite) "حذف از محبوب‌ها" else "افزودن به محبوب‌ها",
                                tint = if (isFavorite) palette.terracotta else palette.muted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ClockFormat.duration(recipe.totalMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted
                    )
                    Text(" • ", color = palette.muted)
                    Text(
                        recipe.difficulty.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    recipe.tags.take(if (recipe.steps.isEmpty()) 1 else 2).forEach { TagChip(it) }
                    if (recipe.steps.isEmpty()) TagChip("بدون مراحل")
                    if (matchPercent != null) {
                        Text(
                            text = PersianText.faDigits(matchPercent.toString()) + "٪ مواد",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (matchPercent >= 100) palette.success else palette.terracotta,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (matchPercent >= 100) palette.sageSoft else palette.peach.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactRecipeCard(
    recipe: Recipe,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isFavorite: Boolean = false
) {
    val palette = AshpaTheme.palette
    Column(
        modifier = modifier
            .width(224.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface)
            .clickable(onClick = onClick)
    ) {
        Box {
            FoodArt(
                style = recipe.heroStyle,
                seed = recipe.id.hashCode(),
                modifier = Modifier.fillMaxWidth().height(142.dp),
                corner = 24.dp
            )
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(palette.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = palette.terracotta, modifier = Modifier.size(16.dp))
                }
            }
        }
        Column(Modifier.padding(13.dp)) {
            Text(
                recipe.title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                ClockFormat.duration(recipe.totalMinutes) + " • " + recipe.difficulty.label,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
        }
    }
}

@Composable
fun MetaChip(icon: ImageVector, text: String) {
    val palette = AshpaTheme.palette
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceHigh)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.forest, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = palette.ink)
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector? = null
) {
    val palette = AshpaTheme.palette
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(palette.sageSoft),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) Icon(icon, null, tint = palette.forest, modifier = Modifier.size(34.dp))
            else Text("🍃", style = MaterialTheme.typography.displaySmall)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = palette.muted, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, palette.forest),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.forest)
            ) { Text(actionLabel) }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String = "در حال آماده‌سازی…") {
    val palette = AshpaTheme.palette
    Column(
        modifier = modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = palette.terracotta, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(14.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    Column(
        modifier = modifier.fillMaxWidth().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌿", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = palette.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = "تلاش دوباره", onClick = onRetry, modifier = Modifier.widthIn(max = 220.dp))
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "تأیید",
    dismissLabel: String = "انصراف",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = AshpaTheme.palette
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = palette.surface,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.ink) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = palette.muted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) palette.danger else palette.terracotta)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel, color = palette.muted) }
        }
    )
}

@Composable
fun QuantityStepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 30
) {
    val palette = AshpaTheme.palette
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(palette.surfaceHigh)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(enabled = value > min, onClick = { onChange((value - 1).coerceAtLeast(min)) }, plus = false)
        Text(
            PersianText.faDigits(value.toString()),
            style = MaterialTheme.typography.titleMedium,
            color = palette.ink,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        StepButton(enabled = value < max, onClick = { onChange((value + 1).coerceAtMost(max)) }, plus = true)
    }
}

@Composable
private fun StepButton(enabled: Boolean, onClick: () -> Unit, plus: Boolean) {
    val palette = AshpaTheme.palette
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (enabled) palette.surface else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(if (plus) "+" else "−", style = MaterialTheme.typography.titleLarge, color = if (enabled) palette.ink else palette.muted.copy(alpha = 0.4f))
    }
}

@Composable
fun AshpaSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AshpaTheme.palette
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.surface,
                checkedTrackColor = palette.forest,
                uncheckedThumbColor = palette.muted,
                uncheckedTrackColor = palette.surfaceHigh
            )
        )
    }
}

@Composable
fun WeekDaySelector(
    weekStart: Long,
    selected: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AshpaTheme.palette
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        for (offset in 0..6) {
            val day = weekStart + offset
            val selectedDay = day == selected
            val date = JalaliCalendar.dateOfEpochDay(day)
            val jalali = JalaliCalendar.from(date)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selectedDay) palette.forest else palette.surface)
                    .clickable { onSelect(day) }
                    .padding(vertical = 10.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    JalaliCalendar.weekdayName(JalaliCalendar.epochMillisOfEpochDay(day)).take(2),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedDay) palette.peach else palette.muted
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    PersianText.faDigits(jalali.day.toString()),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selectedDay) Color(0xFFFFFBF2) else palette.ink
                )
            }
        }
    }
}

@Composable
fun SegmentedOptions(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AshpaTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceHigh)
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (selected) palette.surface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) palette.ink else palette.muted,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AshpaSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val palette = AshpaTheme.palette
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.ink,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, null, tint = palette.background, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.background,
                    modifier = Modifier.weight(1f)
                )
                val actionLabel = data.visuals.actionLabel
                if (actionLabel != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.peach,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { data.performAction() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InlineNotice(text: String, modifier: Modifier = Modifier, tone: String = "sage") {
    val palette = AshpaTheme.palette
    val background = if (tone == "warm") palette.peach.copy(alpha = 0.35f) else palette.sageSoft
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = palette.ink,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    )
}
