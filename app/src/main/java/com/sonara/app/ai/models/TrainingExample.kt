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
