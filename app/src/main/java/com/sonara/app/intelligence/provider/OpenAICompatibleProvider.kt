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
 * OpenAI-compatible provider for OpenRouter and Groq.
 * Both use the same chat/completions endpoint format.
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

        private fun buildPrompt(r: InsightRequest): String {
            val userFeedback = if (r.lyricalTone != null && !r.lyricalTone.matches(Regex("^(unknown|happy|sad|energetic|calm|neutral)$"))) {
                "\nUser feedback: \"${r.lyricalTone}\" — adjust EQ bands accordingly."
            } else ""
            return """You are Sonara, an AI-powered EQ engine. Respond ONLY with valid JSON.
Track: ${r.title} by ${r.artist}
Genre: ${r.genre}, Tags: ${r.tags.joinToString(", ")}
Energy: ${r.energy}, Current EQ: ${r.currentEqBands?.joinToString(",") ?: "flat"}$userFeedback

Return JSON with:
- "summary": 1 sentence about what you changed
- "eq_adjustment": array of 10 floats (31Hz,62Hz,125Hz,250Hz,500Hz,1kHz,2kHz,4kHz,8kHz,16kHz) each -12 to +12 dB
- "preamp": float -6 to +6
- "confidence_note": brief note"""
        }
    }
}
