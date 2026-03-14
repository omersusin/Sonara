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
