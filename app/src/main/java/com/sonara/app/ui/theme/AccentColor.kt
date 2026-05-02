package com.sonara.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class AccentSeed(val id: String, val displayName: String, val seed: Color)

object AccentSeeds {
    val Auto     = AccentSeed("auto",     "Auto",     Color(0xFFD4A574))
    val Amber    = AccentSeed("amber",    "Amber",    Color(0xFFE8A854))
    val Rose     = AccentSeed("rose",     "Rose",     Color(0xFFE07070))
    val Ocean    = AccentSeed("ocean",    "Ocean",    Color(0xFF5A9EC8))
    val Sage     = AccentSeed("sage",     "Sage",     Color(0xFF72B06E))
    val Lavender = AccentSeed("lavender", "Lavender", Color(0xFF9A7BD4))
    val Coral    = AccentSeed("coral",    "Coral",    Color(0xFFE88060))
    val Ice      = AccentSeed("ice",      "Ice",      Color(0xFF60B8D4))
    val Pearl    = AccentSeed("pearl",    "Pearl",    Color(0xFFB0A898))

    val all      = listOf(Auto, Amber, Rose, Ocean, Sage, Lavender, Coral, Ice, Pearl)
    val presets  = listOf(Amber, Rose, Ocean, Sage, Lavender, Coral, Ice, Pearl)

    fun fromId(id: String): AccentSeed = all.find { it.id == id } ?: Amber

    fun toHex(color: Color): String = "#%08X".format(color.toArgb().toLong() and 0xFFFFFFFFL)

    fun fromHex(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) { Amber.seed }

    // Migration: map old AccentColor enum names to their seed equivalents
    fun fromLegacyName(name: String): Color = when (name.lowercase()) {
        "auto"     -> Auto.seed
        "amber"    -> Amber.seed
        "rose"     -> Rose.seed
        "ocean"    -> Ocean.seed
        "sage"     -> Sage.seed
        "lavender" -> Lavender.seed
        "coral"    -> Coral.seed
        "ice"      -> Ice.seed
        "pearl"    -> Pearl.seed
        else       -> Amber.seed
    }
}
