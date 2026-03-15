package com.sonara.app.audio.engine

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper

class AudioEngine(private val context: Context) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var isInitialized: Boolean = false
        private set

    private var pendingBands: FloatArray? = null
    private var pendingBass: Int = 0
    private var pendingVirt: Int = 0
    private var pendingLoud: Int = 0
    private var isEnabled: Boolean = true

    private val routeCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) { reinit() }
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) { reinit() }
    }

    fun init(): Boolean {
        release()
        val success = createEffects()
        if (success) {
            audioManager.registerAudioDeviceCallback(routeCallback, handler)
        }
        return success
    }

    private fun createEffects(): Boolean {
        return try {
            equalizer = Equalizer(0, 0).apply { enabled = isEnabled }
            try { bassBoost = BassBoost(0, 0).apply { enabled = isEnabled } } catch (_: Exception) {}
            try { virtualizer = Virtualizer(0, 0).apply { enabled = isEnabled } } catch (_: Exception) {}
            try { loudness = LoudnessEnhancer(0).apply { enabled = isEnabled } } catch (_: Exception) {}
            isInitialized = true
            reapplyPending()
            true
        } catch (e: Exception) {
            isInitialized = false
            false
        }
    }

    private fun reinit() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            releaseEffectsOnly()
            createEffects()
        }, 500)
    }

    fun applyBands(tenBands: FloatArray) {
        pendingBands = tenBands.copyOf()
        val eq = equalizer ?: return
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
        try { bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort()) } catch (_: Exception) {}
    }

    fun applyVirtualizer(strength: Int) {
        pendingVirt = strength
        try { virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort()) } catch (_: Exception) {}
    }

    fun applyLoudness(gain: Int) {
        pendingLoud = gain
        try { loudness?.setTargetGain(gain) } catch (_: Exception) {}
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        try { equalizer?.enabled = enabled } catch (_: Exception) {}
        try { bassBoost?.enabled = enabled } catch (_: Exception) {}
        try { virtualizer?.enabled = enabled } catch (_: Exception) {}
        try { loudness?.enabled = enabled } catch (_: Exception) {}
    }

    private fun reapplyPending() {
        pendingBands?.let { applyBands(it) }
        if (pendingBass > 0) applyBassBoost(pendingBass)
        if (pendingVirt > 0) applyVirtualizer(pendingVirt)
        if (pendingLoud > 0) applyLoudness(pendingLoud)
    }

    fun release() {
        try { audioManager.unregisterAudioDeviceCallback(routeCallback) } catch (_: Exception) {}
        releaseEffectsOnly()
    }

    private fun releaseEffectsOnly() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudness?.release() } catch (_: Exception) {}
        equalizer = null; bassBoost = null; virtualizer = null; loudness = null
        isInitialized = false
    }

    fun getDeviceBandCount(): Int = equalizer?.numberOfBands?.toInt() ?: 0

    fun getDeviceBandRange(): Pair<Short, Short> {
        val range = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
        return range[0] to range[1]
    }
}
