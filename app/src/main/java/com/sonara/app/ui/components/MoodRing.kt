package com.sonara.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.SonaraCardElevated

@Composable
fun MoodRing(
    mood: String,
    energy: Float,
    genre: String,
    modifier: Modifier = Modifier
) {
    val moodColor by animateColorAsState(
        targetValue = when (mood.lowercase()) {
            "energetic" -> MaterialTheme.colorScheme.error
            "happy" -> MaterialTheme.colorScheme.tertiary
            "chill" -> MaterialTheme.colorScheme.secondary
            "melancholic" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            "intense" -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            "romantic" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "mood_color"
    )

    val energyAnim by animateFloatAsState(
        targetValue = energy,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "energy"
    )

    Box(modifier = modifier.size(140.dp), contentAlignment = Alignment.Center) {
        // Background ring
        val ringBg = SonaraCardElevated
        val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
        val tertiaryColor = MaterialTheme.colorScheme.outline
        Canvas(Modifier.size(130.dp)) {
            drawArc(
                color = ringBg,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 10f, cap = StrokeCap.Round),
                topLeft = Offset(5f, 5f),
                size = Size(size.width - 10f, size.height - 10f)
            )
        }

        // Energy arc
        Canvas(Modifier.size(130.dp)) {
            drawArc(
                color = moodColor,
                startAngle = 135f,
                sweepAngle = 270f * energyAnim,
                useCenter = false,
                style = Stroke(width = 10f, cap = StrokeCap.Round),
                topLeft = Offset(5f, 5f),
                size = Size(size.width - 10f, size.height - 10f)
            )
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(energy * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = moodColor
            )
            Text(
                mood.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = secondaryColor
            )
            Text(
                genre.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = tertiaryColor
            )
        }
    }
}
