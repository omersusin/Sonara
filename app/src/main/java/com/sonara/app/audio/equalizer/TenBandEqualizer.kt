package com.sonara.app.audio.equalizer

class TenBandEqualizer {
    companion object {
        val FREQUENCIES = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        const val BAND_COUNT = 10
        const val MIN_LEVEL = -12f
        const val MAX_LEVEL = 12f
    }
}
