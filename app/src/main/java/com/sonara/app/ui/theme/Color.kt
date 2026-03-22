package com.sonara.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ═══ Semantic colors (don't change with theme) ═══
val SonaraSuccess = Color(0xFF32D74B)
val SonaraError = Color(0xFFFF453A)
val SonaraWarning = Color(0xFFFFD60A)
val SonaraInfo = Color(0xFF64D2FF)

val SonaraBandLow = Color(0xFFD4A574)
val SonaraBandMid = Color(0xFFE8C9A0)
val SonaraBandHigh = Color(0xFF64D2FF)

// ═══ Theme-aware color palette ═══
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
