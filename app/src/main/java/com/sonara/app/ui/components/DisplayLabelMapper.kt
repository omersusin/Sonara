/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sonara.app.ui.components

/**
 * Madde 8: HIP_HOP → Hip-Hop, DRUM_AND_BASS → Drum & Bass
 * UI hiçbir zaman raw enum göstermemeli.
 */
object DisplayLabelMapper {

    private val genreMap = mapOf(
        "HIP_HOP" to "Hip-Hop", "hip-hop" to "Hip-Hop", "hip_hop" to "Hip-Hop",
        "RNB" to "R&B", "rnb" to "R&B", "r&b" to "R&B",
        "DRUM_AND_BASS" to "Drum & Bass", "drum_and_bass" to "Drum & Bass", "dnb" to "DnB",
        "K_POP" to "K-Pop", "k-pop" to "K-Pop", "kpop" to "K-Pop",
        "J_POP" to "J-Pop", "j-pop" to "J-Pop", "jpop" to "J-Pop",
        "C_POP" to "C-Pop", "c-pop" to "C-Pop",
        "SYNTH_POP" to "Synth-Pop", "synth-pop" to "Synth-Pop",
        "DREAM_POP" to "Dream Pop", "dream-pop" to "Dream Pop",
        "LO_FI" to "Lo-Fi", "lo-fi" to "Lo-Fi", "lofi" to "Lo-Fi",
        "TRIP_HOP" to "Trip-Hop", "trip-hop" to "Trip-Hop",
        "POST_ROCK" to "Post-Rock", "post-rock" to "Post-Rock",
        "POST_PUNK" to "Post-Punk", "post-punk" to "Post-Punk",
        "ALT_ROCK" to "Alt-Rock", "alt-rock" to "Alt-Rock",
        "INDIE_ROCK" to "Indie Rock", "indie-rock" to "Indie Rock",
        "INDIE_POP" to "Indie Pop", "indie-pop" to "Indie Pop",
        "DEEP_HOUSE" to "Deep House", "deep-house" to "Deep House",
        "TECH_HOUSE" to "Tech House", "tech-house" to "Tech House",
        "FUTURE_BASS" to "Future Bass", "future-bass" to "Future Bass",
        "DOOM_METAL" to "Doom Metal", "doom-metal" to "Doom Metal",
        "BLACK_METAL" to "Black Metal", "black-metal" to "Black Metal",
        "DEATH_METAL" to "Death Metal", "death-metal" to "Death Metal",
        "THRASH_METAL" to "Thrash Metal", "thrash-metal" to "Thrash Metal",
        "NEO_SOUL" to "Neo-Soul", "neo-soul" to "Neo-Soul",
        "BOOM_BAP" to "Boom Bap", "boom-bap" to "Boom Bap",
        "UK_DRILL" to "UK Drill", "uk-drill" to "UK Drill",
        "LATIN_TRAP" to "Latin Trap", "latin-trap" to "Latin Trap",
        "EMO_RAP" to "Emo Rap", "emo-rap" to "Emo Rap",
        "CLOUD_RAP" to "Cloud Rap", "cloud-rap" to "Cloud Rap",
        "POP_PUNK" to "Pop Punk", "pop-punk" to "Pop Punk",
        "POP_ROCK" to "Pop Rock", "pop-rock" to "Pop Rock",
        "BOSSA_NOVA" to "Bossa Nova", "bossa-nova" to "Bossa Nova",
        "TURKISH_POP" to "Turkish Pop", "turkish-pop" to "Turkish Pop",
        "ELECTRONIC" to "Electronic", "electronic" to "Electronic",
        "POP" to "Pop", "pop" to "Pop",
        "ROCK" to "Rock", "rock" to "Rock",
        "METAL" to "Metal", "metal" to "Metal",
        "JAZZ" to "Jazz", "jazz" to "Jazz",
        "BLUES" to "Blues", "blues" to "Blues",
        "CLASSICAL" to "Classical", "classical" to "Classical",
        "COUNTRY" to "Country", "country" to "Country",
        "FOLK" to "Folk", "folk" to "Folk",
        "REGGAE" to "Reggae", "reggae" to "Reggae",
        "LATIN" to "Latin", "latin" to "Latin",
        "AMBIENT" to "Ambient", "ambient" to "Ambient",
        "SOUL" to "Soul", "soul" to "Soul",
        "FUNK" to "Funk", "funk" to "Funk",
        "PUNK" to "Punk", "punk" to "Punk",
        "INDIE" to "Indie", "indie" to "Indie",
        "ALTERNATIVE" to "Alternative", "alternative" to "Alternative",
        "DANCE" to "Dance", "dance" to "Dance",
        "WORLD" to "World", "world" to "World",
        "PODCAST" to "Podcast", "AUDIOBOOK" to "Audiobook",
        "SPEECH" to "Speech", "UNKNOWN" to "Unknown",
    )

    private val moodMap = mapOf(
        "MELANCHOLIC" to "Melancholic",
        "ENERGETIC" to "Energetic",
        "AGGRESSIVE" to "Aggressive",
        "ROMANTIC" to "Romantic",
        "DREAMY" to "Dreamy",
        "INTENSE" to "Intense",
        "NEUTRAL" to "Neutral",
        "CALM" to "Calm",
        "DARK" to "Dark",
        "HAPPY" to "Happy",
    )

    /** Ana format fonksiyonu — tüm UI burayı kullanmalı */
    fun formatGenre(raw: String): String {
        if (raw.isBlank()) return "Unknown"
        // Önce doğrudan map'te ara
        genreMap[raw]?.let { return it }
        genreMap[raw.lowercase()]?.let { return it }
        genreMap[raw.uppercase()]?.let { return it }

        // Genel formatlama: SNAKE_CASE → Title Case, tire korunur
        return raw.lowercase().replace("_", " ")
            .split(" ", "-")
            .joinToString(" ") { word ->
                if (word.length <= 2 && word.all { it.isLetter() }) word.uppercase()
                else word.replaceFirstChar { it.uppercase() }
            }
            .replace(" - ", "-")
    }

    fun formatMood(raw: String): String {
        if (raw.isBlank()) return "Neutral"
        return moodMap[raw] ?: moodMap[raw.uppercase()] ?: raw.replaceFirstChar { it.uppercase() }
    }

    fun formatSubGenre(raw: String): String = formatGenre(raw)

    fun formatSource(raw: String): String = when (raw.lowercase()) {
        "lastfm", "last.fm" -> "Last.fm"
        "local_classifier", "local ai", "local" -> "Local AI"
        "merged", "ai + last.fm" -> "AI + Last.fm"
        "adaptive_override", "learned" -> "Learned"
        "user_preset" -> "User Preset"
        "cache" -> "Cache"
        "fallback" -> "Fallback"
        "lyrics" -> "Lyrics"
        "gemini" -> "Gemini"
        else -> raw.replaceFirstChar { it.uppercase() }
    }
}
