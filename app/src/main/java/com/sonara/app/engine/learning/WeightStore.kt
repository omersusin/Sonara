package com.sonara.app.engine.learning

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class WeightStore(context: Context) {
    private val file = File(context.filesDir, "classifier_weights.json")
    private val backup = File(context.filesDir, "classifier_weights.bak.json")

    suspend fun save(weights: Map<String, Map<String, Float>>) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            for ((g, kws) in weights) { val o = JSONObject(); for ((k, w) in kws) o.put(k, w.toDouble()); json.put(g, o) }
            val tmp = File(file.parent, "${file.name}.tmp"); tmp.writeText(json.toString(2))
            if (file.exists()) file.copyTo(backup, overwrite = true)
            tmp.renameTo(file)
        } catch (_: Exception) {}
    }

    suspend fun load(): Map<String, Map<String, Float>>? = withContext(Dispatchers.IO) {
        val src = when { file.exists() && file.length() > 2 -> file; backup.exists() && backup.length() > 2 -> backup; else -> return@withContext null }
        try {
            val json = JSONObject(src.readText()); val r = mutableMapOf<String, Map<String, Float>>()
            for (g in json.keys()) { val o = json.getJSONObject(g); val kws = mutableMapOf<String, Float>(); for (k in o.keys()) kws[k] = o.getDouble(k).toFloat(); r[g] = kws }
            r
        } catch (_: Exception) { null }
    }
}
