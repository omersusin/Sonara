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
