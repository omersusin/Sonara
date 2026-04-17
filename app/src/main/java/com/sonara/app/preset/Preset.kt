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

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val bands: String = "0,0,0,0,0,0,0,0,0,0",
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val isBuiltIn: Boolean = false,
    val category: String = "custom",
    val headphoneId: String? = null,
    val genre: String? = null,
    val isFavorite: Boolean = false,
    val lastUsed: Long = 0
) {
    fun bandsArray(): FloatArray = bands.split(",").map { it.trim().replace(",", ".").toFloatOrNull() ?: 0f }.toFloatArray()

    companion object {
        fun fromArray(arr: FloatArray): String = arr.joinToString(",") { String.format(java.util.Locale.US, "%.1f", it) }
    }
}
