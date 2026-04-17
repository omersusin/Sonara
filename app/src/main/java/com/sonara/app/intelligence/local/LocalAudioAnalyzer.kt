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

import com.sonara.app.data.models.TrackInfo

class LocalAudioAnalyzer {
    private val extractor = AudioFeatureExtractor()

    fun analyze(title: String, artist: String): TrackInfo {
        val features = extractor.extract(title, artist)
        return TrackInfo(
            title = title,
            artist = artist,
            genre = features.estimatedGenre,
            mood = features.estimatedMood,
            energy = features.estimatedEnergy,
            confidence = features.confidence,
            source = "local-ai"
        )
    }

    fun suggestEqBands(features: AudioFeatures): FloatArray {
        val bands = FloatArray(10)
        // 31Hz
        bands[0] = lerp(-2f, 6f, features.bassNeed)
        // 62Hz
        bands[1] = lerp(-1.5f, 5.5f, features.bassNeed)
        // 125Hz
        bands[2] = lerp(-1f, 4f, features.bassNeed * 0.8f)
        // 250Hz
        bands[3] = lerp(-1f, 2f, features.vocalPresence * 0.5f)
        // 500Hz
        bands[4] = lerp(-2f, 3f, features.vocalPresence * 0.6f)
        // 1KHz
        bands[5] = lerp(-1.5f, 3.5f, features.vocalPresence * 0.7f)
        // 2KHz
        bands[6] = lerp(-1f, 3f, features.brightness * 0.6f)
        // 4KHz
        bands[7] = lerp(-1f, 3.5f, features.brightness * 0.7f)
        // 8KHz
        bands[8] = lerp(-1.5f, 4f, features.trebleNeed * 0.8f)
        // 16KHz
        bands[9] = lerp(-2f, 4.5f, features.trebleNeed)

        return bands
    }

    private fun lerp(min: Float, max: Float, t: Float): Float = min + (max - min) * t.coerceIn(0f, 1f)
}
