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
