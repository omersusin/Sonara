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

package com.sonara.app.ai.eq

import com.sonara.app.ai.models.SonaraAiResult
import com.sonara.app.ai.models.SonaraMood

class SmartEqGenerator {
    data class EqResult(val bands: FloatArray, val preamp: Float, val isSpectralBased: Boolean) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true; if (other !is EqResult) return false
            return bands.contentEquals(other.bands) && preamp == other.preamp
        }
        override fun hashCode() = bands.contentHashCode()
    }

    fun generate(result: SonaraAiResult, routeType: String = "unknown"): EqResult {
        val spectral = result.spectralProfile
        return if (spectral != null && spectral.size >= 10) fromSpectral(spectral, result.mood, result.energy, routeType)
        else fromGenre(result.primaryGenre.lowercase(), result.mood)
    }

    private fun fromSpectral(bands: FloatArray, mood: SonaraMood, energy: Float, route: String): EqResult {
        val eq = FloatArray(10)
        for (i in 0 until minOf(10, bands.size)) { eq[i] = (0.5f - bands[i]) * 5f }
        if (mood.valence < -0.3f) { eq[0] += 1.5f; eq[1] += 1.0f; eq[8] -= 0.5f; eq[9] -= 1.0f }
        else if (mood.valence > 0.3f) { eq[6] += 0.5f; eq[7] += 1.0f; eq[9] += 0.5f }
        if (mood.arousal > 0.7f) { eq[0] += 1.5f; eq[1] += 1.0f; eq[4] += 0.5f; eq[5] += 0.5f }
        else if (mood.arousal < 0.3f) { eq[3] -= 0.3f; eq[4] -= 0.3f; eq[0] += 0.5f }
        val preamp = when { energy > 0.75f -> -2.0f; energy > 0.55f -> -1.0f; else -> 0f }
        when {
            route.contains("speaker", true) -> { eq[0] -= 2.0f; eq[1] -= 1.0f; eq[4] += 1.0f; eq[5] += 1.0f }
            route.contains("bluetooth", true) -> { eq[0] += 1.0f }
        }
        for (i in eq.indices) eq[i] = eq[i].coerceIn(-8f, 8f)
        return EqResult(eq, preamp.coerceIn(-6f, 3f), true)
    }

    private fun fromGenre(genre: String, mood: SonaraMood): EqResult {
        val eq = when (genre) {
            "rock" -> floatArrayOf(2f,1.5f,0f,-0.5f,0.5f,1f,1.5f,1f,0.5f,0f)
            "pop" -> floatArrayOf(0.5f,1f,1.5f,1f,0f,0f,0.5f,1f,1.5f,1f)
            "electronic","edm","dance" -> floatArrayOf(3f,2.5f,1f,0f,-0.5f,0f,1f,2f,2.5f,2f)
            "hip-hop","rap" -> floatArrayOf(3f,2.5f,2f,0.5f,-0.5f,0f,0.5f,1f,0.5f,0f)
            "jazz" -> floatArrayOf(0.5f,0f,0f,1f,1.5f,1f,0.5f,0f,0f,0.5f)
            "classical" -> floatArrayOf(0f,0f,0f,0.5f,1f,1f,0.5f,0.5f,1f,1.5f)
            "r&b","soul" -> floatArrayOf(2f,1.5f,1f,0.5f,0f,0f,0.5f,1f,0.5f,0f)
            "metal" -> floatArrayOf(3f,2f,0f,-1f,0f,1.5f,2f,1.5f,1f,0.5f)
            "ambient","chill" -> floatArrayOf(0.5f,0.5f,0f,0f,-0.5f,-0.5f,0f,0.5f,1f,1.5f)
            "blues" -> floatArrayOf(1f,0.5f,0f,0.5f,1f,1.5f,1f,0.5f,0f,0f)
            "folk","acoustic","country" -> floatArrayOf(0.5f,0f,0f,0.5f,1.5f,1.5f,1f,0.5f,0.5f,0f)
            "reggae" -> floatArrayOf(2f,1.5f,0f,0f,-0.5f,0f,1f,0.5f,0f,0f)
            "latin" -> floatArrayOf(1.5f,1f,0f,0f,0.5f,1f,1f,0.5f,0.5f,0.5f)
            "indie" -> floatArrayOf(0.5f,0.5f,0f,0f,0.5f,1f,1.5f,1f,0.5f,0.5f)
            "punk" -> floatArrayOf(2f,1f,0f,-0.5f,0.5f,1.5f,2f,1f,0.5f,0f)
            "funk" -> floatArrayOf(2f,1.5f,1f,0f,0.5f,1.5f,1f,0.5f,0f,0f)
            else -> floatArrayOf(0.5f,0.5f,0f,0f,0f,0f,0f,0.5f,0.5f,0.5f)
        }
        return EqResult(eq, 0f, false)
    }
}
