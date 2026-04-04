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
                val a = data.getJSONObject(0)
                val id = a.optInt("id", 0)
                val detail = ArtistDetail(
                    name = a.optString("name", name),
                    imageUrl = a.optString("picture_big", "").ifBlank { a.optString("picture_medium", "") },
                    fans = a.optInt("nb_fan", 0),
                    albums = a.optInt("nb_album", 0),
                    topTracks = emptyList()
                )
                // Fetch top tracks
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
                                    TrackItem(t.optString("title", ""), t.optInt("duration", 0), t.optInt("rank", 0))
                                }
                                return@withContext detail.copy(topTracks = tl)
                            }
                        } else tc.disconnect()
                    } catch (_: Exception) {}
                }
                return@withContext detail
            }
            null
        } catch (e: Exception) { Log.w(TAG, "${e.message}"); null }
    }

    data class ArtistDetail(val name: String, val imageUrl: String, val fans: Int, val albums: Int, val topTracks: List<TrackItem>)
    data class TrackItem(val title: String, val durationSec: Int, val rank: Int)
}
