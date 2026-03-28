package com.sonara.app.ai.enrichment

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LastFmEnricher(private val apiKey: String) {
    companion object {
        private const val TAG = "SonaraLastFm"
        private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
        private const val TIMEOUT_MS = 8000
        private val GENRE_TAG_MAP = mapOf(
            "rock" to "rock", "pop" to "pop", "jazz" to "jazz", "classical" to "classical", "electronic" to "electronic",
            "hip-hop" to "hip-hop", "hip hop" to "hip-hop", "metal" to "metal", "heavy metal" to "metal",
            "r&b" to "r&b", "rnb" to "r&b", "blues" to "blues", "country" to "country", "folk" to "folk",
            "reggae" to "reggae", "latin" to "latin", "dance" to "dance", "indie" to "indie", "indie rock" to "indie",
            "punk" to "punk", "punk rock" to "punk", "funk" to "funk", "soul" to "soul", "ambient" to "ambient",
            "alternative" to "rock", "alternative rock" to "rock", "classic rock" to "rock", "hard rock" to "rock",
            "grunge" to "rock", "indie pop" to "pop", "synth pop" to "pop", "synthpop" to "pop", "k-pop" to "pop",
            "edm" to "electronic", "techno" to "electronic", "house" to "electronic", "trance" to "electronic",
            "dubstep" to "electronic", "drum and bass" to "electronic", "dnb" to "electronic",
            "trap" to "hip-hop", "rap" to "hip-hop", "death metal" to "metal", "black metal" to "metal",
            "thrash metal" to "metal", "metalcore" to "metal", "nu metal" to "metal",
            "smooth jazz" to "jazz", "bebop" to "jazz", "fusion" to "jazz", "neo soul" to "soul",
            "bluegrass" to "folk", "singer-songwriter" to "folk", "acoustic" to "acoustic",
            "chillout" to "ambient", "downtempo" to "ambient", "lo-fi" to "ambient", "lofi" to "ambient",
            "reggaeton" to "latin", "salsa" to "latin", "bossa nova" to "latin",
            "post-punk" to "punk", "hardcore" to "punk", "ska" to "reggae", "dub" to "reggae"
        )
        private val MOOD_TAG_MAP = mapOf(
            "happy" to Pair(0.7f, 0.6f), "upbeat" to Pair(0.6f, 0.7f), "uplifting" to Pair(0.5f, 0.6f),
            "party" to Pair(0.4f, 0.8f), "sad" to Pair(-0.6f, 0.2f), "melancholy" to Pair(-0.5f, 0.2f),
            "dark" to Pair(-0.5f, 0.5f), "angry" to Pair(-0.3f, 0.9f), "aggressive" to Pair(-0.2f, 0.9f),
            "chill" to Pair(0.1f, 0.2f), "relaxing" to Pair(0.2f, 0.15f), "calm" to Pair(0.1f, 0.1f),
            "energetic" to Pair(0.2f, 0.85f), "intense" to Pair(-0.1f, 0.9f), "epic" to Pair(0.1f, 0.8f)
        )
    }

    suspend fun enrich(title: String, artist: String): EnrichmentSignal = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || title.isBlank()) return@withContext EnrichmentSignal.empty("lastfm")
        try {
            val tags = fetchTopTags(title, artist)
            if (tags.isEmpty()) return@withContext EnrichmentSignal.empty("lastfm")
            val genreHints = mutableMapOf<String, Float>()
            var moodValence: Float? = null; var moodArousal: Float? = null; var moodCount = 0
            for ((tag, weight) in tags) {
                val nt = tag.lowercase().trim()
                GENRE_TAG_MAP[nt]?.let { g -> val nw = weight / 100f; genreHints.merge(g, nw) { a, b -> maxOf(a, b) } }
                MOOD_TAG_MAP[nt]?.let { (v, a) -> val w = weight / 100f; moodValence = ((moodValence ?: 0f) * moodCount + v * w) / (moodCount + w); moodArousal = ((moodArousal ?: 0.5f) * moodCount + a * w) / (moodCount + w); moodCount++ }
            }
            val mx = genreHints.values.maxOrNull() ?: 1f; if (mx > 0) genreHints.replaceAll { _, v -> v / mx }
            val conf = when { genreHints.size >= 3 && tags.size >= 5 -> 0.80f; genreHints.size >= 2 -> 0.65f; genreHints.size >= 1 -> 0.50f; else -> 0.25f }
            EnrichmentSignal(source = "lastfm", genreHints = genreHints, moodValence = moodValence, moodArousal = moodArousal, confidence = conf, isValid = genreHints.isNotEmpty())
        } catch (e: Exception) { Log.e(TAG, "Failed: ${e.message}"); EnrichmentSignal.empty("lastfm") }
    }

    private fun fetchTopTags(title: String, artist: String): List<Pair<String, Int>> {
        val et = URLEncoder.encode(title, "UTF-8"); val ea = URLEncoder.encode(artist, "UTF-8")
        val url = "${BASE_URL}?method=track.getTopTags&artist=$ea&track=$et&api_key=$apiKey&format=json"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS; conn.readTimeout = TIMEOUT_MS
        try {
            if (conn.responseCode != 200) return emptyList()
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            if (json.has("error")) return emptyList()
            val tagArray = json.optJSONObject("toptags")?.optJSONArray("tag") ?: return emptyList()
            val result = mutableListOf<Pair<String, Int>>()
            for (i in 0 until minOf(tagArray.length(), 15)) {
                val o = tagArray.getJSONObject(i); val n = o.optString("name", ""); val c = o.optInt("count", 0)
                if (n.isNotBlank() && c > 0) result.add(n to c)
            }
            return result
        } finally { conn.disconnect() }
    }
}
