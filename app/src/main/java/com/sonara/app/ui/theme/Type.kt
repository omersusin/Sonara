package com.sonara.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.sonara.app.R

val interFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val InterFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = interFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = interFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = interFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Inter"), fontProvider = interFontProvider, weight = FontWeight.Bold),
)

val SonaraTypography = Typography(
    displayLarge  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,     fontSize = 40.sp, letterSpacing = (-1.0).sp, lineHeight = 48.sp),
    displayMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,     fontSize = 32.sp, letterSpacing = (-0.5).sp, lineHeight = 40.sp),
    displaySmall  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,     fontSize = 26.sp, letterSpacing = (-0.3).sp, lineHeight = 34.sp),
    headlineMedium= TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge    = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium   = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall    = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium    = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall     = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,   fontSize = 12.sp, letterSpacing = 0.2.sp),
    labelSmall    = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,   fontSize = 10.sp, letterSpacing = 0.3.sp),
)
