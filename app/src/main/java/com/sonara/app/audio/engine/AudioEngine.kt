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

    var isInitialized: Boolean = false
        private set

    fun init(): Boolean {
        if (isInitialized) return true
        release()
        return try {
            equalizer = Equalizer(0, 0).apply { enabled = true }
            try { bassBoost = BassBoost(0, 0).apply { enabled = true } } catch (_: Exception) {}
            try { virtualizer = Virtualizer(0, 0).apply { enabled = true } } catch (_: Exception) {}
            try { loudness = LoudnessEnhancer(0).apply { enabled = true } } catch (_: Exception) {}
            isInitialized = true
            true
        } catch (e: Exception) {
            release()
            false
        }
    }

    fun applyBands(tenBands: FloatArray) {
        val eq = equalizer ?: return
        val count = eq.numberOfBands.toInt()
        if (count == 0) return

        val range = eq.bandLevelRange
        val min = range[0]
        val max = range[1]
        val freqs = IntArray(count) { eq.getCenterFreq(it.toShort()) / 1000 }
        val mapped = BandMapper.mapToDevice(tenBands, count, freqs)

        for (i in 0 until count) {
            try { eq.setBandLevel(i.toShort(), mapped[i].coerceIn(min, max)) } catch (_: Exception) {}
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
        isInitialized = false
    }
}
