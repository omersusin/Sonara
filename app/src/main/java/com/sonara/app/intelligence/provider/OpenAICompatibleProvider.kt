package com.sonara.app.intelligence.provider

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
 * OpenAI-compatible provider for OpenRouter, Groq, and Hugging Face
 * (router.huggingface.co). All three speak the same chat/completions format.
 */
class OpenAICompatibleProvider(
    override val name: String,
    private val baseUrl: String,
    private var apiKey: String,
    private var model: String
) : AIInsightProvider {

    override val isConfigured: Boolean get() = apiKey.isNotBlank()

    @Volatile private var quotaPausedUntil: Long = 0L

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun updateConfig(key: String, modelId: String) {
        apiKey = key; model = modelId
    }

    override suspend fun getInsight(request: InsightRequest): InsightResult {
        if (!isConfigured) return InsightResult(provider = name)
        if (System.currentTimeMillis() < quotaPausedUntil) return InsightResult(provider = name)

        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(request)
                val body = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().put(
                        JSONObject().put("role", "user").put("content", prompt)
                    ))
                    put("temperature", 0.3)
                    put("max_tokens", 500)
                }.toString()

                val httpRequest = Request.Builder()
                    .url("$baseUrl/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(responseBody)

                if (json.has("error")) {
                    val errMsg = json.getJSONObject("error").optString("message", "Unknown error")
                    // Backoff on ANY API error — quota, rate limit, no endpoints, etc.
                    quotaPausedUntil = System.currentTimeMillis() + 5 * 60 * 1000L
                    SonaraLogger.w(name, "API error, pausing 5min: $errMsg")
                    return@withContext InsightResult(provider = name)
                }

                val choices = json.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    SonaraLogger.w(name, "No choices in response")
                    return@withContext InsightResult(provider = name)
                }

                val text = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
                    .removePrefix("```json").removeSuffix("```").trim()

                val result = JSONObject(text)
                InsightResult(
                    summary = result.optString("summary", ""),
                    whyThisEq = result.optString("why_this_eq", ""),
                    listeningFocus = result.optString("listening_focus", ""),
                    lyricalTone = result.optString("lyrical_tone", ""),
                    confidenceNote = result.optString("confidence_note", ""),
                    eqAdjustment = result.optJSONArray("eq_adjustment")?.let { a -> if (a.length() >= 10) FloatArray(10) { i -> a.optDouble(i, 0.0).toFloat().coerceIn(-12f, 12f) } else null },
                    preamp = if (result.has("preamp")) result.optDouble("preamp", 0.0).toFloat().coerceIn(-6f, 6f) else null,
                    bassBoost = if (result.has("bass_boost")) result.optInt("bass_boost", 0).coerceIn(0, 1000) else null,
                    virtualizer = if (result.has("virtualizer")) result.optInt("virtualizer", 0).coerceIn(0, 1000) else null,
                    loudness = if (result.has("loudness")) result.optInt("loudness", 0).coerceIn(0, 3000) else null,
                    success = true, provider = name
                )
            } catch (e: Exception) {
                SonaraLogger.w(name, "Error: ${e.message}")
                InsightResult(provider = name)
            }
        }
    }

    companion object {
        fun openRouter(apiKey: String, model: String = "google/gemini-2.5-flash") =
            OpenAICompatibleProvider("OpenRouter", "https://openrouter.ai/api/v1", apiKey, model)

        fun groq(apiKey: String, model: String = "llama-3.3-70b-versatile") =
            OpenAICompatibleProvider("Groq", "https://api.groq.com/openai/v1", apiKey, model)

        fun huggingFace(apiKey: String, model: String = "meta-llama/Meta-Llama-3.1-8B-Instruct") =
            OpenAICompatibleProvider("HuggingFace", "https://router.huggingface.co/v1", apiKey, model)

        private fun buildPrompt(r: InsightRequest): String {
            val userFeedback = if (!r.userRequest.isNullOrBlank()) {
                "\nUSER REQUEST: \"${r.userRequest}\" — modify the CURRENT state below to satisfy this request."
            } else if (r.lyricalTone != null && !r.lyricalTone.matches(Regex("^(unknown|happy|sad|energetic|calm|neutral)$"))) {
                "\nUSER FEEDBACK: \"${r.lyricalTone}\" — modify the CURRENT state below to satisfy this feedback."
            } else ""
            val currentBands = r.currentEqBands?.joinToString(",") { "%.1f".format(java.util.Locale.US, it) } ?: "0,0,0,0,0,0,0,0,0,0"
            return """You are Sonara, an AI-powered EQ engine. Respond ONLY with valid JSON, no markdown.

Track: ${r.title} by ${r.artist}
Genre: ${r.genre}, Tags: ${r.tags.joinToString(", ")}
Energy: ${r.energy}

CURRENT STATE (these are the values currently applied to the audio output):
- eq_bands: [$currentBands]   (10 values in dB for 31Hz,62Hz,125Hz,250Hz,500Hz,1kHz,2kHz,4kHz,8kHz,16kHz)
- preamp: ${"%.1f".format(java.util.Locale.US, r.currentPreamp)} dB
- bass_boost: ${r.currentBassBoost}   (0-1000, currently applied)
- virtualizer: ${r.currentVirtualizer}   (0-1000, currently applied)
- loudness: ${r.currentLoudness}   (0-3000 centibels, currently applied)$userFeedback

RULES — VERY IMPORTANT:
1. Return ABSOLUTE target values, not deltas. If the user asks for "more X", start from the current X and increase it.
2. Preserve any field you don't intend to change by echoing its CURRENT value — NEVER default to 0.
3. "more loudness"/"louder"/"yüksek sesli"/"daha yüksek" → raise loudness significantly above current (e.g. current+800, capped at 3000) and also nudge low bands (31-250Hz) +1 to +2 dB.
4. "more bass"/"bas fazla"/"daha bas" → raise bass_boost above current (e.g. current+300, capped at 1000) AND raise 31-250Hz bands +2 to +4 dB.
5. "more treble"/"tiz fazla"/"daha parlak" → raise 4-16kHz bands +2 to +4 dB.
6. "wider"/"more space"/"genişlet" → raise virtualizer above current (e.g. current+400, capped at 1000).
7. "clearer"/"temiz"/"daha net" → boost 2-8kHz bands +2 to +4 dB, slightly reduce 125-500Hz by -1 to -2 dB.
8. "less X" / "reduce X" → the inverse: reduce below current.
9. If there is NO user request, return settings tuned for the genre/tags, but keep changes smooth.

Return JSON with these exact keys:
- "summary": 1 sentence describing what you changed (and why)
- "eq_adjustment": array of 10 floats, each -12 to +12 dB (ABSOLUTE target values)
- "preamp": float -6 to +6 (absolute target)
- "bass_boost": int 0-1000 (absolute target — MUST echo current if not changing)
- "virtualizer": int 0-1000 (absolute target — MUST echo current if not changing)
- "loudness": int 0-3000 (absolute target — MUST echo current if not changing)
- "confidence_note": brief note"""
        }
    }
}
