package com.sonara.app.ai.classifier

import android.util.Log
import com.sonara.app.ai.models.TrainingExample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object PrototypeDownloader {
    private const val TAG = "SonaraDownloader"

    suspend fun download(url: String): List<TrainingExample> = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000; conn.readTimeout = 15000
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext emptyList() }
            val json = conn.inputStream.bufferedReader().readText(); conn.disconnect()
            parseJson(json).also { Log.d(TAG, "Downloaded ${it.size} prototypes") }
        } catch (e: Exception) { Log.d(TAG, "Download failed: ${e.message}"); emptyList() }
    }

    private fun parseJson(json: String): List<TrainingExample> {
        val result = mutableListOf<TrainingExample>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val fArr = obj.getJSONArray("features")
                val features = FloatArray(fArr.length()) { fArr.getDouble(it).toFloat() }
                result.add(TrainingExample.create(features = features, genre = obj.getString("genre"),
                    valence = obj.optDouble("valence", 0.0).toFloat(),
                    arousal = obj.optDouble("arousal", 0.5).toFloat(),
                    energy = obj.optDouble("energy", 0.5).toFloat(), source = "prototype"))
            }
        } catch (e: Exception) { Log.e(TAG, "Parse error", e) }
        return result
    }
}
