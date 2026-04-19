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
    animationStyle: LyricsAnimationStyle = LyricsAnimationStyle.KARAOKE,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    when (animationStyle) {
        LyricsAnimationStyle.NONE -> {
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                textAlign = TextAlign.Center,
                style = if (isActive) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        else MaterialTheme.typography.bodyLarge,
                color = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.5f)
            )
        }

        LyricsAnimationStyle.FADE -> {
            val color by animateColorAsState(
                targetValue = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.4f),
                animationSpec = tween(400),
                label = "fade_color"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.5f,
                animationSpec = tween(400),
                label = "fade_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp).alpha(alpha),
                textAlign = TextAlign.Center,
                style = if (isActive) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        else MaterialTheme.typography.bodyLarge,
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
                animationSpec = tween(350),
                label = "glow_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                textAlign = TextAlign.Center,
                style = if (isActive) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        else MaterialTheme.typography.bodyLarge,
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
                animationSpec = tween(300),
                label = "slide_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
                    .graphicsLayer { translationY = offsetY; this.alpha = alpha },
                textAlign = TextAlign.Center,
                style = if (isActive) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        else MaterialTheme.typography.bodyLarge,
                color = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.5f)
            )
        }

        LyricsAnimationStyle.KARAOKE -> {
            if (line.words.isNotEmpty() && isActive) {
                val annotated = buildAnnotatedString {
                    line.words.forEachIndexed { idx, word ->
                        val color = if (idx <= activeWordIndex) primary else SonaraTextSecondary.copy(alpha = 0.5f)
                        val weight = if (idx <= activeWordIndex) FontWeight.Bold else FontWeight.Normal
                        withStyle(SpanStyle(color = color, fontWeight = weight)) { append(word.text) }
                    }
                }
                Text(
                    text = annotated,
                    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                val color by animateColorAsState(
                    targetValue = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.5f),
                    animationSpec = tween(300),
                    label = "karaoke_color"
                )
                Text(
                    text = line.text,
                    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    textAlign = TextAlign.Center,
                    style = if (isActive) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            else MaterialTheme.typography.bodyLarge,
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
                ),
                label = "apple_scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.4f,
                animationSpec = tween(400),
                label = "apple_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
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
                ),
                label = "apple_v2_scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.35f,
                animationSpec = tween(500),
                label = "apple_v2_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isActive) FontWeight.Black else FontWeight.Light
                ),
                color = if (isActive) primary else SonaraTextSecondary.copy(alpha = 0.4f)
            )
        }

        LyricsAnimationStyle.VIVIMUSIC -> {
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
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.45f)
            )
        }

        LyricsAnimationStyle.LYRICS_V2 -> {
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.45f,
                animationSpec = tween(500),
                label = "v2_alpha"
            )
            val offsetY by animateFloatAsState(
                targetValue = if (isActive) 0f else 12f,
                animationSpec = tween(500, easing = FastOutSlowInEasing),
                label = "v2_offset"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
                    .graphicsLayer { translationY = offsetY; this.alpha = alpha },
                textAlign = TextAlign.Center,
                style = if (isActive) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        else MaterialTheme.typography.bodyLarge,
                color = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.5f)
            )
        }

        LyricsAnimationStyle.METRO -> {
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.5f,
                animationSpec = tween(200),
                label = "metro_alpha"
            )
            Text(
                text = line.text,
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).alpha(alpha),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isActive) FontWeight.Black else FontWeight.Normal,
                    letterSpacing = if (isActive) 0.sp else 0.5.sp
                ),
                color = if (isActive) primary else SonaraTextSecondary.copy(alpha = 0.5f)
            )
        }
    }
}
