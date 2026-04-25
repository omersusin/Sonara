package com.sonara.app.intelligence.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL

object LyricsTranslator {
    suspend fun translate(lines: List<String>, targetLang: String): List<String>? = withContext(Dispatchers.IO) {
        if (lines.isEmpty() || targetLang.isBlank()) return@withContext null
        try {
            // Batch lines joined with newline to minimize requests
            val batch = lines.joinToString("\n")
            val encoded = URLEncoder.encode(batch, "UTF-8")
            val url = URL("https://api.mymemory.translated.net/get?q=$encoded&langpair=autodetect|$targetLang")
            val response = url.readText()
            val json = JSONObject(response)
            val translated = json.getJSONObject("responseData").getString("translatedText")
            val translatedLines = translated.split("\n")
            // If we get the same number of lines, return them; otherwise return null
            if (translatedLines.size == lines.size) translatedLines else null
        } catch (_: Exception) { null }
    }
}
