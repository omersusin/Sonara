package com.sonara.app.intelligence.lyrics

enum class LyricsAnimationStyle(val id: String, val displayName: String) {
    NONE("NONE", "None"),
    FADE("FADE", "Fade"),
    GLOW("GLOW", "Glow"),
    SLIDE("SLIDE", "Slide"),
    KARAOKE("KARAOKE", "Karaoke"),
    APPLE("APPLE", "Apple Music"),
    APPLE_V2("APPLE_V2", "Apple Music V2"),
    VIVIMUSIC("VIVIMUSIC", "Vivimusic (Fluid)"),
    LYRICS_V2("LYRICS_V2", "Lyrics V2 (Flowing)"),
    METRO("METRO", "MetroLyrics");

    val label: String get() = displayName

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: KARAOKE
    }
}
