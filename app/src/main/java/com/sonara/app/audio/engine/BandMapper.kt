package com.sonara.app.audio.engine

import com.sonara.app.audio.equalizer.TenBandEqualizer

object BandMapper {
    fun mapToDevice(tenBands: FloatArray, deviceBandCount: Int, deviceFreqs: IntArray): ShortArray {
        if (deviceBandCount == 0) return ShortArray(0)
        val result = ShortArray(deviceBandCount)
        for (i in 0 until deviceBandCount) {
            val deviceFreq = deviceFreqs.getOrElse(i) { 1000 }
            val value = interpolate(tenBands, deviceFreq)
            result[i] = (value * 100).toInt().toShort()
        }
        return result
    }

    private fun interpolate(bands: FloatArray, targetFreq: Int): Float {
        val freqs = TenBandEqualizer.FREQUENCIES
        if (targetFreq <= freqs.first()) return bands.first()
        if (targetFreq >= freqs.last()) return bands.last()
        for (i in 0 until freqs.size - 1) {
            if (targetFreq in freqs[i]..freqs[i + 1]) {
                val ratio = (targetFreq - freqs[i]).toFloat() / (freqs[i + 1] - freqs[i])
                return bands[i] + (bands[i + 1] - bands[i]) * ratio
            }
        }
        return 0f
    }
}

    fun interpolate(source: ShortArray, targetSize: Int): ShortArray {
        if (source.size == targetSize) return source
        if (source.isEmpty()) return ShortArray(targetSize)
        return ShortArray(targetSize) { i ->
            val ratio = i.toFloat() / (targetSize - 1).coerceAtLeast(1)
            val srcIdx = ratio * (source.size - 1)
            val lo = srcIdx.toInt().coerceIn(0, source.size - 1)
            val hi = (lo + 1).coerceIn(0, source.size - 1)
            val frac = srcIdx - lo
            (source[lo] * (1 - frac) + source[hi] * frac).toInt().toShort()
        }
    }
