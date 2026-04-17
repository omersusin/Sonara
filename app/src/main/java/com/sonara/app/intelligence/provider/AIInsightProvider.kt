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

package com.sonara.app.intelligence.provider

/**
 * Common interface for AI insight providers.
 * Implementations: Gemini, OpenRouter, Groq
 */
interface AIInsightProvider {
    val name: String
    val isConfigured: Boolean

    suspend fun getInsight(request: InsightRequest): InsightResult
}

data class InsightRequest(
    val title: String, val artist: String, val genre: String,
    val subGenre: String?, val tags: List<String>,
    val lyricalTone: String?, val energy: Float,
    val confidence: Float, val currentEqBands: FloatArray?,
    val userRequest: String? = null
)

data class InsightResult(
    val summary: String = "",
    val whyThisEq: String = "",
    val listeningFocus: String = "",
    val lyricalTone: String = "",
    val confidenceNote: String = "",
    val success: Boolean = false,
    val provider: String = "",
    val eqAdjustment: FloatArray? = null,
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0
)
