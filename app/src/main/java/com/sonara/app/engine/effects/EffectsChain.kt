package com.sonara.app.engine.effects

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.sonara.app.data.SonaraLogger

/**
 * Manages BassBoost, Virtualizer, and LoudnessEnhancer effects.
 * These are NOT managed by DynamicsProcessing or Equalizer —
 * they are separate AudioEffect instances attached to the same session.
 */
class EffectsChain {
    private val TAG = "EffectsChain"

    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var currentBassStrength: Int = 0
    private var currentVirtStrength: Int = 0
    private var currentLoudnessGain: Int = 0
    private var currentSessionId: Int = 0
    private var isEnabled = true

    /**
     * Initialize effects chain on a given audio session.
     * Session 0 = global output mix.
     */
    fun attach(sessionId: Int) {
        if (sessionId == currentSessionId && bassBoost != null) return
        release()
        currentSessionId = sessionId
        try {
            bassBoost = BassBoost(Int.MAX_VALUE, sessionId).apply {
                enabled = isEnabled && currentBassStrength > 0
                if (strengthSupported) setStrength(currentBassStrength.toShort())
            }
            SonaraLogger.eq("BassBoost attached to session $sessionId")
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "BassBoost failed: ${e.message}")
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(Int.MAX_VALUE, sessionId).apply {
                enabled = isEnabled && currentVirtStrength > 0
                setStrength(currentVirtStrength.toShort())
            }
            SonaraLogger.eq("Virtualizer attached to session $sessionId")
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Virtualizer failed: ${e.message}")
            virtualizer = null
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                enabled = isEnabled && currentLoudnessGain > 0
                setTargetGain(currentLoudnessGain)
            }
            SonaraLogger.eq("LoudnessEnhancer attached to session $sessionId")
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "LoudnessEnhancer failed: ${e.message}")
            loudnessEnhancer = null
        }
    }

    fun setBassBoost(strength: Int) {
        currentBassStrength = strength.coerceIn(0, 1000)
        try {
            bassBoost?.let {
                if (it.strengthSupported) it.setStrength(currentBassStrength.toShort())
                it.enabled = isEnabled && currentBassStrength > 0
            }
        } catch (e: Exception) { Log.w(TAG, "setBassBoost: ${e.message}") }
    }

    fun setVirtualizer(strength: Int) {
        currentVirtStrength = strength.coerceIn(0, 1000)
        try {
            virtualizer?.let {
                it.setStrength(currentVirtStrength.toShort())
                it.enabled = isEnabled && currentVirtStrength > 0
            }
        } catch (e: Exception) { Log.w(TAG, "setVirtualizer: ${e.message}") }
    }

    fun setLoudness(gainMb: Int) {
        currentLoudnessGain = gainMb.coerceIn(0, 3000)
        try {
            loudnessEnhancer?.let {
                it.setTargetGain(currentLoudnessGain)
                it.enabled = isEnabled && currentLoudnessGain > 0
            }
        } catch (e: Exception) { Log.w(TAG, "setLoudness: ${e.message}") }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        bassBoost?.let { it.enabled = enabled && currentBassStrength > 0 }
        virtualizer?.let { it.enabled = enabled && currentVirtStrength > 0 }
        loudnessEnhancer?.let { it.enabled = enabled && currentLoudnessGain > 0 }
    }

    fun applyProfile(bassStrength: Int, virtStrength: Int, loudnessGain: Int) {
        setBassBoost(bassStrength)
        setVirtualizer(virtStrength)
        setLoudness(loudnessGain)
    }

    fun release() {
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        bassBoost = null; virtualizer = null; loudnessEnhancer = null
    }
}
