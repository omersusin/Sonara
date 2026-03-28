package com.sonara.app.intelligence.pipeline

data class SonaraTrackInfo(
    val title: String, val artist: String, val album: String,
    val durationMs: Long, val packageName: String
) {
    val cacheKey: String get() = "${artist.trim().lowercase()}::${title.trim().lowercase()}"
}

data class SonaraPrediction(
    val genre: Genre, val subGenre: String? = null, val mood: Mood,
    val energy: Float, val confidence: Float, val source: PredictionSource,
    val mediaType: MediaType, val reasoning: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class FinalEqProfile(
    val bands: FloatArray, val preamp: Float, val bassBoost: Int,
    val virtualizer: Int, val loudness: Int, val prediction: SonaraPrediction
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true; if (other !is FinalEqProfile) return false
        return bands.contentEquals(other.bands) && preamp == other.preamp && bassBoost == other.bassBoost
    }
    override fun hashCode() = bands.contentHashCode()
    companion object {
        fun neutral() = FinalEqProfile(FloatArray(10), 0f, 0, 0, 0,
            SonaraPrediction(Genre.UNKNOWN, null, Mood.NEUTRAL, 0.5f, 0f, PredictionSource.FALLBACK, MediaType.UNKNOWN))
    }
}

enum class Genre(val displayName: String) {
    POP("Pop"), ROCK("Rock"), METAL("Metal"), HIP_HOP("Hip-Hop"), RNB("R&B"),
    ELECTRONIC("Electronic"), DANCE("Dance"), JAZZ("Jazz"), BLUES("Blues"),
    CLASSICAL("Classical"), COUNTRY("Country"), FOLK("Folk"), REGGAE("Reggae"),
    LATIN("Latin"), AMBIENT("Ambient"), SOUL("Soul"), FUNK("Funk"), PUNK("Punk"),
    INDIE("Indie"), ALTERNATIVE("Alternative"), WORLD("World"),
    VIDEO("Video"),
    PODCAST("Podcast"), AUDIOBOOK("Audiobook"), SPEECH("Speech"), UNKNOWN("Unknown");
    companion object {
        fun fromString(s: String): Genre {
            val l = s.trim().lowercase()
            return entries.firstOrNull { it.name.lowercase() == l || it.displayName.lowercase() == l } ?: when {
                l.contains("hip") || l.contains("rap") -> HIP_HOP
                l.contains("r&b") || l.contains("rnb") -> RNB
                l.contains("electro") || l.contains("edm") || l.contains("house") || l.contains("techno") || l.contains("trance") -> ELECTRONIC
                l.contains("metal") -> METAL; l.contains("punk") -> PUNK
                l.contains("class") -> CLASSICAL; l.contains("jazz") -> JAZZ; l.contains("blues") -> BLUES
                l.contains("soul") -> SOUL; l.contains("funk") -> FUNK
                l.contains("reggae") || l.contains("ska") -> REGGAE
                l.contains("latin") || l.contains("salsa") || l.contains("reggaeton") -> LATIN
                l.contains("folk") || l.contains("acoustic") -> FOLK
                l.contains("indie") -> INDIE; l.contains("alt") -> ALTERNATIVE
                l.contains("country") -> COUNTRY; l.contains("ambient") -> AMBIENT
                l.contains("pop") -> POP; l.contains("rock") -> ROCK
                l.contains("dance") -> DANCE; else -> UNKNOWN
            }
        }
    }
}

enum class Mood(val displayName: String) {
    ENERGETIC("Energetic"), HAPPY("Happy"), MELANCHOLIC("Melancholic"),
    AGGRESSIVE("Aggressive"), CALM("Calm"), DARK("Dark"), ROMANTIC("Romantic"),
    DREAMY("Dreamy"), INTENSE("Intense"), NEUTRAL("Neutral")
}

enum class MediaType { MUSIC, PODCAST, AUDIOBOOK, VIDEO, UNKNOWN }

enum class PredictionSource(val displayName: String) {
    LASTFM("Last.fm"), LOCAL_CLASSIFIER("Local AI"), MERGED("AI + Last.fm"),
    ADAPTIVE_OVERRIDE("Learned"), USER_PRESET("User Preset"), CACHE("Cache"), LYRICS("Lyrics"), GEMINI("Gemini"), FALLBACK("Fallback")
}

enum class AudioRoute(val displayName: String) {
    SPEAKER("Speaker"), WIRED_HEADPHONES("Wired"), BLUETOOTH("Bluetooth"), USB("USB"), UNKNOWN("Unknown")
}
