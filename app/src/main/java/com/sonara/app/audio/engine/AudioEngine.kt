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
    private var lastBands: FloatArray = FloatArray(10)
    private var lastBass: Int = 0
    private var lastVirt: Int = 0
    private var lastLoud: Int = 0
    private var isEnabled: Boolean = true
    private var reinitScheduled = false

    private val routeCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) {
            if (!reinitScheduled) { reinitScheduled = true; handler.postDelayed({ safeReinit() }, 1500) }
        }
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) {
            if (!reinitScheduled) { reinitScheduled = true; handler.postDelayed({ safeReinit() }, 1500) }
        }
    }

    private fun safeReinit() {
        reinitScheduled = false
        Log.d(TAG, "Route changed — recreating effects (keeping pending state)")
        // DON'T release old effects immediately — let new ones take over
        val oldEq = equalizer; val oldBass = bassBoost; val oldVirt = virtualizer; val oldLoud = loudness
        
        try {
            equalizer = Equalizer(Int.MAX_VALUE, 0).apply { enabled = isEnabled }
            try { bassBoost = BassBoost(Int.MAX_VALUE, 0).apply { enabled = isEnabled } } catch (_: Exception) {}
            try { virtualizer = Virtualizer(Int.MAX_VALUE, 0).apply { enabled = isEnabled } } catch (_: Exception) {}
            try { loudness = LoudnessEnhancer(0).apply { enabled = isEnabled } } catch (_: Exception) {}
            
            // Now release old ones
            try { oldEq?.release() } catch (_: Exception) {}
            try { oldBass?.release() } catch (_: Exception) {}
            try { oldVirt?.release() } catch (_: Exception) {}
            try { oldLoud?.release() } catch (_: Exception) {}
            
            isInitialized = true
            reapply()
            Log.d(TAG, "Route reinit OK — effects reapplied")
        } catch (e: Exception) {
            Log.e(TAG, "Route reinit failed: ${e.message}")
            // Restore old effects if new ones failed
            equalizer = oldEq; bassBoost = oldBass; virtualizer = oldVirt; loudness = oldLoud
        }
    }

    fun init(): Boolean {
        if (isInitialized) return true
        return try {
            equalizer = Equalizer(Int.MAX_VALUE, 0).apply { enabled = isEnabled }
            Log.d(TAG, "EQ: bands=${equalizer?.numberOfBands} priority=MAX")
            try { bassBoost = BassBoost(Int.MAX_VALUE, 0).apply { enabled = isEnabled } } catch (e: Exception) { Log.w(TAG, "BassBoost: ${e.message}") }
            try { virtualizer = Virtualizer(Int.MAX_VALUE, 0).apply { enabled = isEnabled } } catch (e: Exception) { Log.w(TAG, "Virtualizer: ${e.message}") }
            try { loudness = LoudnessEnhancer(0).apply { enabled = isEnabled } } catch (e: Exception) { Log.w(TAG, "Loudness: ${e.message}") }
            isInitialized = true
            try { audioManager.registerAudioDeviceCallback(routeCallback, handler) } catch (_: Exception) {}
            reapply()
            true
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

    fun applyBassBoost(s: Int) { lastBass = s; try { bassBoost?.setStrength(s.coerceIn(0, 1000).toShort()); Log.d(TAG, "Bass: $s") } catch (_: Exception) {} }
    fun applyVirtualizer(s: Int) { lastVirt = s; try { virtualizer?.setStrength(s.coerceIn(0, 1000).toShort()); Log.d(TAG, "Virt: $s") } catch (_: Exception) {} }
    fun applyLoudness(g: Int) { lastLoud = g; try { val le = loudness; if (le != null) { le.setTargetGain(g); le.enabled = isEnabled && g > 0; Log.d(TAG, "Loud: ${g}mB") } } catch (e: Exception) { Log.e(TAG, "Loud: ${e.message}") } }

    fun setEnabled(on: Boolean) {
        isEnabled = on
        try { equalizer?.enabled = on } catch (_: Exception) {}
        try { bassBoost?.enabled = on } catch (_: Exception) {}
        try { virtualizer?.enabled = on } catch (_: Exception) {}
        try { loudness?.enabled = on && lastLoud > 0 } catch (_: Exception) {}
    }

    private fun reapply() { applyBands(lastBands); applyBassBoost(lastBass); applyVirtualizer(lastVirt); applyLoudness(lastLoud) }

    fun release() { try { audioManager.unregisterAudioDeviceCallback(routeCallback) } catch (_: Exception) {}
        try { equalizer?.release() } catch (_: Exception) {}; try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}; try { loudness?.release() } catch (_: Exception) {}
        equalizer = null; bassBoost = null; virtualizer = null; loudness = null; isInitialized = false
    }
}
