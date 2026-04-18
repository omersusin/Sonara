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
class GeminiInsightEngine(private var apiKey: String = "") {

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    enum class GeminiModel(val id: String, val displayName: String) {
        FAST("gemini-2.5-flash-lite", "Fast"),
        BALANCED("gemini-2.5-flash", "Balanced"),
        STRONG("gemini-2.5-pro", "Strong")
    }

    data class GeminiInsight(
        val summary: String,
        val whyThisEq: String,
        val listeningFocus: String,
        val lyricalTone: String,
        val confidenceNote: String,
        val success: Boolean = true,
        val eqAdjustment: FloatArray? = null,
        val preamp: Float? = null,
        val bassBoost: Int? = null,
        val virtualizer: Int? = null,
        val loudness: Int? = null
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    var model: GeminiModel = GeminiModel.FAST
    var customModelId: String? = null

    private fun resolvedModelId(): String = customModelId?.takeIf { it.isNotBlank() } ?: model.id

    @Volatile private var quotaPausedUntil: Long = 0L
    private val QUOTA_BACKOFF_MS = 5 * 60 * 1000L // 5 minutes

    fun updateApiKey(key: String) { apiKey = key }
    fun isEnabled(): Boolean = apiKey.isNotBlank()

    /**
     * Track bilgilerinden insight üret.
     * Gemini'ye ham track değil, özet JSON verilir.
     */
    suspend fun getInsight(
        title: String, artist: String, genre: String, subGenre: String?,
        tags: List<String>, lyricalTone: String?, energy: Float,
        confidence: Float, currentEqBands: FloatArray?,
        userRequest: String? = null,
        currentPreamp: Float = 0f,
        currentBassBoost: Int = 0,
        currentVirtualizer: Int = 0,
        currentLoudness: Int = 0
    ): GeminiInsight {
        if (apiKey.isBlank()) return GeminiInsight("", "", "", "", "", false)
        if (System.currentTimeMillis() < quotaPausedUntil) return GeminiInsight("", "", "", "", "", false)

        return withContext(Dispatchers.IO) {
            try {
                val currentBandsStr = currentEqBands?.joinToString(",") { "%.1f".format(java.util.Locale.US, it) } ?: "0,0,0,0,0,0,0,0,0,0"
                val userMsg = if (!userRequest.isNullOrBlank()) {
                    "\nUSER REQUEST: \"$userRequest\" — modify the CURRENT state below to satisfy this request."
                } else ""
                val prompt = """You are Sonara, an AI-powered music EQ engine. Respond ONLY with valid JSON, no markdown backticks.

Track: $title by $artist
Genre: $genre, Subgenre: ${subGenre ?: ""}, Tags: ${tags.joinToString(", ")}
Lyrics tone: ${lyricalTone ?: "unknown"}
Energy: $energy, Confidence: $confidence

CURRENT STATE (values currently applied to the audio output):
- eq_bands: [$currentBandsStr]   (10 values in dB for 31Hz,62Hz,125Hz,250Hz,500Hz,1kHz,2kHz,4kHz,8kHz,16kHz)
- preamp: ${"%.1f".format(java.util.Locale.US, currentPreamp)} dB
- bass_boost: $currentBassBoost   (0-1000, currently applied)
- virtualizer: $currentVirtualizer   (0-1000, currently applied)
- loudness: $currentLoudness   (0-3000 centibels, currently applied)$userMsg

RULES — VERY IMPORTANT:
1. Return ABSOLUTE target values, not deltas. "more X" means start from CURRENT X and increase.
2. Preserve any field you don't intend to change by echoing its CURRENT value — NEVER default to 0.
3. "more loudness"/"louder"/"yüksek"/"daha yüksek" → raise loudness significantly above current (e.g. current+800, cap 3000) and nudge 31-250Hz bands +1 to +2 dB.
4. "more bass"/"bas fazla"/"daha bas" → raise bass_boost above current (e.g. current+300, cap 1000) AND raise 31-250Hz bands +2 to +4 dB.
5. "more treble"/"tiz"/"daha parlak" → raise 4-16kHz bands +2 to +4 dB.
6. "wider"/"more space"/"genişlet" → raise virtualizer above current (e.g. current+400, cap 1000).
7. "clearer"/"temiz"/"daha net" → boost 2-8kHz +2 to +4 dB, reduce 125-500Hz by -1 to -2 dB.
8. "less X"/"reduce X" → the inverse: reduce below current.
9. No user request → pick settings tuned for the genre/tags, keep changes smooth.

Return JSON with exactly these keys:
- "summary": 1-2 sentence description of what you changed
- "why_this_eq": Why these settings suit this track
- "listening_focus": What to listen for
- "lyrical_tone": Brief note on lyrical mood/theme
- "confidence_note": How confident the analysis is
- "eq_adjustment": array of 10 floats, each -12 to +12 dB (ABSOLUTE targets)
- "preamp": float -6 to +6 (absolute target)
- "bass_boost": int 0-1000 (absolute target — MUST echo current if not changing)
- "virtualizer": int 0-1000 (absolute target — MUST echo current if not changing)
- "loudness": int 0-3000 (absolute target — MUST echo current if not changing)"""

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

                val url = "$BASE_URL/${resolvedModelId()}:generateContent"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("X-Goog-Api-Key", apiKey)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(body)

                if (!json.has("candidates") || json.getJSONArray("candidates").length() == 0) {
                    // Quota exceeded → backoff 5 minutes
                    if (json.has("error") && json.toString().contains("quota", ignoreCase = true)) {
                        quotaPausedUntil = System.currentTimeMillis() + QUOTA_BACKOFF_MS
                        SonaraLogger.w("Gemini", "Quota exceeded, pausing for 5 minutes")
                    }
                    val errMsg = if (json.has("error")) json.getJSONObject("error").optString("message", "Unknown API error") else "No candidates in response"
                    SonaraLogger.w("Gemini", "API: $errMsg")
                    return@withContext GeminiInsight("", "", "", "", "", false)
                }
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
                val eqArr = result.optJSONArray("eq_adjustment")?.let { a ->
                    if (a.length() >= 10) FloatArray(10) { i -> a.optDouble(i, 0.0).toFloat().coerceIn(-12f, 12f) } else null
                }
                GeminiInsight(
                    summary = result.optString("summary", ""),
                    whyThisEq = result.optString("why_this_eq", ""),
                    listeningFocus = result.optString("listening_focus", ""),
                    lyricalTone = result.optString("lyrical_tone", ""),
                    confidenceNote = result.optString("confidence_note", ""),
                    eqAdjustment = eqArr,
                    preamp = if (result.has("preamp")) result.optDouble("preamp", 0.0).toFloat().coerceIn(-6f, 6f) else null,
                    bassBoost = if (result.has("bass_boost")) result.optInt("bass_boost", 0).coerceIn(0, 1000) else null,
                    virtualizer = if (result.has("virtualizer")) result.optInt("virtualizer", 0).coerceIn(0, 1000) else null,
                    loudness = if (result.has("loudness")) result.optInt("loudness", 0).coerceIn(0, 3000) else null
                )
            } catch (e: Exception) {
                SonaraLogger.w("Gemini", "Insight error: ${e.message}")
                GeminiInsight("", "", "", "", "", false)
            }
        }
    }
}
