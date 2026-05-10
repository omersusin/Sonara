package com.sonara.app.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.sonara.app.ui.utils.a1
import com.sonara.app.ui.utils.a2
import com.sonara.app.ui.utils.a3
import com.sonara.app.ui.utils.error
import com.sonara.app.ui.utils.n1
import com.sonara.app.ui.utils.n2

// ── Surface / container roles ────────────────────────────────────────────────────
val SonaraBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val SonaraSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val SonaraCard: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer
val SonaraCardElevated: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
val SonaraDivider: Color @Composable get() = MaterialTheme.colorScheme.outline

// ── Text / content roles ─────────────────────────────────────────────────────────
val SonaraTextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val SonaraTextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val SonaraTextTertiary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = 0.6f
    )

// ── Semantic status roles (mapped to MD3 tonal roles) ───────────────────────────
val SonaraError: Color @Composable get() = MaterialTheme.colorScheme.error
val SonaraSuccess: Color @Composable get() = MaterialTheme.colorScheme.tertiary
val SonaraWarning: Color @Composable get() = MaterialTheme.colorScheme.secondary
val SonaraInfo: Color @Composable get() = MaterialTheme.colorScheme.inversePrimary


@Composable
fun lightColorSchemeFromSeed(): ColorScheme {
    return expressiveLightColorScheme().copy(
        primary = 40.a1,
        primaryContainer = 90.a1,
        onPrimary = 100.a1,
        onPrimaryContainer = 10.a1,
        inversePrimary = 80.a1,

        secondary = 40.a2,
        secondaryContainer = 90.a2,
        onSecondary = 100.a2,
        onSecondaryContainer = 10.a2,

        tertiary = 40.a3,
        tertiaryContainer = 90.a3,
        onTertiary = 100.a3,
        onTertiaryContainer = 10.a3,

        error = 40.error,
        errorContainer = 90.error,
        onError = 100.error,
        onErrorContainer = 10.error,

        background = 98.n1,
        onBackground = 10.n1,

        surface = 98.n1,
        onSurface = 10.n1,
        surfaceVariant = 90.n2,
        onSurfaceVariant = 30.n2,
        surfaceDim = 87.n1,
        surfaceBright = 98.n1,
        surfaceContainerLowest = 100.n2,
        surfaceContainerLow = 96.n2,
        surfaceContainer = 94.n2,
        surfaceContainerHigh = 92.n2,
        surfaceContainerHighest = 90.n2,
        inverseSurface = 20.n1,
        inverseOnSurface = 95.n1,

        outline = 50.n2,
        outlineVariant = 80.n2,
    )
}

@Composable
fun darkColorSchemeFromSeed(): ColorScheme {
    return darkColorScheme(
        primary = 80.a1,
        primaryContainer = 30.a1,
        onPrimary = 20.a1,
        onPrimaryContainer = 90.a1,
        inversePrimary = 40.a1,

        secondary = 80.a2,
        secondaryContainer = 30.a2,
        onSecondary = 20.a2,
        onSecondaryContainer = 90.a2,

        tertiary = 80.a3,
        tertiaryContainer = 30.a3,
        onTertiary = 20.a3,
        onTertiaryContainer = 90.a3,

        error = 80.error,
        errorContainer = 30.error,
        onError = 20.error,
        onErrorContainer = 90.error,

        background = 6.n1,
        onBackground = 90.n1,

        surface = 6.n1,
        onSurface = 90.n1,
        surfaceVariant = 30.n2,
        onSurfaceVariant = 80.n2,
        surfaceDim = 6.n1,
        surfaceBright = 24.n1,
        surfaceContainerLowest = 4.n2,
        surfaceContainerLow = 10.n2,
        surfaceContainer = 12.n2,
        surfaceContainerHigh = 17.n2,
        surfaceContainerHighest = 22.n2,
        inverseSurface = 90.n1,
        inverseOnSurface = 20.n1,

        outline = 60.n2,
        outlineVariant = 30.n2,
    )
}

@Composable
fun highContrastDarkColorSchemeFromSeed(): ColorScheme {
    return darkColorSchemeFromSeed().copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = 6.n2,
        surfaceContainer = 10.n2,
        surfaceContainerHigh = 12.n2,
        surfaceContainerHighest = 17.n2,
    )
}

@RequiresApi(Build.VERSION_CODES.S)
fun highContrastDynamicDarkColorScheme(context: Context): ColorScheme =
    with(dynamicDarkColorScheme(context)) {
        return this.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = surfaceContainerLowest,
            surfaceContainer = surfaceContainerLow,
            surfaceContainerHigh = surfaceContainer,
            surfaceContainerHighest = surfaceContainerHigh,
        )
    }

@Composable
fun Color.harmonizeWithPrimary(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 0.1f
): Color = blend(MaterialTheme.colorScheme.primary, fraction)


fun Color.blend(
    color: Color,
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 0.1f
): Color = Color(ColorUtils.blendARGB(this.toArgb(), color.toArgb(), fraction))

fun colorLerp(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)

    val startRed = start.red
    val startGreen = start.green
    val startBlue = start.blue
    val startAlpha = start.alpha

    val endRed = end.red
    val endGreen = end.green
    val endBlue = end.blue
    val endAlpha = end.alpha

    return Color(
        red = startRed + (endRed - startRed) * f,
        green = startGreen + (endGreen - startGreen) * f,
        blue = startBlue + (endBlue - startBlue) * f,
        alpha = startAlpha + (endAlpha - startAlpha) * f
    )
}
