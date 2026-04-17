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

package com.sonara.app.intelligence.pipeline

object PredictionSourceMapper {

    data class SourceDisplay(
        val primary: String,
        val detail: String,
        val contributors: List<String>
    )

    fun map(prediction: SonaraPrediction, hasLyrics: Boolean = false): SourceDisplay {
        val contributors = mutableListOf<String>()

        when (prediction.source) {
            PredictionSource.LASTFM -> {
                contributors.add("Last.fm")
                if (hasLyrics) contributors.add("Lyrics")
            }
            PredictionSource.LOCAL_CLASSIFIER -> {
                contributors.add("Local AI")
                if (hasLyrics) contributors.add("Lyrics")
            }
            PredictionSource.MERGED -> {
                contributors.add("Last.fm")
                contributors.add("Local AI")
                if (hasLyrics) contributors.add("Lyrics")
            }
            PredictionSource.ADAPTIVE_OVERRIDE -> contributors.add("Learned")
            PredictionSource.USER_PRESET -> contributors.add("User Preset")
            PredictionSource.CACHE -> contributors.add("Cache")
            PredictionSource.LYRICS -> contributors.add("Lyrics")
            PredictionSource.GEMINI -> contributors.add("Gemini")
            PredictionSource.FALLBACK -> contributors.add("Fallback")
        }

        val primary = when {
            contributors.size >= 2 -> "Merged"
            contributors.size == 1 -> contributors.first()
            else -> "Unknown"
        }

        val detail = if (contributors.size > 1) contributors.joinToString(" + ") else ""

        return SourceDisplay(primary, detail, contributors)
    }
}
