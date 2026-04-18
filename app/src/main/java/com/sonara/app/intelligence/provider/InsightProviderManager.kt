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
    private var huggingFaceProvider: OpenAICompatibleProvider? = null

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

    fun configureHuggingFace(apiKey: String, model: String) {
        huggingFaceProvider = if (apiKey.isNotBlank()) OpenAICompatibleProvider.huggingFace(apiKey, model) else null
    }

    fun setPrimary(name: String) { primaryProviderName = name }

    fun getConfiguredProviders(): List<String> {
        val list = mutableListOf<String>()
        if (geminiProvider?.isConfigured == true) list.add("gemini")
        if (openRouterProvider?.isConfigured == true) list.add("openrouter")
        if (groqProvider?.isConfigured == true) list.add("groq")
        if (huggingFaceProvider?.isConfigured == true) list.add("huggingface")
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
        val fallbacks = listOf(geminiProvider, openRouterProvider, groqProvider, huggingFaceProvider)
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
        "huggingface" -> huggingFaceProvider
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
                request.userRequest,
                request.currentPreamp, request.currentBassBoost,
                request.currentVirtualizer, request.currentLoudness
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
