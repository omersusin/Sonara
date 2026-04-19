package com.sonara.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.intelligence.lyrics.LyricLine
import com.sonara.app.intelligence.lyrics.LyricsAnimationStyle
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextSecondary

@Composable
fun SyncedLyricLine(
    line: LyricLine,
    isActive: Boolean,
    activeWordIndex: Int = -1,
    estimatedPositionMs: Long = 0L,
    animationStyle: LyricsAnimationStyle = LyricsAnimationStyle.KARAOKE,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    // Multi-singer alignment: v1 = left, v2 = right, v1000/null = center
    val textAlign = when {
        line.isBackground -> TextAlign.Center
        line.agent == "v1" -> TextAlign.Start
        line.agent == "v2" -> TextAlign.End
        else -> TextAlign.Center
    }

    // Background vocals get inset padding and italic style
    val isBackground = line.isBackground
    val basePaddingH  = if (isBackground) 32.dp else 16.dp
    val basePaddingV  = if (isBackground) 2.dp  else 3.dp
    val baseTypography = if (isBackground)
        MaterialTheme.typography.bodySmall
    else
        MaterialTheme.typography.bodyLarge

    when (animationStyle) {

        LyricsAnimationStyle.NONE -> {
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth()
                    .padding(horizontal = basePaddingH, vertical = basePaddingV),
                textAlign = textAlign,
                style = if (isActive) baseTypography.copy(fontWeight = FontWeight.Bold)
                        else baseTypography,
                color = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.5f)
            )
        }

        LyricsAnimationStyle.FADE -> {
            val color by animateColorAsState(
                targetValue = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.4f),
                animationSpec = tween(400), label = "fade_color"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.5f,
                animationSpec = tween(400), label = "fade_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth()
                    .padding(horizontal = basePaddingH, vertical = basePaddingV).alpha(alpha),
                textAlign = textAlign,
                style = if (isActive) baseTypography.copy(fontWeight = FontWeight.Bold)
                        else baseTypography,
                color = color
            )
        }

        LyricsAnimationStyle.GLOW -> {
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "glow_scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.45f,
                animationSpec = tween(350), label = "glow_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth()
                    .padding(horizontal = basePaddingH, vertical = basePaddingV)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                textAlign = textAlign,
                style = if (isActive) baseTypography.copy(fontWeight = FontWeight.Bold)
                        else baseTypography,
                color = if (isActive) primary else SonaraTextSecondary.copy(alpha = 0.5f)
            )
        }

        LyricsAnimationStyle.SLIDE -> {
            val offsetY by animateFloatAsState(
                targetValue = if (isActive) 0f else 10f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "slide_offset"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.45f,
                animationSpec = tween(300), label = "slide_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth()
                    .padding(horizontal = basePaddingH, vertical = basePaddingV)
                    .graphicsLayer { translationY = offsetY; this.alpha = alpha },
                textAlign = textAlign,
                style = if (isActive) baseTypography.copy(fontWeight = FontWeight.Bold)
                        else baseTypography,
                color = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.5f)
            )
        }

        LyricsAnimationStyle.KARAOKE -> {
            if (line.words.isNotEmpty() && isActive) {
                val annotated = buildAnnotatedString {
                    line.words.forEachIndexed { idx, word ->
                        val filled = idx <= activeWordIndex
                        withStyle(SpanStyle(
                            color = if (filled) primary else SonaraTextSecondary.copy(alpha = 0.5f),
                            fontWeight = if (filled) FontWeight.Bold else FontWeight.Normal
                        )) { append(word.text) }
                    }
                }
                Text(
                    text = annotated,
                    modifier = modifier.fillMaxWidth()
                        .padding(horizontal = basePaddingH, vertical = basePaddingV),
                    textAlign = textAlign,
                    style = baseTypography
                )
            } else {
                val color by animateColorAsState(
                    targetValue = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.5f),
                    animationSpec = tween(300), label = "karaoke_color"
                )
                Text(
                    text = line.text,
                    modifier = modifier.fillMaxWidth()
                        .padding(horizontal = basePaddingH, vertical = basePaddingV),
                    textAlign = textAlign,
                    style = if (isActive) baseTypography.copy(fontWeight = FontWeight.Bold)
                            else baseTypography,
                    color = color
                )
            }
        }

        LyricsAnimationStyle.APPLE -> {
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.15f else 0.9f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ), label = "apple_scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.4f,
                animationSpec = tween(400), label = "apple_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth()
                    .padding(horizontal = basePaddingH, vertical = 4.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                textAlign = textAlign,
                style = baseTypography.copy(
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
                ),
                color = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.5f)
            )
        }

        LyricsAnimationStyle.APPLE_V2 -> {
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.2f else 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ), label = "apple_v2_scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.35f,
                animationSpec = tween(500), label = "apple_v2_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth()
                    .padding(horizontal = basePaddingH, vertical = 4.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                textAlign = textAlign,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isActive) FontWeight.Black else FontWeight.Light
                ),
                color = if (isActive) primary else SonaraTextSecondary.copy(alpha = 0.4f)
            )
        }

        LyricsAnimationStyle.VIVIMUSIC -> {
            // Word-by-word fill using startMs/endMs timing when available (vivi-music style).
            // Falls back to scale + alpha when no word timestamps are present.
            if (line.words.isNotEmpty() && isActive && estimatedPositionMs > 0L) {
                val annotated = buildAnnotatedString {
                    line.words.forEach { word ->
                        val fillProgress = if (word.endMs > word.startMs) {
                            ((estimatedPositionMs - word.startMs).toFloat() /
                                    (word.endMs - word.startMs).toFloat()).coerceIn(0f, 1f)
                        } else {
                            if (word.startMs <= estimatedPositionMs) 1f else 0f
                        }
                        val wordColor = lerp(
                            SonaraTextSecondary.copy(alpha = 0.3f),
                            primary,
                            fillProgress
                        )
                        withStyle(SpanStyle(
                            color = wordColor,
                            fontWeight = if (fillProgress > 0.5f) FontWeight.SemiBold else FontWeight.Normal
                        )) { append(word.text) }
                    }
                }
                Text(
                    text = annotated,
                    modifier = modifier.fillMaxWidth()
                        .padding(horizontal = basePaddingH, vertical = 4.dp)
                        .graphicsLayer { scaleX = 1.05f; scaleY = 1.05f },
                    textAlign = textAlign,
                    style = baseTypography
                )
            } else {
                val alpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.4f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    label = "vivi_alpha"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.05f else 0.95f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    label = "vivi_scale"
                )
                Text(
                    text = line.text,
                    modifier = modifier.fillMaxWidth()
                        .padding(horizontal = basePaddingH, vertical = 4.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                    textAlign = textAlign,
                    style = baseTypography.copy(
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.45f)
                )
            }
        }

        LyricsAnimationStyle.LYRICS_V2 -> {
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.45f,
                animationSpec = tween(500), label = "v2_alpha"
            )
            val offsetY by animateFloatAsState(
                targetValue = if (isActive) 0f else 12f,
                animationSpec = tween(500, easing = FastOutSlowInEasing),
                label = "v2_offset"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth()
                    .padding(horizontal = basePaddingH, vertical = basePaddingV)
                    .graphicsLayer { translationY = offsetY; this.alpha = alpha },
                textAlign = textAlign,
                style = if (isActive) baseTypography.copy(fontWeight = FontWeight.SemiBold)
                        else baseTypography,
                color = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.5f)
            )
        }

        LyricsAnimationStyle.METRO -> {
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.5f,
                animationSpec = tween(200), label = "metro_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth()
                    .padding(horizontal = basePaddingH, vertical = 5.dp).alpha(alpha),
                textAlign = textAlign,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isActive) FontWeight.Black else FontWeight.Normal,
                    letterSpacing = if (isActive) 0.sp else 0.5.sp
                ),
                color = if (isActive) primary else SonaraTextSecondary.copy(alpha = 0.5f)
            )
        }
    }
}
