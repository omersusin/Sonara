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
import com.sonara.app.data.models.UserEqPreference

@Dao
interface UserEqPreferenceDao {
    @Query("SELECT * FROM user_eq_preferences WHERE genre = :genre AND audioRoute = :route ORDER BY usageCount DESC, lastUsed DESC LIMIT 1")
    suspend fun getBest(genre: String, route: String): UserEqPreference?

    @Query("SELECT * FROM user_eq_preferences WHERE genre = :genre ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getAllForGenre(genre: String, limit: Int = 10): List<UserEqPreference>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: UserEqPreference): Long

    @Query("UPDATE user_eq_preferences SET usageCount = usageCount + 1, lastUsed = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_eq_preferences WHERE lastUsed < :cutoff")
    suspend fun prune(cutoff: Long)

    @Query("SELECT COUNT(*) FROM user_eq_preferences")
    suspend fun count(): Int
}
