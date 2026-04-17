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

package com.sonara.app.engine.audio

import kotlin.math.*

class SpectralAnalyzer {
    data class SpectralFeatures(val centroid: Float = 0f, val rolloff: Float = 0f, val flatness: Float = 0f, val flux: Float = 0f, val bandEnergies: FloatArray = FloatArray(7))

    fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size; if (n <= 1) return
        var j = 0
        for (i in 1 until n) { var bit = n shr 1; while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }; j = j xor bit; if (i < j) { var t = real[i]; real[i] = real[j]; real[j] = t; t = imag[i]; imag[i] = imag[j]; imag[j] = t } }
        var len = 2
        while (len <= n) { val ang = 2.0 * PI / len; val wR = cos(ang).toFloat(); val wI = sin(ang).toFloat(); var i = 0
            while (i < n) { var cR = 1f; var cI = 0f; for (k in 0 until len / 2) { val uR = real[i + k]; val uI = imag[i + k]; val vR = real[i + k + len / 2] * cR - imag[i + k + len / 2] * cI; val vI = real[i + k + len / 2] * cI + imag[i + k + len / 2] * cR; real[i + k] = uR + vR; imag[i + k] = uI + vI; real[i + k + len / 2] = uR - vR; imag[i + k + len / 2] = uI - vI; val nR = cR * wR - cI * wI; cI = cR * wI + cI * wR; cR = nR }; i += len }; len = len shl 1 }
    }

    fun magnitudeSpectrum(real: FloatArray, imag: FloatArray): FloatArray = FloatArray(real.size / 2) { sqrt(real[it] * real[it] + imag[it] * imag[it]) }

    fun extractFeatures(mags: FloatArray, sampleRate: Int, prev: FloatArray? = null): SpectralFeatures {
        if (mags.isEmpty()) return SpectralFeatures()
        val total = mags.map { it * it }.sum().coerceAtLeast(Float.MIN_VALUE); val fRes = sampleRate.toFloat() / (mags.size * 2)
        var wSum = 0f; var mSum = 0f; for (i in mags.indices) { wSum += i * fRes * mags[i]; mSum += mags[i] }
        val centroid = if (mSum > 0) wSum / mSum else 0f
        var cumE = 0f; var rBin = mags.size - 1; val rThr = total * 0.85f; for (i in mags.indices) { cumE += mags[i] * mags[i]; if (cumE >= rThr) { rBin = i; break } }
        val rolloff = rBin * fRes
        val logS = mags.map { ln((it + 1e-10f).toDouble()) }.sum(); val geo = exp(logS / mags.size).toFloat(); val arith = mSum / mags.size
        val flat = if (arith > 0) (geo / arith).coerceIn(0f, 1f) else 0f
        val flux = if (prev != null && prev.size == mags.size) { var s = 0f; for (i in mags.indices) { val d = mags[i] - prev[i]; s += d * d }; sqrt(s / mags.size) } else 0f
        val edges = floatArrayOf(20f, 60f, 250f, 500f, 2000f, 4000f, 6000f, 20000f); val be = FloatArray(7)
        for (i in mags.indices) { val f = i * fRes; val b = when { f < edges[1] -> 0; f < edges[2] -> 1; f < edges[3] -> 2; f < edges[4] -> 3; f < edges[5] -> 4; f < edges[6] -> 5; else -> 6 }; be[b] += mags[i] * mags[i] }
        for (i in be.indices) be[i] = (be[i] / total).coerceIn(0f, 1f)
        return SpectralFeatures(centroid, rolloff, flat, flux, be)
    }
}
