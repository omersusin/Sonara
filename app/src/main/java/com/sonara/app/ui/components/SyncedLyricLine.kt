package com.sonara.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sonara.app.intelligence.lyrics.LyricLine
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextSecondary

/**
 * Renders one lyric line.
 * - Enhanced LRC: highlights the active word in primary color, rest in secondary.
 * - Standard LRC: highlights entire active line in primary, others in secondary.
 */
@Composable
fun SyncedLyricLine(
    line: LyricLine,
    isActive: Boolean,
    activeWordIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveLineColor by animateColorAsState(
        targetValue = if (isActive) SonaraTextPrimary else SonaraTextSecondary.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "lyric_color"
    )

    if (line.words.isNotEmpty() && isActive) {
        // Enhanced LRC — word-level highlight
        val annotated = buildAnnotatedString {
            line.words.forEachIndexed { idx, word ->
                val color = if (idx <= activeWordIndex) activeColor else SonaraTextSecondary.copy(alpha = 0.5f)
                val weight = if (idx <= activeWordIndex) FontWeight.Bold else FontWeight.Normal
                withStyle(SpanStyle(color = color, fontWeight = weight)) {
                    append(word.text)
                }
            }
        }
        Text(
            text = annotated,
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    } else {
        Text(
            text = line.text,
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
            textAlign = TextAlign.Center,
            style = if (isActive) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodyLarge,
            color = inactiveLineColor
        )
    }
}
