package com.sonara.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.sonara.app.R

val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

fun buildFontFamily(font: SonaraFont): FontFamily {
    if (font == SonaraFont.SYSTEM_DEFAULT || font.googleFontName == null) return FontFamily.Default
    val gf = GoogleFont(font.googleFontName)
    return FontFamily(
        Font(gf, googleFontProvider, FontWeight.Normal),
        Font(gf, googleFontProvider, FontWeight.Medium),
        Font(gf, googleFontProvider, FontWeight.SemiBold),
        Font(gf, googleFontProvider, FontWeight.Bold),
    )
}

fun buildTypography(font: SonaraFont = SonaraFont.INTER): Typography {
    val ff = buildFontFamily(font)
    return Typography(
        displayLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold,     fontSize = 40.sp, letterSpacing = (-1.0).sp, lineHeight = 48.sp),
        displayMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold,     fontSize = 32.sp, letterSpacing = (-0.5).sp, lineHeight = 40.sp),
        displaySmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineLarge = TextStyle(fontFamily = ff, fontWeight = FontWeight.Bold,     fontSize = 26.sp, letterSpacing = (-0.3).sp, lineHeight = 34.sp),
        headlineMedium= TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        headlineSmall = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleLarge    = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleMedium   = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        titleSmall    = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.1.sp),
        bodyLarge     = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium    = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall     = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal,   fontSize = 11.sp, lineHeight = 16.sp),
        labelLarge    = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
        labelMedium   = TextStyle(fontFamily = ff, fontWeight = FontWeight.Medium,   fontSize = 12.sp, letterSpacing = 0.2.sp),
        labelSmall    = TextStyle(fontFamily = ff, fontWeight = FontWeight.Medium,   fontSize = 10.sp, letterSpacing = 0.3.sp),
    )
}

// Legacy compat — remove usages after migration
val SonaraTypography = buildTypography(SonaraFont.INTER)
