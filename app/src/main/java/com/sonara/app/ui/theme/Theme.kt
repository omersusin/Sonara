package com.sonara.app.ui.theme

import android.app.Activity
import android.os.Build
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sonara.app.ui.common.LocalDarkMode
import com.sonara.app.ui.common.LocalDynamicColor
import com.sonara.app.ui.common.LocalHighContrastDarkMode

@Composable
fun SonaraTheme(content: @Composable () -> Unit) {
    val typography = remember { buildTypography(SonaraFont.INTER) }

    val view = LocalView.current
    val context = LocalContext.current
    val darkTheme = LocalDarkMode.current
    val dynamicColor = LocalDynamicColor.current
    val isHighContrastDarkTheme = LocalHighContrastDarkMode.current

    LaunchedEffect(darkTheme) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            view.windowInsetsController?.setSystemBarsAppearance(
                if (darkTheme) 0 else APPEARANCE_LIGHT_STATUS_BARS,
                APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(window, view)

            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme

            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme && isHighContrastDarkTheme) highContrastDynamicDarkColorScheme(context)
            else if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        darkTheme -> {
            if (isHighContrastDarkTheme) highContrastDarkColorSchemeFromSeed()
            else darkColorSchemeFromSeed()
        }

        else -> lightColorSchemeFromSeed()
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = SonaraShapes,
        content = content
    )
}
