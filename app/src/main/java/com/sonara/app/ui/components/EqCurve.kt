package com.sonara.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.SonaraCardElevated
import kotlin.math.roundToInt

@Composable
fun EqCurve(
    bands: FloatArray,
    modifier: Modifier = Modifier,
    minValue: Float = -12f,
    maxValue: Float = 12f,
    interactive: Boolean = false,
    onBandChange: ((Int, Float) -> Unit)? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val range = maxValue - minValue

    val gridColor = SonaraCardElevated
    val dragBandIndex = remember { androidx.compose.runtime.mutableIntStateOf(-1) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .then(
                if (interactive && onBandChange != null) Modifier.pointerInput(bands.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val w = size.width.toFloat()
                            val padding = 16f
                            val usableWidth = w - padding * 2
                            val idx = ((offset.x - padding) / usableWidth * (bands.size - 1)).roundToInt()
                                .coerceIn(0, bands.lastIndex)
                            dragBandIndex.intValue = idx
                        },
                        onDrag = { _, delta ->
                            val idx = dragBandIndex.intValue
                            if (idx >= 0) {
                                val h = size.height.toFloat()
                                val dv = -(delta.y / h) * range
                                val newVal = (bands[idx] + dv).coerceIn(minValue, maxValue)
                                onBandChange(idx, newVal)
                            }
                        },
                        onDragEnd = { dragBandIndex.intValue = -1 },
                        onDragCancel = { dragBandIndex.intValue = -1 }
                    )
                } else Modifier
            )
    ) {
        val w = size.width
        val h = size.height
        val padding = 16f

        // Horizontal gridlines
        drawLine(gridColor, Offset(padding, h / 2), Offset(w - padding, h / 2), strokeWidth = 1f)
        drawLine(gridColor, Offset(padding, h * 0.25f), Offset(w - padding, h * 0.25f), strokeWidth = 0.5f)
        drawLine(gridColor, Offset(padding, h * 0.75f), Offset(w - padding, h * 0.75f), strokeWidth = 0.5f)

        // Vertical gridlines at each band
        if (bands.isNotEmpty()) {
            val usableWidth = w - padding * 2
            for (i in bands.indices) {
                val x = padding + (i.toFloat() / (bands.size - 1)) * usableWidth
                drawLine(gridColor.copy(0.5f), Offset(x, 0f), Offset(x, h), strokeWidth = 0.5f)
            }
        }

        if (bands.isEmpty()) return@Canvas

        val usableWidth = w - padding * 2
        val points = bands.mapIndexed { i, v ->
            val x = padding + (i.toFloat() / (bands.size - 1)) * usableWidth
            val y = h - ((v - minValue) / range) * h
            Offset(x, y)
        }

        val path = Path()
        path.moveTo(points[0].x, points[0].y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2
            path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }

        // Gradient fill below curve
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(points.last().x, h / 2)
        fillPath.lineTo(points.first().x, h / 2)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primary.copy(alpha = 0.3f), primary.copy(alpha = 0.0f)),
                startY = 0f, endY = h
            )
        )

        drawPath(
            path = path,
            color = primary,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        points.forEachIndexed { i, pt ->
            val isActive = i == dragBandIndex.intValue
            drawCircle(color = primary, radius = if (isActive) 5.5f else 3.5f, center = pt)
            if (isActive) drawCircle(color = primary.copy(0.25f), radius = 10f, center = pt)
        }
    }
}
