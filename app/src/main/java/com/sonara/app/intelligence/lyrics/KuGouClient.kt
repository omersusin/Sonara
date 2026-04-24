package com.sonara.app.intelligence.lyrics

import android.util.Base64
import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * KuGou lyrics client — best coverage for Mandarin / Cantonese content.
 *
 * Flow:
 *   1. Search via mobileservice.kugou.com to obtain hash + album_audio_id
 *   2. Download LRC via lyrics.kugou.com (base64-encoded response)
 */
object KuGouClient {

    private const val TAG        = "KuGou"
    private const val SEARCH_URL = "https://mobileservice.kugou.com/api/v3/search/song"
    private const val LYRIC_URL  = "https://lyrics.kugou.com/download"

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("User-Agent", "AndroidApp/9108")
                .build())
        }
        .build()

    /**
     * Returns the raw LRC string, or null when not found.
     */
    suspend fun getRawLyrics(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            // Step 1: search
            val keyword   = "${artist.trim()} ${title.trim()}"
            val searchUrl = "$SEARCH_URL?keyword=${enc(keyword)}" +
                "&page=1&pagesize=8&showtype=1&platform=AndroidFilter&version=9108"
            val searchBody = http.newCall(Request.Builder().url(searchUrl).build())
                .execute().use { it.body?.string() }
            if (searchBody.isNullOrBlank()) return@withContext null

            val info = JSONObject(searchBody)
                .optJSONObject("data")
                ?.optJSONArray("info")
                ?: return@withContext null
            if (info.length() == 0) return@withContext null

            val first   = info.getJSONObject(0)
            val hash    = first.optString("hash").takeIf { it.isNotBlank() } ?: return@withContext null
            val albumId = first.optInt("album_audio_id", 0)

            // Step 2: download lyrics
            val lyricUrl  = "$LYRIC_URL?ver=1&client=pc&id=$albumId&accesskey=$hash&fmt=lrc&charset=utf8"
            val lyricBody = http.newCall(Request.Builder().url(lyricUrl).build())
                .execute().use { it.body?.string() }
            if (lyricBody.isNullOrBlank()) return@withContext null

            val content = JSONObject(lyricBody).optString("content").takeIf { it.isNotBlank() }
                ?: return@withContext null

            // KuGou lyrics content is base64-encoded
            val decoded = try {
                String(Base64.decode(content, Base64.DEFAULT), Charsets.UTF_8)
            } catch (_: Exception) { content }

            decoded.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "getRawLyrics failed for \"$title\" by $artist: ${e.message}")
            null
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
