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
    val cacheKey: String,
    val syncedLyrics: String?,          // Raw LRC or TTML; re-parsed via LyricsHelper.parseRaw
    val plainLyrics: String?,
    val source: String = "lrclib",
    val cachedAt: Long = System.currentTimeMillis(),
    val translatedLyrics: String = "",
    val translationLanguage: String = "",
    val translationMode: String = ""
)
