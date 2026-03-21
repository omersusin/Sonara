package com.sonara.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun EqCurve(
    bands: FloatArray,
    modifier: Modifier = Modifier,
    minValue: Float = -12f,
    maxValue: Float = 12f
) {
    val primary = MaterialTheme.colorScheme.primary
    val range = maxValue - minValue

    val gridColor = SonaraCardElevated
    Canvas(modifier = modifier.fillMaxWidth().height(100.dp)) {
        val w = size.width
        val h = size.height
        val padding = 16f

        drawLine(gridColor, Offset(padding, h / 2), Offset(w - padding, h / 2), strokeWidth = 1f)
        drawLine(gridColor, Offset(padding, h * 0.25f), Offset(w - padding, h * 0.25f), strokeWidth = 0.5f)
        drawLine(gridColor, Offset(padding, h * 0.75f), Offset(w - padding, h * 0.75f), strokeWidth = 0.5f)

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

        drawPath(
            path = path,
            color = primary,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(points.last().x, h)
        fillPath.lineTo(points.first().x, h)
        fillPath.close()

        drawPath(path = fillPath, color = primary.copy(alpha = 0.08f))

        points.forEach { p ->
            drawCircle(color = primary, radius = 3.5f, center = p)
        }
    }
}
