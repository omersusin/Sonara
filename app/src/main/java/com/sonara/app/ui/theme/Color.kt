package com.sonara.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val SonaraSuccess = Color(0xFF34C759)
val SonaraError = Color(0xFFFF3B30)
val SonaraWarning = Color(0xFFFF9F0A)
val SonaraInfo = Color(0xFF5AC8FA)
val SonaraBandLow = Color(0xFFD4A574)
val SonaraBandMid = Color(0xFFE8C9A0)
val SonaraBandHigh = Color(0xFF64D2FF)

data class SonaraColorPalette(
    val background: Color, val surface: Color,
    val surfaceContainer: Color, val surfaceContainerHigh: Color, val surfaceContainerHighest: Color,
    val card: Color, val cardElevated: Color,
    val divider: Color, val textPrimary: Color, val textSecondary: Color, val textTertiary: Color
)

val DarkPalette = SonaraColorPalette(
    background = Color(0xFF0F1114), surface = Color(0xFF1A1B20),
    surfaceContainer = Color(0xFF1F2127), surfaceContainerHigh = Color(0xFF282A31), surfaceContainerHighest = Color(0xFF32343B),
    card = Color(0xFF1F2127), cardElevated = Color(0xFF282A31),
    divider = Color(0xFF3A3C44), textPrimary = Color(0xFFF0F0F5), textSecondary = Color(0xFF9195A1), textTertiary = Color(0xFF62666F)
)
val LightPalette = SonaraColorPalette(
    background = Color(0xFFF2F3F7), surface = Color(0xFFFAFAFC),
    surfaceContainer = Color(0xFFEDEEF2), surfaceContainerHigh = Color(0xFFE5E6EB), surfaceContainerHighest = Color(0xFFDDDEE3),
    card = Color(0xFFE8E9EE), cardElevated = Color(0xFFE0E1E7),
    divider = Color(0xFFCFD0D6), textPrimary = Color(0xFF1B1C20), textSecondary = Color(0xFF5A5D66), textTertiary = Color(0xFF8A8D96)
)
val AmoledPalette = SonaraColorPalette(
    background = Color(0xFF000000), surface = Color(0xFF0A0A0E),
    surfaceContainer = Color(0xFF111116), surfaceContainerHigh = Color(0xFF1A1A20), surfaceContainerHighest = Color(0xFF222228),
    card = Color(0xFF111116), cardElevated = Color(0xFF1A1A20),
    divider = Color(0xFF2A2A30), textPrimary = Color(0xFFF0F0F5), textSecondary = Color(0xFF8A8D96), textTertiary = Color(0xFF62666F)
)

val LocalSonaraColors = staticCompositionLocalOf { DarkPalette }
val SonaraBackground: Color @Composable get() = LocalSonaraColors.current.background
val SonaraSurface: Color @Composable get() = LocalSonaraColors.current.surface
val SonaraCard: Color @Composable get() = LocalSonaraColors.current.card
val SonaraCardElevated: Color @Composable get() = LocalSonaraColors.current.cardElevated
val SonaraDivider: Color @Composable get() = LocalSonaraColors.current.divider
val SonaraTextPrimary: Color @Composable get() = LocalSonaraColors.current.textPrimary
val SonaraTextSecondary: Color @Composable get() = LocalSonaraColors.current.textSecondary
val SonaraTextTertiary: Color @Composable get() = LocalSonaraColors.current.textTertiary
val SonaraPrimary = Color(0xFFD4A574)
val SonaraPrimaryLight = Color(0xFFEDCFAA)
val SonaraPrimaryDark = Color(0xFFB8875A)
val SonaraPrimaryContainer = Color(0x1AD4A574)
