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

data class SoundJourney(
    val totalSongsThisWeek: Int = 0,
    val genreBreakdown: Map<String, Int> = emptyMap(),
    val averageEnergy: Float = 0.5f,
    val peakListeningHour: Int = -1,
    val mostPlayedGenre: String = "Unknown",
    val bassPreference: Float = 0.5f, // 0=low bass, 1=high bass user
    val nightBassIncrease: Boolean = false,
    val totalListeningMinutes: Int = 0
)
