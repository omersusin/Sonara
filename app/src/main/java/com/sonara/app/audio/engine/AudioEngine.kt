package com.sonara.app.audio.engine

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

class AudioEngine(private val context: Context) {
    private val TAG = "SonaraEQ"
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null

    var isInitialized: Boolean = false; private set
    private var lastBands: FloatArray = FloatArray(10)
    private var lastBass: Int = 0
    private var lastVirt: Int = 0
    private var lastLoud: Int = 0
    private var isEnabled: Boolean = true

    fun init(): Boolean {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return true
        }
        return try {
            equalizer = Equalizer(Int.MAX_VALUE, 0).apply { enabled = true }
            val bands = equalizer?.numberOfBands ?: 0
            val range = equalizer?.bandLevelRange
            Log.d(TAG, "╔══ EQ CREATED ══╗")
            Log.d(TAG, "║ Session: 0 (global)")
            Log.d(TAG, "║ Priority: MAX")
            Log.d(TAG, "║ Bands: $bands")
            Log.d(TAG, "║ Range: ${range?.get(0)} to ${range?.get(1)}")
            Log.d(TAG, "║ Enabled: true")

            try {
                bassBoost = BassBoost(Int.MAX_VALUE, 0).apply { enabled = true }
                Log.d(TAG, "║ BassBoost: ✓")
            } catch (e: Exception) { Log.w(TAG, "║ BassBoost: ✗ ${e.message}") }

            try {
                virtualizer = Virtualizer(Int.MAX_VALUE, 0).apply { enabled = true }
                Log.d(TAG, "║ Virtualizer: ✓")
            } catch (e: Exception) { Log.w(TAG, "║ Virtualizer: ✗ ${e.message}") }

            try {
                loudness = LoudnessEnhancer(0).apply { enabled = true }
                Log.d(TAG, "║ Loudness: ✓")
            } catch (e: Exception) { Log.w(TAG, "║ Loudness: ✗ ${e.message}") }

            Log.d(TAG, "╚════════════════╝")
            isInitialized = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "╔══ EQ FAILED ══╗")
            Log.e(TAG, "║ ${e.message}")
            Log.e(TAG, "╚════════════════╝")
            isInitialized = false
            false
        }
    }

    fun applyBands(tenBands: FloatArray) {
        lastBands = tenBands.copyOf()
        val eq = equalizer
        if (eq == null) { Log.e(TAG, "applyBands: Equalizer is NULL!"); return }

        try {
            val count = eq.numberOfBands.toInt()
            if (count == 0) { Log.e(TAG, "applyBands: 0 bands!"); return }

            val range = eq.bandLevelRange
            val freqs = IntArray(count) { eq.getCenterFreq(it.toShort()) / 1000 }
            val mapped = BandMapper.mapToDevice(tenBands, count, freqs)

            for (i in 0 until count) {
                val level = mapped[i].coerceIn(range[0], range[1])
                eq.setBandLevel(i.toShort(), level)
            }

            // Verify by reading back
            val readBack = ShortArray(count) { eq.getBandLevel(it.toShort()) }
            Log.d(TAG, "Bands applied: ${readBack.toList()}")
            Log.d(TAG, "EQ enabled: ${eq.enabled}, hasControl: ${eq.hasControl()}")
        } catch (e: Exception) {
            Log.e(TAG, "applyBands FAILED: ${e.message}")
        }
    }

    fun applyBassBoost(s: Int) {
        lastBass = s
        try {
            val bb = bassBoost
            if (bb != null) {
                bb.setStrength(s.coerceIn(0, 1000).toShort())
                Log.d(TAG, "Bass: requested=$s actual=${bb.roundedStrength} enabled=${bb.enabled} hasControl=${bb.hasControl()}")
            } else Log.w(TAG, "BassBoost is NULL")
        } catch (e: Exception) { Log.e(TAG, "Bass FAILED: ${e.message}") }
    }

    fun applyVirtualizer(s: Int) {
        lastVirt = s
        try {
            val vr = virtualizer
            if (vr != null) {
                vr.setStrength(s.coerceIn(0, 1000).toShort())
                Log.d(TAG, "Virt: requested=$s actual=${vr.roundedStrength} enabled=${vr.enabled} hasControl=${vr.hasControl()}")
            } else Log.w(TAG, "Virtualizer is NULL")
        } catch (e: Exception) { Log.e(TAG, "Virt FAILED: ${e.message}") }
    }

    fun applyLoudness(g: Int) {
        lastLoud = g
        try {
            val le = loudness
            if (le != null) {
                le.setTargetGain(g)
                le.enabled = isEnabled && g > 0
                Log.d(TAG, "Loud: ${g}mB (${g / 100f}dB) enabled=${le.enabled}")
            } else Log.w(TAG, "LoudnessEnhancer is NULL")
        } catch (e: Exception) { Log.e(TAG, "Loud FAILED: ${e.message}") }
    }

    fun setEnabled(on: Boolean) {
        isEnabled = on
        try { equalizer?.enabled = on } catch (_: Exception) {}
        try { bassBoost?.enabled = on } catch (_: Exception) {}
        try { virtualizer?.enabled = on } catch (_: Exception) {}
        try { loudness?.enabled = on && lastLoud > 0 } catch (_: Exception) {}
        Log.d(TAG, "All effects enabled=$on")
    }

    fun release() {
        Log.d(TAG, "Releasing all effects")
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudness?.release() } catch (_: Exception) {}
        equalizer = null; bassBoost = null; virtualizer = null; loudness = null
        isInitialized = false
    }
}
