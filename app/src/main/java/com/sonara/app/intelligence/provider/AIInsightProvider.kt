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
    val userRequest: String? = null,
    val currentPreamp: Float = 0f,
    val currentBassBoost: Int = 0,
    val currentVirtualizer: Int = 0,
    val currentLoudness: Int = 0
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
    val preamp: Float? = null,
    val bassBoost: Int? = null,
    val virtualizer: Int? = null,
    val loudness: Int? = null
)
