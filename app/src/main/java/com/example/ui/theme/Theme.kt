package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ChessGold,
    onPrimary = Color.Black,
    primaryContainer = ChessEmerald,
    onPrimaryContainer = Color.White,
    secondary = ChessEmeraldLight,
    onSecondary = Color.Black,
    tertiary = ChessGoldLight,
    background = ChessNavyDark,
    onBackground = ChessTextPrimary,
    surface = ChessNavySurface,
    onSurface = ChessTextPrimary,
    surfaceVariant = ChessNavyCard,
    onSurfaceVariant = ChessTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = ChessEmerald,
    onPrimary = Color.White,
    primaryContainer = ChessGold,
    onPrimaryContainer = Color.Black,
    secondary = ChessEmeraldLight,
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

