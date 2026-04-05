@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sonara.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Standard 15 M3E type styles — using Roboto Flex
private val StandardFontFamily = FontFamily.Default

private val displayLarge = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp)
private val displayMedium = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp)
private val displaySmall = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp)
private val headlineLarge = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp)
private val headlineMedium = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp)
private val headlineSmall = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp)
private val titleLarge = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp)
private val titleMedium = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp)
private val titleSmall = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp)
private val bodyLarge = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp)
private val bodyMedium = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.25.sp)
private val bodySmall = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp)
private val labelLarge = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp)
private val labelMedium = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp)
private val labelSmall = TextStyle(fontFamily = StandardFontFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp)

// Emphasized 15 M3E type styles — same sizes but bolder
private val EmphasizedWeight = FontWeight.Bold

private val displayLargeEmphasized = displayLarge.copy(fontWeight = EmphasizedWeight)
private val displayMediumEmphasized = displayMedium.copy(fontWeight = EmphasizedWeight)
private val displaySmallEmphasized = displaySmall.copy(fontWeight = EmphasizedWeight)
private val headlineLargeEmphasized = headlineLarge.copy(fontWeight = EmphasizedWeight)
private val headlineMediumEmphasized = headlineMedium.copy(fontWeight = EmphasizedWeight)
private val headlineSmallEmphasized = headlineSmall.copy(fontWeight = EmphasizedWeight)
private val titleLargeEmphasized = titleLarge.copy(fontWeight = EmphasizedWeight)
private val titleMediumEmphasized = titleMedium.copy(fontWeight = EmphasizedWeight)
private val titleSmallEmphasized = titleSmall.copy(fontWeight = EmphasizedWeight)
private val bodyLargeEmphasized = bodyLarge.copy(fontWeight = EmphasizedWeight)
private val bodyMediumEmphasized = bodyMedium.copy(fontWeight = EmphasizedWeight)
private val bodySmallEmphasized = bodySmall.copy(fontWeight = EmphasizedWeight)
private val labelLargeEmphasized = labelLarge.copy(fontWeight = EmphasizedWeight)
private val labelMediumEmphasized = labelMedium.copy(fontWeight = EmphasizedWeight)
private val labelSmallEmphasized = labelSmall.copy(fontWeight = EmphasizedWeight)

val AppTypography = Typography(
    displayLarge = displayLarge,
    displayMedium = displayMedium,
    displaySmall = displaySmall,
    headlineLarge = headlineLarge,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = titleSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelSmall,
    // Emphasized styles — M3E exclusive, aliased via extension properties
    displayLargeEmphasized = displayLargeEmphasized,
    displayMediumEmphasized = displayMediumEmphasized,
    displaySmallEmphasized = displaySmallEmphasized,
    headlineLargeEmphasized = headlineLargeEmphasized,
    headlineMediumEmphasized = headlineMediumEmphasized,
    headlineSmallEmphasized = headlineSmallEmphasized,
    titleLargeEmphasized = titleLargeEmphasized,
    titleMediumEmphasized = titleMediumEmphasized,
    titleSmallEmphasized = titleSmallEmphasized,
    bodyLargeEmphasized = bodyLargeEmphasized,
    bodyMediumEmphasized = bodyMediumEmphasized,
    bodySmallEmphasized = bodySmallEmphasized,
    labelLargeEmphasized = labelLargeEmphasized,
    labelMediumEmphasized = labelMediumEmphasized,
    labelSmallEmphasized = labelSmallEmphasized,
)

// Legacy alias for backwards compat
val SonaraTypography = AppTypography
