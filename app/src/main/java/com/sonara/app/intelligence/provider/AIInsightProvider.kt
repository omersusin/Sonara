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
    val userRequest: String? = null,
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
