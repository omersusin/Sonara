package com.sonara.app.intelligence.local

import android.content.Context
import com.sonara.app.data.models.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * DSP-based waveform genre classifier using PCM16 audio signal features.
 * Extracts RMS energy, zero-crossing rate, high-frequency ratio, beat variance,
 * and dynamic range — then maps them to genre via a weighted scoring model.
 *
 * When assets/genre_model.tflite is present, replace the DSP path with TFLite inference.
 */
class WaveformGenrePlugin(private val context: Context) : LocalInferencePlugin {
    override val name = "WaveformGenre"
    override val priority = 20

    fun init() {
        // TFLite path: load genre_model.tflite from assets when available.
        // DSP path below runs unconditionally without a model file.
    }

    override suspend fun resolve(title: String, artist: String, audioContext: AudioContext?): TrackInfo? =
        withContext(Dispatchers.Default) {
            val pcm = audioContext?.pcm16 ?: return@withContext null
            if (pcm.size < MIN_SAMPLES) return@withContext null

            val f = computeFeatures(pcm)
            if (f.rms < SILENCE_THRESHOLD) return@withContext null

            val (genre, confidence) = classify(f)
            if (confidence < 0.30f) return@withContext null

            TrackInfo(
                title = title, artist = artist,
                genre = genre,
                energy = f.rms.coerceIn(0f, 1f),
                confidence = confidence,
                source = "waveform"
            )
        }

    // ── Feature extraction ────────────────────────────────────────────────────

    private data class Features(
        val rms: Float,          // normalized loudness [0,1]
        val zcr: Float,          // zero-crossing rate [0,1] — spectral brightness proxy
        val hfRatio: Float,      // high-frequency energy ratio [0,1]
        val beatVariance: Float, // per-frame RMS variance — beat strength [0,1]
        val dynamicRange: Float  // peak/RMS ratio [0,1] — compression indicator
    )

    private fun computeFeatures(pcm: ShortArray): Features {
        val n = pcm.size.coerceAtMost(WINDOW_SAMPLES)
        val samples = FloatArray(n) { pcm[it] / 32768f }

        // RMS energy
        var sumSq = 0.0
        for (s in samples) sumSq += s * s.toDouble()
        val rms = sqrt(sumSq / n).toFloat()

        // Zero crossing rate
        var crossings = 0
        for (i in 1 until n) {
            if ((samples[i] >= 0f) != (samples[i - 1] >= 0f)) crossings++
        }
        val zcr = crossings.toFloat() / n

        // High-frequency ratio: first-difference signal amplifies high frequencies
        var diffSumSq = 0.0
        for (i in 1 until n) {
            val d = samples[i] - samples[i - 1]
            diffSumSq += d * d.toDouble()
        }
        val diffRms = sqrt(diffSumSq / (n - 1)).toFloat()
        val hfRatio = if (rms > 0.001f) (diffRms / rms).coerceIn(0f, 3f) / 3f else 0f

        // Beat variance: coefficient of variation of per-frame RMS
        val frameRmsList = mutableListOf<Float>()
        var pos = 0
        while (pos + FRAME_SIZE <= n) {
            var fSq = 0.0
            for (i in pos until pos + FRAME_SIZE) fSq += samples[i] * samples[i].toDouble()
            frameRmsList.add(sqrt(fSq / FRAME_SIZE).toFloat())
            pos += FRAME_SIZE
        }
        val meanFr = frameRmsList.average().toFloat()
        val beatVariance = if (meanFr > 0f) {
            val variance = frameRmsList.map { (it - meanFr) * (it - meanFr) }.average().toFloat()
            (sqrt(variance) / meanFr).coerceIn(0f, 1f)
        } else 0f

        // Dynamic range: peak-to-RMS (high = classical, low = brickwall-compressed)
        val peak = samples.maxOf { abs(it) }
        val dynamicRange = if (rms > 0f) (peak / rms).coerceIn(1f, 20f) / 20f else 0.5f

        return Features(rms, zcr, hfRatio, beatVariance, dynamicRange)
    }

    // ── Classification ────────────────────────────────────────────────────────

    private fun classify(f: Features): Pair<String, Float> {
        val scores = mapOf(
            "electronic" to (0.20f + f.rms * 0.25f + f.beatVariance * 0.35f + (1f - f.dynamicRange) * 0.20f),
            "hip-hop"    to (0.15f + f.beatVariance * 0.45f + (1f - f.zcr) * 0.25f + (1f - f.hfRatio) * 0.15f),
            "metal"      to (0.05f + f.rms * 0.40f + f.hfRatio * 0.35f + f.zcr * 0.20f),
            "rock"       to (0.15f + f.rms * 0.35f + f.beatVariance * 0.25f + f.hfRatio * 0.25f),
            "pop"        to (0.30f + f.beatVariance * 0.30f + (1f - abs(f.zcr - 0.08f) * 4f).coerceIn(0f, 1f) * 0.20f),
            "jazz"       to (0.15f + f.dynamicRange * 0.35f + f.zcr * 0.30f + (1f - f.beatVariance) * 0.20f),
            "classical"  to (0.10f + f.dynamicRange * 0.45f + (1f - f.rms) * 0.25f + (1f - f.beatVariance) * 0.20f),
            "folk"       to (0.20f + (1f - f.rms) * 0.30f + (1f - f.hfRatio) * 0.25f + f.dynamicRange * 0.25f),
            "r&b"        to (0.15f + f.beatVariance * 0.35f + (1f - f.zcr) * 0.30f + f.rms * 0.20f)
        )

        val sorted = scores.entries.sortedByDescending { it.value }
        val top = sorted[0]
        val margin = top.value - (sorted.getOrNull(1)?.value ?: 0f)

        val confidence = when {
            margin > 0.20f && top.value > 0.60f -> 0.65f
            margin > 0.12f && top.value > 0.50f -> 0.50f
            margin > 0.06f && top.value > 0.40f -> 0.38f
            else -> 0.28f
        }
        return top.key to confidence
    }

    companion object {
        private const val MIN_SAMPLES = 8_000      // 0.5 s at 16 kHz
        private const val WINDOW_SAMPLES = 48_000  // 3 s at 16 kHz
        private const val FRAME_SIZE = 512
        private const val SILENCE_THRESHOLD = 0.01f
    }
}
