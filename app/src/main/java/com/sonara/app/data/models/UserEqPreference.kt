package com.sonara.app.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_eq_preferences", indices = [Index(value = ["genre", "audioRoute"])])
data class UserEqPreference(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val genre: String,
    val mood: String? = null,
    val energy: Float = 0.5f,
    val audioRoute: String,
    val bandLevels: String,
    val usageCount: Int = 1,
    val lastUsed: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
