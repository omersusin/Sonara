package com.sonara.app.intelligence.lastfm

import com.sonara.app.data.models.TrackInfo

class LastFmResolver {

    suspend fun resolve(title: String, artist: String, apiKey: String): TrackInfo? {
        if (apiKey.isBlank() || title.isBlank()) return null

        return try {
            val response = LastFmClient.api.getTrackInfo(title, artist, apiKey)
            val track = response.track ?: return tryArtistFallback(artist, apiKey, title)
            val tags = track.toptags?.tag ?: emptyList()

            if (tags.isEmpty()) {
                return tryArtistFallback(artist, apiKey, title)
            }

            val genre = classifyGenre(tags)
            val mood = classifyMood(tags)
            val energy = estimateEnergy(tags, genre)

            TrackInfo(
                title = track.name.ifBlank { title },
                artist = track.artist?.name ?: artist,
                album = track.album?.title ?: "",
                genre = genre,
                mood = mood,
                energy = energy,
                confidence = calculateConfidence(tags),
                source = "lastfm"
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryArtistFallback(artist: String, apiKey: String, title: String): TrackInfo? {
        if (artist.isBlank()) return null
        return try {
            val response = LastFmClient.api.getArtistTags(artist, apiKey)
            val tags = response.toptags?.tag ?: return null
            if (tags.isEmpty()) return null

            val genre = classifyGenre(tags)
            val mood = classifyMood(tags)

            TrackInfo(
                title = title,
                artist = artist,
                genre = genre,
                mood = mood,
                energy = estimateEnergy(tags, genre),
                confidence = calculateConfidence(tags) * 0.7f,
                source = "lastfm-artist"
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun classifyGenre(tags: List<LastFmTag>): String {
        val genreMap = mapOf(
            "rock" to listOf("rock", "alternative", "indie rock", "punk", "grunge", "hard rock", "classic rock"),
            "pop" to listOf("pop", "synth-pop", "electropop", "indie pop", "dream pop", "k-pop"),
            "hip-hop" to listOf("hip-hop", "hip hop", "rap", "trap", "boom bap", "drill"),
            "electronic" to listOf("electronic", "edm", "house", "techno", "trance", "dubstep", "dnb", "drum and bass"),
            "r&b" to listOf("r&b", "rnb", "soul", "neo-soul", "funk"),
            "jazz" to listOf("jazz", "smooth jazz", "bebop", "fusion", "swing"),
            "classical" to listOf("classical", "orchestra", "symphony", "chamber", "baroque", "romantic"),
            "metal" to listOf("metal", "heavy metal", "death metal", "black metal", "thrash", "metalcore", "doom metal"),
            "country" to listOf("country", "bluegrass", "americana", "folk"),
            "folk" to listOf("folk", "acoustic", "singer-songwriter", "indie folk"),
            "blues" to listOf("blues", "delta blues", "chicago blues"),
            "reggae" to listOf("reggae", "ska", "dub", "dancehall"),
            "latin" to listOf("latin", "salsa", "reggaeton", "bossa nova", "samba")
        )

        val tagNames = tags.map { it.name.lowercase() }
        var bestGenre = "other"
        var bestScore = 0

        genreMap.forEach { (genre, keywords) ->
            var score = 0
            tagNames.forEachIndexed { i, tag ->
                if (keywords.any { tag.contains(it) }) {
                    score += (tags.size - i) + (tags.getOrNull(i)?.count ?: 0)
                }
            }
            if (score > bestScore) { bestScore = score; bestGenre = genre }
        }
        return bestGenre
    }

    private fun classifyMood(tags: List<LastFmTag>): String {
        val tagNames = tags.map { it.name.lowercase() }
        val moodMap = mapOf(
            "energetic" to listOf("energetic", "upbeat", "party", "dance", "fast", "power", "aggressive"),
            "chill" to listOf("chill", "relax", "calm", "ambient", "mellow", "downtempo", "lounge"),
            "melancholic" to listOf("sad", "melancholy", "dark", "emotional", "depressing", "gloomy"),
            "happy" to listOf("happy", "fun", "cheerful", "uplifting", "feel good", "bright"),
            "intense" to listOf("intense", "heavy", "brutal", "hard", "loud", "raw"),
            "romantic" to listOf("romantic", "love", "sensual", "smooth", "sexy", "intimate")
        )
        moodMap.forEach { (mood, keywords) ->
            if (tagNames.any { tag -> keywords.any { tag.contains(it) } }) return mood
        }
        return "neutral"
    }

    private fun estimateEnergy(tags: List<LastFmTag>, genre: String): Float {
        val highEnergy = setOf("metal", "electronic", "hip-hop")
        val lowEnergy = setOf("classical", "jazz", "folk", "blues")
        val base = when (genre) {
            in highEnergy -> 0.75f
            in lowEnergy -> 0.35f
            else -> 0.55f
        }
        val tagNames = tags.map { it.name.lowercase() }
        var modifier = 0f
        if (tagNames.any { it.contains("chill") || it.contains("slow") }) modifier -= 0.15f
        if (tagNames.any { it.contains("fast") || it.contains("energy") || it.contains("party") }) modifier += 0.15f
        return (base + modifier).coerceIn(0f, 1f)
    }

    private fun calculateConfidence(tags: List<LastFmTag>): Float {
        if (tags.isEmpty()) return 0f
        val topCount = tags.firstOrNull()?.count ?: 0
        val tagCount = tags.size.coerceAtMost(10)
        val countScore = (topCount / 100f).coerceAtMost(0.5f)
        val diversityScore = (tagCount / 10f) * 0.5f
        return (countScore + diversityScore).coerceIn(0.1f, 1f)
    }
}
