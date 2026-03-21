package com.sonara.app.intelligence.lyrics

/**
 * Madde 14: Lyrics-Aware Insight Engine.
 * Şarkı sözlerinden tone, theme, emotional polarity çıkarır.
 * EQ'ya küçük modifier verir — tek başına EQ belirlemez.
 */
object LyricsInsightEngine {

    data class LyricsInsight(
        val tone: String,         // melancholic, aggressive, romantic, hopeful, dark, dreamy, etc
        val theme: String,        // love, loss, party, introspection, rebellion, etc
        val polarity: Float,      // -1 (dark/sad) .. 0 (neutral) .. +1 (happy/bright)
        val confidence: Float,    // 0..1
        val eqModifier: FloatArray // 10-band küçük modifier
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LyricsInsight) return false
            return tone == other.tone && theme == other.theme && polarity == other.polarity
        }
        override fun hashCode() = tone.hashCode() + theme.hashCode()
    }

    // Keyword → tone mapping
    private val toneKeywords = mapOf(
        "melancholic" to listOf("sad", "cry", "tears", "pain", "hurt", "broken", "lonely", "miss", "sorrow", "grief", "lost", "empty", "ache", "goodbye", "regret"),
        "aggressive" to listOf("kill", "fight", "rage", "anger", "hate", "destroy", "blood", "war", "gun", "violent", "fuck", "bitch", "murder", "beast"),
        "romantic" to listOf("love", "heart", "kiss", "hold", "touch", "forever", "baby", "darling", "beautiful", "eyes", "desire", "passion", "embrace"),
        "hopeful" to listOf("hope", "dream", "believe", "light", "rise", "strong", "future", "fly", "free", "alive", "shine", "miracle", "faith"),
        "dark" to listOf("dark", "shadow", "night", "death", "demon", "evil", "doom", "hell", "black", "grave", "abyss", "wicked", "curse"),
        "dreamy" to listOf("dream", "float", "sky", "cloud", "star", "moon", "ocean", "ethereal", "cosmic", "space", "glow", "whisper"),
        "party" to listOf("party", "dance", "club", "tonight", "drink", "celebrate", "crazy", "wild", "fun", "groove", "move", "bass"),
        "introspective" to listOf("think", "wonder", "mind", "soul", "feel", "inside", "truth", "question", "meaning", "journey", "self", "understand"),
    )

    // Tone → EQ modifier (küçük, Madde 14'e uygun)
    private val toneModifiers = mapOf(
        //                              31    62   125   250   500    1k    2k    4k    8k   16k
        "melancholic" to   floatArrayOf(0.3f, 0.5f, 0.3f, 0.2f, 0.0f, 0.0f,-0.2f,-0.3f,-0.2f, 0.0f),  // warmth
        "aggressive" to    floatArrayOf(0.2f, 0.2f, 0.0f, 0.0f, 0.0f, 0.2f, 0.3f, 0.3f, 0.2f, 0.0f),  // presence
        "romantic" to      floatArrayOf(0.0f, 0.0f, 0.0f, 0.2f, 0.0f, 0.3f, 0.3f, 0.0f,-0.2f,-0.2f),  // vocal clarity
        "hopeful" to       floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.2f, 0.2f, 0.3f, 0.3f, 0.2f, 0.0f),  // brightness
        "dark" to          floatArrayOf(0.3f, 0.5f, 0.3f, 0.0f, 0.0f,-0.2f,-0.3f,-0.3f,-0.2f,-0.2f),  // warmth + dark
        "dreamy" to        floatArrayOf(0.2f, 0.3f, 0.2f, 0.2f, 0.0f, 0.0f,-0.2f,-0.2f, 0.2f, 0.3f),  // softness/space
        "party" to         floatArrayOf(0.3f, 0.3f, 0.2f, 0.0f, 0.0f, 0.0f, 0.2f, 0.2f, 0.2f, 0.0f),  // energy
        "introspective" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.2f, 0.0f, 0.2f, 0.3f, 0.0f,-0.2f,-0.2f),  // vocal intimacy
    )

    fun analyze(lyrics: String): LyricsInsight {
        if (lyrics.isBlank()) return neutral()

        val words = lyrics.lowercase().split(Regex("\\W+"))
        val wordSet = words.toSet()
        val totalWords = words.size.coerceAtLeast(1)

        // Tone scoring
        val scores = mutableMapOf<String, Int>()
        for ((tone, keywords) in toneKeywords) {
            var count = 0
            for (kw in keywords) {
                count += words.count { it == kw || it.contains(kw) }
            }
            if (count > 0) scores[tone] = count
        }

        val bestTone = scores.maxByOrNull { it.value }
        val tone = bestTone?.key ?: "neutral"
        val toneStrength = (bestTone?.value?.toFloat() ?: 0f) / totalWords

        // Theme detection
        val theme = detectTheme(scores)

        // Polarity: positive tones vs negative tones
        val positive = (scores["hopeful"] ?: 0) + (scores["party"] ?: 0) + (scores["romantic"] ?: 0)
        val negative = (scores["melancholic"] ?: 0) + (scores["aggressive"] ?: 0) + (scores["dark"] ?: 0)
        val total = (positive + negative).coerceAtLeast(1)
        val polarity = ((positive - negative).toFloat() / total).coerceIn(-1f, 1f)

        // Confidence: ne kadar keyword bulundu
        val confidence = (toneStrength * 20f).coerceIn(0.1f, 0.85f)

        // EQ modifier (küçük)
        val modifier = toneModifiers[tone] ?: FloatArray(10)
        val scaledModifier = FloatArray(10) { (modifier[it] * confidence.coerceAtMost(0.7f)) }

        return LyricsInsight(tone, theme, polarity, confidence, scaledModifier)
    }

    private fun detectTheme(scores: Map<String, Int>): String {
        return when {
            (scores["romantic"] ?: 0) > 3 -> "love"
            (scores["melancholic"] ?: 0) > 3 -> "loss"
            (scores["party"] ?: 0) > 3 -> "celebration"
            (scores["aggressive"] ?: 0) > 3 -> "rebellion"
            (scores["dark"] ?: 0) > 3 -> "darkness"
            (scores["dreamy"] ?: 0) > 3 -> "escapism"
            (scores["introspective"] ?: 0) > 3 -> "introspection"
            (scores["hopeful"] ?: 0) > 3 -> "hope"
            else -> "general"
        }
    }

    private fun neutral() = LyricsInsight("neutral", "general", 0f, 0f, FloatArray(10))
}
