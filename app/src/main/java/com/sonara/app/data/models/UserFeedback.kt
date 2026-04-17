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

package com.sonara.app.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_feedback", indices = [Index(value = ["suggestedGenre"]), Index(value = ["timestamp"])])
data class UserFeedback(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackTitle: String? = null,
    val trackArtist: String? = null,
    val suggestedGenre: String,
    val correctedGenre: String? = null,
    val suggestedBands: String? = null,
    val correctedBands: String? = null,
    val accepted: Boolean = true,
    val audioRoute: String,
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
