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
