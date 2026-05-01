package com.sonara.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DotLoadingProgress — animated three-dot indicator used for instrumental gaps
 * in synced lyrics (CROSS-01).
 *
 * Each dot pulses with a staggered delay to create a flowing "waiting" rhythm.
 *
 * @param color     Dot color; defaults to MaterialTheme.colorScheme.primary.
 * @param dotSize   Diameter of each dot.
 * @param spacing   Gap between dots.
 * @param progress  Optional [0..1] progress value — when > 0, dots scale based
 *                  on position relative to progress (visual time fill effect).
 *                  When 0, all dots pulse uniformly.
 */
@Composable
fun DotLoadingProgress(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    dotSize: Dp = 8.dp,
    spacing: Dp = 6.dp,
    progress: Float = 0f
) {
    val resolvedColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "dot_pulse")

    val dotCount = 3
    val scales = Array(dotCount) { i ->
        val delay = i * 160
        // ignore progress param for now — uniform pulse
        val scale by transition.animateFloat(
            initialValue   = 0.6f,
            targetValue    = 1f,
            animationSpec  = infiniteRepeatable(
                animation    = tween(520, delayMillis = delay, easing = FastOutSlowInEasing),
                repeatMode   = RepeatMode.Reverse
            ),
            label = "dot_$i"
        )
        scale
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until dotCount) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scales[i])
                    .background(resolvedColor, CircleShape)
            )
        }
    }
}
