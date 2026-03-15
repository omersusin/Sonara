package com.sonara.app.audio.engine

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

class AudioEngine {
    private val TAG = "SonaraEQ"
    private val sessions = mutableMapOf<Int, SessionEffects>()
    private var pendingBands: FloatArray = FloatArray(10)
    private var pendingBass: Int = 0
    private var pendingVirt: Int = 0
    private var pendingLoud: Int = 0
    private var enabled: Boolean = true

    var isInitialized: Boolean = false
        private set

    private class SessionEffects(
        val sessionId: Int,
        var equalizer: Equalizer? = null,
        var bassBoost: BassBoost? = null,
        var virtualizer: Virtualizer? = null,
        var loudness: LoudnessEnhancer? = null
    ) {
        fun release() {
            try { equalizer?.release() } catch (_: Exception) {}
            try { bassBoost?.release() } catch (_: Exception) {}
            try { virtualizer?.release() } catch (_: Exception) {}
            try { loudness?.release() } catch (_: Exception) {}
        }
    }

    fun init(): Boolean {
        return attachSession(0)
    }

    fun attachSession(sessionId: Int): Boolean {
        if (sessions.containsKey(sessionId)) return true
        return try {
            val effects = SessionEffects(sessionId)
            effects.equalizer = Equalizer(1, sessionId).apply { enabled = this@AudioEngine.enabled }
            try { effects.bassBoost = BassBoost(1, sessionId).apply { enabled = this@AudioEngine.enabled } } catch (_: Exception) {}
            try { effects.virtualizer = Virtualizer(1, sessionId).apply { enabled = this@AudioEngine.enabled } } catch (_: Exception) {}
            try { effects.loudness = LoudnessEnhancer(sessionId).apply { enabled = this@AudioEngine.enabled } } catch (_: Exception) {}
            sessions[sessionId] = effects
            isInitialized = true
            applyPendingTo(effects)
            Log.d(TAG, "Attached session $sessionId, total=${sessions.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed session $sessionId: ${e.message}")
            false
        }
    }

    fun detachSession(sessionId: Int) {
        if (sessionId == 0) return
        sessions.remove(sessionId)?.release()
    }

    fun applyBands(tenBands: FloatArray) {
        pendingBands = tenBands.copyOf()
        sessions.values.forEach { applyBandsTo(it, tenBands) }
    }

    private fun applyBandsTo(effects: SessionEffects, tenBands: FloatArray) {
        val eq = effects.equalizer ?: return
        val count = eq.numberOfBands.toInt()
        if (count == 0) return
        val range = eq.bandLevelRange
        val min = range[0]; val max = range[1]
        val freqs = IntArray(count) { eq.getCenterFreq(it.toShort()) / 1000 }
        val mapped = BandMapper.mapToDevice(tenBands, count, freqs)
        for (i in 0 until count) {
            try { eq.setBandLevel(i.toShort(), mapped[i].coerceIn(min, max)) } catch (_: Exception) {}
        }
    }

    fun applyBassBoost(strength: Int) {
        pendingBass = strength
        sessions.values.forEach { s ->
            try { s.bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort()) } catch (_: Exception) {}
        }
    }

    fun applyVirtualizer(strength: Int) {
        pendingVirt = strength
        sessions.values.forEach { s ->
            try { s.virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort()) } catch (_: Exception) {}
        }
    }

    fun applyLoudness(gain: Int) {
        pendingLoud = gain
        sessions.values.forEach { s ->
            try { s.loudness?.setTargetGain(gain) } catch (_: Exception) {}
        }
    }

    fun setEnabled(on: Boolean) {
        enabled = on
        sessions.values.forEach { s ->
            try { s.equalizer?.enabled = on } catch (_: Exception) {}
            try { s.bassBoost?.enabled = on } catch (_: Exception) {}
            try { s.virtualizer?.enabled = on } catch (_: Exception) {}
            try { s.loudness?.enabled = on } catch (_: Exception) {}
        }
    }

    private fun applyPendingTo(effects: SessionEffects) {
        applyBandsTo(effects, pendingBands)
        try { effects.bassBoost?.setStrength(pendingBass.coerceIn(0, 1000).toShort()) } catch (_: Exception) {}
        try { effects.virtualizer?.setStrength(pendingVirt.coerceIn(0, 1000).toShort()) } catch (_: Exception) {}
        try { effects.loudness?.setTargetGain(pendingLoud) } catch (_: Exception) {}
    }

    fun release() {
        sessions.values.forEach { it.release() }
        sessions.clear()
        isInitialized = false
    }

    fun getDeviceBandCount(): Int = sessions[0]?.equalizer?.numberOfBands?.toInt() ?: 0
}
