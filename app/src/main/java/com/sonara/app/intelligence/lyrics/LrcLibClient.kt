package com.sonara.app.intelligence.lyrics

import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * LRCLIB API — ücretsiz, kayıt gerektirmez.
 * Synced (LRC) ve plain lyrics döner.
 * Docs: https://lrclib.net/docs
 */
object LrcLibClient {
    private const val TAG = "LrcLib"
    private const val BASE = "https://lrclib.net/api"

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class LrcLibResult(
        val syncedLyrics: String?,   // LRC format (enhanced ya da standard)
        val plainLyrics: String?,
        val trackName: String,
        val artistName: String,
        val albumName: String,
        val duration: Int
    )

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /**
     * En doğru sonuç için title + artist + album + duration ile sorgular.
     * duration saniye cinsinden, bilinmiyorsa 0 geç.
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        album: String = "",
        durationSec: Int = 0
    ): LrcLibResult? = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append("$BASE/get?track_name=${enc(title)}&artist_name=${enc(artist)}")
                if (album.isNotBlank()) append("&album_name=${enc(album)}")
                if (durationSec > 0) append("&duration=$durationSec")
            }
            val req = Request.Builder().url(url)
                .header("Lrclib-Client", "Sonara/1.0.0 (contact@sonara.app)")
                .build()
            val body = http.newCall(req).execute().use { it.body?.string() }
            if (body.isNullOrBlank()) return@withContext null

            val obj = JSONObject(body)
            if (obj.has("statusCode")) return@withContext null // 404 gibi hatalar

            LrcLibResult(
                syncedLyrics = obj.optString("syncedLyrics").takeIf { it.isNotBlank() },
                plainLyrics = obj.optString("plainLyrics").takeIf { it.isNotBlank() },
                trackName = obj.optString("trackName"),
                artistName = obj.optString("artistName"),
                albumName = obj.optString("albumName"),
                duration = obj.optInt("duration", 0)
            )
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Lyrics fetch failed for \"$title\" by $artist: ${e.message}")
            null
        }
    }

    /** Fuzzy search — artist/album bilinmiyorsa sadece title ile de çalışır */
    suspend fun searchLyrics(query: String): LrcLibResult? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$BASE/search?q=${enc(query)}&limit=1")
                .header("Lrclib-Client", "Sonara/1.0.0 (contact@sonara.app)")
                .build()
            val body = http.newCall(req).execute().use { it.body?.string() }
            if (body.isNullOrBlank()) return@withContext null

            val arr = org.json.JSONArray(body)
            if (arr.length() == 0) return@withContext null
            val obj = arr.getJSONObject(0)

            LrcLibResult(
                syncedLyrics = obj.optString("syncedLyrics").takeIf { it.isNotBlank() },
                plainLyrics = obj.optString("plainLyrics").takeIf { it.isNotBlank() },
                trackName = obj.optString("trackName"),
                artistName = obj.optString("artistName"),
                albumName = obj.optString("albumName"),
                duration = obj.optInt("duration", 0)
            )
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Lyrics search failed: ${e.message}")
            null
        }
    }
}
