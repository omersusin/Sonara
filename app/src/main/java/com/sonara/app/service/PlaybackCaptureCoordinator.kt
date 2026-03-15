package com.sonara.app.service

import android.os.Build

/**
 * Manages optional audio playback capture for waveform analysis.
 * Only works on Android 10+ with RECORD_AUDIO permission and user-granted MediaProjection.
 *
 * This is NOT required for core functionality.
 * When available, provides PCM audio data to WaveformGenrePlugin.
 */
data class CaptureCapability(val supported: Boolean, val reason: String)

object PlaybackCaptureCoordinator {

    fun checkCapability(
        hasRecordAudio: Boolean = false,
        hasProjectionToken: Boolean = false
    ): CaptureCapability {
        if (Build.VERSION.SDK_INT < 29) return CaptureCapability(false, "Requires Android 10+")
        if (!hasRecordAudio) return CaptureCapability(false, "RECORD_AUDIO permission needed")
        if (!hasProjectionToken) return CaptureCapability(false, "MediaProjection consent needed")
        return CaptureCapability(true, "Ready for waveform capture")
    }
}
