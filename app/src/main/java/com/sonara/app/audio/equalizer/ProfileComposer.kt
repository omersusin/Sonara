package com.sonara.app.audio.equalizer

import com.sonara.app.data.models.EqProfile

object ProfileComposer {
    fun compose(
        presetBands: FloatArray,
        autoEqBands: FloatArray,
        aiBands: FloatArray,
        manualBands: FloatArray,
        preamp: Float = 0f,
        autoEqWeight: Float = 1f,
        aiWeight: Float = 0.6f
    ): EqProfile {
        val bands = FloatArray(10)
        for (i in 0 until 10) {
            val base = presetBands.getOrElse(i) { 0f }
            val correction = autoEqBands.getOrElse(i) { 0f } * autoEqWeight
            val aiAdj = aiBands.getOrElse(i) { 0f } * aiWeight
            val manual = manualBands.getOrElse(i) { 0f }
            bands[i] = TenBandEqualizer.clamp(base + correction + aiAdj + manual)
        }
        return EqProfile(bands = bands, preamp = preamp)
    }

    fun diff(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return Float.MAX_VALUE
        return a.zip(b.toList()).map { (x, y) -> kotlin.math.abs(x - y) }.average().toFloat()
    }
}
