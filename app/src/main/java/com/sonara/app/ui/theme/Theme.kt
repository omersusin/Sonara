package com.sonara.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SonaraColorScheme = darkColorScheme(
    primary = SonaraPrimary,
    onPrimary = SonaraBackground,
    primaryContainer = SonaraPrimaryContainer,
    onPrimaryContainer = SonaraPrimary,
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

@Composable
fun SonaraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SonaraColorScheme,
        typography = SonaraTypography,
        shapes = SonaraShapes,
        content = content
    )
}
