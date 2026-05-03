package com.sonara.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SonaraTheme(
    seedColor: Color = AccentSeeds.Amber.seed,
    themeMode: String = "dark",
    dynamicColor: Boolean = false,
    highContrast: Boolean = false,
    isAmoled: Boolean = false,
    font: SonaraFont = SonaraFont.INTER,
    paletteStyle: SonaraPaletteStyle = SonaraPaletteStyle.EXPRESSIVE,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "light" -> false
        "dark"  -> true
        else    -> isSystemInDarkTheme()
    }

    val effectiveSeed = if (dynamicColor && Build.VERSION.SDK_INT >= 31) {
        val ctx = LocalContext.current
        if (isDark) dynamicDarkColorScheme(ctx).primary
        else dynamicLightColorScheme(ctx).primary
    } else seedColor

    var scheme = rememberDynamicColorScheme(
        seedColor = effectiveSeed,
        isDark = isDark,
        style = paletteStyle.toMaterialKolor()
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
            onBackground = Color.White, onSurface = Color.White, onSurfaceVariant = Color.White
        ) else scheme.copy(
            onBackground = Color.Black, onSurface = Color.Black, onSurfaceVariant = Color.Black
        )
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        typography = buildTypography(font),
        shapes = SonaraShapes,
        content = content
    )
}
