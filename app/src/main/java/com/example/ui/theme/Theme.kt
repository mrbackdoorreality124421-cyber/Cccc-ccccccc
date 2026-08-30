package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.AccentGold,
    onPrimary = Color(0xFF1E1500),
    primaryContainer = Color(0xFF3D2E00),
    onPrimaryContainer = AppColors.AccentGoldLight,
    secondary = AppColors.AccentEmerald,
    onSecondary = Color(0xFF002114),
    secondaryContainer = AppColors.AccentEmeraldDark,
    onSecondaryContainer = AppColors.AccentEmeraldLight,
    tertiary = AppColors.AccentBlue,
    onTertiary = Color.White,
    tertiaryContainer = AppColors.AccentBlueDark,
    onTertiaryContainer = Color(0xFFBFDBFE),
    background = AppColors.BackgroundDark,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.BackgroundSurface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.BackgroundCard,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.BackgroundCardBorder,
    error = AppColors.AccentRed,
    errorContainer = AppColors.AccentRedDark,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = darkColorScheme( // For a dark, ASMR chess aesthetic by default
    primary = AppColors.AccentGold,
    onPrimary = Color(0xFF1E1500),
    background = AppColors.BackgroundDark,
    surface = AppColors.BackgroundSurface,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = ChessTypography,
        content = content
    )
}

@Composable
fun ChessMasterProTheme(content: @Composable () -> Unit) {
    MyApplicationTheme(content = content)
}
