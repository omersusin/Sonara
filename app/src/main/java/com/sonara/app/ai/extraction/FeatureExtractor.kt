package com.sonara.app.ai.extraction

import com.sonara.app.ai.models.AudioFeatureVector
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.sqrt

class FeatureExtractor {

    fun extract(
        fftFrames: List<ByteArray>,
        waveformFrames: List<ByteArray>
    ): AudioFeatureVector? {
        if (fftFrames.size < 20) return null
        val magnitudeFrames = fftFrames.mapNotNull { fftToMagnitudes(it) }
        if (magnitudeFrames.size < 15) return null
        val avgMag = averageMagnitudes(magnitudeFrames)
        if (avgMag.size < 4) return null

        val bands = computeBandEnergies(avgMag, 10)
        val totalBandEnergy = bands.sum().coerceAtLeast(0.001f)
        val centroid = spectralCentroid(avgMag)
        val rolloff = spectralRolloff(avgMag, 0.85f)
        val bandwidth = spectralBandwidth(avgMag, centroid)
        val flatness = spectralFlatness(avgMag)
        val flux = spectralFlux(magnitudeFrames)
        val zcr = zeroCrossingRate(waveformFrames)
        val rms = rmsEnergy(waveformFrames)
        val dynRange = dynamicRange(waveformFrames)
        val onsets = onsetDensity(magnitudeFrames)

        val bassR = ((bands[0] + bands[1] + bands[2]) / totalBandEnergy).coerceIn(0f, 1f)
        val trebleR = ((bands[7] + bands[8] + bands[9]) / totalBandEnergy).coerceIn(0f, 1f)
        val midR = ((bands[3] + bands[4] + bands[5] + bands[6]) / totalBandEnergy).coerceIn(0f, 1f)

        return AudioFeatureVector(
            spectralCentroid = centroid, spectralRolloff = rolloff,
            spectralBandwidth = bandwidth, spectralFlatness = flatness,
            spectralFluxNorm = (flux / 100f).coerceIn(0f, 2f),
            band0 = bands[0], band1 = bands[1], band2 = bands[2],
            band3 = bands[3], band4 = bands[4], band5 = bands[5],
            band6 = bands[6], band7 = bands[7], band8 = bands[8],
            band9 = bands[9],
            zeroCrossingRate = zcr, rmsEnergy = rms,
            dynamicRange = dynRange, onsetDensity = onsets,
            bassRatio = bassR, trebleRatio = trebleR, midRatio = midR
        )
    }

    private fun fftToMagnitudes(fft: ByteArray): FloatArray? {
        if (fft.size < 4) return null
        val n = fft.size / 2
        return FloatArray(n) { i ->
            val ri = 2 * i; val ii = ri + 1
            if (ii >= fft.size) return@FloatArray 0f
            val real = fft[ri].toFloat(); val imag = fft[ii].toFloat()
            sqrt(real * real + imag * imag)
        }
    }

    private fun averageMagnitudes(frames: List<FloatArray>): FloatArray {
        if (frames.isEmpty()) return FloatArray(0)
        val n = frames.minOf { it.size }
        if (n == 0) return FloatArray(0)
        val avg = FloatArray(n)
        for (frame in frames) { for (i in 0 until n) avg[i] += frame[i] }
        val count = frames.size.toFloat()
        for (i in avg.indices) avg[i] /= count
        return avg
    }

    private fun spectralCentroid(mag: FloatArray): Float {
        var wSum = 0f; var total = 0f
        for (i in mag.indices) { wSum += i * mag[i]; total += mag[i] }
        return if (total > 0f) (wSum / total / mag.size).coerceIn(0f, 1f) else 0.5f
    }

    private fun spectralRolloff(mag: FloatArray, thresh: Float): Float {
        val totalE = mag.sumOf { (it * it).toDouble() }.toFloat()
        if (totalE <= 0f) return 0.5f
        var cum = 0f
        for (i in mag.indices) {
            cum += mag[i] * mag[i]
            if (cum >= thresh * totalE) return (i.toFloat() / mag.size).coerceIn(0f, 1f)
        }
        return 1f
    }

