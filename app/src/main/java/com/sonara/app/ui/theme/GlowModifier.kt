package com.sonara.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GlowModifier — CROSS-10 glow effect for active lyric lines.
 *
 * Draws a soft shadow/glow behind the composable using [Canvas] with a
 * MaskFilter blur. Works on API 28+ (BlurMaskFilter via legacy rendering).
 *
 * Usage:
 *   Text(..., modifier = Modifier.glow(color = accentColor, radius = 12.dp))
 */

/**
 * Applies a soft glow behind the composable.
 *
 * @param color   Glow color (typically the accent or primary color).
 * @param radius  Blur radius of the glow.
 * @param alpha   Opacity of the glow layer (0f = invisible, 1f = full).
 * @param enabled If false, no glow is drawn (avoids allocations when disabled).
 */
fun Modifier.glow(
    color: Color,
    radius: Dp = 12.dp,
    alpha: Float = 0.55f,
    enabled: Boolean = true
): Modifier {
    if (!enabled || alpha <= 0f) return this
    return this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().also { p ->
                p.asFrameworkPaint().apply {
                    isAntiAlias = true
                    this.color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        radius.toPx(),
                        0f, 0f,
                        color.copy(alpha = alpha).toArgb()
                    )
                }
            }
            canvas.drawRoundRect(
                left   = 0f,
                top    = 0f,
                right  = size.width,
                bottom = size.height,
                radiusX = 8.dp.toPx(),
                radiusY = 8.dp.toPx(),
                paint  = paint
            )
        }
    }
}
