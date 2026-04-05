@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sonara.app.ui.theme

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ═══ Semantic colors (don't change with theme) ═══
val SonaraSuccess = Color(0xFF32D74B)
val SonaraError = Color(0xFFFF453A)
val SonaraWarning = Color(0xFFFFD60A)
val SonaraInfo = Color(0xFF64D2FF)

val SonaraBandLow = Color(0xFFD4A574)
val SonaraBandMid = Color(0xFFE8C9A0)
val SonaraBandHigh = Color(0xFF64D2FF)

// ═══ Theme-aware color palette (legacy compat) ═══
data class SonaraColorPalette(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color
)

val DarkPalette = SonaraColorPalette(
    background = Color(0xFF111113),
    surface = Color(0xFF1C1C1F),
    card = Color(0xFF252529),
    cardElevated = Color(0xFF2E2E33),
    divider = Color(0xFF3A3A3E),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFF636366)
)

val LightPalette = SonaraColorPalette(
    background = Color(0xFFF8F8FA),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF2F2F4),
    divider = Color(0xFFD1D1D6),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF636366),
    textTertiary = Color(0xFF8E8E93)
)

val AmoledPalette = SonaraColorPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    card = Color(0xFF141414),
    cardElevated = Color(0xFF1A1A1A),
    divider = Color(0xFF2A2A2E),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFF636366)
)

val LocalSonaraColors = staticCompositionLocalOf { DarkPalette }

// ═══ Compat aliases — use in @Composable context ═══
val SonaraBackground: Color @Composable get() = LocalSonaraColors.current.background
val SonaraSurface: Color @Composable get() = LocalSonaraColors.current.surface
val SonaraCard: Color @Composable get() = LocalSonaraColors.current.card
val SonaraCardElevated: Color @Composable get() = LocalSonaraColors.current.cardElevated
val SonaraDivider: Color @Composable get() = LocalSonaraColors.current.divider
val SonaraTextPrimary: Color @Composable get() = LocalSonaraColors.current.textPrimary
val SonaraTextSecondary: Color @Composable get() = LocalSonaraColors.current.textSecondary
val SonaraTextTertiary: Color @Composable get() = LocalSonaraColors.current.textTertiary

// ═══ Accent colors (unchanged) ═══
val SonaraPrimary = Color(0xFFD4A574)
val SonaraPrimaryLight = Color(0xFFEDCFAA)
val SonaraPrimaryDark = Color(0xFFB8875A)
val SonaraPrimaryContainer = Color(0x1AD4A574)

// ═══ M3E Full Color Token Set — Light ═══
internal val lightColorScheme = lightColorScheme(
    // Primary group
    primary = SonaraPrimary,
    onPrimary = Color.White,
    primaryContainer = SonaraPrimary.copy(alpha = 0.10f),
    onPrimaryContainer = SonaraPrimaryDark,
    // Secondary group
    secondary = Color(0xFF5A7A8A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5A7A8A).copy(alpha = 0.12f),
    onSecondaryContainer = Color(0xFF3A5A6A),
    // Tertiary group
    tertiary = SonaraInfo,
    onTertiary = Color.White,
    tertiaryContainer = SonaraInfo.copy(alpha = 0.12f),
    onTertiaryContainer = Color(0xFF3A8AB0),
    // Error group
    error = SonaraError,
    onError = Color.White,
    errorContainer = SonaraError.copy(alpha = 0.12f),
    onErrorContainer = Color(0xFFBA1A1A),
    // Surface group
    surface = Color(0xFFF8F8FA),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF636366),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F2F4),
    surfaceContainer = Color(0xFFEDEDEF),
    surfaceContainerHigh = Color(0xFFE7E7E9),
    surfaceContainerHighest = Color(0xFFE1E1E4),
    inverseSurface = Color(0xFF2C2C2E),
    inverseOnSurface = Color(0xFFF5F5F5),
    inversePrimary = SonaraPrimaryLight,
    // Utility
    outline = Color(0xFFD1D1D6),
    outlineVariant = Color(0xFFE5E5EA),
    scrim = Color(0xFF000000),
)

// ═══ M3E Full Color Token Set — Dark ═══
internal val darkColorScheme = darkColorScheme(
    // Primary group
    primary = SonaraPrimary,
    onPrimary = Color(0xFF111113),
    primaryContainer = SonaraPrimary.copy(alpha = 0.1f),
    onPrimaryContainer = SonaraPrimaryLight,
    // Secondary group
    secondary = SonaraInfo,
    onSecondary = Color(0xFF111113),
    secondaryContainer = SonaraInfo.copy(alpha = 0.12f),
    onSecondaryContainer = Color(0xFF7A9AAA),
    // Tertiary group
    tertiary = SonaraInfo,
    onTertiary = Color(0xFF111113),
    tertiaryContainer = SonaraInfo.copy(alpha = 0.12f),
    onTertiaryContainer = Color(0xFF8AC0E0),
    // Error group
    error = SonaraError,
    onError = Color(0xFF111113),
    errorContainer = SonaraError.copy(alpha = 0.12f),
    onErrorContainer = Color(0xFFFFB4AB),
    // Surface group
    surface = Color(0xFF111113),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF252529),
    onSurfaceVariant = Color(0xFF8E8E93),
    surfaceContainerLowest = Color(0xFF0C0C0E),
    surfaceContainerLow = Color(0xFF1A1A1E),
    surfaceContainer = Color(0xFF1E1E22),
    surfaceContainerHigh = Color(0xFF252529),
    surfaceContainerHighest = Color(0xFF2E2E33),
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = Color(0xFF2C2C2E),
    inversePrimary = SonaraPrimaryDark,
    // Utility
    outline = Color(0xFF3A3A3E),
    outlineVariant = Color(0xFF3A3A3E),
    scrim = Color(0xFF000000),
)

// ═══ Dynamic color scheme resolver ═══
@Composable
internal fun sonaraColorScheme(
    darkTheme: Boolean,
    dynamicColors: Boolean,
    paletteOverride: SonaraColorPalette? = null
): androidx.compose.material3.ColorScheme {
    val context = LocalContext.current
    val useDynamic = dynamicColors && Build.VERSION.SDK_INT >= 31

    val baseScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    val palette = paletteOverride ?: if (!darkTheme) LightPalette else DarkPalette

    return baseScheme.copy(
        background = palette.background,
        surface = palette.surface,
        surfaceVariant = palette.card,
        onBackground = palette.textPrimary,
        onSurface = palette.textPrimary,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.divider,
        outlineVariant = palette.divider,
        error = SonaraError,
    )
}
