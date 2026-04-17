/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sonara.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.sonara.app.ui.theme.*

@Composable
fun MoodRing(
    mood: String,
    energy: Float,
    genre: String,
    modifier: Modifier = Modifier
) {
    val moodColor by animateColorAsState(
        targetValue = when (mood.lowercase()) {
            "energetic" -> Color(0xFFFF6B35)   // Orange
            "happy" -> Color(0xFF4CAF50)        // Green
            "chill" -> Color(0xFF42A5F5)        // Blue
            "melancholic" -> Color(0xFF9C27B0)  // Purple
            "intense" -> Color(0xFFE53935)      // Red
            "romantic" -> Color(0xFFEC407A)      // Pink
            else -> MaterialTheme.colorScheme.primary  // Default accent
        },
        animationSpec = tween(800),
        label = "mood_color"
    )

    val energyAnim by animateFloatAsState(
        targetValue = energy,
        animationSpec = tween(600),
        label = "energy"
    )

    Box(modifier = modifier.size(140.dp), contentAlignment = Alignment.Center) {
        // Background ring
        val ringBg = SonaraCardElevated
        val secondaryColor = SonaraTextSecondary
        val tertiaryColor = SonaraTextTertiary
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
