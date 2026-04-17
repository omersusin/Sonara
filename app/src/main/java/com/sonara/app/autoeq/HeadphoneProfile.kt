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

package com.sonara.app.autoeq

data class HeadphoneProfile(
    val name: String,
    val correctionBands: FloatArray = FloatArray(10),
    val matchConfidence: Float = 0f,
    val source: String = "built-in"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeadphoneProfile) return false
        return name == other.name && correctionBands.contentEquals(other.correctionBands)
    }
    override fun hashCode() = name.hashCode()
}
