package com.sonara.app.ai.cloud

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GitHubSync(private val context: Context, private val queue: ContributionQueue) {
    companion object {
        private const val TAG = "SonaraGitHub"
        private const val REPO_OWNER = "omersusin"
        private const val REPO_NAME = "sonara-models"
        private const val BRANCH = "main"
        private const val API_BASE = "https://api.github.com"
        private const val RAW_BASE = "https://raw.githubusercontent.com"
        private const val CONTRIBUTIONS_DIR = "contributions"
        private const val TIMEOUT_MS = 15000
    }
    private val prefs = context.getSharedPreferences("sonara_github", Context.MODE_PRIVATE)
    private val anonymousId = AnonymousIdentity.getId(context)

    suspend fun checkAndDownloadPrototypes(): Int = withContext(Dispatchers.IO) {
        try {
            val cv = prefs.getInt("version", 0)
            val vJson = httpGet("$RAW_BASE/$REPO_OWNER/$REPO_NAME/$BRANCH/version.json") ?: return@withContext 0
            val rv = JSONObject(vJson).optInt("version", 0)
            if (rv <= cv) return@withContext 0
            val pJson = httpGet("$RAW_BASE/$REPO_OWNER/$REPO_NAME/$BRANCH/prototypes.json") ?: return@withContext 0
            // Basic integrity check
            if (!pJson.trimStart().startsWith("[") || pJson.length < 100) {
                Log.w(TAG, "prototypes.json failed integrity check, skipping")
                return@withContext 0
            }
            val protos = parsePrototypes(pJson)
            if (protos.isNotEmpty()) prefs.edit().putInt("version", rv).apply()
            protos.size
        } catch (e: Exception) { Log.d(TAG, "Check failed: ${e.message}"); 0 }
    }

    fun parsePrototypes(json: String): List<com.sonara.app.ai.models.TrainingExample> {
        val result = mutableListOf<com.sonara.app.ai.models.TrainingExample>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i); val fa = o.getJSONArray("features")
                val features = FloatArray(fa.length()) { fa.getDouble(it).toFloat() }
                result.add(com.sonara.app.ai.models.TrainingExample.create(features = features, genre = o.getString("genre"),
                    valence = o.optDouble("valence", 0.0).toFloat(), arousal = o.optDouble("arousal", 0.5).toFloat(),
                    energy = o.optDouble("energy", 0.5).toFloat(), source = "prototype"))
            }
        } catch (e: Exception) { Log.e(TAG, "Parse: ${e.message}") }
        return result
    }

    suspend fun uploadContributions(githubToken: String): Int = withContext(Dispatchers.IO) {
        if (!queue.isEnabled) return@withContext 0
        val contributions = queue.getAll(); if (contributions.length() == 0) return@withContext 0
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val filePath = "$CONTRIBUTIONS_DIR/${anonymousId}_${today}.json"
            val content = JSONObject().apply { put("id", anonymousId); put("date", today); put("count", contributions.length()); put("contributions", contributions) }.toString(2)
            val b64 = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val sha = getFileSha(filePath, githubToken)
            val body = JSONObject().apply { put("message", "contribution: $anonymousId ($today)"); put("content", b64); put("branch", BRANCH); if (sha != null) put("sha", sha) }
            val url = "$API_BASE/repos/$REPO_OWNER/$REPO_NAME/contents/$filePath"
            val resp = httpPut(url, body.toString(), githubToken)
            if (resp != null) { val c = contributions.length(); queue.recordSent(c); queue.clear(); Log.d(TAG, "Uploaded $c"); c } else 0
        } catch (e: Exception) { Log.e(TAG, "Upload: ${e.message}"); 0 }
    }

    private fun getFileSha(path: String, token: String): String? {
        return try { val r = httpGetAuth("$API_BASE/repos/$REPO_OWNER/$REPO_NAME/contents/$path?ref=$BRANCH", token) ?: return null; JSONObject(r).optString("sha", null) } catch (_: Exception) { null }
    }

    private fun httpGet(url: String): String? {
        return try { val c = URL(url).openConnection() as HttpURLConnection; c.connectTimeout = TIMEOUT_MS; c.readTimeout = TIMEOUT_MS; if (c.responseCode != 200) { c.disconnect(); null } else { val r = c.inputStream.bufferedReader().readText(); c.disconnect(); r } } catch (_: Exception) { null }
    }
    private fun httpGetAuth(url: String, token: String): String? {
        return try { val c = URL(url).openConnection() as HttpURLConnection; c.connectTimeout = TIMEOUT_MS; c.readTimeout = TIMEOUT_MS; c.setRequestProperty("Authorization", "Bearer $token"); c.setRequestProperty("Accept", "application/vnd.github+json"); if (c.responseCode != 200) { c.disconnect(); null } else { val r = c.inputStream.bufferedReader().readText(); c.disconnect(); r } } catch (_: Exception) { null }
    }
    private fun httpPut(url: String, body: String, token: String): String? {
        return try { val c = URL(url).openConnection() as HttpURLConnection; c.requestMethod = "PUT"; c.connectTimeout = TIMEOUT_MS; c.readTimeout = TIMEOUT_MS; c.setRequestProperty("Authorization", "Bearer $token"); c.setRequestProperty("Accept", "application/vnd.github+json"); c.setRequestProperty("Content-Type", "application/json"); c.doOutput = true; c.outputStream.bufferedWriter().use { it.write(body) }; if (c.responseCode in 200..201) { val r = c.inputStream.bufferedReader().readText(); c.disconnect(); r } else { c.disconnect(); null } } catch (_: Exception) { null }
    }
}
