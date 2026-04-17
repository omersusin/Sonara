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

package com.sonara.app.ai.personalization

import android.content.Context
import android.util.Log
import com.sonara.app.ai.eq.SmartEqGenerator
import com.sonara.app.ai.models.SonaraAiResult
import org.json.JSONObject
import kotlin.math.abs

class Personalizer(context: Context) {
    companion object {
        private const val TAG = "SonaraPersonal"
        private const val PREFS = "sonara_personalization"
        private const val ALPHA = 0.25f
        private const val MAX_BAND_DELTA = 6f
        private const val MAX_TOTAL_DRIFT = 40f
    }
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun applyPersonalization(eq: SmartEqGenerator.EqResult, result: SonaraAiResult, route: String): SmartEqGenerator.EqResult {
        val key = clusterKey(result, route)
        val deltas = getDeltas(key) ?: return eq
        val newBands = FloatArray(eq.bands.size) { i -> (eq.bands[i] + (deltas.getOrNull(i) ?: 0f)).coerceIn(-12f, 12f) }
        return SmartEqGenerator.EqResult(newBands, eq.preamp, eq.isSpectralBased)
    }

    fun recordFeedback(feedbackType: String, result: SonaraAiResult, route: String) {
        val key = clusterKey(result, route)
        val adjustment = feedbackToDeltas(feedbackType)
        val existing = getDeltas(key) ?: FloatArray(10)
        val updated = FloatArray(10) { i -> (ALPHA * adjustment[i] + (1 - ALPHA) * existing[i]).coerceIn(-MAX_BAND_DELTA, MAX_BAND_DELTA) }
        val totalDrift = updated.sumOf { abs(it.toDouble()) }.toFloat()
        if (totalDrift > MAX_TOTAL_DRIFT) {
            Log.w(TAG, "Drift too high ($totalDrift), clamping")
            for (i in updated.indices) updated[i] *= (MAX_TOTAL_DRIFT / totalDrift)
        }
        saveDeltas(key, updated); Log.d(TAG, "Feedback: $feedbackType -> $key")
    }

    fun reset() { prefs.edit().clear().apply(); Log.d(TAG, "Personalization reset") }

    private fun clusterKey(result: SonaraAiResult, route: String): String {
        val genre = result.primaryGenre.lowercase()
        val mq = when { result.mood.valence > 0 && result.mood.arousal > 0.5f -> "be"; result.mood.valence > 0 -> "bc"; result.mood.arousal > 0.5f -> "de"; else -> "dc" }
        val r = when { route.contains("bluetooth", true) -> "bt"; route.contains("speaker", true) -> "sp"; route.contains("wired", true) -> "wd"; else -> "ot" }
        return "${genre}_${mq}_$r"
    }

    private fun feedbackToDeltas(type: String): FloatArray = when (type) {
        "too_bassy" -> floatArrayOf(-3f,-2f,-1f,0f,0f,0f,0f,0f,0f,0f)
        "too_bright" -> floatArrayOf(0f,0f,0f,0f,0f,0f,-1f,-2f,-2.5f,-3f)
        "too_thin" -> floatArrayOf(2f,1.5f,1f,0.5f,0f,0f,0f,0f,0f,0f)
        "too_harsh" -> floatArrayOf(0f,0f,0f,0f,0f,-1f,-2f,-1.5f,-1f,0f)
        "too_flat" -> floatArrayOf(1.5f,1f,0f,0f,-0.5f,-0.5f,0f,0f,1f,1.5f)
        "prefer_warmer" -> floatArrayOf(1f,0.5f,0.5f,0f,0f,0f,0f,-0.5f,-1f,-1f)
        "prefer_clearer" -> floatArrayOf(-0.5f,0f,0f,0f,0f,0.5f,1f,1f,0.5f,0f)
        "perfect" -> FloatArray(10)
        else -> FloatArray(10)
    }

    private fun getDeltas(key: String): FloatArray? {
        val json = prefs.getString(key, null) ?: return null
        return try { val obj = JSONObject(json); FloatArray(10) { i -> obj.optDouble("b$i", 0.0).toFloat() } } catch (_: Exception) { null }
    }

    private fun saveDeltas(key: String, deltas: FloatArray) {
        val json = JSONObject(); for (i in deltas.indices) json.put("b$i", deltas[i].toDouble())
        prefs.edit().putString(key, json.toString()).apply()
    }
}
