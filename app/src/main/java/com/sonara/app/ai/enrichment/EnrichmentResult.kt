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
