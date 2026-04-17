/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
    background = Color(0xFF08090A),
    surface = Color(0xFF111316),
    surfaceContainer = Color(0xFF181B1F),
    surfaceContainerHigh = Color(0xFF22252B),
    surfaceContainerHighest = Color(0xFF2C3038),
    card = Color(0xFF181B1F),
    cardElevated = Color(0xFF22252B),
    divider = Color(0xFF2C3038),
    textPrimary = Color(0xFFF0F2F5),
    textSecondary = Color(0xFFA1AAB9),
    textTertiary = Color(0xFF717C8C)
)
val LightPalette = SonaraColorPalette(
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEFF1F6),
    surfaceContainerHigh = Color(0xFFE5E9F0),
    surfaceContainerHighest = Color(0xFFDBE1EA),
    card = Color(0xFFEFF1F6),
    cardElevated = Color(0xFFE5E9F0),
    divider = Color(0xFFDBE1EA),
    textPrimary = Color(0xFF111418),
    textSecondary = Color(0xFF535D6E),
    textTertiary = Color(0xFF8692A6)
)
val AmoledPalette = SonaraColorPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0B0D),
    surfaceContainer = Color(0xFF121417),
    surfaceContainerHigh = Color(0xFF1B1E22),
    surfaceContainerHighest = Color(0xFF24282D),
    card = Color(0xFF121417),
    cardElevated = Color(0xFF1B1E22),
    divider = Color(0xFF24282D),
    textPrimary = Color(0xFFF0F2F5),
    textSecondary = Color(0xFFA1AAB9),
    textTertiary = Color(0xFF717C8C)
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
