package com.sonara.app.audio.engine

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

class AudioEngine {
    private val TAG = "SonaraEQ"
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null

    var isInitialized: Boolean = false
        private set

    private var lastBands: FloatArray = FloatArray(10)
    private var lastBass: Int = 0
    private var lastVirt: Int = 0
    private var lastLoud: Int = 0
    private var isEnabled: Boolean = true

    fun init(): Boolean {
        if (isInitialized) return true
        return try {
            equalizer = Equalizer(0, 0).apply { enabled = isEnabled }
            Log.d(TAG, "EQ created: ${equalizer?.numberOfBands} bands")
            try { bassBoost = BassBoost(0, 0).apply { enabled = isEnabled } } catch (e: Exception) { Log.w(TAG, "BassBoost failed: ${e.message}") }
            try { virtualizer = Virtualizer(0, 0).apply { enabled = isEnabled } } catch (e: Exception) { Log.w(TAG, "Virtualizer failed: ${e.message}") }
            try { loudness = LoudnessEnhancer(0).apply { enabled = isEnabled } } catch (e: Exception) { Log.w(TAG, "Loudness failed: ${e.message}") }
            isInitialized = true
            reapply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "EQ init failed: ${e.message}")
            isInitialized = false
            false
        }
    }

    fun applyBands(tenBands: FloatArray) {
        lastBands = tenBands.copyOf()
        val eq = equalizer ?: return
        try {
            val count = eq.numberOfBands.toInt()
            if (count == 0) return
            val range = eq.bandLevelRange
            val min = range[0]; val max = range[1]
            val freqs = IntArray(count) { eq.getCenterFreq(it.toShort()) / 1000 }
            val mapped = BandMapper.mapToDevice(tenBands, count, freqs)
            for (i in 0 until count) {
                eq.setBandLevel(i.toShort(), mapped[i].coerceIn(min, max))
            }
            Log.d(TAG, "Applied bands: ${tenBands.take(5).map { "%.1f".format(it) }}...")
        } catch (e: Exception) {
            Log.e(TAG, "Apply bands failed: ${e.message}")
        }
    }

    fun applyBassBoost(strength: Int) {
        lastBass = strength
        try { bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort()) } catch (_: Exception) {}
    }

    fun applyVirtualizer(strength: Int) {
        lastVirt = strength
        try { virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort()) } catch (_: Exception) {}
    }

    fun applyLoudness(gain: Int) {
        lastLoud = gain
        try { loudness?.setTargetGain(gain) } catch (_: Exception) {}
    }

    fun setEnabled(on: Boolean) {
        isEnabled = on
        try { equalizer?.enabled = on } catch (_: Exception) {}
        try { bassBoost?.enabled = on } catch (_: Exception) {}
        try { virtualizer?.enabled = on } catch (_: Exception) {}
        try { loudness?.enabled = on } catch (_: Exception) {}
    }

    private fun reapply() {
        applyBands(lastBands)
        applyBassBoost(lastBass)
        applyVirtualizer(lastVirt)
        applyLoudness(lastLoud)
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
