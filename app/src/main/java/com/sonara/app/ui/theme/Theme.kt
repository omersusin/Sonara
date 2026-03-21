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

    // AMOLED overrides
    val amoledBg = Color(0xFF000000)
    val amoledSurface = Color(0xFF0A0A0A)
    val amoledCard = Color(0xFF141414)
    val amoledCardElev = Color(0xFF1A1A1A)

    val colorScheme = when {
        // Dynamic + dark
        useDynamic && useDark -> {
            val base = dynamicDarkColorScheme(context)
            if (amoledMode) {
                base.copy(background = amoledBg, surface = amoledSurface, surfaceVariant = amoledCard,
                    onBackground = if (highContrast) Color.White else SonaraTextPrimary,
                    onSurface = if (highContrast) Color.White else SonaraTextPrimary,
                    onSurfaceVariant = if (highContrast) SonaraTextPrimary else SonaraTextSecondary,
                    outline = Color(0xFF2A2A2E), outlineVariant = Color(0xFF2A2A2E), error = SonaraError)
            } else {
                base.copy(background = SonaraBackground, surface = SonaraSurface, surfaceVariant = SonaraCard,
                    onBackground = if (highContrast) Color.White else SonaraTextPrimary,
                    onSurface = if (highContrast) Color.White else SonaraTextPrimary,
                    onSurfaceVariant = if (highContrast) SonaraTextPrimary else SonaraTextSecondary,
                    outline = SonaraDivider, outlineVariant = SonaraDivider, error = SonaraError)
            }
        }
        // Dynamic + light
        useDynamic && !useDark -> {
            val base = dynamicLightColorScheme(context)
            base.copy(
                background = Color(0xFFF8F8FA), surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFF0F0F2),
                onBackground = if (highContrast) Color.Black else Color(0xFF1C1C1E),
                onSurface = if (highContrast) Color.Black else Color(0xFF1C1C1E),
                onSurfaceVariant = if (highContrast) Color(0xFF1C1C1E) else Color(0xFF636366),
                outline = Color(0xFFD1D1D6), error = SonaraError)
        }
        // Light theme (proper)
        !useDark -> lightColorScheme(
            primary = p.primary,
            onPrimary = Color.White,
            primaryContainer = p.primary.copy(alpha = 0.10f),
            onPrimaryContainer = p.primaryDark,
            secondary = Color(0xFF5A7A8A),
            onSecondary = Color.White,
            background = Color(0xFFF8F8FA),
            onBackground = if (highContrast) Color.Black else Color(0xFF1C1C1E),
            surface = Color(0xFFFFFFFF),
            onSurface = if (highContrast) Color.Black else Color(0xFF1C1C1E),
            surfaceVariant = Color(0xFFF0F0F2),
            onSurfaceVariant = if (highContrast) Color(0xFF1C1C1E) else Color(0xFF636366),
            outline = Color(0xFFD1D1D6),
            outlineVariant = Color(0xFFE5E5EA),
            error = Color(0xFFD32F2F),
            inverseSurface = Color(0xFF2C2C2E),
            inverseOnSurface = Color(0xFFF5F5F5)
        )
        // AMOLED dark
        amoledMode -> darkColorScheme(
            primary = p.primary,
            onPrimary = amoledBg,
            primaryContainer = p.primary.copy(alpha = 0.12f),
            onPrimaryContainer = p.primaryLight,
            secondary = SonaraInfo,
            background = amoledBg,
            surface = amoledSurface,
            surfaceVariant = amoledCard,
            onBackground = if (highContrast) Color.White else SonaraTextPrimary,
            onSurface = if (highContrast) Color.White else SonaraTextPrimary,
            onSurfaceVariant = if (highContrast) SonaraTextPrimary else SonaraTextSecondary,
            outline = Color(0xFF2A2A2E),
            outlineVariant = Color(0xFF2A2A2E),
            error = SonaraError
        )
        // Normal dark
        else -> darkColorScheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SonaraTypography,
        shapes = SonaraShapes,
        content = content
    )
}
