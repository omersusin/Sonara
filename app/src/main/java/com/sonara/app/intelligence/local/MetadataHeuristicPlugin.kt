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
