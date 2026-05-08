package com.sonara.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE8B76E),
    onPrimary = Color(0xFF3F2500),
    primaryContainer = Color(0xFF5A3700),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFCBBC8E),
    onSecondary = Color(0xFF382E06),
    secondaryContainer = Color(0xFF50451A),
    onSecondaryContainer = Color(0xFFE8D8A8),
    tertiary = Color(0xFFA3C98A),
    onTertiary = Color(0xFF1B3505),
    tertiaryContainer = Color(0xFF304C1A),
    onTertiaryContainer = Color(0xFFBFE5A4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF17130E),
    onBackground = Color(0xFFEDE1D4),
    surface = Color(0xFF17130E),
    onSurface = Color(0xFFEDE1D4),
    surfaceVariant = Color(0xFF4E4437),
    onSurfaceVariant = Color(0xFFD1C3B0),
    outline = Color(0xFF9A8D7B),
    outlineVariant = Color(0xFF4E4437),
    inverseSurface = Color(0xFFEDE1D4),
    inverseOnSurface = Color(0xFF352A1F),
    inversePrimary = Color(0xFF7B5000),
    surfaceContainerLowest = Color(0xFF110D09),
    surfaceContainerLow = Color(0xFF201A14),
    surfaceContainer = Color(0xFF241E18),
    surfaceContainerHigh = Color(0xFF2F281F),
    surfaceContainerHighest = Color(0xFF3A332A),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7B5000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF271700),
    secondary = Color(0xFF6B5C30),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5DFA6),
    onSecondaryContainer = Color(0xFF221A00),
    tertiary = Color(0xFF4A6530),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCBECA9),
    onTertiaryContainer = Color(0xFF102006),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF201A15),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF201A15),
    surfaceVariant = Color(0xFFF2E2CE),
    onSurfaceVariant = Color(0xFF514538),
    outline = Color(0xFF847464),
    outlineVariant = Color(0xFFD5C5B1),
    inverseSurface = Color(0xFF352F29),
    inverseOnSurface = Color(0xFFFBEFE6),
    inversePrimary = Color(0xFFE8B76E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1E6),
    surfaceContainer = Color(0xFFFAEBDE),
    surfaceContainerHigh = Color(0xFFF4E5D8),
    surfaceContainerHighest = Color(0xFFEFDFD2),
)

@Composable
fun SonaraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = remember { buildTypography(SonaraFont.INTER) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = SonaraShapes,
        content = content
    )
}
