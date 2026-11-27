package ir.ashpazyar.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.view.WindowCompat

data class AshpaPalette(
    val ink: Color,
    val muted: Color,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val forest: Color,
    val sage: Color,
    val sageSoft: Color,
    val terracotta: Color,
    val peach: Color,
    val line: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val isDark: Boolean
)

val LocalAshpaPalette = staticCompositionLocalOf {
    AshpaPalette(
        ink = Charcoal, muted = MutedInk, background = Cream, surface = WarmWhite,
        surfaceHigh = Color(0xFFF1E8DA), forest = Forest, sage = Sage, sageSoft = SageLight,
        terracotta = Terracotta, peach = SoftPeach, line = Line, success = Success,
        warning = Warning, danger = Danger, isDark = false
    )
}

private val LightScheme = lightColorScheme(
    primary = Forest,
    onPrimary = WarmWhite,
    primaryContainer = SageLight,
    onPrimaryContainer = DeepForest,
    secondary = Terracotta,
    onSecondary = WarmWhite,
    secondaryContainer = SoftPeach,
    onSecondaryContainer = TerracottaDeep,
    tertiary = Sage,
    onTertiary = WarmWhite,
    background = Cream,
    onBackground = Charcoal,
    surface = WarmWhite,
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFF0E7D9),
    onSurfaceVariant = MutedInk,
    outline = Line,
    outlineVariant = Sand,
    error = Danger,
    onError = WarmWhite,
    errorContainer = Color(0xFFF5DAD5),
    onErrorContainer = Danger
)

private val DarkScheme = darkColorScheme(
    primary = DarkSage,
    onPrimary = DeepForest,
    primaryContainer = Color(0xFF2C3E31),
    onPrimaryContainer = Color(0xFFD7E7D1),
    secondary = DarkPeach,
    onSecondary = Color(0xFF3C2417),
    secondaryContainer = Color(0xFF4A3021),
    onSecondaryContainer = Color(0xFFF8D6B8),
    tertiary = Sage,
    onTertiary = DeepForest,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkMuted,
    outline = DarkLine,
    outlineVariant = Color(0xFF2B372F),
    error = Color(0xFFE6A79D),
    onError = Color(0xFF3B110C),
    errorContainer = Color(0xFF53221B),
    onErrorContainer = Color(0xFFF6CEC7)
)

@Composable
fun AshpazyarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) {
        AshpaPalette(
            ink = DarkInk, muted = DarkMuted, background = DarkBackground, surface = DarkSurface,
            surfaceHigh = DarkSurfaceHigh, forest = DarkSage, sage = Sage,
            sageSoft = Color(0xFF2A3A2D), terracotta = DarkPeach, peach = Color(0xFF5A3A26),
            line = DarkLine, success = Color(0xFF8FC5A0), warning = Color(0xFFE0B36C),
            danger = Color(0xFFE6A79D), isDark = true
        )
    } else {
        AshpaPalette(
            ink = Charcoal, muted = MutedInk, background = Cream, surface = WarmWhite,
            surfaceHigh = Color(0xFFF1E8DA), forest = Forest, sage = Sage, sageSoft = SageLight,
            terracotta = Terracotta, peach = SoftPeach, line = Line, success = Success,
            warning = Warning, danger = Danger, isDark = false
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(
        LocalAshpaPalette provides palette,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AshpaTypography,
            content = content
        )
    }
}

object AshpaTheme {
    val palette: AshpaPalette
        @Composable get() = LocalAshpaPalette.current
}
