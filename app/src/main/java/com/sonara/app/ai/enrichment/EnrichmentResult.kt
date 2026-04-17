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

package com.sonara.app.ai.enrichment

data class EnrichmentSignal(
    val source: String, val genreHints: Map<String, Float> = emptyMap(),
    val moodValence: Float? = null, val moodArousal: Float? = null,
    val energy: Float? = null, val confidence: Float = 0f,
    val metadata: Map<String, String> = emptyMap(), val isValid: Boolean = true
) {
    companion object { fun empty(source: String) = EnrichmentSignal(source = source, isValid = false) }
}

data class EnrichmentBundle(
    val lastFm: EnrichmentSignal = EnrichmentSignal.empty("lastfm"),
    val lyrics: EnrichmentSignal = EnrichmentSignal.empty("lyrics"),
    val apiAi: EnrichmentSignal = EnrichmentSignal.empty("api_ai"),
    val fetchTimeMs: Long = 0
) {
    val hasAnyData: Boolean get() = lastFm.isValid || lyrics.isValid || apiAi.isValid
    val validCount: Int get() = listOf(lastFm, lyrics, apiAi).count { it.isValid }
    fun toSignalList(): List<EnrichmentSignal> = listOf(lastFm, lyrics, apiAi).filter { it.isValid }
}
