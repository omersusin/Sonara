package com.sonara.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun SonaraTheme(
    accentColor: AccentColor = AccentColor.Amber,
    themeMode: String = "dark",
    dynamicColors: Boolean = false,
    highContrast: Boolean = false,
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "light" -> false; "dark" -> true; else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val useDynamic = dynamicColors && Build.VERSION.SDK_INT >= 31
    val p = if (accentColor == AccentColor.Auto) AccentColor.Amber else accentColor
    val palette = when {
        !useDark -> LightPalette; amoledMode -> AmoledPalette; else -> DarkPalette
    }

    val colorScheme = when {
        useDynamic && useDark -> dynamicDarkColorScheme(context).copy(
            background = palette.background, surface = palette.surface,
            surfaceVariant = palette.card,
            surfaceContainer = palette.surfaceContainer,
            surfaceContainerHigh = palette.surfaceContainerHigh,
            surfaceContainerHighest = palette.surfaceContainerHighest,
            onBackground = if (highContrast) Color.White else palette.textPrimary,
            onSurface = if (highContrast) Color.White else palette.textPrimary,
            onSurfaceVariant = if (highContrast) palette.textPrimary else palette.textSecondary,
            outline = palette.divider, outlineVariant = palette.divider.copy(0.5f), error = SonaraError
        )
        useDynamic && !useDark -> dynamicLightColorScheme(context).copy(
            background = palette.background, surface = palette.surface,
            surfaceVariant = palette.card,
            surfaceContainer = palette.surfaceContainer,
            surfaceContainerHigh = palette.surfaceContainerHigh,
            surfaceContainerHighest = palette.surfaceContainerHighest,
            onBackground = if (highContrast) Color.Black else palette.textPrimary,
            onSurface = if (highContrast) Color.Black else palette.textPrimary,
            onSurfaceVariant = if (highContrast) palette.textPrimary else palette.textSecondary,
            outline = palette.divider, error = SonaraError
        )
        !useDark -> lightColorScheme(
            primary = p.primary, onPrimary = Color.White,
            primaryContainer = p.primary.copy(0.12f), onPrimaryContainer = p.primaryDark,
            secondary = Color(0xFF5A7A8A), onSecondary = Color.White,
            tertiary = p.primaryLight, onTertiary = Color.Black,
            background = palette.background, onBackground = if (highContrast) Color.Black else palette.textPrimary,
            surface = palette.surface, onSurface = if (highContrast) Color.Black else palette.textPrimary,
            surfaceVariant = palette.card, onSurfaceVariant = if (highContrast) palette.textPrimary else palette.textSecondary,
            surfaceContainer = palette.surfaceContainer,
            surfaceContainerHigh = palette.surfaceContainerHigh,
            surfaceContainerHighest = palette.surfaceContainerHighest,
            outline = palette.divider, outlineVariant = palette.divider.copy(0.5f),
            error = Color(0xFFD32F2F), inverseSurface = Color(0xFF2C2C2E), inverseOnSurface = Color(0xFFF5F5F5)
        )
        else -> darkColorScheme(
            primary = p.primary, onPrimary = palette.background,
            primaryContainer = p.primary.copy(0.12f), onPrimaryContainer = p.primaryLight,
            secondary = SonaraInfo, tertiary = p.primaryLight, onTertiary = palette.background,
            background = palette.background, surface = palette.surface,
            surfaceVariant = palette.card,
            surfaceContainer = palette.surfaceContainer,
            surfaceContainerHigh = palette.surfaceContainerHigh,
            surfaceContainerHighest = palette.surfaceContainerHighest,
            onBackground = if (highContrast) Color.White else palette.textPrimary,
            onSurface = if (highContrast) Color.White else palette.textPrimary,
            onSurfaceVariant = if (highContrast) palette.textPrimary else palette.textSecondary,
            outline = palette.divider, outlineVariant = palette.divider.copy(0.5f), error = SonaraError
        )
    }

    CompositionLocalProvider(LocalSonaraColors provides palette) {
        MaterialTheme(colorScheme = colorScheme, typography = SonaraTypography, shapes = SonaraShapes, content = content)
    }
}
