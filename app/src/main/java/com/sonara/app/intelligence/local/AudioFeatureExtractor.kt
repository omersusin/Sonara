package com.sonara.app.intelligence.local

data class AudioFeatures(
    val estimatedGenre: String = "other", val estimatedMood: String = "neutral",
    val estimatedEnergy: Float = 0.5f, val bassNeed: Float = 0.5f, val trebleNeed: Float = 0.5f,
    val vocalPresence: Float = 0.5f, val brightness: Float = 0.5f, val confidence: Float = 0.3f
)

class AudioFeatureExtractor {

    private val artistGenreDb = mapOf(
        "taylor swift" to "pop", "ed sheeran" to "pop", "dua lipa" to "pop", "billie eilish" to "pop",
        "the weeknd" to "pop", "adele" to "pop", "ariana grande" to "pop", "justin bieber" to "pop",
        "harry styles" to "pop", "olivia rodrigo" to "pop", "bruno mars" to "pop", "lady gaga" to "pop",
        "katy perry" to "pop", "rihanna" to "pop", "sia" to "pop", "shawn mendes" to "pop",
        "post malone" to "pop", "doja cat" to "pop", "charlie puth" to "pop", "sam smith" to "pop",
        "selena gomez" to "pop", "miley cyrus" to "pop", "halsey" to "pop", "lizzo" to "pop",
        "sabrina carpenter" to "pop", "chappell roan" to "pop", "coldplay" to "pop",
        "imagine dragons" to "pop", "maroon 5" to "pop", "one direction" to "pop",

        "drake" to "hip-hop", "kendrick lamar" to "hip-hop", "travis scott" to "hip-hop",
        "kanye west" to "hip-hop", "j. cole" to "hip-hop", "eminem" to "hip-hop",
        "lil baby" to "hip-hop", "21 savage" to "hip-hop", "future" to "hip-hop",
        "cardi b" to "hip-hop", "nicki minaj" to "hip-hop", "lil nas x" to "hip-hop",
        "tyler, the creator" to "hip-hop", "metro boomin" to "hip-hop",

        "queen" to "rock", "the beatles" to "rock", "led zeppelin" to "rock", "pink floyd" to "rock",
        "nirvana" to "rock", "foo fighters" to "rock", "arctic monkeys" to "rock", "radiohead" to "rock",
        "muse" to "rock", "u2" to "rock", "green day" to "rock", "linkin park" to "rock",
        "red hot chili peppers" to "rock", "the killers" to "rock", "oasis" to "rock", "ac/dc" to "rock",

        "daft punk" to "electronic", "calvin harris" to "electronic", "marshmello" to "electronic",
        "avicii" to "electronic", "deadmau5" to "electronic", "skrillex" to "electronic",
        "tiesto" to "electronic", "zedd" to "electronic", "kygo" to "electronic",
        "martin garrix" to "electronic", "david guetta" to "electronic",

        "sza" to "r&b", "frank ocean" to "r&b", "daniel caesar" to "r&b", "h.e.r." to "r&b",
        "summer walker" to "r&b", "chris brown" to "r&b", "usher" to "r&b", "khalid" to "r&b",

        "metallica" to "metal", "iron maiden" to "metal", "slipknot" to "metal",
        "system of a down" to "metal", "tool" to "metal", "rammstein" to "metal",
        "bring me the horizon" to "metal", "avenged sevenfold" to "metal",

        "miles davis" to "jazz", "john coltrane" to "jazz", "norah jones" to "jazz",
        "beethoven" to "classical", "mozart" to "classical", "bach" to "classical",
        "chopin" to "classical", "hans zimmer" to "classical",

        "bob marley" to "reggae", "bad bunny" to "latin", "shakira" to "latin",

        "tarkan" to "pop", "sezen aksu" to "pop", "duman" to "rock", "manga" to "rock",
        "mor ve ötesi" to "rock", "mabel matiz" to "pop", "ezhel" to "hip-hop",
        "ben fero" to "hip-hop", "ceza" to "hip-hop", "sagopa kajmer" to "hip-hop",
        "barış manço" to "rock", "cem karaca" to "rock", "ajda pekkan" to "pop",
        "gülşen" to "pop", "aleyna tilki" to "pop", "hadise" to "pop"
    )

    private val genreKeywords = mapOf(
        "pop" to listOf("pop", "mainstream", "radio edit", "single", "acoustic version", "remix",
            "love", "baby", "tonight", "dance", "summer", "heart", "feel"),
        "hip-hop" to listOf("feat", "ft.", "lil ", "dj ", "mc ", "young ", "big ", "yung",
            "freestyle", "trap", "drill", "gang", "hustle", "flex"),
        "electronic" to listOf("remix", "mix", "dj ", "bass", "beat", "drop", "synth",
            "club", "rave", "techno", "house", "edm"),
        "classical" to listOf("symphony", "concerto", "opus", "sonata", "orchestra", "quartet",
            "prelude", "nocturne", "etude", "in d minor", "in c major", "op."),
        "jazz" to listOf("swing", "bop", "blues", "trio", "quartet", "standard"),
        "metal" to listOf("death", "blood", "dark", "demon", "hell", "war", "chaos", "rage",
            "scream", "brutal", "destruction"),
        "rock" to listOf("rock", "guitar", "band", "live", "electric", "anthem", "rebel"),
        "folk" to listOf("acoustic", "folk", "unplugged", "traditional", "ballad"),
        "r&b" to listOf("soul", "groove", "vibe", "smooth", "slow jam"),
        "country" to listOf("country", "cowboy", "truck", "whiskey", "nashville"),
        "reggae" to listOf("reggae", "ska", "dub", "dancehall", "rasta"),
        "latin" to listOf("reggaeton", "salsa", "bachata", "cumbia", "samba", "perreo")
    )

