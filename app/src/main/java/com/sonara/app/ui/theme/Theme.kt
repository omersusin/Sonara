package com.sonara.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Madde 9 FIX: Theme ayarları artık gerçekten uygulanıyor.
 * themeMode: "system" / "light" / "dark"
 * dynamicColors: Android 12+ wallpaper renkleri
 * highContrast: Metin kontrastını artır
 */
@Composable
fun SonaraTheme(
    accentColor: AccentColor = AccentColor.Amber,
    themeMode: String = "dark",
    dynamicColors: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val useDynamic = dynamicColors && Build.VERSION.SDK_INT >= 31

    val colorScheme = when {
        // Dynamic colors — Android 12+
        useDynamic && useDark -> {
            val dynamic = dynamicDarkColorScheme(context)
            dynamic.copy(
                background = SonaraBackground,
                surface = SonaraSurface,
                surfaceVariant = SonaraCard,
                onBackground = if (highContrast) Color.White else SonaraTextPrimary,
                onSurface = if (highContrast) Color.White else SonaraTextPrimary,
                onSurfaceVariant = if (highContrast) SonaraTextPrimary else SonaraTextSecondary,
                outline = SonaraDivider,
                outlineVariant = SonaraDivider,
                error = SonaraError
            )
        }
        useDynamic && !useDark -> {
            val dynamic = dynamicLightColorScheme(context)
            dynamic.copy(
                onBackground = if (highContrast) Color.Black else Color(0xFF1C1C1E),
                onSurface = if (highContrast) Color.Black else Color(0xFF1C1C1E)
            )
        }
        // Accent color auto fallback
        !useDark -> {
            val p = if (accentColor == AccentColor.Auto) AccentColor.Amber else accentColor
            lightColorScheme(
                primary = p.primary,
                onPrimary = Color.White,
                primaryContainer = p.primary.copy(alpha = 0.12f),
                onPrimaryContainer = p.primary,
                background = Color(0xFFFAFAFA),
                surface = Color.White,
                surfaceVariant = Color(0xFFF0F0F0),
                onBackground = if (highContrast) Color.Black else Color(0xFF1C1C1E),
                onSurface = if (highContrast) Color.Black else Color(0xFF1C1C1E),
                onSurfaceVariant = if (highContrast) Color(0xFF1C1C1E) else Color(0xFF636366),
                outline = Color(0xFFD1D1D6),
                error = SonaraError
            )
        }
        else -> {
            val p = if (accentColor == AccentColor.Auto) AccentColor.Amber else accentColor
            darkColorScheme(
                primary = p.primary,
                onPrimary = SonaraBackground,
                primaryContainer = p.primary.copy(alpha = 0.1f),
                onPrimaryContainer = p.primaryLight,
                secondary = SonaraInfo,
                background = SonaraBackground,
                surface = SonaraSurface,
                surfaceVariant = SonaraCard,
                onBackground = if (highContrast) Color.White else SonaraTextPrimary,
                onSurface = if (highContrast) Color.White else SonaraTextPrimary,
                onSurfaceVariant = if (highContrast) SonaraTextPrimary else SonaraTextSecondary,
                outline = SonaraDivider,
                outlineVariant = SonaraDivider,
                error = SonaraError
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SonaraTypography,
        shapes = SonaraShapes,
        content = content
    )
}
