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

package com.sonara.app.autoeq

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sonara.app.data.SonaraLogger

/**
 * Loads 5621 headphone correction profiles from Wavelet AutoEQ database.
 * Profiles are stored as a JSON asset: headphone name → 10-band correction gains (dB).
 * Bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
 */
object WaveletAutoEqLoader {
    private const val TAG = "WaveletAutoEQ"
    private const val ASSET_FILE = "wavelet_autoeq.json"
    private var profiles: Map<String, List<Float>>? = null

    fun load(context: Context): Int {
        if (profiles != null) return profiles!!.size
        return try {
            val json = context.assets.open(ASSET_FILE).bufferedReader().readText()
            val type = object : TypeToken<Map<String, List<Float>>>() {}.type
            profiles = Gson().fromJson(json, type)
            SonaraLogger.i(TAG, "Loaded ${profiles!!.size} Wavelet profiles")
            profiles!!.size
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Failed to load Wavelet DB: ${e.message}")
            profiles = emptyMap()
            0
        }
    }

    fun findProfile(deviceName: String, context: Context): HeadphoneProfile? {
        load(context)
        val db = profiles ?: return null
        val name = deviceName.trim()
        if (name.isBlank()) return null
        val nameLower = name.lowercase()

        // 1. Exact match
        db[name]?.let {
            return HeadphoneProfile(name, it.toFloatArray(), 0.98f, "wavelet-exact")
        }

        // 2. Case-insensitive exact
        db.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.let {
            return HeadphoneProfile(it.key, it.value.toFloatArray(), 0.95f, "wavelet-exact-ci")
        }

        // 3. Substring match (device name contains profile name)
        db.entries.firstOrNull { nameLower.contains(it.key.lowercase()) }?.let {
            return HeadphoneProfile(it.key, it.value.toFloatArray(), 0.85f, "wavelet-contains")
        }

        // 4. Reverse match (profile name contains device name)
        db.entries.firstOrNull { it.key.lowercase().contains(nameLower) }?.let {
            return HeadphoneProfile(it.key, it.value.toFloatArray(), 0.80f, "wavelet-reverse")
        }

        // 5. Word matching (2+ words match)
        val deviceWords = nameLower.split(Regex("[\\s\\-_]+")).filter { it.length > 2 }
        db.entries.filter { entry ->
            val profileWords = entry.key.lowercase().split(Regex("[\\s\\-_]+"))
            deviceWords.count { dw -> profileWords.any { pw -> pw.contains(dw) || dw.contains(pw) } } >= 2
        }.maxByOrNull { entry ->
            val profileWords = entry.key.lowercase().split(Regex("[\\s\\-_]+"))
            deviceWords.count { dw -> profileWords.any { pw -> pw.contains(dw) } }
        }?.let {
            return HeadphoneProfile(it.key, it.value.toFloatArray(), 0.65f, "wavelet-words")
        }

        return null
    }

    fun searchProfiles(query: String, context: Context, limit: Int = 20): List<String> {
        load(context)
        val db = profiles ?: return emptyList()
        val q = query.lowercase().trim()
        if (q.isBlank()) return db.keys.take(limit).sorted()
        return db.keys.filter { it.lowercase().contains(q) }.take(limit).sorted()
    }

    fun profileCount(context: Context): Int {
        load(context)
        return profiles?.size ?: 0
    }
}
