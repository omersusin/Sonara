package com.sonara.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

// 3 draggable points: Mids top (90°), Treble bottom-left (210°), Bass bottom-right (330°)
// Center = 0 dB, max radius = maxDb
@Composable
fun RadialEqWidget(
    bass: Float,
    mids: Float,
    treble: Float,
    onBassChange: (Float) -> Unit,
    onMidsChange: (Float) -> Unit,
    onTrebleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = Color(0xFF80CBC4),
    maxDb: Float = 12f,
) {
    val midsAngle = Math.toRadians(90.0).toFloat()
    val trebleAngle = Math.toRadians(210.0).toFloat()
    val bassAngle = Math.toRadians(330.0).toFloat()

    fun dbToR(db: Float, maxR: Float) = (db / maxDb).coerceIn(-1f, 1f) * maxR
    fun rToDb(r: Float, maxR: Float) = (r / maxR * maxDb).coerceIn(-maxDb, maxDb)
    fun pt(angle: Float, r: Float, cx: Float, cy: Float) = Offset(cx + r * cos(angle), cy - r * sin(angle))

    Box(modifier = modifier.aspectRatio(1f)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) Modifier.pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val maxR = size.width / 2f * 0.72f
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = change.position.x - cx
                            val dy = cy - change.position.y
                            val angle = atan2(dy, dx)
                            val dist = sqrt(dx * dx + dy * dy).coerceAtMost(maxR)
                            val nearest = listOf(midsAngle to "m", trebleAngle to "t", bassAngle to "b")
                                .minByOrNull { (a, _) ->
                                    abs(angle - a).let { if (it > PI) (2 * PI - it).toFloat() else it }
                                }?.second
                            val db = rToDb(dist, maxR)
                            when (nearest) {
                                "m" -> onMidsChange(db)
                                "t" -> onTrebleChange(db)
                                "b" -> onBassChange(db)
                            }
                        }
                    } else Modifier
                )
        ) {
            val maxR = size.width / 2f * 0.72f
            val cx = size.width / 2f
            val cy = size.height / 2f

            drawCircle(accentColor.copy(alpha = 0.08f), maxR, Offset(cx, cy), style = Stroke(1.5f))
            drawCircle(accentColor.copy(alpha = 0.04f), maxR * 0.5f, Offset(cx, cy), style = Stroke(1f))

            listOf(midsAngle, trebleAngle, bassAngle).forEach { ang ->
                drawLine(
                    accentColor.copy(alpha = 0.12f), Offset(cx, cy), pt(ang, maxR, cx, cy),
                    strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }

            val mP = pt(midsAngle, dbToR(mids, maxR), cx, cy)
            val tP = pt(trebleAngle, dbToR(treble, maxR), cx, cy)
            val bP = pt(bassAngle, dbToR(bass, maxR), cx, cy)

            val tri = Path().apply {
                moveTo(mP.x, mP.y); lineTo(tP.x, tP.y); lineTo(bP.x, bP.y); close()
            }
            drawPath(tri, accentColor.copy(alpha = 0.18f))
            drawPath(tri, accentColor, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))

            listOf(mP, tP, bP).forEach { point ->
                drawCircle(accentColor.copy(alpha = 0.25f), 14f, point)
                drawCircle(Color.White, 6f, point)
            }
        }

        val labels = listOf("Mids" to mids, "Treble" to treble, "Bass" to bass)
        val aligns = listOf(Alignment.TopCenter, Alignment.BottomStart, Alignment.BottomEnd)
        labels.forEachIndexed { i, (name, db) ->
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.align(aligns[i]).padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(name, color = accentColor, fontSize = 11.sp)
                    Text(
                        "${if (db >= 0) "+" else ""}${"%.0f".format(db)}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
