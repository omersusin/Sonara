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
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun SonaraTheme(accentColor: AccentColor = AccentColor.Amber, themeMode: String = "dark",
    dynamicColors: Boolean = false, highContrast: Boolean = false, amoledMode: Boolean = false,
    content: @Composable () -> Unit) {
    val useDark = when (themeMode) { "light" -> false; "dark" -> true; else -> isSystemInDarkTheme() }
    val ctx = LocalContext.current; val useDyn = dynamicColors && Build.VERSION.SDK_INT >= 31
    val p = if (accentColor == AccentColor.Auto) AccentColor.Amber else accentColor
    val pal = when { !useDark -> LightPalette; amoledMode -> AmoledPalette; else -> DarkPalette }
    val hc = highContrast
    val cs = when {
        useDyn && useDark -> dynamicDarkColorScheme(ctx).copy(
            primary = p.primary, onPrimary = pal.background, primaryContainer = p.primary.copy(0.2f),
            background = pal.background, surface = pal.surface, 
            surfaceVariant = pal.card, surfaceContainer = pal.surfaceContainer, 
            surfaceContainerHigh = pal.surfaceContainerHigh, surfaceContainerHighest = pal.surfaceContainerHighest, 
            onBackground = if (hc) Color.White else pal.textPrimary, onSurface = if (hc) Color.White else pal.textPrimary, 
            onSurfaceVariant = if (hc) pal.textPrimary else pal.textSecondary, outline = pal.divider, 
            outlineVariant = pal.divider.copy(0.5f), error = SonaraError
        )
        useDyn && !useDark -> dynamicLightColorScheme(ctx).copy(
            primary = p.primary, onPrimary = Color.White, primaryContainer = p.primary.copy(0.2f),
            background = pal.background, surface = pal.surface, 
            surfaceVariant = pal.card, surfaceContainer = pal.surfaceContainer, 
            surfaceContainerHigh = pal.surfaceContainerHigh, surfaceContainerHighest = pal.surfaceContainerHighest, 
            onBackground = if (hc) Color.Black else pal.textPrimary, onSurface = if (hc) Color.Black else pal.textPrimary, 
            onSurfaceVariant = if (hc) pal.textPrimary else pal.textSecondary, outline = pal.divider, error = SonaraError
        )
        !useDark -> lightColorScheme(
            primary = p.primary, onPrimary = Color.White, primaryContainer = p.primary.copy(0.15f), 
            onPrimaryContainer = p.primaryDark, secondary = p.primary.copy(0.8f), tertiary = p.primaryLight, 
            background = pal.background, onBackground = if (hc) Color.Black else pal.textPrimary, 
            surface = pal.surface, onSurface = if (hc) Color.Black else pal.textPrimary, 
            surfaceVariant = pal.card, onSurfaceVariant = if (hc) pal.textPrimary else pal.textSecondary, 
            surfaceContainer = pal.surfaceContainer, surfaceContainerHigh = pal.surfaceContainerHigh, 
            surfaceContainerHighest = pal.surfaceContainerHighest, outline = pal.divider, 
            outlineVariant = pal.divider.copy(0.5f), error = Color(0xFFD32F2F)
        )
        else -> darkColorScheme(
            primary = p.primary, onPrimary = pal.background, primaryContainer = p.primary.copy(0.15f), 
            onPrimaryContainer = p.primaryLight, secondary = p.primary.copy(0.8f), tertiary = p.primaryLight, 
            background = pal.background, surface = pal.surface, 
            surfaceVariant = pal.card, surfaceContainer = pal.surfaceContainer, 
            surfaceContainerHigh = pal.surfaceContainerHigh, surfaceContainerHighest = pal.surfaceContainerHighest, 
            onBackground = if (hc) Color.White else pal.textPrimary, onSurface = if (hc) Color.White else pal.textPrimary, 
            onSurfaceVariant = if (hc) pal.textPrimary else pal.textSecondary, outline = pal.divider, 
            outlineVariant = pal.divider.copy(0.5f), error = SonaraError
        )
    }
    CompositionLocalProvider(LocalSonaraColors provides pal) { 
        MaterialTheme(colorScheme = cs, typography = SonaraTypography, shapes = SonaraShapes, content = content) 
    }
}
