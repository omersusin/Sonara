package com.sonara.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.intelligence.lyrics.LyricLine
import com.sonara.app.intelligence.lyrics.LyricsAnimationStyle
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextSecondary

// Duration for the timed sweep fallback when no word timestamps are available
private const val LINE_SWEEP_MS = 3500f

@Composable
fun SyncedLyricLine(
    line: LyricLine,
    isActive: Boolean,
    activeWordIndex: Int = -1,
    estimatedPositionMs: Long = 0L,
    animationStyle: LyricsAnimationStyle = LyricsAnimationStyle.KARAOKE,
    textSizeSp: Float = 0f,
    modifier: Modifier = Modifier,
    distanceFromActive: Int = 0,
    lyricsLineSpacing: Float = 1.3f,
    lyricsBlurInactive: Boolean = true,
    lyricsPosition: String = "center",
    blurEnabled: Boolean = true,
    isInstrumental: Boolean = false,
    instrumentalProgress: () -> Float = { 0f },
    accentColor: Color = Color.Unspecified,
    lyricsGlowEnabled: Boolean = false
) {
    val primary  = if (accentColor != Color.Unspecified) accentColor else MaterialTheme.colorScheme.primary
    val dimColor = SonaraTextSecondary.copy(alpha = 0.35f)

    // Multi-singer alignment: v1 = left, v2 = right, lyricsPosition for non-agent lines
    val textAlign = when {
        line.isBackground  -> TextAlign.Center
        line.agent == "v1" -> TextAlign.Start
        line.agent == "v2" -> TextAlign.End
        lyricsPosition == "left"  -> TextAlign.Start
        lyricsPosition == "right" -> TextAlign.End
        else               -> TextAlign.Center
    }
    val hPad      = if (line.isBackground) 28.dp else 14.dp
    val vPad      = if (line.isBackground) 2.dp  else 3.dp
    val baseStyle = if (line.isBackground) MaterialTheme.typography.bodySmall
                   else MaterialTheme.typography.bodyLarge
    val effectiveStyle = if (textSizeSp > 0f)
        baseStyle.copy(fontSize = textSizeSp.sp, lineHeight = (textSizeSp * lyricsLineSpacing).sp)
    else baseStyle

    // CROSS-04: Scale animation
    val scale by animateFloatAsState(
        targetValue  = if (isActive) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label        = "lineScale"
    )

    // CROSS-05: Blur for inactive lines
    val shouldBlur = lyricsBlurInactive && blurEnabled
    val blur by animateDpAsState(
        targetValue  = if (!shouldBlur || distanceFromActive == 0) 0.dp
                       else (distanceFromActive * 2).coerceIn(0, 8).dp,
        animationSpec = tween(200),
        label        = "lineBlur"
    )

    // If this is an instrumental gap, show dots instead of text
    if (isInstrumental && isActive) {
        Box(
            modifier = modifier.fillMaxWidth().padding(horizontal = hPad, vertical = vPad),
            contentAlignment = Alignment.Center
        ) {
            DotLoadingProgress(
                color    = primary,
                progress = instrumentalProgress()
            )
        }
        return
    }

    // ── Shared sweep progress [0..1] for VIVIMUSIC / LYRICS_V2 ───────────────
    val sweepTarget = sweepTarget(line, isActive, estimatedPositionMs)
    val sweep by animateFloatAsState(
        targetValue   = sweepTarget,
        animationSpec = if (sweepTarget > 0f) tween(350, easing = FastOutSlowInEasing) else snap(),
        label         = "sweep"
    )

    // ── Style dispatch ────────────────────────────────────────────────────────
    when (animationStyle) {

        // ── NONE ─────────────────────────────────────────────────────────────
        LyricsAnimationStyle.NONE -> {
            Text(
                text      = line.text,
                modifier  = modifier.fillMaxWidth().padding(horizontal = hPad, vertical = vPad)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
                textAlign = textAlign,
                style     = if (isActive) effectiveStyle.copy(fontWeight = FontWeight.Bold) else effectiveStyle,
                color     = if (isActive) SonaraTextPrimary else dimColor
            )
        }

        // ── FADE ─────────────────────────────────────────────────────────────
        LyricsAnimationStyle.FADE -> {
            val color by animateColorAsState(
                if (isActive) SonaraTextPrimary else dimColor, tween(450), label = "fade_c"
            )
            val alpha by animateFloatAsState(
                if (isActive) 1f else 0.45f, tween(450), label = "fade_a"
            )
            Text(
                text      = line.text,
                modifier  = modifier.fillMaxWidth().padding(horizontal = hPad, vertical = vPad).alpha(alpha)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
                textAlign = textAlign,
                style     = if (isActive) effectiveStyle.copy(fontWeight = FontWeight.Bold) else effectiveStyle,
                color     = color
            )
        }

        // ── GLOW ─────────────────────────────────────────────────────────────
        LyricsAnimationStyle.GLOW -> {
            val glowScale by animateFloatAsState(
                if (isActive) 1.10f else 1f,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "glow_s"
            )
            val alpha by animateFloatAsState(if (isActive) 1f else 0.38f, tween(350), label = "glow_a")
            Text(
                text      = line.text,
                modifier  = modifier.fillMaxWidth().padding(horizontal = hPad, vertical = vPad)
                    .graphicsLayer { scaleX = glowScale; scaleY = glowScale; this.alpha = alpha }
                    .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
                textAlign = textAlign,
                style     = if (isActive) effectiveStyle.copy(fontWeight = FontWeight.Bold) else effectiveStyle,
                color     = if (isActive) primary else dimColor
            )
        }

        // ── SLIDE ─────────────────────────────────────────────────────────────
        LyricsAnimationStyle.SLIDE -> {
            val offsetY by animateFloatAsState(
                if (isActive) 0f else 20f,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "slide_y"
            )
            val alpha by animateFloatAsState(if (isActive) 1f else 0.38f, tween(320), label = "slide_a")
            Text(
                text      = line.text,
                modifier  = modifier.fillMaxWidth().padding(horizontal = hPad, vertical = vPad)
                    .graphicsLayer { translationY = offsetY; this.alpha = alpha; scaleX = scale; scaleY = scale }
                    .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
                textAlign = textAlign,
                style     = if (isActive) effectiveStyle.copy(fontWeight = FontWeight.Bold) else effectiveStyle,
                color     = if (isActive) SonaraTextPrimary else dimColor
            )
        }

        // ── KARAOKE ───────────────────────────────────────────────────────────
        // Uses real word timestamps when available; otherwise simulates 450 ms/word reveal
        LyricsAnimationStyle.KARAOKE -> {
            val syntheticWords = remember(line.text) {
                line.text.split(" ").filter { it.isNotBlank() }
            }
            // Effective word index: real timestamps preferred, synthetic timing as fallback
            val effectiveIdx = when {
                line.words.isNotEmpty() -> activeWordIndex
                isActive && estimatedPositionMs > line.startMs ->
                    ((estimatedPositionMs - line.startMs) / 450L).toInt()
                        .coerceIn(0, syntheticWords.lastIndex)
                else -> -1
            }

            val wordTexts = if (line.words.isNotEmpty()) line.words.map { it.text }
                           else if (syntheticWords.size > 1) syntheticWords
                           else null

            if (wordTexts != null) {
                val annotated = buildAnnotatedString {
                    wordTexts.forEachIndexed { idx, w ->
                        val filled = idx <= effectiveIdx
                        withStyle(SpanStyle(
                            color      = if (filled) primary else dimColor,
                            fontWeight = if (filled) FontWeight.Bold else FontWeight.Normal
                        )) {
                            append(w)
                            // Re-insert space between synthetic words
                            if (line.words.isEmpty() && idx < wordTexts.lastIndex) append(" ")
                        }
                    }
                }
                Text(
                    text      = annotated,
                    modifier  = modifier.fillMaxWidth().padding(horizontal = hPad, vertical = vPad)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
                    textAlign = textAlign,
                    style     = effectiveStyle
                )
            } else {
                val color by animateColorAsState(
                    if (isActive) SonaraTextPrimary else dimColor, tween(300), label = "kar_c"
                )
                Text(
                    text      = line.text,
                    modifier  = modifier.fillMaxWidth().padding(horizontal = hPad, vertical = vPad)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
                    textAlign = textAlign,
                    style     = if (isActive) effectiveStyle.copy(fontWeight = FontWeight.Bold) else effectiveStyle,
                    color     = color
                )
            }
        }

        // ── APPLE ─────────────────────────────────────────────────────────────
        LyricsAnimationStyle.APPLE -> {
            val targetScale = when {
                isActive -> 1.20f
                distanceFromActive == 1 -> 0.88f
                else -> 0.78f
            }
            val targetAlpha = when {
                isActive -> 1.0f
                distanceFromActive == 1 -> 0.55f
                else -> 0.35f
            }
            val targetWeight = when {
                isActive -> FontWeight.ExtraBold
                distanceFromActive == 1 -> FontWeight.Medium
                else -> FontWeight.Light
            }
            val appleScale by animateFloatAsState(
                targetScale,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "apple_s"
            )
            val alpha by animateFloatAsState(targetAlpha, tween(420), label = "apple_a")
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth().padding(horizontal = hPad, vertical = 4.dp)
                    .graphicsLayer { scaleX = appleScale; scaleY = appleScale; this.alpha = alpha }
                    .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
                textAlign = textAlign,
                style = effectiveStyle.copy(fontWeight = targetWeight),
                color = if (isActive) SonaraTextPrimary else dimColor
            )
        }

        // ── APPLE_V2 ──────────────────────────────────────────────────────────
        LyricsAnimationStyle.APPLE_V2 -> {
            val targetScale = when {
                isActive -> 1.24f
                distanceFromActive == 1 -> 0.88f
                else -> 0.78f
            }
            val targetAlpha = when {
                isActive -> 1.0f
                distanceFromActive == 1 -> 0.55f
                else -> 0.35f
            }
            val targetWeight = when {
                isActive -> FontWeight.Black
                distanceFromActive == 1 -> FontWeight.Medium
                else -> FontWeight.Light
            }
            val apple2Scale by animateFloatAsState(
                targetScale,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "apple2_s"
            )
            val alpha by animateFloatAsState(targetAlpha, tween(520), label = "apple2_a")
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth().padding(horizontal = hPad, vertical = 4.dp)
                    .graphicsLayer { scaleX = apple2Scale; scaleY = apple2Scale; this.alpha = alpha }
                    .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
                textAlign = textAlign,
                style = effectiveStyle.copy(
                    fontWeight = targetWeight,
                    letterSpacing = if (isActive) 0.sp else 1.2.sp
                ),
                color = if (isActive) primary else dimColor
            )
        }

        // ── VIVIMUSIC ─────────────────────────────────────────────────────────
        // Word-by-word horizontal gradient sweep.
        // When word-level timestamps exist → precise per-word fill.
        // When only line-level LRC → timed sweep over LINE_SWEEP_MS from line start.
        LyricsAnimationStyle.VIVIMUSIC -> {
            val alpha by animateFloatAsState(
                if (isActive) 1f else 0.45f, tween(400), label = "vivi_a"
            )
            val viviScale by animateFloatAsState(
                if (isActive) 1.05f else 1f, tween(400), label = "vivi_s"
            )
            val brush = sweepBrush(sweep, primary, dimColor, soft = 0.03f)
            Text(
                text      = line.text,
                modifier  = modifier.fillMaxWidth().padding(horizontal = hPad, vertical = 4.dp)
                    .graphicsLayer { scaleX = viviScale; scaleY = viviScale; this.alpha = alpha }
                    .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
                textAlign = textAlign,
                style     = effectiveStyle.copy(
                    brush      = brush,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                ),
                // color is only used when brush == null (inactive / sweep not started)
                color = if (brush == null) (if (isActive) primary else dimColor) else Color.Unspecified
            )
        }

        // ── LYRICS_V2 ─────────────────────────────────────────────────────────
        // Word-level animated highlight via LyricsLineV2.
        LyricsAnimationStyle.LYRICS_V2 -> {
            val isPast = !isActive && line.startMs < estimatedPositionMs
            LyricsLineV2(
                line               = line,
                isActive           = isActive,
                isPast             = isPast,
                effectivePositionMs = estimatedPositionMs + 150L,
                accentColor        = primary,
                inactiveAlpha      = 0.35f,
                baseFontSize       = if (textSizeSp > 0f) textSizeSp else 24f,
                modifier           = modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad, vertical = vPad)
                    .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
            )
        }

        // ── METRO ─────────────────────────────────────────────────────────────
        // Metro tile: pulsing background swatch + FontWeight.Black + fast scale pop.
        LyricsAnimationStyle.METRO -> {
            val metroScale by animateFloatAsState(
                if (isActive) 1.07f else 1f, tween(140), label = "metro_s"
            )
            val bgAlpha by animateFloatAsState(
                if (isActive) 0.15f else 0f, tween(120), label = "metro_bg"
            )
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .background(primary.copy(alpha = bgAlpha), RoundedCornerShape(6.dp))
                    .padding(horizontal = hPad - 6.dp, vertical = 5.dp)
                    .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier)
            ) {
                Text(
                    text      = line.text,
                    modifier  = Modifier.fillMaxWidth()
                        .graphicsLayer { scaleX = metroScale; scaleY = metroScale },
                    textAlign = textAlign,
                    style     = effectiveStyle.copy(
                        fontWeight    = if (isActive) FontWeight.Black else FontWeight.Normal,
                        letterSpacing = if (isActive) 0.sp else 0.5.sp
                    ),
                    color     = if (isActive) primary else SonaraTextSecondary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Computes [0..1] horizontal sweep progress for gradient-fill animation styles.
 *
 * - When word-level timestamps are present: advances word-by-word using startMs/endMs.
 * - Otherwise: simple elapsed-time sweep over [LINE_SWEEP_MS] from the line's startMs.
 */
private fun sweepTarget(line: LyricLine, isActive: Boolean, posMs: Long): Float {
    if (!isActive) return 0f
    if (line.words.isNotEmpty() && posMs > 0L) {
        val n   = line.words.size.toFloat()
        val idx = line.words.indexOfLast { it.startMs <= posMs }.coerceAtLeast(0)
        val w   = line.words[idx]
        val wProg = if (w.endMs > w.startMs)
            ((posMs - w.startMs).toFloat() / (w.endMs - w.startMs)).coerceIn(0f, 1f)
        else 1f
        return ((idx + wProg) / n).coerceIn(0f, 1f)
    }
    val elapsed = (posMs - line.startMs).coerceAtLeast(0L).toFloat()
    return (elapsed / LINE_SWEEP_MS).coerceIn(0f, 1f)
}

/**
 * Builds a horizontal gradient [ShaderBrush] that fills [fill] from 0 to [sweep],
 * transitioning to [dim] over a [soft] feather zone.
 *
 * Returns null when [sweep] ≤ 0 (nothing to fill yet).
 * VIVIMUSIC uses soft ≈ 0.03 (sharp edge); LYRICS_V2 uses soft ≈ 0.22 (liquid edge).
 */
private fun sweepBrush(sweep: Float, fill: Color, dim: Color, soft: Float): ShaderBrush? {
    if (sweep <= 0f) return null
    if (sweep >= 1f) return Brush.horizontalGradient(listOf(fill, fill)) as ShaderBrush
    val lo = maxOf(0.001f, sweep - soft)
    val hi = minOf(0.999f, sweep + soft)
    return Brush.horizontalGradient(
        colorStops = arrayOf(0f to fill, lo to fill, hi to dim, 1f to dim)
    ) as ShaderBrush
}
