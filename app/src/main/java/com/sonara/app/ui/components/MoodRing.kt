package com.sonara.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.M3ECircle
import com.sonara.app.ui.theme.M3EHeart
import com.sonara.app.ui.theme.M3EStar
import com.sonara.app.ui.theme.M3ERoundedSquare
import com.sonara.app.ui.theme.MorphPolygonShape
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.m3eMorph

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    // M3E shape morphing background — mood-based shape selection
    val (morphStart, morphEnd) = when (mood.lowercase()) {
        "energetic" -> M3EStar to M3ECircle
        "happy" -> M3ECircle to M3ERoundedSquare
        "chill" -> M3ERoundedSquare to M3ECircle
        "melancholic" -> M3EHeart to M3ECircle
        "romantic" -> M3EHeart to M3ERoundedSquare
        "intense" -> M3EStar to M3EHeart
        else -> M3ERoundedSquare to M3ECircle
    }
    val morph = remember { m3eMorph(morphStart, morphEnd) }
    val infiniteTransition = rememberInfiniteTransition(label = "mood_morph")
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mood_morph_progress"
    )

    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mood_pulse"
    )

    Box(modifier = modifier.size(140.dp), contentAlignment = Alignment.Center) {
        // Morphing background shape
        Canvas(
            modifier = Modifier.size(130.dp)
        ) {
            val s = size.minDimension
            val matrix = androidx.graphics.shapes.Matrix().apply {
                translate(s / 2f, s / 2f)
                scale(s / 2f * 0.85f, s / 2f * 0.85f)
            }
            drawPath(
                path = morph.toPath(morphProgress, matrix).asComposePath(),
                color = moodColor.copy(alpha = 0.08f + 0.07f * energyAnim),
            )
        }

        // Background ring
        Canvas(Modifier.size(130.dp)) {
            val s = size
            drawArc(
                color = SonaraCardElevated,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 10f, cap = StrokeCap.Round),
                topLeft = Offset(5f, 5f),
                size = Size(s.width - 10f, s.height - 10f)
            )
        }

        // Energy arc
        Canvas(Modifier.size(130.dp)) {
            val s = size
            drawArc(
                color = moodColor,
                startAngle = 135f,
                sweepAngle = 270f * energyAnim,
                useCenter = false,
                style = Stroke(width = 10f, cap = StrokeCap.Round),
                topLeft = Offset(5f, 5f),
                size = Size(s.width - 10f, s.height - 10f)
            )
        }

        // Center text with M3E pulse
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = pulseAnim
                scaleY = pulseAnim
            }
        ) {
            Text(
                "${(energy * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = moodColor
            )
            Text(
                mood.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                genre.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
