package com.sonara.app.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_feedback", indices = [Index(value = ["suggestedGenre"]), Index(value = ["timestamp"])])
data class UserFeedback(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackTitle: String? = null,
    val trackArtist: String? = null,
    val suggestedGenre: String,
    val correctedGenre: String? = null,
    val suggestedBands: String? = null,
    val correctedBands: String? = null,
    val accepted: Boolean = true,
    val audioRoute: String,
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
