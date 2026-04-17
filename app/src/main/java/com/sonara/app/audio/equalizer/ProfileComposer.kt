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
