package com.sonara.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Decorative EQ-visualizer colors (intentionally fixed; not theme-generated) ──
val SonaraBandLow  = Color(0xFFD4A574)
val SonaraBandMid  = Color(0xFFE8C9A0)
val SonaraBandHigh = Color(0xFF64D2FF)

// ── Surface / container roles ────────────────────────────────────────────────────
val SonaraBackground:        Color @Composable get() = MaterialTheme.colorScheme.background
val SonaraSurface:           Color @Composable get() = MaterialTheme.colorScheme.surface
val SonaraCard:              Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer
val SonaraCardElevated:      Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
val SonaraDivider:           Color @Composable get() = MaterialTheme.colorScheme.outline

// ── Text / content roles ─────────────────────────────────────────────────────────
val SonaraTextPrimary:   Color @Composable get() = MaterialTheme.colorScheme.onSurface
val SonaraTextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val SonaraTextTertiary:  Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

// ── Accent / primary roles ───────────────────────────────────────────────────────
val SonaraPrimary:          Color @Composable get() = MaterialTheme.colorScheme.primary
val SonaraPrimaryLight:     Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
val SonaraPrimaryDark:      Color @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer
val SonaraPrimaryContainer: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer

// ── Semantic status roles (mapped to MD3 tonal roles) ───────────────────────────
val SonaraError:   Color @Composable get() = MaterialTheme.colorScheme.error
val SonaraSuccess: Color @Composable get() = MaterialTheme.colorScheme.tertiary
val SonaraWarning: Color @Composable get() = MaterialTheme.colorScheme.secondary
val SonaraInfo:    Color @Composable get() = MaterialTheme.colorScheme.inversePrimary
