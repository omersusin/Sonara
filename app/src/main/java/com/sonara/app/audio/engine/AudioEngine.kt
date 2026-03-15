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
import android.util.Log

class AudioEngine(private val context: Context) {
    private val TAG = "SonaraEQ"
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var isInitialized: Boolean = false; private set
    private var currentSessionId: Int = 0
    private var lastBands: FloatArray = FloatArray(10)
    private var lastBass: Int = 0
    private var lastVirt: Int = 0
    private var lastLoud: Int = 0
    private var isEnabled: Boolean = true

    private val routeCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) { scheduleReinit() }
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) { scheduleReinit() }
    }

    private fun scheduleReinit() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ Log.d(TAG, "Route change — re-init"); releaseEffects(); createEffects(currentSessionId) }, 800)
    }

    fun init(sessionId: Int = 0): Boolean {
        if (isInitialized && currentSessionId == sessionId) return true
        if (isInitialized) releaseEffects()
        currentSessionId = sessionId
        val ok = createEffects(sessionId)
        if (ok) try { audioManager.registerAudioDeviceCallback(routeCallback, handler) } catch (_: Exception) {}
        return ok
    }

    private fun createEffects(sid: Int): Boolean {
        return try {
            equalizer = Equalizer(Int.MAX_VALUE, sid).apply { enabled = isEnabled }
            Log.d(TAG, "EQ: session=$sid bands=${equalizer?.numberOfBands} priority=MAX")
            try { bassBoost = BassBoost(Int.MAX_VALUE, sid).apply { enabled = isEnabled } } catch (e: Exception) { Log.w(TAG, "BassBoost: ${e.message}") }
            try { virtualizer = Virtualizer(Int.MAX_VALUE, sid).apply { enabled = isEnabled } } catch (e: Exception) { Log.w(TAG, "Virtualizer: ${e.message}") }
            try { loudness = LoudnessEnhancer(sid).apply { enabled = isEnabled } } catch (e: Exception) { Log.w(TAG, "Loudness: ${e.message}") }
            isInitialized = true; reapply(); true
        } catch (e: Exception) { Log.e(TAG, "Init failed: ${e.message}"); isInitialized = false; false }
    }

    fun applyBands(tenBands: FloatArray) {
        lastBands = tenBands.copyOf(); val eq = equalizer ?: return
        try {
            val count = eq.numberOfBands.toInt(); if (count == 0) return
            val range = eq.bandLevelRange; val freqs = IntArray(count) { eq.getCenterFreq(it.toShort()) / 1000 }
            val mapped = BandMapper.mapToDevice(tenBands, count, freqs)
            for (i in 0 until count) eq.setBandLevel(i.toShort(), mapped[i].coerceIn(range[0], range[1]))
        } catch (e: Exception) { Log.e(TAG, "applyBands: ${e.message}") }
    }

    fun applyBassBoost(s: Int) { lastBass = s; try { bassBoost?.setStrength(s.coerceIn(0, 1000).toShort()) } catch (_: Exception) {} }
    fun applyVirtualizer(s: Int) { lastVirt = s; try { virtualizer?.setStrength(s.coerceIn(0, 1000).toShort()) } catch (_: Exception) {} }

    fun applyLoudness(g: Int) {
        lastLoud = g
        try {
            val le = loudness
            if (le != null) { le.setTargetGain(g); le.enabled = isEnabled && g > 0; Log.d(TAG, "Loudness: ${g}mB (${g/100f}dB) enabled=${le.enabled}") }
            else Log.w(TAG, "LoudnessEnhancer null")
        } catch (e: Exception) { Log.e(TAG, "Loudness: ${e.message}") }
    }

    fun setEnabled(on: Boolean) {
        isEnabled = on
        try { equalizer?.enabled = on } catch (_: Exception) {}
        try { bassBoost?.enabled = on } catch (_: Exception) {}
        try { virtualizer?.enabled = on } catch (_: Exception) {}
        try { loudness?.enabled = on && lastLoud > 0 } catch (_: Exception) {}
    }

    private fun reapply() { applyBands(lastBands); applyBassBoost(lastBass); applyVirtualizer(lastVirt); applyLoudness(lastLoud) }

    fun release() { try { audioManager.unregisterAudioDeviceCallback(routeCallback) } catch (_: Exception) {}; releaseEffects() }

    private fun releaseEffects() {
        try { equalizer?.release() } catch (_: Exception) {}; try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}; try { loudness?.release() } catch (_: Exception) {}
        equalizer = null; bassBoost = null; virtualizer = null; loudness = null; isInitialized = false
    }
}
