package com.sonara.app.intelligence.lyrics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LyricsCacheDao {

    @Query("SELECT * FROM lyrics_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getByKey(key: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache WHERE cachedAt < :cutoff")
    suspend fun evictOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM lyrics_cache")
    suspend fun count(): Int
}
