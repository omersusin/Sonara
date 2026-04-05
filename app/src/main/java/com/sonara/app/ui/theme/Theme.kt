@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sonara.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun SonaraTheme(
    accentColor: AccentColor = AccentColor.Amber,
    themeMode: String = "dark",
    dynamicColors: Boolean = false,
    highContrast: Boolean = false,
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val useDynamic = dynamicColors && Build.VERSION.SDK_INT >= 31
    val p = if (accentColor == AccentColor.Auto) AccentColor.Amber else accentColor

    // ═══ Pick the right Sonara palette ═══
    val palette = when {
        !useDark -> LightPalette
        amoledMode -> AmoledPalette
        else -> DarkPalette
    }

    // ═══ Material color scheme ═══
    val colorScheme = when {
        useDynamic && useDark -> {
            val base = androidx.compose.material3.dynamicDarkColorScheme(context)
            base.copy(
                background = palette.background, surface = palette.surface,
                surfaceVariant = palette.card,
                onBackground = if (highContrast) Color.White else palette.textPrimary,
                onSurface = if (highContrast) Color.White else palette.textPrimary,
                onSurfaceVariant = if (highContrast) palette.textPrimary else palette.textSecondary,
                outline = palette.divider, outlineVariant = palette.divider, error = SonaraError,
                primary = p.primary, onPrimary = palette.background,
                primaryContainer = p.primary.copy(alpha = 0.1f), onPrimaryContainer = p.primaryLight,
                secondary = SonaraInfo,
            )
        }
        useDynamic && !useDark -> {
            val base = androidx.compose.material3.dynamicLightColorScheme(context)
            base.copy(
                background = palette.background, surface = palette.surface,
                surfaceVariant = palette.card,
                onBackground = if (highContrast) Color.Black else palette.textPrimary,
                onSurface = if (highContrast) Color.Black else palette.textPrimary,
                onSurfaceVariant = if (highContrast) palette.textPrimary else palette.textSecondary,
                outline = palette.divider, error = SonaraError,
                primary = p.primary, onPrimary = Color.White,
                primaryContainer = p.primary.copy(alpha = 0.10f), onPrimaryContainer = p.primaryDark,
                secondary = Color(0xFF5A7A8A),
            )
        }
        !useDark -> lightColorScheme.copy(
            primary = p.primary, onPrimary = Color.White,
            primaryContainer = p.primary.copy(alpha = 0.10f), onPrimaryContainer = p.primaryDark,
            secondary = Color(0xFF5A7A8A), onSecondary = Color.White,
            background = palette.background, onBackground = if (highContrast) Color.Black else palette.textPrimary,
            surface = palette.surface, onSurface = if (highContrast) Color.Black else palette.textPrimary,
            surfaceVariant = palette.card, onSurfaceVariant = if (highContrast) palette.textPrimary else palette.textSecondary,
            outline = palette.divider, outlineVariant = Color(0xFFE5E5EA),
            error = SonaraError, inverseSurface = Color(0xFF2C2C2E), inverseOnSurface = Color(0xFFF5F5F5),
        )
        else -> darkColorScheme.copy(
            primary = p.primary, onPrimary = palette.background,
            primaryContainer = p.primary.copy(alpha = 0.1f), onPrimaryContainer = p.primaryLight,
            secondary = SonaraInfo,
            background = palette.background, surface = palette.surface,
            surfaceVariant = palette.card,
            onBackground = if (highContrast) Color.White else palette.textPrimary,
            onSurface = if (highContrast) Color.White else palette.textPrimary,
            onSurfaceVariant = if (highContrast) palette.textPrimary else palette.textSecondary,
            outline = palette.divider, outlineVariant = palette.divider, error = SonaraError,
        )
    }

    CompositionLocalProvider(LocalSonaraColors provides palette) {
        androidx.compose.material3.MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = SonaraShapes,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}
