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
        "anghami" to "Anghami",
        "boomplay" to "Boomplay"
    )

    suspend fun getLinks(title: String, artist: String): List<PlatformLink> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PlatformLink>()
        try {
            val q = URLEncoder.encode("$artist $title", "UTF-8")
            val deezerConn = URL("https://api.deezer.com/search/track?q=$q&limit=1").openConnection() as HttpURLConnection
            deezerConn.connectTimeout = 5000; deezerConn.readTimeout = 5000
            if (deezerConn.responseCode == 200) {
                val deezerJson = deezerConn.inputStream.bufferedReader().readText(); deezerConn.disconnect()
                val data = JSONObject(deezerJson).optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val trackId = data.getJSONObject(0).optLong("id", 0)
                    if (trackId > 0) {
                        val deezerUrl = "https://www.deezer.com/track/$trackId"
                        val odesliUrl = "https://api.song.link/v1-alpha.1/links?url=${URLEncoder.encode(deezerUrl, "UTF-8")}&songIfSingle=true"
                        val conn = URL(odesliUrl).openConnection() as HttpURLConnection
                        conn.connectTimeout = 8000; conn.readTimeout = 8000
                        if (conn.responseCode == 200) {
                            val body = conn.inputStream.bufferedReader().readText(); conn.disconnect()
                            val links = JSONObject(body).optJSONObject("linksByPlatform")
                            if (links != null) {
                                for ((key, displayName) in platformMap) {
                                    val url = links.optJSONObject(key)?.optString("url", "") ?: ""
                                    if (url.isNotBlank()) result.add(PlatformLink(displayName, url, key))
                                }
                            }
                        } else conn.disconnect()
                    }
                }
            } else deezerConn.disconnect()
        } catch (e: Exception) { Log.w(TAG, "Odesli: ${e.message}") }

        // Fallback: add search URLs for major platforms if missing
        val enc = URLEncoder.encode("$artist $title", "UTF-8")
        val encTrack = URLEncoder.encode(title, "UTF-8")
        val encArtist = URLEncoder.encode(artist, "UTF-8")
        val keys = result.map { it.key }.toSet()
        if ("spotify" !in keys) result.add(0, PlatformLink("Spotify", "https://open.spotify.com/search/$enc", "spotify"))
        if ("appleMusic" !in keys) result.add(minOf(1, result.size), PlatformLink("Apple Music", "https://music.apple.com/search?term=$enc", "appleMusic"))
        if ("youtubeMusic" !in keys) result.add(minOf(2, result.size), PlatformLink("YouTube Music", "https://music.youtube.com/search?q=$enc", "youtubeMusic"))
        if ("youtube" !in keys) result.add(minOf(3, result.size), PlatformLink("YouTube", "https://www.youtube.com/results?search_query=$enc", "youtube"))

        result
    }

    suspend fun getArtistLinks(artistName: String): List<PlatformLink> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PlatformLink>()
        val enc = URLEncoder.encode(artistName, "UTF-8")
        var deezerId = 0L

        // Get Deezer artist ID
        try {
            val conn = URL("https://api.deezer.com/search/artist?q=$enc&limit=1").openConnection() as HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().readText(); conn.disconnect()
                val data = JSONObject(json).optJSONArray("data")
                if (data != null && data.length() > 0) deezerId = data.getJSONObject(0).optLong("id", 0)
            } else conn.disconnect()
        } catch (e: Exception) { Log.w(TAG, "Deezer artist: ${e.message}") }

        // Build artist-specific search URLs (NOT track URLs)
        result.add(PlatformLink("Spotify", "https://open.spotify.com/search/$enc", "spotify"))
        result.add(PlatformLink("Apple Music", "https://music.apple.com/search?term=$enc", "appleMusic"))
        result.add(PlatformLink("YouTube Music", "https://music.youtube.com/search?q=$enc", "youtubeMusic"))
        result.add(PlatformLink("YouTube", "https://www.youtube.com/results?search_query=$enc", "youtube"))
        if (deezerId > 0) result.add(PlatformLink("Deezer", "https://www.deezer.com/artist/$deezerId", "deezer"))
        result.add(PlatformLink("SoundCloud", "https://soundcloud.com/search?q=$enc", "soundcloud"))
        result.add(PlatformLink("Tidal", "https://listen.tidal.com/search?q=$enc", "tidal"))

        result
    }
}
