package com.sonara.app.intelligence.gemini

import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Madde 11: Gemini insight/explanation katmanı.
 * Final EQ'yu tek başına üretmez — sadece insight, explanation, refinement yapar.
 */
class GeminiInsightEngine(private var apiKey: String = com.sonara.app.BuildConfig.GEMINI_API_KEY) {

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    enum class GeminiModel(val id: String, val displayName: String) {
        FAST("gemini-2.0-flash", "Fast"),
        BALANCED("gemini-1.5-flash", "Balanced"),
        STRONG("gemini-1.5-pro", "Strong")
    }

    data class GeminiInsight(
        val summary: String,
        val whyThisEq: String,
        val listeningFocus: String,
        val lyricalTone: String,
        val confidenceNote: String,
        val success: Boolean = true
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    var model: GeminiModel = GeminiModel.FAST

    fun updateApiKey(key: String) { apiKey = key }
    fun isEnabled(): Boolean = apiKey.isNotBlank()

    /**
     * Track bilgilerinden insight üret.
     * Gemini'ye ham track değil, özet JSON verilir.
     */
    suspend fun getInsight(
        title: String, artist: String, genre: String, subGenre: String?,
        tags: List<String>, lyricalTone: String?, energy: Float,
        confidence: Float, currentEqBands: FloatArray?
    ): GeminiInsight {
        if (apiKey.isBlank()) return GeminiInsight("", "", "", "", "", false)

        return withContext(Dispatchers.IO) {
            try {
                val trackJson = JSONObject().apply {
                    put("title", title)
                    put("artist", artist)
                    put("genre", genre)
                    put("subgenre", subGenre ?: "")
                    put("tags", JSONArray(tags))
                    put("lyrics_tone", lyricalTone ?: "unknown")
                    put("energy", energy)
                    put("confidence", confidence)
                    put("current_eq", currentEqBands?.joinToString(",") ?: "")
                }

                val prompt = """You are Sonara, an AI-powered music EQ engine. 
Given this track analysis, provide a JSON response with these exact fields:
- "summary": 1-2 sentence description of the track's character
- "why_this_eq": Why the current EQ settings suit this track  
- "listening_focus": What to listen for with these settings
- "lyrical_tone": Brief note on lyrical mood/theme
- "confidence_note": How confident the analysis is and why

Track data: $trackJson

Respond ONLY with valid JSON, no markdown backticks."""

                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put("text", prompt)
                        ))
                    ))
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.3)
                        put("maxOutputTokens", 500)
                    })
                }.toString()

                val url = "$BASE_URL/${model.id}:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(body)

                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
                    .removePrefix("```json")
                    .removeSuffix("```")
                    .trim()

                val result = JSONObject(text)
                GeminiInsight(
                    summary = result.optString("summary", ""),
                    whyThisEq = result.optString("why_this_eq", ""),
                    listeningFocus = result.optString("listening_focus", ""),
                    lyricalTone = result.optString("lyrical_tone", ""),
                    confidenceNote = result.optString("confidence_note", "")
                )
            } catch (e: Exception) {
                SonaraLogger.w("Gemini", "Insight error: ${e.message}")
                GeminiInsight("", "", "", "", "", false)
            }
        }
    }
}
