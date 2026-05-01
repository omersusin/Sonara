package com.sonara.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun SonaraTheme(
    seedColor: Color = AccentSeeds.Amber.seed,
    themeMode: String = "dark",
    dynamicColor: Boolean = false,
    highContrast: Boolean = false,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "light" -> false
        "dark"  -> true
        else    -> isSystemInDarkTheme()
    }

    // When dynamic color is on (Android 12+), extract the wallpaper primary as seed
    val effectiveSeed = if (dynamicColor && Build.VERSION.SDK_INT >= 31) {
        val ctx = LocalContext.current
        if (isDark) dynamicDarkColorScheme(ctx).primary
        else dynamicLightColorScheme(ctx).primary
    } else {
        seedColor
    }

    var scheme = rememberDynamicColorScheme(
        seedColor = effectiveSeed,
        isDark = isDark,
        style = PaletteStyle.Expressive
    )

    if (isAmoled && isDark) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color(0xFF0A0A0E),
            surfaceContainer = Color(0xFF111116),
            surfaceContainerLow = Color(0xFF0D0D12),
            surfaceContainerHigh = Color(0xFF1A1A20),
            surfaceContainerHighest = Color(0xFF222228)
        )
    }

    if (highContrast) {
        scheme = if (isDark) scheme.copy(
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.White
        ) else scheme.copy(
            onBackground = Color.Black,
            onSurface = Color.Black,
            onSurfaceVariant = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = SonaraTypography,
        shapes = SonaraShapes,
        content = content
    )
}
