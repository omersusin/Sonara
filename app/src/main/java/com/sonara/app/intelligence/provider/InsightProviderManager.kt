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

import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.gemini.GeminiInsightEngine

/**
 * Manages multiple AI insight providers with fallback.
 * Primary → Fallback chain.
 */
class InsightProviderManager {

    private var geminiProvider: GeminiAdapter? = null
    private var openRouterProvider: OpenAICompatibleProvider? = null
    private var groqProvider: OpenAICompatibleProvider? = null

    var primaryProviderName: String = "gemini"
        private set

    fun configureGemini(engine: GeminiInsightEngine) {
        geminiProvider = GeminiAdapter(engine)
    }

    fun configureOpenRouter(apiKey: String, model: String) {
        openRouterProvider = if (apiKey.isNotBlank()) OpenAICompatibleProvider.openRouter(apiKey, model) else null
    }

    fun configureGroq(apiKey: String, model: String) {
        groqProvider = if (apiKey.isNotBlank()) OpenAICompatibleProvider.groq(apiKey, model) else null
    }

    fun setPrimary(name: String) { primaryProviderName = name }

    fun getConfiguredProviders(): List<String> {
        val list = mutableListOf<String>()
        if (geminiProvider?.isConfigured == true) list.add("gemini")
        if (openRouterProvider?.isConfigured == true) list.add("openrouter")
        if (groqProvider?.isConfigured == true) list.add("groq")
        return list
    }

    suspend fun getInsight(request: InsightRequest): InsightResult {
        // Try primary first
        val primary = getProvider(primaryProviderName)
        if (primary != null && primary.isConfigured) {
            val result = primary.getInsight(request)
            if (result.success) return result
            SonaraLogger.w("ProviderManager", "${primary.name} failed, trying fallback")
        }

        // Fallback: try others
        val fallbacks = listOf(geminiProvider, openRouterProvider, groqProvider)
            .filterNotNull()
            .filter { it.isConfigured && it.name.lowercase() != primaryProviderName }

        for (fb in fallbacks) {
            val result = fb.getInsight(request)
            if (result.success) {
                SonaraLogger.i("ProviderManager", "Fallback to ${fb.name} succeeded")
                return result
            }
        }

        return InsightResult(provider = "none")
    }

    private fun getProvider(name: String): AIInsightProvider? = when (name.lowercase()) {
        "gemini" -> geminiProvider
        "openrouter" -> openRouterProvider
        "groq" -> groqProvider
        else -> null
    }

    /** Adapter to make GeminiInsightEngine work as AIInsightProvider */
    private class GeminiAdapter(private val engine: GeminiInsightEngine) : AIInsightProvider {
        override val name = "Gemini"
        override val isConfigured get() = engine.isEnabled()

        override suspend fun getInsight(request: InsightRequest): InsightResult {
            val r = engine.getInsight(
                request.title, request.artist, request.genre, request.subGenre,
                request.tags, request.lyricalTone, request.energy,
                request.confidence, request.currentEqBands,
                request.userRequest
            )
            return InsightResult(
                summary = r.summary, whyThisEq = r.whyThisEq,
                listeningFocus = r.listeningFocus, lyricalTone = r.lyricalTone,
                confidenceNote = r.confidenceNote, success = r.success,
                provider = "Gemini",
                eqAdjustment = r.eqAdjustment,
                preamp = r.preamp,
                bassBoost = r.bassBoost,
                virtualizer = r.virtualizer,
                loudness = r.loudness
            )
        }
    }
}
