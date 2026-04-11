package com.sonara.app.ui.theme
import androidx.compose.ui.graphics.Color
enum class AccentColor(val displayName: String, val primary: Color, val primaryLight: Color, val primaryDark: Color) {
    Auto("Auto", Color(0xFF8E8E93), Color(0xFFAAAAAA), Color(0xFF6E6E73)),
    Amber("Amber", Color(0xFFE8A854), Color(0xFFF5C882), Color(0xFFC48B3A)),
    Rose("Rose", Color(0xFFE07070), Color(0xFFEF9E9E), Color(0xFFC05050)),
    Ocean("Ocean", Color(0xFF5A9EC8), Color(0xFF8BC0DD), Color(0xFF3E7EA8)),
    Sage("Sage", Color(0xFF72B06E), Color(0xFF9FCC9D), Color(0xFF52904E)),
    Lavender("Lavender", Color(0xFF9A7BD4), Color(0xFFBDA6E4), Color(0xFF7E5FB8)),
    Coral("Coral", Color(0xFFE88060), Color(0xFFF0A890), Color(0xFFC86040)),
    Ice("Ice", Color(0xFF60B8D4), Color(0xFF90D0E4), Color(0xFF4098B4)),
    Pearl("Pearl", Color(0xFFB0A898), Color(0xFFCCC4B8), Color(0xFF948C7C))
}
