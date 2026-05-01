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
 * SimpMusic — YouTube Music TTML/LRC lyrics via the simpmusic.xyz proxy.
 * Returns the raw format string for LyricsHelper to parse.
 */
object SimpMusicClient {

    private const val TAG  = "SimpMusic"
    private const val BASE = "https://music.simpmusic.xyz/lyrics"

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("User-Agent", "Sonara/1.0 (contact@sonara.app)")
                .build())
        }
        .build()

    /**
     * Returns the raw lyrics string (TTML preferred, LRC fallback), or null when not found.
     */
    suspend fun getRawLyrics(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE?title=${enc(title)}&artist=${enc(artist)}"
            val body = http.newCall(Request.Builder().url(url).build())
                .execute().use { it.body?.string() }
            if (body.isNullOrBlank()) return@withContext null

            val json = JSONObject(body)
            if (json.optBoolean("error", false)) return@withContext null

            // Prefer TTML (word-level timestamps), fall back to LRC
            json.optString("ttml").takeIf { it.isNotBlank() }
                ?: json.optString("lrc").takeIf { it.isNotBlank() }
                ?: json.optString("syncedLyrics").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "getRawLyrics failed for \"$title\" by $artist: ${e.message}")
            null
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
