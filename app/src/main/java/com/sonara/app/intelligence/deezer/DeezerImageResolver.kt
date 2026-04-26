package com.sonara.app.intelligence.deezer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object DeezerImageResolver {
    private const val TAG = "DeezerImg"
    private const val BASE = "https://api.deezer.com"
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun getArtistImage(name: String): String? = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext null
        cache[name.lowercase()]?.let { return@withContext it }
        try {
            val q = URLEncoder.encode(name, "UTF-8")
            val conn = URL("$BASE/search/artist?q=$q&limit=1").openConnection() as HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext null }
            val json = conn.inputStream.bufferedReader().readText(); conn.disconnect()
            val data = JSONObject(json).optJSONArray("data")
            if (data != null && data.length() > 0) {
                val img = data.getJSONObject(0).optString("picture_medium", "")
                if (img.isNotBlank()) { cache[name.lowercase()] = img; return@withContext img }
            }
            null
        } catch (e: Exception) { Log.w(TAG, "${e.message}"); null }
    }

    suspend fun getArtistDetail(name: String): ArtistDetail? = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(name, "UTF-8")
            val conn = URL("$BASE/search/artist?q=$q&limit=1").openConnection() as HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext null }
            val json = conn.inputStream.bufferedReader().readText(); conn.disconnect()
            val data = JSONObject(json).optJSONArray("data")
            if (data != null && data.length() > 0) {
                val a = data.getJSONObject(0); val id = a.optInt("id", 0)
                val d = ArtistDetail(name = a.optString("name", name),
                    imageUrl = a.optString("picture_big", "").ifBlank { a.optString("picture_medium", "") },
                    fans = a.optInt("nb_fan", 0), albums = a.optInt("nb_album", 0), topTracks = emptyList())
                if (id > 0) {
                    try {
                        val tc = URL("$BASE/artist/$id/top?limit=5").openConnection() as HttpURLConnection
                        tc.connectTimeout = 5000; tc.readTimeout = 5000
                        if (tc.responseCode == 200) {
                            val tj = tc.inputStream.bufferedReader().readText(); tc.disconnect()
                            val tracks = JSONObject(tj).optJSONArray("data")
                            if (tracks != null) {
                                val tl = (0 until tracks.length()).map { i ->
                                    val t = tracks.getJSONObject(i)
                                    val art = t.optJSONObject("album")?.optString("cover_medium", "") ?: ""
                                    TrackItem(t.optString("title", ""), t.optInt("duration", 0), t.optInt("rank", 0), art.ifBlank { null })
                                }
                                return@withContext d.copy(topTracks = tl)
                            }
                        } else tc.disconnect()
                    } catch (_: Exception) {}
                }
                return@withContext d
            }
            null
        } catch (e: Exception) { Log.w(TAG, "${e.message}"); null }
    }

    data class ArtistDetail(val name: String, val imageUrl: String, val fans: Int, val albums: Int, val topTracks: List<TrackItem>)
    data class TrackItem(val title: String, val durationSec: Int, val rank: Int, val albumArt: String? = null)

    suspend fun getTrackImage(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val key = "${artist.lowercase()}::${title.lowercase()}"
        cache[key]?.let { return@withContext it }
        try {
            val q = URLEncoder.encode("$artist $title", "UTF-8")
            val conn = URL("$BASE/search/track?q=$q&limit=1").openConnection() as HttpURLConnection
            conn.connectTimeout = 4000; conn.readTimeout = 4000
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext null }
            val json = conn.inputStream.bufferedReader().readText(); conn.disconnect()
            val data = JSONObject(json).optJSONArray("data")
            if (data != null && data.length() > 0) {
                val album = data.getJSONObject(0).optJSONObject("album")
                val img = album?.optString("cover_medium", "") ?: ""
                if (img.isNotBlank()) { cache[key] = img; return@withContext img }
            }
            null
        } catch (e: Exception) { Log.w(TAG, "${e.message}"); null }
    }


    // ═══ iTunes API fallback (free, no auth) ═══
    private suspend fun getItunesArtwork(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(query, "UTF-8")
            val conn = URL("https://itunes.apple.com/search?term=$q&media=music&limit=1").openConnection() as HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext null }
            val json = conn.inputStream.bufferedReader().readText(); conn.disconnect()
            val results = JSONObject(json).optJSONArray("results")
            if (results != null && results.length() > 0) {
                val artwork = results.getJSONObject(0).optString("artworkUrl100", "")
                if (artwork.isNotBlank()) return@withContext artwork.replace("100x100bb", "600x600bb")
            }
            null
        } catch (e: Exception) { Log.w(TAG, "iTunes: ${e.message}"); null }
    }

    suspend fun getTrackImageWithFallback(title: String, artist: String): String? {
        return getTrackImage(title, artist) ?: getItunesArtwork("$artist $title")
    }

    /**
     * Resolves the best available track artwork with the correct album.
     * Priority: Last.fm track.getInfo album image → Deezer search → iTunes.
     * Use this wherever a "correct" album art per track is needed.
     */
    suspend fun resolveTrackArtwork(
        title: String,
        artist: String,
        lastFmApiKey: String = ""
    ): String? = withContext(Dispatchers.IO) {
        // 1. Last.fm — most accurate: returns the exact album the track belongs to
        if (lastFmApiKey.isNotBlank()) {
            try {
                val resp = com.sonara.app.intelligence.lastfm.LastFmClient.api.getTrackInfo(title, artist, lastFmApiKey)
                val img = resp.track?.album?.imageUrl
                if (!img.isNullOrBlank()) return@withContext img
            } catch (_: Exception) {}
        }
        // 2. Deezer (may pick wrong album for compilation tracks)
        getTrackImage(title, artist) ?: getItunesArtwork("$artist $title")
    }

    suspend fun getArtistImageWithFallback(name: String): String? {
        return getArtistImage(name) ?: getItunesArtwork(name)
    }

}
