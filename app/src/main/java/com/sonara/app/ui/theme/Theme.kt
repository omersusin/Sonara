package com.sonara.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun SonaraTheme(
    accentColor: AccentColor = AccentColor.Amber,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        accentColor == AccentColor.Auto && Build.VERSION.SDK_INT >= 31 -> {
            val dynamic = dynamicDarkColorScheme(LocalContext.current)
            dynamic.copy(
                background = SonaraBackground,
                surface = SonaraSurface,
                surfaceVariant = SonaraCard,
                onBackground = SonaraTextPrimary,
                onSurface = SonaraTextPrimary,
                onSurfaceVariant = SonaraTextSecondary,
                outline = SonaraDivider,
                outlineVariant = SonaraDivider,
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
                onBackground = SonaraTextPrimary,
                onSurface = SonaraTextPrimary,
                onSurfaceVariant = SonaraTextSecondary,
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
