package com.sonara.app.audio.engine

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer

class AudioEngine {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null
    private var currentSessionId: Int = -1

    val isInitialized: Boolean get() = equalizer != null
    val sessionId: Int get() = currentSessionId

    fun init(audioSessionId: Int): Boolean {
        if (audioSessionId <= 0) return false
        if (audioSessionId == currentSessionId && isInitialized) return true

        release()
        return try {
            currentSessionId = audioSessionId
            equalizer = Equalizer(Int.MAX_VALUE, audioSessionId).apply { enabled = true }
            try { bassBoost = BassBoost(Int.MAX_VALUE, audioSessionId).apply { enabled = true } } catch (_: Exception) {}
            try { virtualizer = Virtualizer(Int.MAX_VALUE, audioSessionId).apply { enabled = true } } catch (_: Exception) {}
            try { loudness = LoudnessEnhancer(audioSessionId).apply { enabled = true } } catch (_: Exception) {}
            true
        } catch (e: Exception) {
            release()
            false
        }
    }

    fun applyBands(tenBands: FloatArray) {
        val eq = equalizer ?: return
        val deviceBandCount = eq.numberOfBands.toInt()
        if (deviceBandCount == 0) return

        val range = eq.bandLevelRange
        val minLevel = range[0]
        val maxLevel = range[1]

        val deviceFreqs = IntArray(deviceBandCount) { eq.getCenterFreq(it.toShort()) / 1000 }
        val mapped = BandMapper.mapToDevice(tenBands, deviceBandCount, deviceFreqs)

        for (i in 0 until deviceBandCount) {
            try {
                val level = mapped[i].coerceIn(minLevel, maxLevel)
                eq.setBandLevel(i.toShort(), level)
            } catch (_: Exception) {}
        }
    }

    fun applyBassBoost(strength: Int) {
        try { bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort()) } catch (_: Exception) {}
    }

    fun applyVirtualizer(strength: Int) {
        try { virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort()) } catch (_: Exception) {}
    }

    fun applyLoudness(gain: Int) {
        try { loudness?.setTargetGain(gain) } catch (_: Exception) {}
    }

    fun setEnabled(enabled: Boolean) {
        try { equalizer?.enabled = enabled } catch (_: Exception) {}
        try { bassBoost?.enabled = enabled } catch (_: Exception) {}
        try { virtualizer?.enabled = enabled } catch (_: Exception) {}
        try { loudness?.enabled = enabled } catch (_: Exception) {}
    }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudness?.release() } catch (_: Exception) {}
        equalizer = null; bassBoost = null; virtualizer = null; loudness = null
        currentSessionId = -1
    }
}
