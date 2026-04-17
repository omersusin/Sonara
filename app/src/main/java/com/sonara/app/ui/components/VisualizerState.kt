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

package com.sonara.app.ui.components

/**
 * Madde 9: Visualizer canlı mı, simüle mi — dürüstçe göster.
 */
enum class VisualizerMode(val label: String, val description: String) {
    LIVE("Live", "Real-time audio capture"),
    ENHANCED("Enhanced", "Audio capture + beat estimation"),
    SIMULATED("Simulated", "Playback state-based animation")
}

/**
 * Mevcut duruma göre hangi modda olunduğunu belirle.
 */
object VisualizerStateDetector {
    fun detect(hasAudioSession: Boolean, hasVisualizerPermission: Boolean, isPlaying: Boolean): VisualizerMode {
        return when {
            hasAudioSession && hasVisualizerPermission && isPlaying -> VisualizerMode.LIVE
            isPlaying -> VisualizerMode.SIMULATED
            else -> VisualizerMode.SIMULATED
        }
    }
}
