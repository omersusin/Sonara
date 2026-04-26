package com.sonara.app.ai.enrichment


class LyricsEnricher {
    companion object {
        private val POSITIVE = mapOf("love" to 0.5f, "happy" to 0.7f, "joy" to 0.6f, "smile" to 0.4f, "dance" to 0.5f, "sunshine" to 0.6f, "beautiful" to 0.4f, "dream" to 0.3f, "hope" to 0.4f, "together" to 0.3f)
        private val NEGATIVE = mapOf("hate" to -0.6f, "sad" to -0.5f, "cry" to -0.5f, "pain" to -0.5f, "die" to -0.6f, "death" to -0.7f, "alone" to -0.4f, "lonely" to -0.5f, "broken" to -0.5f, "dark" to -0.4f, "lost" to -0.3f, "tears" to -0.4f, "hurt" to -0.4f)
        private val HIGH_E = mapOf("fight" to 0.7f, "run" to 0.6f, "scream" to 0.7f, "fire" to 0.6f, "wild" to 0.6f, "power" to 0.6f, "rage" to 0.7f)
        private val LOW_E = mapOf("sleep" to -0.5f, "quiet" to -0.4f, "gentle" to -0.3f, "whisper" to -0.5f, "soft" to -0.3f, "slow" to -0.4f, "peace" to -0.4f, "calm" to -0.5f)
    }

    fun analyze(lyrics: String?): EnrichmentSignal {
        if (lyrics.isNullOrBlank() || lyrics.length < 50) return EnrichmentSignal.empty("lyrics")
        val words = lyrics.lowercase().replace(Regex("[^a-z\\s]"), " ").split(Regex("\\s+")).filter { it.length > 2 }
        if (words.size < 20) return EnrichmentSignal.empty("lyrics")
        val tw = words.size.toFloat()
        var ps = 0f; var ns = 0f; var he = 0f; var le = 0f
        for (w in words) { POSITIVE[w]?.let { ps += it }; NEGATIVE[w]?.let { ns += it }; HIGH_E[w]?.let { he += it }; LOW_E[w]?.let { le += it } }
        val valence = ((ps + ns) / tw * 50f).coerceIn(-1f, 1f)
        val arousal = (0.5f + (he + le) / tw * 30f).coerceIn(0f, 1f)
        val mc = words.count { w -> POSITIVE.containsKey(w) || NEGATIVE.containsKey(w) || HIGH_E.containsKey(w) || LOW_E.containsKey(w) }
        val conf = when { mc > 15 -> 0.60f; mc > 8 -> 0.45f; mc > 3 -> 0.30f; else -> 0.15f }
        return EnrichmentSignal(source = "lyrics", moodValence = valence, moodArousal = arousal, confidence = conf, isValid = mc >= 3)
    }
}
