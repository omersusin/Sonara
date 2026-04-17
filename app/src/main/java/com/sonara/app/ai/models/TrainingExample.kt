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

package com.sonara.app.ai.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sonara_training_examples")
data class TrainingExample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val featureVector: String,
    val genre: String,
    val moodValence: Float,
    val moodArousal: Float,
    val energy: Float,
    val source: String,
    val trackTitle: String = "",
    val trackArtist: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val useCount: Int = 0
) {
    fun getFeatureArray(): FloatArray {
        return try {
            AudioFeatureVector.stringToVector(featureVector)
        } catch (e: Exception) {
            FloatArray(AudioFeatureVector.SIZE)
        }
    }

    companion object {
        fun create(
            features: FloatArray,
            genre: String,
            valence: Float,
            arousal: Float,
            energy: Float,
            source: String,
            title: String = "",
            artist: String = ""
        ): TrainingExample = TrainingExample(
            featureVector = AudioFeatureVector.vectorToString(features),
            genre = genre.lowercase().trim(),
            moodValence = valence,
            moodArousal = arousal,
            energy = energy,
            source = source,
            trackTitle = title,
            trackArtist = artist
        )
    }
}
