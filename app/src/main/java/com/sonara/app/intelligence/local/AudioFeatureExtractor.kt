package com.sonara.app.intelligence.local

data class AudioFeatures(
    val estimatedGenre: String = "other",
    val estimatedMood: String = "neutral",
    val estimatedEnergy: Float = 0.5f,
    val bassNeed: Float = 0.5f,
    val trebleNeed: Float = 0.5f,
    val vocalPresence: Float = 0.5f,
    val brightness: Float = 0.5f,
    val confidence: Float = 0.3f
)

class AudioFeatureExtractor {

    private val genreKeywords = mapOf(
        "hip-hop" to listOf("feat", "ft.", "lil ", "dj ", "mc ", "young ", "big ", "yung"),
        "electronic" to listOf("remix", "mix", "dj ", "bass", "beat", "drop", "synth"),
        "classical" to listOf("symphony", "concerto", "opus", "sonata", "orchestra", "quartet", "prelude", "nocturne", "etude"),
        "jazz" to listOf("swing", "bop", "blues", "trio", "quartet", "standard"),
        "metal" to listOf("death", "blood", "dark", "demon", "hell", "war", "chaos", "rage"),
        "rock" to listOf("rock", "guitar", "band", "live", "electric"),
        "folk" to listOf("acoustic", "folk", "unplugged", "traditional"),
        "r&b" to listOf("love", "baby", "heart", "soul", "groove")
    )

    private val moodKeywords = mapOf(
        "melancholic" to listOf("sad", "cry", "tears", "alone", "lonely", "pain", "rain", "broken", "lost", "miss"),
        "happy" to listOf("happy", "joy", "sun", "smile", "dance", "party", "fun", "good", "yeah"),
        "energetic" to listOf("fire", "power", "fight", "run", "wild", "crazy", "fast", "rage", "beast"),
        "romantic" to listOf("love", "kiss", "heart", "forever", "beautiful", "dream", "angel"),
        "chill" to listOf("night", "sleep", "calm", "peace", "quiet", "soft", "gentle", "slow")
    )

    fun extract(title: String, artist: String): AudioFeatures {
        val combined = "$title $artist".lowercase()

        val genre = detectGenre(combined)
        val mood = detectMood(combined)
        val energy = estimateEnergy(genre, mood)

        return AudioFeatures(
            estimatedGenre = genre,
            estimatedMood = mood,
            estimatedEnergy = energy,
            bassNeed = bassNeedFor(genre, mood),
            trebleNeed = trebleNeedFor(genre, mood),
            vocalPresence = vocalPresenceFor(genre),
            brightness = brightnessFor(genre, mood),
            confidence = 0.3f
        )
    }

    private fun detectGenre(text: String): String {
        var best = "other"
        var bestScore = 0
        genreKeywords.forEach { (genre, keywords) ->
            val score = keywords.count { text.contains(it) }
            if (score > bestScore) { bestScore = score; best = genre }
        }
        return best
    }

    private fun detectMood(text: String): String {
        var best = "neutral"
        var bestScore = 0
        moodKeywords.forEach { (mood, keywords) ->
            val score = keywords.count { text.contains(it) }
            if (score > bestScore) { bestScore = score; best = mood }
        }
        return best
    }

    private fun estimateEnergy(genre: String, mood: String): Float {
        val genreEnergy = mapOf("metal" to 0.9f, "electronic" to 0.8f, "hip-hop" to 0.7f, "rock" to 0.7f, "pop" to 0.6f, "r&b" to 0.5f, "jazz" to 0.4f, "folk" to 0.35f, "classical" to 0.3f)
        val moodEnergy = mapOf("energetic" to 0.2f, "happy" to 0.1f, "romantic" to -0.05f, "chill" to -0.15f, "melancholic" to -0.1f)
        val base = genreEnergy[genre] ?: 0.5f
        val mod = moodEnergy[mood] ?: 0f
        return (base + mod).coerceIn(0.1f, 1f)
    }

    private fun bassNeedFor(genre: String, mood: String): Float = when (genre) {
        "hip-hop" -> 0.85f; "electronic" -> 0.8f; "r&b" -> 0.7f; "metal" -> 0.65f; "pop" -> 0.55f
        "rock" -> 0.6f; "jazz" -> 0.4f; "classical" -> 0.3f; "folk" -> 0.3f; else -> 0.5f
    }

    private fun trebleNeedFor(genre: String, mood: String): Float = when (genre) {
        "classical" -> 0.75f; "jazz" -> 0.7f; "folk" -> 0.65f; "pop" -> 0.6f; "rock" -> 0.6f
        "electronic" -> 0.55f; "metal" -> 0.5f; "hip-hop" -> 0.4f; "r&b" -> 0.5f; else -> 0.5f
    }

    private fun vocalPresenceFor(genre: String): Float = when (genre) {
        "pop" -> 0.8f; "r&b" -> 0.8f; "folk" -> 0.75f; "jazz" -> 0.6f; "rock" -> 0.6f
        "hip-hop" -> 0.7f; "classical" -> 0.2f; "electronic" -> 0.3f; "metal" -> 0.5f; else -> 0.5f
    }

    private fun brightnessFor(genre: String, mood: String): Float {
        val base = when (genre) {
            "pop" -> 0.7f; "classical" -> 0.65f; "jazz" -> 0.6f; "folk" -> 0.6f
            "electronic" -> 0.6f; "rock" -> 0.5f; "hip-hop" -> 0.45f; "metal" -> 0.4f; "r&b" -> 0.55f; else -> 0.5f
        }
        val moodMod = if (mood == "melancholic") -0.1f else if (mood == "happy") 0.1f else 0f
        return (base + moodMod).coerceIn(0.1f, 1f)
    }
}
