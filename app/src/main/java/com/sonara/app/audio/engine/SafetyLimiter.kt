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

package com.sonara.app.audio.engine

import com.sonara.app.audio.equalizer.TenBandEqualizer

object SafetyLimiter {
    private const val MAX_TOTAL_GAIN = 15f
    private const val CLIP_THRESHOLD = 10f

    fun limit(bands: FloatArray, preamp: Float): Pair<FloatArray, Float> {
        val maxGain = bands.max() + preamp
        if (maxGain <= CLIP_THRESHOLD) return bands to preamp

        val reduction = maxGain - CLIP_THRESHOLD
        val safePreamp = (preamp - reduction).coerceAtLeast(TenBandEqualizer.MIN_LEVEL)
        val safeBands = bands.map { TenBandEqualizer.clamp(it) }.toFloatArray()
        return safeBands to safePreamp
    }

    fun wouldClip(bands: FloatArray, preamp: Float): Boolean {
        return (bands.max() + preamp) > CLIP_THRESHOLD
    }
}
