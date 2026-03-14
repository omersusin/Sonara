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
