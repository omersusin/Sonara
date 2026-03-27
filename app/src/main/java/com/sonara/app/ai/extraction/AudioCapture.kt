package com.sonara.app.ai.extraction

import android.media.audiofx.Visualizer
import android.util.Log
import java.util.concurrent.ConcurrentLinkedDeque

class AudioCapture {
    companion object {
        private const val TAG = "SonaraCapture"
        private const val MAX_BUFFER = 200
        const val MIN_FRAMES = 25
    }

    private var visualizer: Visualizer? = null
    private val fftBuffer = ConcurrentLinkedDeque<ByteArray>()
    private val waveBuffer = ConcurrentLinkedDeque<ByteArray>()

    @Volatile var isAttached = false; private set
    @Volatile var currentSessionId: Int? = null; private set

    fun attach(audioSessionId: Int): Boolean {
        if (isAttached && currentSessionId == audioSessionId) return true
        release()
        return try {
            val v = Visualizer(audioSessionId)
            val range = Visualizer.getCaptureSizeRange()
            v.captureSize = minOf(range[1], 1024)
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vis: Visualizer, wave: ByteArray, rate: Int) {
                    waveBuffer.addLast(wave.copyOf())
                    while (waveBuffer.size > MAX_BUFFER) waveBuffer.pollFirst()
                }
                override fun onFftDataCapture(vis: Visualizer, fft: ByteArray, rate: Int) {
                    fftBuffer.addLast(fft.copyOf())
                    while (fftBuffer.size > MAX_BUFFER) fftBuffer.pollFirst()
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, true)
            v.enabled = true
            visualizer = v; isAttached = true; currentSessionId = audioSessionId
            Log.d(TAG, "Attached to session $audioSessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Attach failed: ${e.message}")
            isAttached = false; false
        }
    }

    fun getFFTFrames(): List<ByteArray> = fftBuffer.toList()
    fun getWaveFrames(): List<ByteArray> = waveBuffer.toList()
    fun hasEnoughData(): Boolean = fftBuffer.size >= MIN_FRAMES
    fun getFrameCount(): Int = fftBuffer.size
    fun clearBuffers() { fftBuffer.clear(); waveBuffer.clear() }

    fun release() {
        try { visualizer?.enabled = false; visualizer?.release() } catch (_: Exception) {}
        visualizer = null; isAttached = false; currentSessionId = null; clearBuffers()
    }
}
