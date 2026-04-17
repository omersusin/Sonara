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
