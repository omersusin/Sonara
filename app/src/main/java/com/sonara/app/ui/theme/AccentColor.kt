package com.sonara.app.ui.theme

import androidx.compose.ui.graphics.Color

enum class AccentColor(
    val displayName: String,
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color
) {
    Auto("Auto", Color(0xFF8E8E93), Color(0xFFAAAAAA), Color(0xFF6E6E73)),
    Amber("Amber", Color(0xFFD4A574), Color(0xFFEDCFAA), Color(0xFFB8875A)),
    Rose("Rose", Color(0xFFCF8E8E), Color(0xFFE4B3B3), Color(0xFFB37272)),
    Ocean("Ocean", Color(0xFF7B9EB8), Color(0xFFA3BFD4), Color(0xFF5F8099)),
    Sage("Sage", Color(0xFF8BA888), Color(0xFFAFC4AD), Color(0xFF6F8C6C)),
    Lavender("Lavender", Color(0xFFA08BC4), Color(0xFFBFADD9), Color(0xFF846FAA)),
    Coral("Coral", Color(0xFFCC8B74), Color(0xFFE0AFA0), Color(0xFFAA6F5A)),
    Ice("Ice", Color(0xFF88B5C4), Color(0xFFAACDD8), Color(0xFF6C99AA)),
    Pearl("Pearl", Color(0xFFADA69C), Color(0xFFC8C2B8), Color(0xFF918A80))
}
