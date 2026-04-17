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
