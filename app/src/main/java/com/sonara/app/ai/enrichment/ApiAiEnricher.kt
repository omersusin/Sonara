package com.sonara.app.ai.enrichment

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiAiEnricher(private val apiKey: String, private val endpoint: String, private val model: String = "gemini-2.0-flash") {
    companion object {
        private const val TAG = "SonaraApiAi"
        private const val TIMEOUT_MS = 12000
        private fun buildPrompt(title: String, artist: String): String = "Analyze this song and respond ONLY with JSON. No other text.\n\nSong: \"$title\" by $artist\n\nJSON format:\n{\"genres\":[\"primary\",\"secondary\"],\"mood_valence\":0.0,\"mood_arousal\":0.0,\"energy\":0.0,\"bpm_estimate\":0,\"key\":\"\"}\n\nRules: genres 1-3 lowercase, mood_valence -1 to 1, mood_arousal 0 to 1, energy 0 to 1"
    }

    suspend fun enrich(title: String, artist: String): EnrichmentSignal = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || title.isBlank()) return@withContext EnrichmentSignal.empty("api_ai")
        try {
            val response = callApi(buildPrompt(title, artist)) ?: return@withContext EnrichmentSignal.empty("api_ai")
            parseResponse(response)
        } catch (e: Exception) { Log.e(TAG, "Failed: ${e.message}"); EnrichmentSignal.empty("api_ai") }
    }

    private fun callApi(prompt: String): String? {
        val isGemini = endpoint.contains("generativeai") || endpoint.contains("gemini")
        val body = if (isGemini) {
            JSONObject().apply { put("contents", org.json.JSONArray().apply { put(JSONObject().apply { put("parts", org.json.JSONArray().apply { put(JSONObject().put("text", prompt)) }) }) }) }.toString()
        } else {
            JSONObject().apply { put("model", model); put("messages", org.json.JSONArray().apply { put(JSONObject().apply { put("role", "user"); put("content", prompt) }) }); put("temperature", 0.3); put("max_tokens", 200) }.toString()
        }
        val finalUrl = if (isGemini) "$endpoint/v1beta/models/$model:generateContent?key=$apiKey" else endpoint
        val conn = URL(finalUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"; conn.connectTimeout = TIMEOUT_MS; conn.readTimeout = TIMEOUT_MS
        conn.setRequestProperty("Content-Type", "application/json")
        if (!isGemini) conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.doOutput = true; conn.outputStream.bufferedWriter().use { it.write(body) }
        return try {
            if (conn.responseCode != 200) { Log.e(TAG, "API error: ${conn.responseCode}"); null }
            else {
                val r = conn.inputStream.bufferedReader().readText(); val json = JSONObject(r)
                if (isGemini) json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                else json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            }
        } finally { conn.disconnect() }
    }

    private fun parseResponse(text: String): EnrichmentSignal {
        return try {
            val jsonStr = text.replace("```json", "").replace("```", "").trim().let { raw -> val s = raw.indexOf('{'); val e = raw.lastIndexOf('}'); if (s >= 0 && e > s) raw.substring(s, e + 1) else raw }
            val json = JSONObject(jsonStr); val genreHints = mutableMapOf<String, Float>()
            val genres = json.optJSONArray("genres")
            if (genres != null) { for (i in 0 until genres.length()) { val g = genres.optString(i, "").lowercase().trim(); if (g.isNotBlank()) genreHints[g] = 1f - (i * 0.3f) } }
            val mv = json.optDouble("mood_valence", Double.NaN); val ma = json.optDouble("mood_arousal", Double.NaN); val en = json.optDouble("energy", Double.NaN)
            EnrichmentSignal(source = "api_ai", genreHints = genreHints, moodValence = if (!mv.isNaN()) mv.toFloat() else null, moodArousal = if (!ma.isNaN()) ma.toFloat() else null, energy = if (!en.isNaN()) en.toFloat() else null, confidence = if (genreHints.isNotEmpty()) 0.70f else 0.30f, isValid = genreHints.isNotEmpty())
        } catch (e: Exception) { Log.e(TAG, "Parse failed: ${e.message}"); EnrichmentSignal.empty("api_ai") }
    }
}
