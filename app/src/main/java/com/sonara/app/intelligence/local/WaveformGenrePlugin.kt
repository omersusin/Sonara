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

package com.sonara.app.intelligence.local

import android.content.Context
import android.util.Log
import com.sonara.app.data.models.TrackInfo

/**
 * Stub for future on-device waveform genre classification.
 * When a real TFLite model (e.g. GTZAN-trained) is placed in assets/genre_model.tflite,
 * this plugin will classify audio clips into genres.
 *
 * Currently returns null — metadata heuristic handles everything.
 */
class WaveformGenrePlugin(private val context: Context) : LocalInferencePlugin {
    override val name = "WaveformGenre"
    override val priority = 20  // tried after metadata

    private var isModelAvailable = false

    fun init() {
        // Check if model file exists in assets
        isModelAvailable = try {
            context.assets.open("genre_model.tflite").close()
            Log.d("WaveformPlugin", "Genre model found in assets")
            true
        } catch (e: Exception) {
            Log.d("WaveformPlugin", "No genre model — waveform classification disabled")
            false
        }
    }

    override suspend fun resolve(title: String, artist: String, audioContext: AudioContext?): TrackInfo? {
        if (!isModelAvailable) return null
        val pcm = audioContext?.pcm16 ?: return null
        if (pcm.isEmpty()) return null

        // TODO: When model is available:
        // 1. Convert pcm16 to TensorAudio
        // 2. Run inference
        // 3. Map label to genre
        // 4. Return TrackInfo with confidence

        return null  // Not implemented yet
    }
}
