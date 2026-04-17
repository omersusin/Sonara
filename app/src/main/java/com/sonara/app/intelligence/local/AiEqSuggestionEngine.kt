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

import com.sonara.app.data.models.TrackInfo

object AiEqSuggestionEngine {
    data class EqSuggestion(val bands: FloatArray = FloatArray(10), val bassBoost: Int = 0, val virtualizer: Int = 0, val preamp: Float = 0f, val reasoning: String = "") {
        override fun equals(other: Any?): Boolean { if (this === other) return true; if (other !is EqSuggestion) return false; return bands.contentEquals(other.bands) }
        override fun hashCode() = bands.contentHashCode()
    }

    fun suggest(trackInfo: TrackInfo): EqSuggestion {
        val genreBands = genreEqMap[trackInfo.genre] ?: genreEqMap["other"]!!
        val moodMod = moodModifier(trackInfo.mood)
        val energyMod = energyModifier(trackInfo.energy)

        // CHANGED: Minimum 0.6 scaling — EQ always audible
        val cs = when {
            trackInfo.confidence >= 0.7f -> 1.0f
            trackInfo.confidence >= 0.4f -> 0.85f
            trackInfo.confidence >= 0.2f -> 0.7f
            else -> 0.6f  // Was 0.3 — now 0.6 minimum
        }

        val bands = FloatArray(10) { i ->
            val base = genreBands.getOrElse(i) { 0f }
            val mood = moodMod.getOrElse(i) { 0f }
            val energy = energyMod.getOrElse(i) { 0f }
            ((base + mood * 0.5f + energy * 0.3f) * cs).coerceIn(-12f, 12f)
        }

        val bassBoost = (when (trackInfo.genre.lowercase()) {
            "hip-hop" -> 500; "electronic" -> 450; "latin" -> 400; "reggae" -> 420; "r&b" -> 350
            "metal" -> 300; "rock" -> 200; "pop" -> 200; "blues" -> 200; else -> 100
        }.toFloat() * cs).toInt()

        val virtualizer = (when {
            trackInfo.mood == "chill" -> 350; trackInfo.mood == "romantic" -> 300
            trackInfo.genre == "electronic" -> 250; trackInfo.genre == "jazz" -> 250
            trackInfo.genre == "classical" -> 200; else -> 150
        }.toFloat() * cs).toInt()

        val maxBand = bands.maxOrNull() ?: 0f
        val preamp = if (maxBand > 6f) -(maxBand - 6f) * 0.5f else 0f

        return EqSuggestion(bands, bassBoost, virtualizer, preamp, buildReasoning(trackInfo))
    }

    private val genreEqMap = mapOf(
        "rock" to floatArrayOf(4f, 3f, 1f, 0f, -1f, 0f, 2.5f, 3.5f, 4f, 3.5f),
        "pop" to floatArrayOf(-1f, 0f, 2.5f, 3.5f, 4f, 3.5f, 2f, 0f, -0.5f, -1f),
        "hip-hop" to floatArrayOf(6f, 5.5f, 3.5f, 1f, 0f, -0.5f, 1f, 0.5f, 2.5f, 3f),
        "electronic" to floatArrayOf(5f, 4f, 2.5f, 0f, -1f, 0f, 1.5f, 3.5f, 4.5f, 5f),
        "r&b" to floatArrayOf(3.5f, 4f, 3f, 1f, -0.5f, 0f, 1f, 2f, 2.5f, 2f),
        "jazz" to floatArrayOf(2.5f, 2f, 0f, 1f, -1f, -1f, 0f, 1.5f, 3f, 3.5f),
        "classical" to floatArrayOf(3f, 2.5f, 0.5f, 0f, -0.5f, -0.5f, 0f, 2f, 3f, 4f),
        "metal" to floatArrayOf(5f, 3.5f, 0f, -1f, -2f, -1f, 1.5f, 4f, 5f, 4.5f),
        "folk" to floatArrayOf(2f, 1.5f, 0f, 1f, 2.5f, 2.5f, 2f, 2f, 2.5f, 2f),
        "country" to floatArrayOf(2.5f, 2f, 0.5f, 1.5f, 2.5f, 3f, 2.5f, 2f, 2.5f, 2f),
        "blues" to floatArrayOf(3f, 2.5f, 1f, 0.5f, -0.5f, 0f, 1.5f, 2f, 2.5f, 2.5f),
        "reggae" to floatArrayOf(3.5f, 4f, 2.5f, 0f, -1.5f, -1f, 0.5f, 2f, 2.5f, 1.5f),
        "latin" to floatArrayOf(2.5f, 3f, 2.5f, 1f, 0f, 0.5f, 1.5f, 2f, 2.5f, 2.5f),
        "soul" to floatArrayOf(3f, 3.5f, 2f, 1f, 0f, 0.5f, 1f, 1.5f, 2f, 1.5f),
        "other" to floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    )

    private fun moodModifier(m: String) = when (m) {
        "energetic" -> floatArrayOf(1.5f, 0.5f, 0f, -0.5f, -1f, 0f, 0.5f, 1f, 1.5f, 1f)
        "chill" -> floatArrayOf(-0.5f, 0f, 0.5f, 0.5f, 0f, -0.5f, -0.5f, 0f, -0.5f, -1f)
        "melancholic" -> floatArrayOf(0.5f, 1f, 0.5f, 0f, 0.5f, 1f, 0.5f, 0f, -0.5f, -1f)
        "happy" -> floatArrayOf(0f, 0f, 0.5f, 1f, 1f, 0.5f, 0f, 0.5f, 1f, 0.5f)
        "intense" -> floatArrayOf(1.5f, 1f, 0f, -1f, -1.5f, 0f, 1f, 1.5f, 2f, 1.5f)
        "romantic" -> floatArrayOf(0f, 0.5f, 0.5f, 0.5f, 1f, 1f, 0.5f, 0f, -0.5f, -0.5f)
        else -> FloatArray(10)
    }

    private fun energyModifier(e: Float): FloatArray { val f = (e - 0.5f) * 2f; return floatArrayOf(0.5f*f,0.3f*f,0f,-0.2f*f,-0.3f*f,0f,0.2f*f,0.3f*f,0.5f*f,0.4f*f) }

    private fun buildReasoning(i: TrackInfo): String {
        val p = mutableListOf("${i.genre.replaceFirstChar { it.uppercase() }}", "${i.mood.replaceFirstChar { it.uppercase() }}", "${(i.energy * 100).toInt()}% energy")
        when (i.genre) { "hip-hop" -> p.add("Bass emphasis"); "rock" -> p.add("Guitar presence"); "classical" -> p.add("Dynamic range"); "jazz" -> p.add("Warm mids"); "electronic" -> p.add("Sub-bass + crisp highs"); "pop" -> p.add("Vocal clarity"); "metal" -> p.add("Aggressive V-curve"); "r&b" -> p.add("Warm bass + smooth vocals") }
        if (i.source.contains("lastfm")) p.add("via Last.fm") else if (i.source.contains("local") || i.source.contains("cache")) p.add("via AI")
        return p.joinToString(" · ")
    }

    fun empty() = EqSuggestion()
}
