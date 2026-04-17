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

package com.sonara.app.intelligence.local

import com.sonara.app.preset.BuiltInPresets
import com.sonara.app.preset.Preset

object GenrePresetMapper {
    private val genreToPreset = mapOf(
        "rock" to "Rock", "pop" to "Pop", "hip-hop" to "Hip-Hop", "electronic" to "Electronic",
        "r&b" to "R&B", "jazz" to "Jazz", "classical" to "Classical", "metal" to "Rock",
        "folk" to "Acoustic", "country" to "Acoustic", "blues" to "Jazz", "reggae" to "Bass Boost",
        "latin" to "Bass Boost", "other" to "Flat"
    )
    private val moodToPreset = mapOf(
        "energetic" to "Workout", "chill" to "Chill", "melancholic" to "Late Night",
        "happy" to "Morning", "romantic" to "Chill", "intense" to "Workout", "neutral" to "Flat"
    )
    fun findBestPreset(genre: String, mood: String) = genreToPreset[genre] ?: moodToPreset[mood] ?: "Flat"
    fun findPresetFromBuiltIn(genre: String, mood: String): Preset? = BuiltInPresets.ALL.firstOrNull { it.name == findBestPreset(genre, mood) }
    fun suggestPresets(genre: String, mood: String): List<String> {
        val s = mutableListOf<String>(); genreToPreset[genre]?.let { s.add(it) }; moodToPreset[mood]?.let { if (it !in s) s.add(it) }
        listOf("Flat","V-Shape","Loudness").forEach { if (it !in s) s.add(it) }; return s.take(6)
    }
}
