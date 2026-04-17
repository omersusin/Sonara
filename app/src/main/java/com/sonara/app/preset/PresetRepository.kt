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

import kotlinx.coroutines.flow.Flow

class PresetRepository(private val dao: PresetDao) {

    fun allPresets(): Flow<List<Preset>> = dao.getAllPresets()
    fun builtInPresets(): Flow<List<Preset>> = dao.getBuiltInPresets()
    fun customPresets(): Flow<List<Preset>> = dao.getCustomPresets()
    fun favorites(): Flow<List<Preset>> = dao.getFavorites()
    fun byCategory(cat: String): Flow<List<Preset>> = dao.getByCategory(cat)

    suspend fun initBuiltIns() {
        if (dao.builtInCount() == 0) {
            dao.insertAll(BuiltInPresets.ALL)
        }
    }

    suspend fun save(preset: Preset): Long = dao.insert(preset)

    suspend fun update(preset: Preset) = dao.update(preset)

    suspend fun delete(preset: Preset) = dao.delete(preset)

    suspend fun toggleFavorite(id: Long, current: Boolean) = dao.setFavorite(id, !current)

    suspend fun markUsed(id: Long) = dao.updateLastUsed(id, System.currentTimeMillis())

    suspend fun duplicate(preset: Preset): Long {
        val copy = preset.copy(id = 0, name = "${preset.name} (Copy)", isBuiltIn = false, lastUsed = System.currentTimeMillis())
        return dao.insert(copy)
    }
}
