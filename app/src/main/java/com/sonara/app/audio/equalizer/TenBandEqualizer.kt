package com.sonara.app.audio.equalizer

object TenBandEqualizer {
    val FREQUENCIES = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    const val BAND_COUNT = 10
    const val MIN_LEVEL = -12f
    const val MAX_LEVEL = 12f

    val LABELS = arrayOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")

    fun defaultBands(): FloatArray = FloatArray(BAND_COUNT) { 0f }

    fun clamp(value: Float): Float = value.coerceIn(MIN_LEVEL, MAX_LEVEL)
}
