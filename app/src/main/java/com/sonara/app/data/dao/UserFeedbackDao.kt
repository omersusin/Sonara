package com.sonara.app.data.dao

import androidx.room.*
import com.sonara.app.data.models.UserFeedback

@Dao
interface UserFeedbackDao {
    @Insert
    suspend fun insert(fb: UserFeedback): Long

    @Query("SELECT * FROM user_feedback WHERE suggestedGenre = :genre ORDER BY timestamp DESC LIMIT :limit")
    suspend fun forGenre(genre: String, limit: Int = 50): List<UserFeedback>

    @Query("SELECT CAST(SUM(CASE WHEN accepted = 1 THEN 1 ELSE 0 END) AS FLOAT) / MAX(COUNT(*), 1) FROM user_feedback WHERE suggestedGenre = :genre")
    suspend fun acceptanceRate(genre: String): Float

    @Query("SELECT correctedGenre FROM user_feedback WHERE suggestedGenre = :genre AND correctedGenre IS NOT NULL GROUP BY correctedGenre ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun topCorrection(genre: String): String?

    @Query("DELETE FROM user_feedback WHERE timestamp < :cutoff")
    suspend fun prune(cutoff: Long)

    @Query("SELECT COUNT(*) FROM user_feedback")
    suspend fun count(): Int
}
