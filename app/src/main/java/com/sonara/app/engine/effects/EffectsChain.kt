package com.sonara.app.engine.effects

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
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
    private var presetReverb: PresetReverb? = null

    private var currentBassStrength: Int = 0
    private var currentVirtStrength: Int = 0
    private var currentLoudnessGain: Int = 0
    private var currentReverbPreset: Int = 0
    private var currentSessionId: Int = 0
    private var isEnabled = true

    companion object {
        fun reverbName(preset: Int) = when (preset) {
            0 -> "Off"; 1 -> "Small Room"; 2 -> "Medium Room"
            3 -> "Large Room"; 4 -> "Medium Hall"; 5 -> "Large Hall"
            6 -> "Plate"; else -> "Off"
        }
    }

    /**
     * Initialize effects chain on a given audio session.
     * Session 0 = global output mix.
     */
    fun attach(sessionId: Int, force: Boolean = false) {
        if (!force && sessionId == currentSessionId && bassBoost != null) return
        release()
        currentSessionId = sessionId
        // Android docs: configure parameters BEFORE enabling, otherwise the
        // first frames pass through unaffected and on some chips the strength
        // setter is silently dropped while the effect is enabled.
        try {
            bassBoost = BassBoost(Int.MAX_VALUE, sessionId).apply {
                if (strengthSupported) setStrength(currentBassStrength.toShort())
                enabled = isEnabled && currentBassStrength > 0
            }
            SonaraLogger.eq("BassBoost attached to session $sessionId")
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "BassBoost failed: ${e.message}")
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(Int.MAX_VALUE, sessionId).apply {
                setStrength(currentVirtStrength.toShort())
                enabled = isEnabled && currentVirtStrength > 0
            }
            SonaraLogger.eq("Virtualizer attached to session $sessionId")
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Virtualizer failed: ${e.message}")
            virtualizer = null
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                setTargetGain(currentLoudnessGain)
                enabled = isEnabled && currentLoudnessGain > 0
            }
            SonaraLogger.eq("LoudnessEnhancer attached to session $sessionId")
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "LoudnessEnhancer failed: ${e.message}")
            loudnessEnhancer = null
        }

        try {
            presetReverb = PresetReverb(Int.MAX_VALUE, sessionId).apply {
                preset = currentReverbPreset.toShort()
                enabled = isEnabled && currentReverbPreset > 0
            }
            SonaraLogger.eq("PresetReverb attached to session $sessionId")
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "PresetReverb failed: ${e.message}")
            presetReverb = null
        }
    }

    // Same set-then-enable order as in attach(): some chipsets silently drop a
    // setStrength/setTargetGain that arrives while the effect is already enabled.
    fun setBassBoost(strength: Int) {
        currentBassStrength = strength.coerceIn(0, 1000)
        try {
            bassBoost?.let {
                val wantEnabled = isEnabled && currentBassStrength > 0
                it.enabled = false
                if (it.strengthSupported) it.setStrength(currentBassStrength.toShort())
                it.enabled = wantEnabled
            }
        } catch (e: Exception) { Log.w(TAG, "setBassBoost: ${e.message}") }
    }

    fun setVirtualizer(strength: Int) {
        currentVirtStrength = strength.coerceIn(0, 1000)
        try {
            virtualizer?.let {
                val wantEnabled = isEnabled && currentVirtStrength > 0
                it.enabled = false
                it.setStrength(currentVirtStrength.toShort())
                it.enabled = wantEnabled
            }
        } catch (e: Exception) { Log.w(TAG, "setVirtualizer: ${e.message}") }
    }

    fun setLoudness(gainMb: Int) {
        currentLoudnessGain = gainMb.coerceIn(0, 3000)
        try {
            loudnessEnhancer?.let {
                val wantEnabled = isEnabled && currentLoudnessGain > 0
                it.enabled = false
                it.setTargetGain(currentLoudnessGain)
                it.enabled = wantEnabled
            }
        } catch (e: Exception) { Log.w(TAG, "setLoudness: ${e.message}") }
    }

    fun setReverb(preset: Int) {
        currentReverbPreset = preset.coerceIn(0, 6)
        try {
            presetReverb?.let {
                val wantEnabled = isEnabled && currentReverbPreset > 0
                it.enabled = false
                it.preset = currentReverbPreset.toShort()
                it.enabled = wantEnabled
            }
        } catch (e: Exception) { Log.w(TAG, "setReverb: ${e.message}") }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        bassBoost?.let { it.enabled = enabled && currentBassStrength > 0 }
        virtualizer?.let { it.enabled = enabled && currentVirtStrength > 0 }
        loudnessEnhancer?.let { it.enabled = enabled && currentLoudnessGain > 0 }
        presetReverb?.let { it.enabled = enabled && currentReverbPreset > 0 }
    }

    fun applyProfile(bassStrength: Int, virtStrength: Int, loudnessGain: Int, reverbPreset: Int = currentReverbPreset) {
        setBassBoost(bassStrength)
        setVirtualizer(virtStrength)
        setLoudness(loudnessGain)
        setReverb(reverbPreset)
    }

    val isAttached: Boolean get() = bassBoost != null || virtualizer != null || loudnessEnhancer != null || presetReverb != null
    val attachedSession: Int get() = currentSessionId

    fun forceReattach(sessionId: Int) {
        SonaraLogger.eq("Effects forceReattach to session $sessionId (was $currentSessionId)")
        attach(sessionId, force = true)
        applyProfile(currentBassStrength, currentVirtStrength, currentLoudnessGain, currentReverbPreset)
    }

    fun release() {
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        try { presetReverb?.release() } catch (_: Exception) {}
        bassBoost = null; virtualizer = null; loudnessEnhancer = null; presetReverb = null
    }
}
