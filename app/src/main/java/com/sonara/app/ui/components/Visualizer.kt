package com.sonara.app.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.SonaraCardElevated
import kotlin.math.sin

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
                val barHeight = if (isPlaying) {
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
