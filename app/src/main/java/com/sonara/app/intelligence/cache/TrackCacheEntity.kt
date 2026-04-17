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
