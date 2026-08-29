package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ChessEmeraldLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF125D45),
    onPrimaryContainer = Color(0xFFD5F7E9),
    secondary = ChessGold,
    onSecondary = Color(0xFF1B1608),
    secondaryContainer = Color(0xFF4D4014),
    onSecondaryContainer = Color(0xFFFFEFAF),
    tertiary = Color(0xFF8CB4FF),
    background = ChessNavyDark,
    onBackground = ChessTextPrimary,
    surface = ChessNavySurface,
    onSurface = ChessTextPrimary,
    surfaceVariant = ChessNavyCard,
    onSurfaceVariant = ChessTextSecondary,
    outline = Color(0xFF334250),
    error = Color(0xFFFF6B6B),
    errorContainer = Color(0xFF5B1C22),
    onErrorContainer = Color(0xFFFFDAD9)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF087A53),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC5F1DE),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF806000),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE29A),
    onSecondaryContainer = Color(0xFF271A00),
    tertiary = Color(0xFF4169A1),
    background = Color(0xFFF7F9FB),
    onBackground = Color(0xFF11181F),
    surface = Color.White,
    onSurface = Color(0xFF11181F),
    surfaceVariant = Color(0xFFEAF0F4),
    onSurfaceVariant = Color(0xFF53616D),
    outline = Color(0xFF7C8A95),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
