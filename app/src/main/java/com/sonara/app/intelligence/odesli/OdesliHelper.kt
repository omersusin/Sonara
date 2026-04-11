package com.sonara.app.intelligence.odesli

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object OdesliHelper {
    private const val TAG = "Odesli"

    data class PlatformLink(val name: String, val url: String, val key: String)

    private val platformMap = linkedMapOf(
        "spotify" to "Spotify",
        "appleMusic" to "Apple Music",
        "youtubeMusic" to "YouTube Music",
        "youtube" to "YouTube",
        "deezer" to "Deezer",
        "tidal" to "Tidal",
        "amazonMusic" to "Amazon Music",
        "soundcloud" to "SoundCloud",
        "pandora" to "Pandora",
        "napster" to "Napster",
        "yandex" to "Yandex Music",
        "audiomack" to "Audiomack",
        "audius" to "Audius",
        "anghami" to "Anghami",
        "boomplay" to "Boomplay"
    )

    suspend fun getLinks(title: String, artist: String): List<PlatformLink> = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode("$artist $title", "UTF-8")
            val deezerConn = URL("https://api.deezer.com/search/track?q=$q&limit=1").openConnection() as HttpURLConnection
            deezerConn.connectTimeout = 5000; deezerConn.readTimeout = 5000
            if (deezerConn.responseCode != 200) { deezerConn.disconnect(); return@withContext emptyList() }
            val deezerJson = deezerConn.inputStream.bufferedReader().readText(); deezerConn.disconnect()
            val data = JSONObject(deezerJson).optJSONArray("data")
            if (data == null || data.length() == 0) return@withContext emptyList()
            val trackId = data.getJSONObject(0).optLong("id", 0)
            if (trackId == 0L) return@withContext emptyList()
            val deezerUrl = "https://www.deezer.com/track/$trackId"

            val odesliUrl = "https://api.song.link/v1-alpha.1/links?url=${URLEncoder.encode(deezerUrl, "UTF-8")}&songIfSingle=true"
            val conn = URL(odesliUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000; conn.readTimeout = 8000
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext emptyList() }
            val body = conn.inputStream.bufferedReader().readText(); conn.disconnect()
            val links = JSONObject(body).optJSONObject("linksByPlatform") ?: return@withContext emptyList()

            val result = mutableListOf<PlatformLink>()
            for ((key, displayName) in platformMap) {
                val obj = links.optJSONObject(key)
                val url = obj?.optString("url", "") ?: ""
                if (url.isNotBlank()) result.add(PlatformLink(displayName, url, key))
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getArtistLinks(artistName: String): List<PlatformLink> = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(artistName, "UTF-8")
            val conn = URL("https://api.deezer.com/search/artist?q=$q&limit=1").openConnection() as HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext emptyList() }
            val json = conn.inputStream.bufferedReader().readText(); conn.disconnect()
            val data = JSONObject(json).optJSONArray("data")
            if (data == null || data.length() == 0) return@withContext emptyList()
            val artistId = data.getJSONObject(0).optLong("id", 0)
            if (artistId == 0L) return@withContext emptyList()

            val topConn = URL("https://api.deezer.com/artist/$artistId/top?limit=1").openConnection() as HttpURLConnection
            topConn.connectTimeout = 5000; topConn.readTimeout = 5000
            if (topConn.responseCode != 200) { topConn.disconnect(); return@withContext emptyList() }
            val topJson = topConn.inputStream.bufferedReader().readText(); topConn.disconnect()
            val tracks = JSONObject(topJson).optJSONArray("data")
            if (tracks == null || tracks.length() == 0) return@withContext emptyList()
            val trackId = tracks.getJSONObject(0).optLong("id", 0)
            if (trackId == 0L) return@withContext emptyList()

            val deezerUrl = "https://www.deezer.com/track/$trackId"
            val odesliUrl = "https://api.song.link/v1-alpha.1/links?url=${URLEncoder.encode(deezerUrl, "UTF-8")}&songIfSingle=true"
            val oConn = URL(odesliUrl).openConnection() as HttpURLConnection
            oConn.connectTimeout = 8000; oConn.readTimeout = 8000
            if (oConn.responseCode != 200) { oConn.disconnect(); return@withContext emptyList() }
            val body = oConn.inputStream.bufferedReader().readText(); oConn.disconnect()
            val links = JSONObject(body).optJSONObject("linksByPlatform") ?: return@withContext emptyList()

            val result = mutableListOf<PlatformLink>()
            for ((key, displayName) in platformMap) {
                val obj = links.optJSONObject(key)
                val url = obj?.optString("url", "") ?: ""
                if (url.isNotBlank()) {
                    val artistUrl = when (key) {
                        "spotify" -> "https://open.spotify.com/search/${URLEncoder.encode(artistName, "UTF-8")}"
                        "youtubeMusic" -> "https://music.youtube.com/search?q=${URLEncoder.encode(artistName, "UTF-8")}"
                        "youtube" -> "https://www.youtube.com/results?search_query=${URLEncoder.encode(artistName, "UTF-8")}"
                        "deezer" -> "https://www.deezer.com/artist/$artistId"
                        else -> url
                    }
                    result.add(PlatformLink(displayName, artistUrl, key))
                }
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Artist links error: ${e.message}")
            emptyList()
        }
    }
}
