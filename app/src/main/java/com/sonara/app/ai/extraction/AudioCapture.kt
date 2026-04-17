/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
        Log.d(TAG, "attach() called — sessionId=$audioSessionId, currentlyAttached=$isAttached, currentSession=$currentSessionId")
        if (isAttached && currentSessionId == audioSessionId) {
            Log.d(TAG, "Already attached to session $audioSessionId")
            return true
        }
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
            Log.d(TAG, "SUCCESS — Attached to session $audioSessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "FAILED — Attach to session $audioSessionId: ${e.message}")
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
