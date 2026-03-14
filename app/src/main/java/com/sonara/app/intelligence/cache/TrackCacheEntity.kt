package com.sonara.app.intelligence.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_cache")
data class TrackCacheEntity(
    @PrimaryKey val cacheKey: String,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
    val mood: String = "",
    val energy: Float = 0.5f,
    val confidence: Float = 0f,
    val source: String = "unknown",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun makeKey(title: String, artist: String): String =
            "${title.lowercase().trim()}::${artist.lowercase().trim()}"

        const val CACHE_TTL = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > CACHE_TTL
}
