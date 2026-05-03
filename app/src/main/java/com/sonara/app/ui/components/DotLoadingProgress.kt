package com.sonara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * Three-dot instrumental gap indicator.
 *
 * When [progress] is 0..1, dots fill left-to-right as the gap progresses
 * (Rush-style staggered scale+alpha). This is purely driven by playback position —
 * no looping animation, so it freezes correctly when paused.
 *
 * Dot 0 starts animating at progress=0.00, peaks at ~0.21
 * Dot 1 starts animating at progress=0.15, peaks at ~0.36
 * Dot 2 starts animating at progress=0.30, peaks at ~0.51
 * All three are full-bright by progress=0.51, then hold.
 *
 * Pass [progress] = 0f to show all dots at dim/small state (gap just started).
 */
@Composable
fun DotLoadingProgress(
    progress: Float = 0f,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    dotSize: Dp = 8.dp,
    spacing: Dp = 16.dp,
) {
    val resolvedColor = if (color != Color.Unspecified) color
                        else MaterialTheme.colorScheme.primary
    val dotCount = 3

    Row(
        modifier = modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(dotCount) { index ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        val clamped = progress.coerceIn(0f, 1f)
                        // Each dot's local progress: offset by 0.15 per dot, stretched by 1.4
                        val dotProgress = ((clamped - index * 0.15f) * 1.4f).coerceIn(0f, 1f)
                        val scale = lerp(1f, 1.8f, dotProgress)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(0.3f, 1f, dotProgress)
                    }
                    .background(resolvedColor, CircleShape)
            )
        }
    }
}
