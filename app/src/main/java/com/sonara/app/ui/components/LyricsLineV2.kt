package com.sonara.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.intelligence.lyrics.LyricLine
import com.sonara.app.ui.theme.SonaraTextSecondary

/**
 * LyricsLineV2 — BUG-01 fixed version of the flowing sweep lyric renderer.
 *
 * Differences from the LYRICS_V2 branch in SyncedLyricLine:
 * - Correct sweep anchor: sweep goes from 0→1 over 750 ms while active,
 *   and snaps back to 0 instantly when deactivated.
 * - Separate alpha and vertical-offset animations for a smooth entrance.
 * - accentColor param allows album-art-derived tinting.
 * - lyricsLineSpacing and lyricsPosition params for layout customisation.
 */
@Composable
fun LyricsLineV2(
    line: LyricLine,
    isActive: Boolean,
    textSizeSp: Float = 0f,
    lyricsLineSpacing: Float = 1.3f,
    lyricsPosition: String = "center",
    accentColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val primary = if (accentColor != Color.Unspecified) accentColor
                  else MaterialTheme.colorScheme.primary
    val dimColor = SonaraTextSecondary.copy(alpha = 0.35f)

    val textAlign = when {
        line.isBackground  -> TextAlign.Center
        line.agent == "v1" -> TextAlign.Start
        line.agent == "v2" -> TextAlign.End
        lyricsPosition == "left"  -> TextAlign.Start
        lyricsPosition == "right" -> TextAlign.End
        else               -> TextAlign.Center
    }

    val hPad = if (line.isBackground) 28.dp else 14.dp
    val vPad = if (line.isBackground) 2.dp  else 3.dp
    val baseStyle = if (line.isBackground) MaterialTheme.typography.bodySmall
                    else MaterialTheme.typography.bodyLarge
    val effectiveStyle = if (textSizeSp > 0f)
        baseStyle.copy(fontSize = textSizeSp.sp, lineHeight = (textSizeSp * lyricsLineSpacing).sp)
    else baseStyle

    // Sweep: 0→1 over 750 ms when active, snap to 0 when inactive
    val v2Target = if (isActive) 1f else 0f
    val sweep by animateFloatAsState(
        targetValue   = v2Target,
        animationSpec = if (v2Target > 0f) tween(750, easing = FastOutSlowInEasing) else snap(),
        label         = "v2_sweep"
    )

    val alpha by animateFloatAsState(
        targetValue   = if (isActive) 1f else 0.4f,
        animationSpec = tween(500),
        label         = "v2_alpha"
    )

    val offsetY by animateFloatAsState(
        targetValue   = if (isActive) 0f else 12f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "v2_offset"
    )

    val brush = buildSweepBrush(sweep, primary, dimColor, soft = 0.22f)

    Text(
        text      = line.text,
        modifier  = modifier
            .fillMaxWidth()
            .padding(horizontal = hPad, vertical = vPad)
            .graphicsLayer { translationY = offsetY; this.alpha = alpha },
        textAlign = textAlign,
        style = if (isActive) effectiveStyle.copy(
            brush      = brush,
            fontWeight = FontWeight.SemiBold
        ) else effectiveStyle,
        color = if (brush == null) (if (isActive) MaterialTheme.colorScheme.onSurface else dimColor)
                else Color.Unspecified
    )
}

/**
 * Builds a horizontal-gradient sweep brush for the V2 flowing highlight.
 * Returns null when sweep ≤ 0 (nothing filled yet).
 */
private fun buildSweepBrush(sweep: Float, fill: Color, dim: Color, soft: Float): ShaderBrush? {
    if (sweep <= 0f) return null
    if (sweep >= 1f) return Brush.horizontalGradient(listOf(fill, fill)) as ShaderBrush
    val lo = maxOf(0.001f, sweep - soft)
    val hi = minOf(0.999f, sweep + soft)
    return Brush.horizontalGradient(
        colorStops = arrayOf(0f to fill, lo to fill, hi to dim, 1f to dim)
    ) as ShaderBrush
}
