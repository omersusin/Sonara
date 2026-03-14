package com.sonara.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SonaraTheme(
    accentColor: AccentColor = AccentColor.Amber,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = accentColor.primary,
        onPrimary = SonaraBackground,
        primaryContainer = accentColor.primary.copy(alpha = 0.1f),
        onPrimaryContainer = accentColor.primaryLight,
        secondary = SonaraInfo,
        background = SonaraBackground,
        surface = SonaraSurface,
        surfaceVariant = SonaraCard,
        onBackground = SonaraTextPrimary,
        onSurface = SonaraTextPrimary,
        onSurfaceVariant = SonaraTextSecondary,
        outline = SonaraDivider,
        outlineVariant = SonaraDivider,
        error = SonaraError
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SonaraTypography,
        shapes = SonaraShapes,
        content = content
    )
}
