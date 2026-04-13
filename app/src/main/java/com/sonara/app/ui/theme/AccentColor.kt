package com.sonara.app.ui.theme
import androidx.compose.ui.graphics.Color
enum class AccentColor(val displayName: String, val primary: Color, val primaryLight: Color, val primaryDark: Color) {
    Auto("Auto", Color(0xFF0061A4), Color(0xFFD1E4FF), Color(0xFF003258)),
    Amber("Amber", Color(0xFFFFB900), Color(0xFFFFE082), Color(0xFFC78D00)),
    Rose("Rose", Color(0xFFFF5252), Color(0xFFFF8A80), Color(0xFFD32F2F)),
    Ocean("Ocean", Color(0xFF00B0FF), Color(0xFF80D8FF), Color(0xFF0091EA)),
    Sage("Sage", Color(0xFF00E676), Color(0xFFB9F6CA), Color(0xFF00C853)),
    Lavender("Lavender", Color(0xFF7C4DFF), Color(0xFFB388FF), Color(0xFF651FFF)),
    Coral("Coral", Color(0xFFFF6E40), Color(0xFFFF9E80), Color(0xFFFF3D00)),
    Ice("Ice", Color(0xFF18FFFF), Color(0xFF84FFFF), Color(0xFF00E5FF)),
    Pearl("Pearl", Color(0xFFECEFF1), Color(0xFFFFFFFF), Color(0xFFB0BEC5))
}
