package com.sonara.app.ui.components

import android.media.audiofx.Visualizer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.SonaraCardElevated
import kotlin.math.log10
import kotlin.math.sin

/**
 * Returns a FloatArray (0..1 per band) backed by android.media.audiofx.Visualizer on
 * audioSessionId=0 (global output mix). Returns null when permission is unavailable.
 */
@Composable
fun rememberSonaraVisualizerFft(barCount: Int = 32, enabled: Boolean = true): FloatArray? {
    var fftBands by remember { mutableStateOf<FloatArray?>(null) }

    DisposableEffect(enabled) {
        if (!enabled) { fftBands = null; return@DisposableEffect onDispose {} }
        val viz = try { Visualizer(0) } catch (_: Exception) { null }
        if (viz == null) return@DisposableEffect onDispose {}

        viz.captureSize = Visualizer.getCaptureSizeRange()[1]
        viz.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
            override fun onWaveFormDataCapture(v: Visualizer, waveform: ByteArray, sr: Int) {}
            override fun onFftDataCapture(v: Visualizer, fft: ByteArray, sr: Int) {
                val bands = FloatArray(barCount)
                val halfLen = fft.size / 2
                for (b in 0 until barCount) {
                    val start = (b.toFloat() / barCount * halfLen).toInt()
                    val end = ((b + 1).toFloat() / barCount * halfLen).toInt().coerceAtMost(halfLen - 1)
                    var mag = 0f
                    for (k in start..end) {
                        val re = fft[k * 2].toFloat()
                        val im = if (k * 2 + 1 < fft.size) fft[k * 2 + 1].toFloat() else 0f
                        mag += re * re + im * im
                    }
                    val db = if (mag > 0) 10f * log10(mag / (end - start + 1).coerceAtLeast(1)) else -80f
                    bands[b] = ((db + 80f) / 80f).coerceIn(0f, 1f)
                }
                fftBands = bands
            }
        }, Visualizer.getMaxCaptureRate() / 2, false, true)
        viz.enabled = true

        onDispose {
            try { viz.enabled = false; viz.release() } catch (_: Exception) {}
        }
    }

    return fftBands
}

@Composable
fun SonaraVisualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    barCount: Int = 32
,
    fftData: FloatArray? = null) {
    val primary = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "viz")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val seeds = remember { FloatArray(barCount) { (it * 0.7f + it * it * 0.01f) } }

    val idleColor = SonaraCardElevated
    FluentCard(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            val w = size.width
            val h = size.height
            val barWidth = (w / barCount) * 0.65f
            val gap = (w / barCount) * 0.35f

            for (i in 0 until barCount) {
                val barHeight = if (fftData != null && fftData.size >= barCount && isPlaying) {
                    // Real FFT data
                    (fftData[i].coerceIn(0.05f, 1f)) * h
                } else if (isPlaying) {
                    val wave1 = sin(phase + seeds[i]).toFloat() * 0.3f
                    val wave2 = sin(phase * 1.7f + seeds[i] * 0.5f).toFloat() * 0.2f
                    val wave3 = sin(phase * 0.5f + seeds[i] * 1.3f).toFloat() * 0.15f
                    val base = 0.15f + pulse * 0.2f
                    ((base + wave1 + wave2 + wave3).coerceIn(0.05f, 1f)) * h
                } else {
                    val idle = 0.08f + sin(seeds[i]).toFloat() * 0.04f
                    idle * h
                }

                val x = i * (barWidth + gap) + gap / 2
                val alpha = if (isPlaying) 0.4f + (barHeight / h) * 0.5f else 0.15f

                drawRoundRect(
                    color = if (isPlaying) primary.copy(alpha = alpha) else idleColor,
                    topLeft = Offset(x, h - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}