    private val moodKeywords = mapOf(
        "melancholic" to listOf("sad", "cry", "tears", "alone", "lonely", "pain", "rain", "broken", "lost", "miss"),
        "happy" to listOf("happy", "joy", "sun", "smile", "dance", "party", "fun", "good", "yeah"),
        "energetic" to listOf("fire", "power", "fight", "run", "wild", "crazy", "fast", "rage", "beast"),
        "romantic" to listOf("love", "kiss", "heart", "forever", "beautiful", "dream", "angel"),
        "chill" to listOf("night", "sleep", "calm", "peace", "quiet", "soft", "gentle", "slow")
    )

    fun extract(title: String, artist: String): AudioFeatures {
        val lTitle = title.lowercase().trim()
        val lArtist = artist.lowercase().trim()
        val combined = "$lTitle $lArtist"

        // Stage 1: Artist DB (highest confidence)
        var genre = "other"
        var confidence = 0.3f
        val artistMatch = artistGenreDb.entries.firstOrNull { (k, _) -> lArtist.contains(k) || lArtist == k }
        if (artistMatch != null) {
            genre = artistMatch.value; confidence = 0.75f
        } else {
            // Stage 2: Keyword scoring
            val scores = mutableMapOf<String, Int>()
            genreKeywords.forEach { (g, kws) ->
                var score = 0
                kws.forEach { kw -> if (combined.contains(kw)) score += 2; if (lTitle.contains(kw)) score += 1 }
                if (score > 0) scores[g] = score
            }
            if (scores.isNotEmpty()) {
                val best = scores.maxByOrNull { it.value }!!
                genre = best.key; confidence = (best.value / 6f).coerceIn(0.2f, 0.6f)
            }
            // Stage 3: feat/ft → pop fallback
            if (genre == "other" && (combined.contains("feat") || combined.contains("ft."))) {
                genre = "pop"; confidence = 0.35f
            }
        }

        val mood = detectMood(combined)
        val energy = estimateEnergy(genre, mood)

        return AudioFeatures(genre, mood, energy, bassNeedFor(genre), trebleNeedFor(genre),
            vocalPresenceFor(genre), brightnessFor(genre, mood), confidence)
    }

    private fun detectMood(text: String): String {
        moodKeywords.forEach { (mood, kws) -> if (kws.any { text.contains(it) }) return mood }
        return "neutral"
    }

    private fun estimateEnergy(genre: String, mood: String): Float {
        val g = mapOf("metal" to 0.9f, "electronic" to 0.8f, "hip-hop" to 0.7f, "rock" to 0.7f,
            "pop" to 0.6f, "r&b" to 0.5f, "jazz" to 0.4f, "folk" to 0.35f, "classical" to 0.3f)
        val m = mapOf("energetic" to 0.2f, "happy" to 0.1f, "romantic" to -0.05f, "chill" to -0.15f, "melancholic" to -0.1f)
        return ((g[genre] ?: 0.5f) + (m[mood] ?: 0f)).coerceIn(0.1f, 1f)
    }

    private fun bassNeedFor(g: String) = mapOf("hip-hop" to 0.85f, "electronic" to 0.8f, "r&b" to 0.7f, "metal" to 0.65f, "rock" to 0.6f, "pop" to 0.55f, "jazz" to 0.4f, "classical" to 0.3f, "folk" to 0.3f)[g] ?: 0.5f
    private fun trebleNeedFor(g: String) = mapOf("classical" to 0.75f, "jazz" to 0.7f, "folk" to 0.65f, "pop" to 0.6f, "rock" to 0.6f, "electronic" to 0.55f, "metal" to 0.5f, "hip-hop" to 0.4f, "r&b" to 0.5f)[g] ?: 0.5f
    private fun vocalPresenceFor(g: String) = mapOf("pop" to 0.8f, "r&b" to 0.8f, "folk" to 0.75f, "hip-hop" to 0.7f, "jazz" to 0.6f, "rock" to 0.6f, "metal" to 0.5f, "electronic" to 0.3f, "classical" to 0.2f)[g] ?: 0.5f
    private fun brightnessFor(g: String, m: String): Float {
        val b = mapOf("pop" to 0.7f, "classical" to 0.65f, "jazz" to 0.6f, "folk" to 0.6f, "electronic" to 0.6f, "rock" to 0.5f, "hip-hop" to 0.45f, "metal" to 0.4f, "r&b" to 0.55f)[g] ?: 0.5f
        val mod = if (m == "melancholic") -0.1f else if (m == "happy") 0.1f else 0f
        return (b + mod).coerceIn(0.1f, 1f)
    }
}
