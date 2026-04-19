package com.sonara.app.intelligence.lyrics

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lyrics_cache",
    indices = [Index(value = ["cacheKey"], unique = true)]
)
data class LyricsCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cacheKey: String,          // "title|artist" normalized
    val syncedLyrics: String?,     // LRC text, null = unavailable
    val plainLyrics: String?,
    val source: String = "lrclib",
    val cachedAt: Long = System.currentTimeMillis()
)
