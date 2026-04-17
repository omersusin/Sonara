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

package com.sonara.app.ai.cloud

import android.content.Context
import android.util.Log
import com.sonara.app.ai.models.AudioFeatureVector
import com.sonara.app.ai.models.SonaraMood
import org.json.JSONArray
import org.json.JSONObject

class ContributionQueue(context: Context) {
    companion object {
        private const val TAG = "SonaraQueue"; private const val PREFS = "sonara_contributions"
        private const val KEY_QUEUE = "queue"; private const val KEY_ENABLED = "enabled"
        private const val KEY_TOTAL_SENT = "total_sent"; private const val MAX_QUEUE_SIZE = 200
    }
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    fun enqueue(features: AudioFeatureVector, genre: String, mood: SonaraMood, energy: Float, sourceType: String) {
        if (!isEnabled) return
        val c = JSONObject().apply {
            put("features", JSONArray().apply { features.toFloatArray().forEach { put(it.toDouble()) } })
            put("genre", genre.lowercase().trim()); put("valence", "%.3f".format(mood.valence).toDouble())
            put("arousal", "%.3f".format(mood.arousal).toDouble()); put("energy", "%.3f".format(energy).toDouble())
            put("source", sourceType); put("timestamp", (System.currentTimeMillis() / 3600000) * 3600000) // rounded to hour
        }
        val q = getQueue(); while (q.length() >= MAX_QUEUE_SIZE) q.remove(0)
        q.put(c); saveQueue(q); Log.d(TAG, "Enqueued: $genre ($sourceType). Size: ${q.length()}")
    }

    fun getAll(): JSONArray = getQueue()
    fun size(): Int = getQueue().length()
    fun clear() { saveQueue(JSONArray()) }
    fun recordSent(count: Int) { prefs.edit().putInt(KEY_TOTAL_SENT, prefs.getInt(KEY_TOTAL_SENT, 0) + count).apply() }
    fun getTotalSent(): Int = prefs.getInt(KEY_TOTAL_SENT, 0)
    fun enqueueFeedback(trackKey: String, feedback: String, eqBands: FloatArray, preamp: Float) {
        if (!isEnabled) return
        val entry = JSONObject().apply {
            put("type", "feedback")
            put("track", trackKey)
            put("feedback", feedback)
            put("eq", JSONArray().apply { eqBands.forEach { put(it.toDouble()) } })
            put("preamp", preamp.toDouble())
            put("timestamp", (System.currentTimeMillis() / 3600000) * 3600000)
        }
        val q = getQueue(); while (q.length() >= MAX_QUEUE_SIZE) q.remove(0)
        q.put(entry); saveQueue(q)
        Log.d(TAG, "Feedback enqueued: $trackKey → $feedback")
    }

    fun reset() { prefs.edit().clear().apply() }
    private fun getQueue(): JSONArray { val j = prefs.getString(KEY_QUEUE, "[]") ?: "[]"; return try { JSONArray(j) } catch (_: Exception) { JSONArray() } }
    private fun saveQueue(q: JSONArray) { prefs.edit().putString(KEY_QUEUE, q.toString()).apply() }
}
