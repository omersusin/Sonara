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

package com.sonara.app.audio.equalizer

object TenBandEqualizer {
    val FREQUENCIES = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    const val BAND_COUNT = 10
    const val MIN_LEVEL = -12f
    const val MAX_LEVEL = 12f

    val LABELS = arrayOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")

    fun defaultBands(): FloatArray = FloatArray(BAND_COUNT) { 0f }

    fun clamp(value: Float): Float = value.coerceIn(MIN_LEVEL, MAX_LEVEL)
}
