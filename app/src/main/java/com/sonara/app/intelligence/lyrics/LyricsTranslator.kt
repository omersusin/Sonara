package com.sonara.app.intelligence.lyrics

import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object LyricsTranslator {

    private const val TAG = "LyricsTranslator"
    private const val BASE = "https://api.mymemory.translated.net/get"
    // Most lyrics are English; fixed source avoids autodetect failures on short/unusual text
    private const val SOURCE_LANG = "en"

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun translate(lines: List<String>, targetLang: String): List<String>? =
        withContext(Dispatchers.IO) {
            if (lines.isEmpty() || targetLang.isBlank()) return@withContext null
            if (targetLang == SOURCE_LANG) return@withContext null

            try {
                val result = mutableListOf<String>()

                // 10 lines per batch keeps rate-limit usage manageable and preserves line counts
                for (batch in lines.chunked(10)) {
                    val batchText = batch.joinToString(" ||| ")
                    val encoded = URLEncoder.encode(batchText, "UTF-8")
                    val url = "$BASE?q=$encoded&langpair=$SOURCE_LANG|$targetLang"

                    val body = try {
                        http.newCall(Request.Builder().url(url).build())
                            .execute().use { it.body?.string() }
                    } catch (e: Exception) {
                        SonaraLogger.w(TAG, "Batch request failed: ${e.message}")
                        null
                    }

                    if (body == null) {
                        result.addAll(batch)
                        continue
                    }

                    val json = JSONObject(body)
                    if (json.optInt("responseStatus", 0) != 200) {
                        SonaraLogger.w(TAG, "MyMemory error ${json.optInt("responseStatus")} for batch")
                        result.addAll(batch)
                        continue
                    }

                    val translated = json.getJSONObject("responseData").getString("translatedText")
                    val translatedBatch = translated.split("|||").map { it.trim() }

                    if (translatedBatch.size == batch.size) {
                        result.addAll(translatedBatch)
                    } else {
                        // Line count mismatch — keep originals rather than show garbled text
                        SonaraLogger.w(TAG, "Line count mismatch: sent ${batch.size}, got ${translatedBatch.size}")
                        result.addAll(batch)
                    }
                }

                if (result.size == lines.size) result else null
            } catch (e: Exception) {
                SonaraLogger.w(TAG, "Translation failed: ${e.message}")
                null
            }
        }
}
