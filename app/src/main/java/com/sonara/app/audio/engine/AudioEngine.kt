package com.sonara.app.audio.engine

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.sonara.app.audio.equalizer.TenBandEqualizer

class AudioEngine {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null
    private var sessionId: Int = 0

    val isInitialized: Boolean get() = equalizer != null

    fun init(audioSessionId: Int): Boolean {
        return try {
            sessionId = audioSessionId
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }
            loudness = LoudnessEnhancer(audioSessionId).apply { enabled = true }
            true
        } catch (e: Exception) {
            release()
            false
        }
    }

    fun applyBands(tenBands: FloatArray) {
        val eq = equalizer ?: return
        val deviceBandCount = eq.numberOfBands.toInt()
        val deviceFreqs = IntArray(deviceBandCount) { eq.getCenterFreq(it.toShort()) / 1000 }
        val mapped = BandMapper.mapToDevice(tenBands, deviceBandCount, deviceFreqs)

        val range = eq.bandLevelRange
        val min = range[0]
        val max = range[1]

        for (i in 0 until deviceBandCount) {
            val level = mapped[i].coerceIn(min, max)
            eq.setBandLevel(i.toShort(), level)
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
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        loudness?.enabled = enabled
    }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudness?.release() } catch (_: Exception) {}
        equalizer = null; bassBoost = null; virtualizer = null; loudness = null
    }

    fun getDeviceBandCount(): Int = equalizer?.numberOfBands?.toInt() ?: 0
}
