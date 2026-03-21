package com.sonara.app.intelligence.lastfm

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Entity(tableName = "pending_scrobbles")
data class PendingScrobble(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val track: String,
    val artist: String,
    val album: String,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

@Dao
interface PendingScrobbleDao {
    @Query("SELECT * FROM pending_scrobbles ORDER BY timestamp ASC LIMIT 50")
    suspend fun getPending(): List<PendingScrobble>

    @Query("SELECT COUNT(*) FROM pending_scrobbles")
    suspend fun count(): Int

    @Insert
    suspend fun insert(scrobble: PendingScrobble)

    @Delete
    suspend fun delete(scrobble: PendingScrobble)

    @Query("DELETE FROM pending_scrobbles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_scrobbles WHERE retryCount > 5")
    suspend fun pruneStale()

    @Query("UPDATE pending_scrobbles SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)
}
