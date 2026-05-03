package com.sonara.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.intelligence.lyrics.LyricLine
import com.sonara.app.intelligence.lyrics.LyricWord
import com.sonara.app.ui.theme.SonaraTextSecondary
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsLineV2(
    line: LyricLine,
    isActive: Boolean,
    isPast: Boolean,
    effectivePositionMs: Long,
    accentColor: Color,
    inactiveAlpha: Float = 0.35f,
    baseFontSize: Float = 24f,
    lyricsPosition: String = "center",
    modifier: Modifier = Modifier,
) {
    val words = line.words
    if (words.isNotEmpty()) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = when {
                line.isBackground -> Arrangement.Center
                line.agent == "v1" -> Arrangement.Start
                line.agent == "v2" -> Arrangement.End
                lyricsPosition == "left"  -> Arrangement.Start
                lyricsPosition == "right" -> Arrangement.End
                else -> Arrangement.Center
            },
        ) {
            words.forEachIndexed { idx, word ->
                AnimatedWordV2(
                    word = word,
                    isLineActive = isActive,
                    isLinePast = isPast,
                    effectivePositionMs = effectivePositionMs,
                    accentColor = accentColor,
                    inactiveAlpha = inactiveAlpha,
                    fontSize = if (line.isBackground) baseFontSize * 0.85f else baseFontSize,
                    isBackground = line.isBackground,
                )
                if (idx < words.lastIndex) {
                    Text(" ", fontSize = baseFontSize.sp)
                }
            }
        }
    } else {
        Text(
            text = line.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = (if (line.isBackground) baseFontSize * 0.85f else baseFontSize).sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontStyle = if (line.isBackground) FontStyle.Italic else FontStyle.Normal,
            ),
            color = accentColor.copy(alpha = if (isActive) 1f else inactiveAlpha),
            textAlign = when {
                line.isBackground -> TextAlign.Center
                line.agent == "v1" -> TextAlign.Start
                line.agent == "v2" -> TextAlign.End
                lyricsPosition == "left"  -> TextAlign.Start
                lyricsPosition == "right" -> TextAlign.End
                else -> TextAlign.Center
            },
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AnimatedWordV2(
    word: LyricWord,
    isLineActive: Boolean,
    isLinePast: Boolean,
    effectivePositionMs: Long,
    accentColor: Color,
    inactiveAlpha: Float,
    fontSize: Float,
    isBackground: Boolean,
) {
    val wordStartMs = word.startMs
    val wordEndMs = word.endMs.takeIf { it > wordStartMs } ?: (wordStartMs + 500L)
    val wordDuration = wordEndMs - wordStartMs
    val isComplete = isLinePast || effectivePositionMs >= wordEndMs
    val isWordActive = isLineActive && effectivePositionMs in wordStartMs until wordEndMs
    val progress = when {
        isComplete -> 1f
        !isLineActive || effectivePositionMs <= wordStartMs -> 0f
        else -> ((effectivePositionMs - wordStartMs).toFloat() / wordDuration).coerceIn(0f, 1f)
    }

    val sinProg = sin(progress * PI).toFloat()
    val wordScale = 1f + 0.015f * sinProg
    val targetFloat = if (isWordActive) -4f * sinProg else 0f
    val floatOffset by animateFloatAsState(
        targetValue = targetFloat,
        animationSpec = tween(if (isWordActive) 50 else 350, easing = FastOutSlowInEasing),
        label = "wordFloat"
    )
    val glowAlpha = if (isWordActive) (progress * 2f).coerceAtMost(1f) * 0.45f else 0f
    val glowRadius = if (isWordActive) (progress * 2f).coerceAtMost(1f) * 12f else 0f
    val weight = if (isLineActive) FontWeight.Bold else FontWeight.SemiBold

    Box(modifier = Modifier.graphicsLayer {
        translationY = floatOffset * density
        scaleX = wordScale; scaleY = wordScale
    }) {
        // Layer 1 — dim base
        Text(
            text = word.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = fontSize.sp,
                fontWeight = weight,
                fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
            ),
            color = accentColor.copy(alpha = if (isBackground) inactiveAlpha * 0.7f else inactiveAlpha),
        )
        // Layer 2 — filled overlay with liquid mask
        if (isComplete || isWordActive) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = fontSize.sp,
                    fontWeight = weight,
                    fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
                    shadow = if (glowAlpha > 0f) Shadow(
                        color = accentColor.copy(alpha = glowAlpha),
                        offset = Offset.Zero,
                        blurRadius = glowRadius.coerceAtLeast(1f)
                    ) else null
                ),
                color = accentColor.copy(alpha = if (isBackground) 0.75f else 1f),
                modifier = if (isWordActive) Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val edgePx = 8.dp.toPx()
                        val center = (size.width + edgePx * 2) * progress - edgePx
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startX = center - edgePx,
                                endX = center + edgePx,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    } else Modifier
            )
        }
    }
}
