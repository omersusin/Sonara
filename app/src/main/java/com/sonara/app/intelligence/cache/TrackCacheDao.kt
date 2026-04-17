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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackCacheDao {
    @Query("SELECT * FROM track_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): TrackCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TrackCacheEntity)

    @Query("DELETE FROM track_cache WHERE :now - timestamp > :ttl")
    suspend fun cleanup(now: Long = System.currentTimeMillis(), ttl: Long = TrackCacheEntity.CACHE_TTL)

    @Query("SELECT COUNT(*) FROM track_cache")
    suspend fun count(): Int

    @Query("DELETE FROM track_cache")
    suspend fun clearAll()
}
