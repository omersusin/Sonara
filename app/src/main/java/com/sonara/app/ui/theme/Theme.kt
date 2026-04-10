package com.sonara.app.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
        "dark"  -> true
        else    -> isSystemInDarkTheme()
    }

    val context    = LocalContext.current
    val useDynamic = dynamicColors && Build.VERSION.SDK_INT >= 31
    val p          = if (accentColor == AccentColor.Auto) AccentColor.Amber else accentColor

    // ── Palet seçimi ──────────────────────────────────────────
    val palette = when {
        !useDark    -> LightPalette
        amoledMode  -> AmoledPalette
        else        -> DarkPalette
    }

    // ── MD3 Expressive renk şeması ────────────────────────────
    // Expressive'in farkı: tertiary ve surface container rolleri
    // daha zengin ve birbirine kontrastlı renk değerleri alır.
    val colorScheme = when {
        useDynamic && useDark -> {
            val base = dynamicDarkColorScheme(context)
            base.copy(
                background            = palette.background,
                surface               = palette.surface,
                surfaceVariant        = palette.surfaceContainer,
                surfaceContainer      = palette.surfaceContainer,
                surfaceContainerHigh  = palette.surfaceContainerHigh,
                surfaceContainerHighest = palette.surfaceContainerHighest,
                onBackground          = if (highContrast) Color.White else palette.textPrimary,
                onSurface             = if (highContrast) Color.White else palette.textPrimary,
                onSurfaceVariant      = if (highContrast) palette.textPrimary else palette.textSecondary,
                outline               = palette.divider,
                outlineVariant        = palette.divider,
                error                 = SonaraError
            )
        }
        useDynamic && !useDark -> {
            val base = dynamicLightColorScheme(context)
            base.copy(
                background            = palette.background,
                surface               = palette.surface,
                surfaceVariant        = palette.surfaceContainer,
                surfaceContainer      = palette.surfaceContainer,
                surfaceContainerHigh  = palette.surfaceContainerHigh,
                surfaceContainerHighest = palette.surfaceContainerHighest,
                onBackground          = if (highContrast) Color.Black else palette.textPrimary,
                onSurface             = if (highContrast) Color.Black else palette.textPrimary,
                onSurfaceVariant      = if (highContrast) palette.textPrimary else palette.textSecondary,
                outline               = palette.divider,
                error                 = SonaraError
            )
        }
        !useDark -> lightColorScheme(
            primary                 = p.primary,
            onPrimary               = Color.White,
            primaryContainer        = p.primary.copy(alpha = 0.14f),
            onPrimaryContainer      = p.primaryDark,
            secondary               = SonaraSecondaryAccent,
            onSecondary             = Color.White,
            secondaryContainer      = SonaraSecondaryAccent.copy(alpha = 0.14f),
            onSecondaryContainer    = Color(0xFF3D2B5C),
            tertiary                = SonaraInfo,
            onTertiary              = Color(0xFF00363F),
            tertiaryContainer       = palette.tertiaryContainer,
            onTertiaryContainer     = palette.onTertiaryContainer,
            background              = palette.background,
            onBackground            = if (highContrast) Color.Black else palette.textPrimary,
            surface                 = palette.surface,
            onSurface               = if (highContrast) Color.Black else palette.textPrimary,
            surfaceVariant          = palette.surfaceContainer,
            onSurfaceVariant        = if (highContrast) palette.textPrimary else palette.textSecondary,
            surfaceContainer        = palette.surfaceContainer,
            surfaceContainerHigh    = palette.surfaceContainerHigh,
            surfaceContainerHighest = palette.surfaceContainerHighest,
            outline                 = palette.divider,
            outlineVariant          = palette.divider.copy(alpha = 0.5f),
            error                   = SonaraError,
            inverseSurface          = Color(0xFF1C1B22),
            inverseOnSurface        = Color(0xFFF2F2F7),
            inversePrimary          = palette.inversePrimary
        )
        else -> darkColorScheme(
            primary                 = p.primary,
            onPrimary               = palette.background,
            primaryContainer        = p.primary.copy(alpha = 0.16f),
            onPrimaryContainer      = p.primaryLight,
            secondary               = SonaraSecondaryAccent,
            onSecondary             = Color(0xFF1F0F38),
            secondaryContainer      = SonaraSecondaryAccent.copy(alpha = 0.16f),
            onSecondaryContainer    = Color(0xFFE0D0FF),
            tertiary                = SonaraInfo,
            onTertiary              = Color(0xFF003B47),
            tertiaryContainer       = palette.tertiaryContainer,
            onTertiaryContainer     = palette.onTertiaryContainer,
            background              = palette.background,
            surface                 = palette.surface,
            onBackground            = if (highContrast) Color.White else palette.textPrimary,
            onSurface               = if (highContrast) Color.White else palette.textPrimary,
            surfaceVariant          = palette.surfaceContainer,
            onSurfaceVariant        = if (highContrast) palette.textPrimary else palette.textSecondary,
            surfaceContainer        = palette.surfaceContainer,
            surfaceContainerHigh    = palette.surfaceContainerHigh,
            surfaceContainerHighest = palette.surfaceContainerHighest,
            outline                 = palette.divider,
            outlineVariant          = palette.divider.copy(alpha = 0.5f),
            error                   = SonaraError,
            inverseSurface          = Color(0xFFF2F2F7),
            inverseOnSurface        = Color(0xFF1C1B22),
            inversePrimary          = palette.inversePrimary
        )
    }

    CompositionLocalProvider(LocalSonaraColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = SonaraTypography,
            shapes      = SonaraShapes,
            content     = content
        )
    }
}
