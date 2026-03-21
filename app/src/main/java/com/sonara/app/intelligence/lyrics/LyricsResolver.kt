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
 * Madde 14: LRCLIB ücretsiz lyrics API.
 * API key gerektirmez. Lyrics bulunamazsa sessizce geçer.
 */
object LyricsResolver {

    private const val BASE_URL = "https://lrclib.net/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("User-Agent", "Sonara/1.0.0")
                .build())
        }
        .build()

    data class LyricsResult(
        val plainLyrics: String,
        val syncedLyrics: String? = null,
        val source: String = "lrclib"
    )

    /** Şarkı sözlerini al. Bulamazsa null döner — sistem bozulmaz. */
    suspend fun resolve(title: String, artist: String, duration: Long = 0): LyricsResult? {
        if (title.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                // Önce doğrudan eşleşme dene
                val direct = fetchDirect(title, artist, duration)
                if (direct != null) return@withContext direct

                // Sonra arama yap
                fetchSearch(title, artist)
            } catch (e: Exception) {
                SonaraLogger.w("Lyrics", "Resolve error: ${e.message}")
                null
            }
        }
    }

    private fun fetchDirect(title: String, artist: String, duration: Long): LyricsResult? {
        return try {
            val url = buildString {
                append("$BASE_URL/get?")
                append("track_name=${URLEncoder.encode(title, "UTF-8")}")
                append("&artist_name=${URLEncoder.encode(artist, "UTF-8")}")
                if (duration > 0) append("&duration=${duration / 1000}")
            }
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            parseLyrics(body)
        } catch (_: Exception) { null }
    }

    private fun fetchSearch(title: String, artist: String): LyricsResult? {
        return try {
            val query = URLEncoder.encode("$artist $title", "UTF-8")
            val url = "$BASE_URL/search?q=$query"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val array = org.json.JSONArray(body)
            if (array.length() == 0) return null
            parseLyrics(array.getJSONObject(0).toString())
        } catch (_: Exception) { null }
    }

    private fun parseLyrics(json: String): LyricsResult? {
        val obj = JSONObject(json)
        val plain = obj.optString("plainLyrics", "")
        if (plain.isBlank() || plain == "null") return null
        return LyricsResult(
            plainLyrics = plain,
            syncedLyrics = obj.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
        )
    }
}
