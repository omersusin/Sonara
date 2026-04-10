package com.sonara.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ═══ Semantic / status colors ════════════════════════════════
val SonaraSuccess  = Color(0xFF1DB954)   // Spotify yeşili gibi canlı
val SonaraError    = Color(0xFFFF453A)
val SonaraWarning  = Color(0xFFFFD60A)
val SonaraInfo     = Color(0xFF64D2FF)

// ═══ EQ band renkleri ════════════════════════════════════════
val SonaraBandLow  = Color(0xFFFFB347)   // Turuncu — bas
val SonaraBandMid  = Color(0xFFFFD966)   // Sarı — orta
val SonaraBandHigh = Color(0xFF4FC3F7)   // Açık mavi — tiz

// ═══ MD3 Expressive — zengin surface katmanları ==============
// Expressive'de her surface tonu farklı bir renk tonu taşır
data class SonaraColorPalette(
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,        // yeni: orta kap
    val surfaceContainerHigh: Color,    // yeni: yüksek kap
    val surfaceContainerHighest: Color, // yeni: en yüksek kap
    val card: Color,
    val cardElevated: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    // MD3 Expressive ek renk rolleri
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val inversePrimary: Color
)

// ─── Dark palette ─────────────────────────────────────────────
val DarkPalette = SonaraColorPalette(
    background              = Color(0xFF0E0E11),
    surface                 = Color(0xFF1A1A1E),
    surfaceContainer        = Color(0xFF212126),
    surfaceContainerHigh    = Color(0xFF2A2A30),
    surfaceContainerHighest = Color(0xFF313138),
    card                    = Color(0xFF252529),
    cardElevated            = Color(0xFF2E2E35),
    divider                 = Color(0xFF3D3D45),
    textPrimary             = Color(0xFFF2F2F7),
    textSecondary           = Color(0xFF9898A5),
    textTertiary            = Color(0xFF68686E),
    tertiaryContainer       = Color(0xFF2B1F3A),   // mor tonu
    onTertiaryContainer     = Color(0xFFD4BCFE),
    inversePrimary          = Color(0xFF6F4E37)
)

// ─── Light palette ────────────────────────────────────────────
val LightPalette = SonaraColorPalette(
    background              = Color(0xFFF5F5FA),
    surface                 = Color(0xFFFFFFFF),
    surfaceContainer        = Color(0xFFF0EFF5),
    surfaceContainerHigh    = Color(0xFFEAE9F0),
    surfaceContainerHighest = Color(0xFFE4E3EB),
    card                    = Color(0xFFFFFFFF),
    cardElevated            = Color(0xFFF7F6FC),
    divider                 = Color(0xFFD0CFD9),
    textPrimary             = Color(0xFF1C1B22),
    textSecondary           = Color(0xFF5F5E6B),
    textTertiary            = Color(0xFF8E8D9A),
    tertiaryContainer       = Color(0xFFEDE0FF),
    onTertiaryContainer     = Color(0xFF4A2B7A),
    inversePrimary          = Color(0xFFEDCFAA)
)

// ─── AMOLED palette ───────────────────────────────────────────
val AmoledPalette = SonaraColorPalette(
    background              = Color(0xFF000000),
    surface                 = Color(0xFF080808),
    surfaceContainer        = Color(0xFF111118),
    surfaceContainerHigh    = Color(0xFF181820),
    surfaceContainerHighest = Color(0xFF1F1F28),
    card                    = Color(0xFF121218),
    cardElevated            = Color(0xFF1A1A22),
    divider                 = Color(0xFF282832),
    textPrimary             = Color(0xFFF2F2F7),
    textSecondary           = Color(0xFF9898A5),
    textTertiary            = Color(0xFF55555E),
    tertiaryContainer       = Color(0xFF1A0F2E),
    onTertiaryContainer     = Color(0xFFCAABF5),
    inversePrimary          = Color(0xFF6F4E37)
)

val LocalSonaraColors = staticCompositionLocalOf { DarkPalette }

// ─── Composable aliases ───────────────────────────────────────
val SonaraBackground: Color              @Composable get() = LocalSonaraColors.current.background
val SonaraSurface: Color                 @Composable get() = LocalSonaraColors.current.surface
val SonaraSurfaceContainer: Color        @Composable get() = LocalSonaraColors.current.surfaceContainer
val SonaraSurfaceContainerHigh: Color    @Composable get() = LocalSonaraColors.current.surfaceContainerHigh
val SonaraSurfaceContainerHighest: Color @Composable get() = LocalSonaraColors.current.surfaceContainerHighest
val SonaraCard: Color                    @Composable get() = LocalSonaraColors.current.card
val SonaraCardElevated: Color            @Composable get() = LocalSonaraColors.current.cardElevated
val SonaraDivider: Color                 @Composable get() = LocalSonaraColors.current.divider
val SonaraTextPrimary: Color             @Composable get() = LocalSonaraColors.current.textPrimary
val SonaraTextSecondary: Color           @Composable get() = LocalSonaraColors.current.textSecondary
val SonaraTextTertiary: Color            @Composable get() = LocalSonaraColors.current.textTertiary
val SonaraTertiaryContainer: Color       @Composable get() = LocalSonaraColors.current.tertiaryContainer
val SonaraOnTertiaryContainer: Color     @Composable get() = LocalSonaraColors.current.onTertiaryContainer

// ─── Accent renkleri (değişmez) ───────────────────────────────
val SonaraPrimary          = Color(0xFFD4A574)
val SonaraPrimaryLight     = Color(0xFFEDCFAA)
val SonaraPrimaryDark      = Color(0xFFB8875A)
val SonaraPrimaryContainer = Color(0x26D4A574)   // %15 alpha (daha belirgin)

// Expressive ek: ikincil vurgu tonu
val SonaraSecondaryAccent  = Color(0xFFB09FCC)   // yumuşak lavanta
