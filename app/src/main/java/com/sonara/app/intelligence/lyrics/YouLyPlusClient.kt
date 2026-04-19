package com.sonara.app.intelligence.lyrics

import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * YouLyPlus — queries multiple mirror servers for synced LRC lyrics.
 * Supports both standard `syncedLyrics` LRC strings and KPoe line arrays.
 */
object YouLyPlusClient {

    private const val TAG = "YouLyPlus"

    private val SERVERS = listOf(
        "https://lyrics.run",
        "https://lyricsplus.vercel.app",
        "https://lyricsplus.fly.dev",
        "https://lyricsplus-mirror.vercel.app"
    )

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Tries each mirror in order and returns the first valid raw LRC string.
     * Returns null when every server fails or returns no lyrics.
     */
    suspend fun getRawLyrics(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        for (server in SERVERS) {
            val raw = tryServer(server, title, artist)
            if (raw != null) return@withContext raw
        }
        SonaraLogger.w(TAG, "All servers failed for \"$title\" by $artist")
        null
    }

    private fun tryServer(base: String, title: String, artist: String): String? {
        return try {
            val url  = "$base/api/lyrics?title=${enc(title)}&artist=${enc(artist)}"
            val body = http.newCall(Request.Builder().url(url).build())
                .execute().use { it.body?.string() }
            if (body.isNullOrBlank()) return null

            val json = JSONObject(body)

            // Standard LRC string
            val synced = json.optString("syncedLyrics").takeIf { it.isNotBlank() }
            if (synced != null) return synced

            // KPoe-style line array: [{startTimeMs, words}, ...]
            val lines = json.optJSONArray("lines")
            if (lines != null) return kpoeToLrc(lines)

            null
        } catch (_: Exception) { null }
    }

    /** Converts a KPoe line array to a standard LRC string. */
    private fun kpoeToLrc(lines: JSONArray): String? {
        val sb = StringBuilder()
        for (i in 0 until lines.length()) {
            val line    = lines.optJSONObject(i) ?: continue
            val startMs = line.optLong("startTimeMs", -1L)
            if (startMs < 0) continue
            val text = line.optString("words").trim()
            if (text.isBlank()) continue
            val min = startMs / 60_000L
            val sec = (startMs % 60_000L) / 1000L
            val cs  = (startMs % 1000L) / 10L
            sb.appendLine("[%02d:%02d.%02d]%s".format(min, sec, cs, text))
        }
        return sb.toString().takeIf { it.isNotBlank() }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
