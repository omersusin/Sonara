package com.sonara.app.intelligence.local

import com.sonara.app.data.models.TrackInfo

object AiEqSuggestionEngine {

    data class EqSuggestion(
        val bands: FloatArray = FloatArray(10),
        val bassBoost: Int = 0,
        val virtualizer: Int = 0,
        val preamp: Float = 0f,
        val reasoning: String = ""
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EqSuggestion) return false
            return bands.contentEquals(other.bands) &&
                bassBoost == other.bassBoost &&
                virtualizer == other.virtualizer &&
                preamp == other.preamp &&
                reasoning == other.reasoning
        }
        override fun hashCode(): Int {
            var result = bands.contentHashCode()
            result = 31 * result + bassBoost
            result = 31 * result + virtualizer
            result = 31 * result + preamp.hashCode()
            result = 31 * result + reasoning.hashCode()
            return result
        }
    }

    fun suggest(trackInfo: TrackInfo): EqSuggestion {
        val genreBands = genreEqMap[trackInfo.genre] ?: FloatArray(10)
        val moodMod = moodModifier(trackInfo.mood)
        val energyMod = energyModifier(trackInfo.energy)

        val bands = FloatArray(10) { i ->
            val base = genreBands.getOrElse(i) { 0f }
            val mood = moodMod.getOrElse(i) { 0f }
            val energy = energyMod.getOrElse(i) { 0f }
            (base + mood * 0.5f + energy * 0.3f).coerceIn(-12f, 12f)
        }

        val bassBoost = when (trackInfo.genre) {
            "hip-hop" -> 400
            "electronic" -> 350
            "r&b" -> 300
            "metal" -> 250
            "pop" -> 150
            else -> 0
        }

        val virtualizer = when {
            trackInfo.mood == "chill" -> 300
            trackInfo.mood == "romantic" -> 250
            trackInfo.genre == "electronic" -> 200
            trackInfo.genre == "jazz" -> 200
            else -> 0
        }

        val preamp = calculatePreamp(bands)

        val reasoning = buildReasoning(trackInfo)

        return EqSuggestion(bands, bassBoost, virtualizer, preamp, reasoning)
    }

    private val genreEqMap = mapOf(
        "rock" to floatArrayOf(3.5f, 2.5f, 1f, 0f, -1f, 0f, 2f, 3f, 3.5f, 3f),
        "pop" to floatArrayOf(-1f, 0f, 2f, 3f, 3.5f, 3f, 1.5f, 0f, -0.5f, -1f),
        "hip-hop" to floatArrayOf(5f, 4.5f, 3f, 1f, 0f, -0.5f, 1f, 0.5f, 2f, 2.5f),
        "electronic" to floatArrayOf(4f, 3.5f, 2f, 0f, -1f, 0f, 1f, 3f, 4f, 4.5f),
        "r&b" to floatArrayOf(3f, 3.5f, 2.5f, 1f, -0.5f, 0f, 1f, 1.5f, 2f, 1.5f),
        "jazz" to floatArrayOf(2f, 1.5f, 0f, 1f, -1f, -1f, 0f, 1f, 2.5f, 3f),
        "classical" to floatArrayOf(2.5f, 2f, 0.5f, 0f, -0.5f, -0.5f, 0f, 1.5f, 2.5f, 3.5f),
        "metal" to floatArrayOf(4f, 3f, 0f, -1f, -2f, -1f, 1f, 3.5f, 4.5f, 4f),
        "folk" to floatArrayOf(1.5f, 1f, 0f, 1f, 2f, 2f, 1.5f, 1.5f, 2f, 1.5f),
        "country" to floatArrayOf(2f, 1.5f, 0.5f, 1f, 2f, 2.5f, 2f, 1.5f, 2f, 1.5f),
        "blues" to floatArrayOf(2.5f, 2f, 1f, 0.5f, -0.5f, 0f, 1f, 1.5f, 2f, 2f),
        "reggae" to floatArrayOf(3f, 3.5f, 2f, 0f, -1.5f, -1f, 0.5f, 1.5f, 2f, 1f),
        "latin" to floatArrayOf(2f, 2.5f, 2f, 1f, 0f, 0.5f, 1f, 1.5f, 2f, 2f),
        "other" to floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    )

    private fun moodModifier(mood: String): FloatArray = when (mood) {
        "energetic" -> floatArrayOf(1f, 0.5f, 0f, -0.5f, -1f, 0f, 0.5f, 1f, 1.5f, 1f)
        "chill" -> floatArrayOf(-0.5f, 0f, 0.5f, 0.5f, 0f, -0.5f, -0.5f, 0f, -0.5f, -1f)
        "melancholic" -> floatArrayOf(0.5f, 1f, 0.5f, 0f, 0.5f, 1f, 0.5f, 0f, -0.5f, -1f)
        "happy" -> floatArrayOf(0f, 0f, 0.5f, 1f, 1f, 0.5f, 0f, 0.5f, 1f, 0.5f)
        "intense" -> floatArrayOf(1.5f, 1f, 0f, -1f, -1.5f, 0f, 1f, 1.5f, 2f, 1.5f)
        "romantic" -> floatArrayOf(0f, 0.5f, 0.5f, 0.5f, 1f, 1f, 0.5f, 0f, -0.5f, -0.5f)
        else -> FloatArray(10)
    }

    private fun energyModifier(energy: Float): FloatArray {
        val factor = (energy - 0.5f) * 2f
        return floatArrayOf(
            0.5f * factor, 0.3f * factor, 0f, -0.2f * factor, -0.3f * factor,
            0f, 0.2f * factor, 0.3f * factor, 0.5f * factor, 0.4f * factor
        )
    }

    private fun calculatePreamp(bands: FloatArray): Float {
        val maxGain = bands.maxOrNull() ?: 0f
        return if (maxGain > 6f) -(maxGain - 6f) * 0.5f else 0f
    }

    private fun buildReasoning(info: TrackInfo): String {
        val parts = mutableListOf<String>()
        parts.add("Genre: ${info.genre.replaceFirstChar { it.uppercase() }}")
        parts.add("Mood: ${info.mood.replaceFirstChar { it.uppercase() }}")
        parts.add("Energy: ${(info.energy * 100).toInt()}%")

        when (info.genre) {
            "hip-hop" -> parts.add("Enhanced sub-bass and low-end for beats")
            "rock" -> parts.add("Boosted lows and highs for guitar presence")
            "classical" -> parts.add("Gentle boost to preserve dynamic range")
            "jazz" -> parts.add("Warm mids with airy highs for instrument separation")
            "electronic" -> parts.add("Sub-bass emphasis with crisp highs")
            "pop" -> parts.add("Mid-focused for vocal clarity")
            "metal" -> parts.add("V-curve for aggressive sound")
            "r&b" -> parts.add("Warm bass with smooth vocal range")
            "folk" -> parts.add("Natural tone with gentle presence boost")
        }

        when (info.mood) {
            "energetic" -> parts.add("Extra punch and brightness")
            "chill" -> parts.add("Reduced highs for relaxation")
            "melancholic" -> parts.add("Warm tone with emotional mids")
        }

        if (info.source.contains("lastfm")) parts.add("Source: Last.fm (high confidence)")
        else if (info.source.contains("local")) parts.add("Source: Local AI (estimated)")

        return parts.joinToString(" · ")
    }

    fun empty() = EqSuggestion()
}
