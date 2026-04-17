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

package com.sonara.app.ai.models

data class SonaraAiResult(
    val genres: Map<String, Float> = emptyMap(),
    val mood: SonaraMood = SonaraMood(),
    val energy: Float = 0.5f,
    val confidence: Float = 0f,
    val confidenceLevel: SonaraConfidence = SonaraConfidence.NONE,
    val source: SonaraSource = SonaraSource.NONE,
    val spectralProfile: FloatArray? = null,
    val eqBands: FloatArray = FloatArray(10),
    val eqPreamp: Float = 0f,
    val isSpectralEq: Boolean = false,
    val explanation: SonaraExplanation = SonaraExplanation()
) {
    val primaryGenre: String
        get() = genres.maxByOrNull { it.value }?.key
            ?.replaceFirstChar { it.uppercase() } ?: "Unknown"

    val topGenres: List<String>
        get() = genres.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key.replaceFirstChar { c -> c.uppercase() } }

    val summary: String
        get() = "$primaryGenre · ${mood.description} · $energyLabel"

    val energyLabel: String
        get() = when {
            energy > 0.7f -> "High energy"
            energy > 0.4f -> "Moderate"
            else -> "Calm"
        }

    val sourceBadge: String get() = source.displayName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SonaraAiResult) return false
        return genres == other.genres && mood == other.mood &&
            confidence == other.confidence && source == other.source
    }
    override fun hashCode(): Int = genres.hashCode() * 31 + source.hashCode()

    companion object {
        fun empty() = SonaraAiResult()
    }
}

data class SonaraMood(
    val valence: Float = 0f,
    val arousal: Float = 0.5f
) {
    val description: String
        get() {
            val v = when {
                valence > 0.3f -> "Bright"
                valence < -0.3f -> "Dark"
                else -> "Neutral"
            }
            val a = when {
                arousal > 0.6f -> "Energetic"
                arousal < 0.3f -> "Calm"
                else -> "Moderate"
            }
            return "$v · $a"
        }
}

enum class SonaraConfidence(val label: String) {
    HIGH("High confidence"),
    MODERATE("Moderate confidence"),
    LOW("Low confidence"),
    NONE("No analysis")
}

enum class SonaraSource(val displayName: String) {
    AUDIO_LEARNED("Audio analysis ✦"),
    AUDIO_PROTOTYPE("Audio analysis"),
    AUDIO_RULES("Audio analysis"),
    LASTFM("Track info"),
    METADATA("Track info"),
    CACHED("Recognized"),
    NONE("—"),
    FALLBACK("Limited")
}

data class SonaraExplanation(
    val summary: String = "",
    val eqReason: String = "",
    val sourceHonesty: String = ""
)
