package com.sonara.app.intelligence.adaptive

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sonara.app.intelligence.pipeline.AudioRoute
import com.sonara.app.intelligence.pipeline.Genre
import java.io.File

class AdaptiveLearningEngine(private val context: Context) {
    companion object { private const val FILE = "sonara_adaptive.json"; private const val LR = 0.2f; private const val BANDS = 10 }
    data class Profile(val offsets: List<Float>, val samples: Int, val updated: Long)

    private var profiles = mutableMapOf<String, Profile>()
    private val gson = Gson()

    fun load() { try { val f = File(context.filesDir, FILE); if (f.exists()) { val type = object : TypeToken<Map<String, Profile>>() {}.type; profiles = gson.fromJson(f.readText(), type) ?: mutableMapOf() } } catch (_: Exception) { profiles = mutableMapOf() } }
    private fun save() { try { File(context.filesDir, FILE).writeText(gson.toJson(profiles)) } catch (_: Exception) {} }

    fun getOffset(genre: Genre, route: AudioRoute): FloatArray? {
        val p = profiles["${genre.name}::${route.name}"] ?: return null
        return if (p.samples >= 2) p.offsets.toFloatArray() else null
    }

    fun recordFeedback(genre: Genre, route: AudioRoute, aiSuggestion: FloatArray, userFinal: FloatArray) {
        val k = "${genre.name}::${route.name}"
        val existing = profiles[k] ?: Profile(List(BANDS) { 0f }, 0, System.currentTimeMillis())
        val delta = FloatArray(BANDS) { i -> if (i < aiSuggestion.size && i < userFinal.size) userFinal[i] - aiSuggestion[i] else 0f }
        val newOffsets = List(BANDS) { i -> LR * delta[i] + (1f - LR) * existing.offsets[i] }
        profiles[k] = Profile(newOffsets, existing.samples + 1, System.currentTimeMillis())
        save()
    }

    fun getTotalSamples(): Int = profiles.values.sumOf { it.samples }
}
