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

package com.sonara.app.preset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY isBuiltIn DESC, lastUsed DESC")
    fun getAllPresets(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE isBuiltIn = 1")
    fun getBuiltInPresets(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE isBuiltIn = 0")
    fun getCustomPresets(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE category = :category")
    fun getByCategory(category: String): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE headphoneId = :hpId")
    fun getByHeadphone(hpId: String): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getById(id: Long): Preset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: Preset): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<Preset>)

    @Update
    suspend fun update(preset: Preset)

    @Delete
    suspend fun delete(preset: Preset)

    @Query("UPDATE presets SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("UPDATE presets SET lastUsed = :time WHERE id = :id")
    suspend fun updateLastUsed(id: Long, time: Long)

    @Query("SELECT COUNT(*) FROM presets WHERE isBuiltIn = 1")
    suspend fun builtInCount(): Int
    @androidx.room.Query("DELETE FROM presets WHERE isBuiltIn = 0")
    suspend fun deleteAllCustom()
}
