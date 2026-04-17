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

package com.sonara.app.ai.explanation

import com.sonara.app.ai.eq.SmartEqGenerator
import com.sonara.app.ai.models.*

object ExplanationBuilder {
    fun build(result: SonaraAiResult, eq: SmartEqGenerator.EqResult?, title: String = "", artist: String = ""): SonaraExplanation {
        val genre = result.primaryGenre
        val moodDesc = result.mood.description.lowercase()
        val energyDesc = result.energyLabel.lowercase()

        val sourceHonesty = when (result.source) {
            SonaraSource.AUDIO_LEARNED -> "Based on audio analysis (learned from your feedback)"
            SonaraSource.AUDIO_PROTOTYPE -> "Based on audio analysis"
            SonaraSource.AUDIO_RULES -> "Based on audio characteristics"
            SonaraSource.LASTFM -> "Based on community tags"
            SonaraSource.METADATA -> "Based on track information"
            SonaraSource.CACHED -> "Previously analyzed"
            else -> "Limited analysis"
        }

        val summary = buildString {
            if (title.isNotBlank()) { append("\"$title\""); if (artist.isNotBlank()) append(" by $artist"); append(" — ") }
            append("$genre with a $moodDesc feel and $energyDesc.")
        }

        val eqReason = if (eq != null) {
            val parts = mutableListOf<String>()
            if (eq.isSpectralBased) parts.add("EQ shaped from actual audio spectrum") else parts.add("EQ based on genre profile")
            val avgBass = (eq.bands[0] + eq.bands[1]) / 2; val avgMid = (eq.bands[4] + eq.bands[5]) / 2; val avgTreble = (eq.bands[8] + eq.bands[9]) / 2
            if (avgBass > 1.5f) parts.add("Enhanced bass for warmth") else if (avgBass < -1f) parts.add("Reduced bass for clarity")
            if (avgMid > 1f) parts.add("Boosted mids for presence")
            if (avgTreble > 1f) parts.add("Lifted highs for detail") else if (avgTreble < -1f) parts.add("Tamed treble for smoothness")
            if (eq.preamp < -1f) parts.add("Preamp lowered to prevent distortion")
            parts.joinToString(". ") + "."
        } else "No EQ adjustments applied."

        return SonaraExplanation(summary, eqReason, sourceHonesty)
    }
}
