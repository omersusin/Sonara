package com.sonara.app.intelligence.odesli

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object OdesliHelper {
    private const val TAG = "Odesli"

    data class PlatformLink(
        val name: String,
        val url: String,
        val key: String,
        val deepLink: String? = null
    )

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

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
        val query = "$artist $title"

        // Try Odesli via Deezer
        try {
            val q = enc(query)
            val dConn = URL("https://api.deezer.com/search/track?q=$q&limit=1").openConnection() as HttpURLConnection
            dConn.connectTimeout = 5000; dConn.readTimeout = 5000
            if (dConn.responseCode == 200) {
                val dJson = dConn.inputStream.bufferedReader().readText(); dConn.disconnect()
                val data = JSONObject(dJson).optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val trackId = data.getJSONObject(0).optLong("id", 0)
                    if (trackId > 0) {
                        val oUrl = "https://api.song.link/v1-alpha.1/links?url=${enc("https://www.deezer.com/track/$trackId")}&songIfSingle=true"
                        val oConn = URL(oUrl).openConnection() as HttpURLConnection
                        oConn.connectTimeout = 8000; oConn.readTimeout = 8000
                        if (oConn.responseCode == 200) {
                            val body = oConn.inputStream.bufferedReader().readText(); oConn.disconnect()
                            val links = JSONObject(body).optJSONObject("linksByPlatform")
                            if (links != null) {
                                for ((key, name) in platformMap) {
                                    val url = links.optJSONObject(key)?.optString("url", "") ?: ""
                                    if (url.isNotBlank()) result.add(PlatformLink(name, url, key))
                                }
                            }
                        } else oConn.disconnect()
                    }
                }
            } else dConn.disconnect()
        } catch (e: Exception) { Log.w(TAG, "Odesli: ${e.message}") }

        // Ensure major platforms always present with working links
        val keys = result.map { it.key }.toSet()
        val e = enc(query)

        if ("spotify" !in keys) {
            result.add(0, PlatformLink("Spotify",
                "https://open.spotify.com/search/$e",
                "spotify",
                "spotify:search:$query"))
        } else {
            // Replace with deep link version
            val idx = result.indexOfFirst { it.key == "spotify" }
            if (idx >= 0) result[idx] = result[idx].copy(deepLink = "spotify:search:$query")
        }

        if ("appleMusic" !in keys) {
            result.add(minOf(1, result.size), PlatformLink("Apple Music",
                "https://music.apple.com/search?term=$e",
                "appleMusic"))
        }

        if ("youtubeMusic" !in keys) {
            result.add(minOf(2, result.size), PlatformLink("YouTube Music",
                "https://music.youtube.com/search?q=$e",
                "youtubeMusic"))
        }

        if ("youtube" !in keys) {
            result.add(minOf(3, result.size), PlatformLink("YouTube",
                "https://www.youtube.com/results?search_query=$e",
                "youtube"))
        }

        result
    }

    suspend fun getArtistLinks(artistName: String): List<PlatformLink> = withContext(Dispatchers.IO) {
        val e = enc(artistName)
        var deezerId = 0L

        try {
            val conn = URL("https://api.deezer.com/search/artist?q=$e&limit=1").openConnection() as HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().readText(); conn.disconnect()
                val data = JSONObject(json).optJSONArray("data")
                if (data != null && data.length() > 0) deezerId = data.getJSONObject(0).optLong("id", 0)
            } else conn.disconnect()
        } catch (ex: Exception) { Log.w(TAG, "Deezer: ${ex.message}") }

        val result = mutableListOf<PlatformLink>()
        result.add(PlatformLink("Spotify", "https://open.spotify.com/search/$e", "spotify", "spotify:search:$artistName"))
        result.add(PlatformLink("Apple Music", "https://music.apple.com/search?term=$e", "appleMusic"))
        result.add(PlatformLink("YouTube Music", "https://music.youtube.com/search?q=$e", "youtubeMusic"))
        result.add(PlatformLink("YouTube", "https://www.youtube.com/results?search_query=$e", "youtube"))
        if (deezerId > 0) result.add(PlatformLink("Deezer", "https://www.deezer.com/artist/$deezerId", "deezer"))
        result.add(PlatformLink("SoundCloud", "https://soundcloud.com/search?q=$e", "soundcloud"))
        result.add(PlatformLink("Tidal", "https://listen.tidal.com/search?q=$e", "tidal"))

        result
    }

    suspend fun getSongLinkUrl(title: String, artist: String): String = withContext(Dispatchers.IO) {
        val query = "$artist $title"
        val e = enc(query)
        try {
            val dConn = URL("https://api.deezer.com/search/track?q=$e&limit=1").openConnection() as HttpURLConnection
            dConn.connectTimeout = 5000; dConn.readTimeout = 5000
            if (dConn.responseCode == 200) {
                val dJson = dConn.inputStream.bufferedReader().readText(); dConn.disconnect()
                val data = JSONObject(dJson).optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val trackId = data.getJSONObject(0).optLong("id", 0)
                    if (trackId > 0) {
                        val oUrl = "https://api.song.link/v1-alpha.1/links?url=${enc("https://www.deezer.com/track/$trackId")}&songIfSingle=true"
                        val oConn = URL(oUrl).openConnection() as HttpURLConnection
                        oConn.connectTimeout = 8000; oConn.readTimeout = 8000
                        if (oConn.responseCode == 200) {
                            val body = oConn.inputStream.bufferedReader().readText(); oConn.disconnect()
                            val pageUrl = JSONObject(body).optString("pageUrl", "")
                            if (pageUrl.isNotBlank()) return@withContext pageUrl
                        } else oConn.disconnect()
                    }
                }
            } else dConn.disconnect()
        } catch (ex: Exception) { Log.w(TAG, "getSongLinkUrl: ${ex.message}") }
        "https://song.link/s/$e"
    }

    /**
     * Open a platform link. Tries deep link first (for installed apps), falls back to URL.
     */
    fun openLink(ctx: Context, link: PlatformLink) {
        // Try deep link first (e.g. spotify:search:...)
        if (link.deepLink != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.deepLink))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (intent.resolveActivity(ctx.packageManager) != null) {
                    ctx.startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }

        // Try the URL (may open in installed app via intent filter)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }
}
