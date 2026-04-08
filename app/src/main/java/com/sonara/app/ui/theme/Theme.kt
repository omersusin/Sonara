package com.sonara.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun SonaraTheme(
    accentColor: AccentColor = AccentColor.Amber,
    themeMode: String = "dark",
    dynamicColors: Boolean = true,
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

    val palette = when {
        !useDark -> LightPalette
        amoledMode -> AmoledPalette
        else -> DarkPalette
    }

    val colorScheme = when {
        useDynamic && useDark -> {
            val base = dynamicDarkColorScheme(context)
            base.copy(
                background = palette.background,
                surface = palette.surface,
                surfaceVariant = palette.card,
                surfaceTint = p.primary, // 🔥 ADD
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary,
                onSurfaceVariant = palette.textSecondary,
                outline = palette.divider,
                outlineVariant = palette.divider,
                error = SonaraError
            )
        }

        useDynamic && !useDark -> {
            val base = dynamicLightColorScheme(context)
            base.copy(
                background = Color(0xFFF4F6FA), // 🔥 FIX WHITE ISSUE
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFEAF0F8),
                surfaceTint = p.primary,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary,
                onSurfaceVariant = palette.textSecondary,
                outline = palette.divider,
                error = SonaraError
            )
        }

        !useDark -> lightColorScheme(
            primary = p.primary,
            onPrimary = Color.White,

            primaryContainer = p.primary.copy(alpha = 0.18f), // 🔥 FIX

            onPrimaryContainer = p.primaryDark,

            secondary = Color(0xFF5A7A8A),
            onSecondary = Color.White,

            background = Color(0xFFF4F6FA), // 🔥 MAIN FIX
            onBackground = palette.textPrimary,

            surface = Color(0xFFFFFFFF),
            onSurface = palette.textPrimary,

            surfaceVariant = Color(0xFFEAF0F8),
            onSurfaceVariant = palette.textSecondary,

            outline = palette.divider,
            outlineVariant = Color(0xFFD7DDE7),

            surfaceTint = p.primary, // 🔥 ADD

            error = Color(0xFFD32F2F),

            inverseSurface = Color(0xFF2C2C2E),
            inverseOnSurface = Color(0xFFF5F5F5)
        )

        else -> darkColorScheme(
            primary = p.primary,
            onPrimary = palette.background,

            primaryContainer = p.primary.copy(alpha = 0.15f),

            onPrimaryContainer = p.primaryLight,

            secondary = SonaraInfo,

            background = palette.background,
            surface = palette.surface,

            surfaceVariant = palette.card,
            surfaceTint = p.primary, // 🔥 ADD

            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary,

            outline = palette.divider,
            outlineVariant = palette.divider,

            error = SonaraError
        )
    }

    CompositionLocalProvider(LocalSonaraColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SonaraTypography,
            shapes = SonaraShapes,
            content = content
        )
    }
}
