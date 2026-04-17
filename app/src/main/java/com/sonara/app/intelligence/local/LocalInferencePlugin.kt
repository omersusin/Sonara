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

package com.sonara.app.intelligence.local

import com.sonara.app.data.models.TrackInfo

data class AudioContext(
    val pcm16: ShortArray? = null,
    val sampleRate: Int = 16000,
    val source: String = "none"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true; if (other !is AudioContext) return false
        return pcm16?.contentEquals(other.pcm16) == true && sampleRate == other.sampleRate
    }
    override fun hashCode() = pcm16?.contentHashCode() ?: 0
}

interface LocalInferencePlugin {
    val name: String
    val priority: Int  // lower = tried first
    suspend fun resolve(title: String, artist: String, audioContext: AudioContext? = null): TrackInfo?
}
