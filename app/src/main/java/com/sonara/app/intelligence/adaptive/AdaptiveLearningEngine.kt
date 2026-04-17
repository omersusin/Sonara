/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sonara.app.intelligence.adaptive

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.pipeline.AudioRoute
import com.sonara.app.intelligence.pipeline.Genre
import java.io.File

/**
 * FIX: Gson Double→Int crash.
 * Gson reads all numbers as Double by default when using TypeToken.
 * Now uses safe Number→Int/Float conversion.
 */
class AdaptiveLearningEngine(private val context: Context) {
    companion object { private const val FILE = "sonara_adaptive.json"; private const val LR = 0.2f; private const val BANDS = 10 }
    data class Profile(val offsets: List<Float>, val samples: Int, val updated: Long)

    private var profiles = mutableMapOf<String, Profile>()
    private val gson = Gson()

    @Suppress("UNCHECKED_CAST")
    fun load() {
        try {
            val f = File(context.filesDir, FILE)
            if (!f.exists()) return
            val type = object : TypeToken<Map<String, Map<String, Any>>>() {}.type
            val raw: Map<String, Map<String, Any>> = gson.fromJson(f.readText(), type) ?: return
            profiles = mutableMapOf()
            for ((key, map) in raw) {
                try {
                    val offsets = (map["offsets"] as? List<*>)?.map { (it as Number).toFloat() } ?: List(BANDS) { 0f }
                    profiles[key] = Profile(
                        offsets = offsets,
                        samples = (map["samples"] as? Number)?.toInt() ?: 0,
                        updated = (map["updated"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            SonaraLogger.w("AdaptiveLearning", "Load error (clearing corrupted): ${e.message}")
            profiles = mutableMapOf()
            try { File(context.filesDir, FILE).delete() } catch (_: Exception) {}
        }
    }

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
