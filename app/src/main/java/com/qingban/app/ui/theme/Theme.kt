package com.qingban.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = InkGreen,
    onPrimary = Color.White,
    primaryContainer = SoftSage,
    onPrimaryContainer = InkGreenDark,
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E8D6),
    onSecondaryContainer = Color(0xFF46331E),
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = CoralSoft,
    onTertiaryContainer = Color(0xFF4C1F14),
    background = WarmPaper,
    onBackground = DeepInk,
    surface = PaperSurface,
    onSurface = DeepInk,
    surfaceVariant = Color(0xFFF0ECE4),
    onSurfaceVariant = MutedInk,
    outline = WarmOutline,
    outlineVariant = Color(0xFFECE7DD),
    error = Coral,
    onError = Color.White,
    errorContainer = CoralSoft,
    onErrorContainer = Color(0xFF4C1F14),
    surfaceTint = InkGreen,
)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF0C332C),
    primaryContainer = Color(0xFF264D43),
    onPrimaryContainer = Color(0xFFD0E9E1),
    secondary = Color(0xFFD9BD8B),
    onSecondary = Color(0xFF3B2D14),
    secondaryContainer = Color(0xFF4C3A1E),
    onSecondaryContainer = Color(0xFFF3E1C2),
    tertiary = Color(0xFFE3A08B),
    onTertiary = Color(0xFF4A1F14),
    tertiaryContainer = Color(0xFF5C2A1E),
    onTertiaryContainer = Color(0xFFFFDAD0),
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightMuted,
    outline = NightOutline,
    outlineVariant = Color(0xFF2C322C),
    error = Color(0xFFE8A08E),
    onError = Color(0xFF4A1D12),
    errorContainer = Color(0xFF5F2B1F),
    onErrorContainer = Color(0xFFFFDAD1),
    surfaceTint = Mint,
)

@Composable
fun QingBanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QingBanTypography,
        content = content,
    )
}
