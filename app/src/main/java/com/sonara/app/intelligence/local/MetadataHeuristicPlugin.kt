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

class MetadataHeuristicPlugin : LocalInferencePlugin {
    override val name = "MetadataHeuristic"
    override val priority = 10

    private val analyzer = LocalAudioAnalyzer()

    override suspend fun resolve(title: String, artist: String, audioContext: AudioContext?): TrackInfo? {
        if (title.isBlank()) return null
        val result = analyzer.analyze(title, artist)
        return if (result.genre != "other" || result.confidence >= 0.3f) result else null
    }
}
