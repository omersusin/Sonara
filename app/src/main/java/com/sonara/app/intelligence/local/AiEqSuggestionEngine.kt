package com.sonara.app.intelligence.local

import com.sonara.app.data.models.TrackInfo

object AiEqSuggestionEngine {
    data class EqSuggestion(val bands: FloatArray = FloatArray(10), val bassBoost: Int = 0, val virtualizer: Int = 0, val preamp: Float = 0f, val reasoning: String = "") {
        override fun equals(other: Any?): Boolean { if (this === other) return true; if (other !is EqSuggestion) return false; return bands.contentEquals(other.bands) }
        override fun hashCode() = bands.contentHashCode()
    }

    fun suggest(trackInfo: TrackInfo): EqSuggestion {
        val genreBands = genreEqMap[trackInfo.genre] ?: FloatArray(10)
        val moodMod = moodModifier(trackInfo.mood)
        val energyMod = energyModifier(trackInfo.energy)

        val cs = when { trackInfo.confidence >= 0.7f -> 1.0f; trackInfo.confidence >= 0.5f -> 0.8f; trackInfo.confidence >= 0.3f -> 0.6f; else -> 0.3f }

        val bands = FloatArray(10) { i ->
            val base = genreBands.getOrElse(i) { 0f }; val mood = moodMod.getOrElse(i) { 0f }; val energy = energyMod.getOrElse(i) { 0f }
            ((base + mood * 0.5f + energy * 0.3f) * cs).coerceIn(-12f, 12f)
        }

        val bassBoost = ((when (trackInfo.genre.lowercase()) {
            "hip-hop" -> 400; "electronic" -> 350; "latin" -> 300; "reggae" -> 350; "r&b" -> 300
            "metal" -> 250; "pop" -> 150; "rock" -> 100; "blues" -> 150; else -> 0
        }) * cs).toInt()

        val virtualizer = ((when {
            trackInfo.mood == "chill" -> 300; trackInfo.mood == "romantic" -> 250
            trackInfo.genre == "electronic" -> 200; trackInfo.genre == "jazz" -> 200
            trackInfo.genre == "classical" -> 150; else -> 0
        }) * cs).toInt()

        return EqSuggestion(bands, bassBoost, virtualizer, calculatePreamp(bands), buildReasoning(trackInfo))
    }

    private val genreEqMap = mapOf(
        "rock" to floatArrayOf(3.5f,2.5f,1f,0f,-1f,0f,2f,3f,3.5f,3f),
        "pop" to floatArrayOf(-1f,0f,2f,3f,3.5f,3f,1.5f,0f,-0.5f,-1f),
        "hip-hop" to floatArrayOf(5f,4.5f,3f,1f,0f,-0.5f,1f,0.5f,2f,2.5f),
        "electronic" to floatArrayOf(4f,3.5f,2f,0f,-1f,0f,1f,3f,4f,4.5f),
        "r&b" to floatArrayOf(3f,3.5f,2.5f,1f,-0.5f,0f,1f,1.5f,2f,1.5f),
        "jazz" to floatArrayOf(2f,1.5f,0f,1f,-1f,-1f,0f,1f,2.5f,3f),
        "classical" to floatArrayOf(2.5f,2f,0.5f,0f,-0.5f,-0.5f,0f,1.5f,2.5f,3.5f),
        "metal" to floatArrayOf(4f,3f,0f,-1f,-2f,-1f,1f,3.5f,4.5f,4f),
        "folk" to floatArrayOf(1.5f,1f,0f,1f,2f,2f,1.5f,1.5f,2f,1.5f),
        "country" to floatArrayOf(2f,1.5f,0.5f,1f,2f,2.5f,2f,1.5f,2f,1.5f),
        "blues" to floatArrayOf(2.5f,2f,1f,0.5f,-0.5f,0f,1f,1.5f,2f,2f),
        "reggae" to floatArrayOf(3f,3.5f,2f,0f,-1.5f,-1f,0.5f,1.5f,2f,1f),
        "latin" to floatArrayOf(2f,2.5f,2f,1f,0f,0.5f,1f,1.5f,2f,2f),
        "other" to floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f)
    )

    private fun moodModifier(m: String) = when (m) {
        "energetic" -> floatArrayOf(1f,0.5f,0f,-0.5f,-1f,0f,0.5f,1f,1.5f,1f)
        "chill" -> floatArrayOf(-0.5f,0f,0.5f,0.5f,0f,-0.5f,-0.5f,0f,-0.5f,-1f)
        "melancholic" -> floatArrayOf(0.5f,1f,0.5f,0f,0.5f,1f,0.5f,0f,-0.5f,-1f)
        "happy" -> floatArrayOf(0f,0f,0.5f,1f,1f,0.5f,0f,0.5f,1f,0.5f)
        "intense" -> floatArrayOf(1.5f,1f,0f,-1f,-1.5f,0f,1f,1.5f,2f,1.5f)
        "romantic" -> floatArrayOf(0f,0.5f,0.5f,0.5f,1f,1f,0.5f,0f,-0.5f,-0.5f)
        else -> FloatArray(10)
    }

    private fun energyModifier(e: Float): FloatArray { val f = (e - 0.5f) * 2f; return floatArrayOf(0.5f*f,0.3f*f,0f,-0.2f*f,-0.3f*f,0f,0.2f*f,0.3f*f,0.5f*f,0.4f*f) }
    private fun calculatePreamp(b: FloatArray): Float { val m = b.max(); return if (m > 6f) -(m - 6f) * 0.5f else 0f }

    private fun buildReasoning(i: TrackInfo): String {
        val p = mutableListOf("Genre: ${i.genre.replaceFirstChar { it.uppercase() }}", "Mood: ${i.mood.replaceFirstChar { it.uppercase() }}", "Energy: ${(i.energy * 100).toInt()}%")
        when (i.genre) {
            "hip-hop" -> p.add("Sub-bass emphasis for beats"); "rock" -> p.add("Boosted lows and highs for guitar")
            "classical" -> p.add("Gentle boost for dynamic range"); "jazz" -> p.add("Warm mids with airy highs")
            "electronic" -> p.add("Sub-bass emphasis with crisp highs"); "pop" -> p.add("Mid-focused for vocal clarity")
            "metal" -> p.add("V-curve for aggressive sound"); "r&b" -> p.add("Warm bass with smooth vocals")
            "folk" -> p.add("Natural tone with presence"); "country" -> p.add("Vocal clarity with warmth")
            "blues" -> p.add("Warm mids for guitar tone"); "reggae" -> p.add("Heavy bass with clean mids")
            "latin" -> p.add("Bass punch with rhythmic clarity")
        }
        when (i.mood) { "energetic" -> p.add("Extra punch and brightness"); "chill" -> p.add("Reduced highs for relaxation"); "melancholic" -> p.add("Warm tone with emotional mids") }
        if (i.source.contains("lastfm")) p.add("Source: Last.fm (high confidence)") else if (i.source.contains("local")) p.add("Source: Local AI (estimated)")
        return p.joinToString(" · ")
    }

    fun empty() = EqSuggestion()
}
