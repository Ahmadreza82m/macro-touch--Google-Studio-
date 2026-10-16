package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ColorGold500,
    onPrimary = ColorVoid,
    primaryContainer = ColorSurface3,
    onPrimaryContainer = ColorGold100,
    secondary = ColorGold300,
    onSecondary = ColorVoid,
    secondaryContainer = ColorSurface2,
    onSecondaryContainer = ColorTextPrimary,
    tertiary = ColorEmerald,
    onTertiary = ColorVoid,
    error = ColorDanger,
    onError = ColorVoid,
    background = ColorVoid,
    onBackground = ColorTextPrimary,
    surface = ColorSurface1,
    onSurface = ColorTextPrimary,
    surfaceVariant = ColorSurface2,
    onSurfaceVariant = ColorTextSecondary,
    outline = ColorGoldBorder,
    outlineVariant = ColorSurface3
)

@Composable
fun MacroTouchTheme(
    content: @Composable () -> Unit
) {
    // Macro Touch Pro always enforces the custom Dark Premium aesthetic (Section 2.2)
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
