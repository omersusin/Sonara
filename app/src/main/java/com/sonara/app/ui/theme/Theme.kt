package com.sonara.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SonaraColorScheme = darkColorScheme(
    primary = SonaraPrimary,
    onPrimary = SonaraBlack,
    primaryContainer = SonaraPrimaryDark,
    secondary = SonaraAccentBlue,
    background = SonaraBlack,
    surface = SonaraSurface,
    surfaceVariant = SonaraCard,
    onBackground = SonaraTextPrimary,
    onSurface = SonaraTextPrimary,
    onSurfaceVariant = SonaraTextSecondary,
    error = SonaraAccentRed
)

@Composable
fun SonaraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SonaraColorScheme,
        typography = SonaraTypography,
        content = content
    )
}
