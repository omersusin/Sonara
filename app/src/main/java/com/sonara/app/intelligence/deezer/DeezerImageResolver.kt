package com.sonara.app.intelligence.deezer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object DeezerImageResolver {
    private const val TAG = "DeezerImg"
    private const val BASE = "https://api.deezer.com"
    private val cache = mutableMapOf<String, String>()

    suspend fun getArtistImage(artistName: String): String? = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext null
        cache[artistName.lowercase()]?.let { return@withContext it }
        try {
            val q = URLEncoder.encode(artistName, "UTF-8")
            val url = "$BASE/search/artist?q=$q&limit=1"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext null }
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val data = JSONObject(json).optJSONArray("data")
            if (data != null && data.length() > 0) {
                val artist = data.getJSONObject(0)
                val img = artist.optString("picture_medium", "")
                    .ifBlank { artist.optString("picture_big", "") }
                    .ifBlank { artist.optString("picture", "") }
                if (img.isNotBlank()) { cache[artistName.lowercase()] = img; return@withContext img }
            }
            null
        } catch (e: Exception) { Log.w(TAG, "Failed for $artistName: ${e.message}"); null }
    }

    suspend fun getArtistInfo(artistName: String): ArtistInfo? = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(artistName, "UTF-8")
            val url = "$BASE/search/artist?q=$q&limit=1"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext null }
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val data = JSONObject(json).optJSONArray("data")
            if (data != null && data.length() > 0) {
                val a = data.getJSONObject(0)
                return@withContext ArtistInfo(
                    name = a.optString("name", artistName),
                    imageUrl = a.optString("picture_big", "").ifBlank { a.optString("picture_medium", "") },
                    fans = a.optInt("nb_fan", 0),
                    albums = a.optInt("nb_album", 0),
                    deezerId = a.optInt("id", 0)
                )
            }
            null
        } catch (e: Exception) { Log.w(TAG, "Info failed: ${e.message}"); null }
    }

    data class ArtistInfo(
        val name: String, val imageUrl: String,
        val fans: Int, val albums: Int, val deezerId: Int
    )
}