    private fun spectralBandwidth(mag: FloatArray, centroid: Float): Float {
        val cBin = centroid * mag.size
        var wVar = 0f; var total = 0f
        for (i in mag.indices) { val d = i - cBin; wVar += mag[i] * d * d; total += mag[i] }
        return if (total > 0f) (sqrt(wVar / total) / mag.size).coerceIn(0f, 1f) else 0f
    }

    private fun spectralFlatness(mag: FloatArray): Float {
        val filtered = mag.filter { it > 0.01f }
        if (filtered.isEmpty()) return 0f
        val logSum = filtered.sumOf { ln(it.toDouble().coerceAtLeast(1e-10)) }
        val geoMean = exp(logSum / filtered.size).toFloat()
        val ariMean = filtered.average().toFloat()
        return if (ariMean > 0f) (geoMean / ariMean).coerceIn(0f, 1f) else 0f
    }

    private fun spectralFlux(frames: List<FloatArray>): Float {
        if (frames.size < 2) return 0f
        var total = 0f
        for (i in 1 until frames.size) {
            var ff = 0f; val len = minOf(frames[i].size, frames[i - 1].size)
            for (j in 0 until len) { val d = frames[i][j] - frames[i - 1][j]; ff += d * d }
            total += sqrt(ff)
        }
        return total / (frames.size - 1)
    }

    private fun zeroCrossingRate(frames: List<ByteArray>): Float {
        if (frames.isEmpty()) return 0f
        var crossings = 0; var total = 0
        for (frame in frames) {
            for (i in 1 until frame.size) {
                val prev = (frame[i - 1].toInt() and 0xFF) - 128
                val curr = (frame[i].toInt() and 0xFF) - 128
                if ((prev >= 0 && curr < 0) || (prev < 0 && curr >= 0)) crossings++
            }
            total += frame.size - 1
        }
        return if (total > 0) (crossings.toFloat() / total).coerceIn(0f, 1f) else 0f
    }

    private fun rmsEnergy(frames: List<ByteArray>): Float {
        if (frames.isEmpty()) return 0f
        var sum = 0.0; var count = 0
        for (frame in frames) {
            for (sample in frame) {
                val n = ((sample.toInt() and 0xFF) - 128) / 128.0
                sum += n * n; count++
            }
        }
        return sqrt(sum / count.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    }

    private fun dynamicRange(frames: List<ByteArray>): Float {
        if (frames.size < 3) return 0f
        val rmsPerFrame = frames.map { frame ->
            var s = 0.0
            for (b in frame) { val n = ((b.toInt() and 0xFF) - 128) / 128.0; s += n * n }
            sqrt(s / frame.size.coerceAtLeast(1)).toFloat()
        }
        val mx = rmsPerFrame.maxOrNull() ?: return 0f
        val mn = rmsPerFrame.minOrNull() ?: return 0f
        return if (mx > 0.001f) ((mx - mn) / mx).coerceIn(0f, 1f) else 0f
    }

    private fun onsetDensity(frames: List<FloatArray>): Float {
        if (frames.size < 3) return 0f
        val fluxVals = mutableListOf<Float>()
        for (i in 1 until frames.size) {
            var f = 0f; val len = minOf(frames[i].size, frames[i - 1].size)
            for (j in 0 until len) { val d = frames[i][j] - frames[i - 1][j]; if (d > 0) f += d }
            fluxVals.add(f)
        }
        if (fluxVals.isEmpty()) return 0f
        val mean = fluxVals.average().toFloat()
        val threshold = mean * 1.5f
        return (fluxVals.count { it > threshold }.toFloat() / fluxVals.size).coerceIn(0f, 1f)
    }

    private fun computeBandEnergies(mag: FloatArray, numBands: Int): FloatArray {
        if (mag.isEmpty()) return FloatArray(numBands)
        val bandSize = maxOf(1, mag.size / numBands)
        val bands = FloatArray(numBands); var maxE = 0f
        for (b in 0 until numBands) {
            val start = b * bandSize; val end = minOf(start + bandSize, mag.size)
            var e = 0f; for (i in start until end) e += mag[i] * mag[i]
            bands[b] = e; if (e > maxE) maxE = e
        }
        if (maxE > 0f) for (b in bands.indices) bands[b] /= maxE
        return bands
    }
}
