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

package com.sonara.app.ai.models

data class AudioFeatureVector(
    val spectralCentroid: Float,
    val spectralRolloff: Float,
    val spectralBandwidth: Float,
    val spectralFlatness: Float,
    val spectralFluxNorm: Float,
    val band0: Float, val band1: Float, val band2: Float,
    val band3: Float, val band4: Float, val band5: Float,
    val band6: Float, val band7: Float, val band8: Float,
    val band9: Float,
    val zeroCrossingRate: Float,
    val rmsEnergy: Float,
    val dynamicRange: Float,
    val onsetDensity: Float,
    val bassRatio: Float,
    val trebleRatio: Float,
    val midRatio: Float,
    val reserved: Float = 0f
) {
    fun toFloatArray(): FloatArray = floatArrayOf(
        spectralCentroid, spectralRolloff, spectralBandwidth,
        spectralFlatness, spectralFluxNorm,
        band0, band1, band2, band3, band4,
        band5, band6, band7, band8, band9,
        zeroCrossingRate, rmsEnergy, dynamicRange, onsetDensity,
        bassRatio, trebleRatio, midRatio, reserved
    )

    val bandEnergies: FloatArray
        get() = floatArrayOf(band0, band1, band2, band3, band4,
            band5, band6, band7, band8, band9)

    companion object {
        const val SIZE = 23

        fun fromFloatArray(arr: FloatArray): AudioFeatureVector {
            require(arr.size >= SIZE) { "Array size ${arr.size} < $SIZE" }
            return AudioFeatureVector(
                spectralCentroid = arr[0], spectralRolloff = arr[1],
                spectralBandwidth = arr[2], spectralFlatness = arr[3],
                spectralFluxNorm = arr[4],
                band0 = arr[5], band1 = arr[6], band2 = arr[7],
                band3 = arr[8], band4 = arr[9], band5 = arr[10],
                band6 = arr[11], band7 = arr[12], band8 = arr[13],
                band9 = arr[14],
                zeroCrossingRate = arr[15], rmsEnergy = arr[16],
                dynamicRange = arr[17], onsetDensity = arr[18],
                bassRatio = arr[19], trebleRatio = arr[20],
                midRatio = arr[21], reserved = arr.getOrElse(22) { 0f }
            )
        }

        fun vectorToString(arr: FloatArray): String =
            arr.joinToString(",", "[", "]") { "%.5f".format(it) }

        fun stringToVector(s: String): FloatArray =
            s.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().toFloat() }
                .toFloatArray()
    }
}
