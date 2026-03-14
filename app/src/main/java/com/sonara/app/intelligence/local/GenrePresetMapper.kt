package com.sonara.app.intelligence.local

import com.sonara.app.preset.BuiltInPresets
import com.sonara.app.preset.Preset

object GenrePresetMapper {

    private val genreToPreset = mapOf(
        "rock" to "Rock",
        "pop" to "Pop",
        "hip-hop" to "Hip-Hop",
        "electronic" to "Electronic",
        "r&b" to "R&B",
        "jazz" to "Jazz",
        "classical" to "Classical",
        "metal" to "Rock",
        "folk" to "Acoustic",
        "country" to "Acoustic",
        "blues" to "Jazz",
        "reggae" to "Bass Boost",
        "latin" to "Pop"
    )

    private val moodToPreset = mapOf(
        "energetic" to "Workout",
        "chill" to "Chill",
        "melancholic" to "Late Night",
        "happy" to "Morning",
        "romantic" to "Chill"
    )

    fun findBestPreset(genre: String, mood: String): String {
        return genreToPreset[genre] ?: moodToPreset[mood] ?: "Flat"
    }

    fun findPresetFromBuiltIn(genre: String, mood: String): Preset? {
        val name = findBestPreset(genre, mood)
        return BuiltInPresets.ALL.firstOrNull { it.name == name }
    }

    fun suggestPresets(genre: String, mood: String): List<String> {
        val suggestions = mutableListOf<String>()

        genreToPreset[genre]?.let { suggestions.add(it) }
        moodToPreset[mood]?.let { if (it !in suggestions) suggestions.add(it) }

        val alwaysRelevant = listOf("Flat", "V-Shape", "Loudness")
        alwaysRelevant.forEach { if (it !in suggestions) suggestions.add(it) }

        return suggestions.take(6)
    }
}
