package com.sonara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.SonaraTextTertiary

@Composable
fun BandSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    minValue: Float,
    maxValue: Float,
    modifier: Modifier,
    enabled: Boolean
) {
    val trackHeight = 140.dp
    val thumbSize = 14.dp
    val trackHeightPx = LocalDensity.current.run { trackHeight.toPx() }
    val halfThumbSizePx = LocalDensity.current.run { thumbSize.toPx() / 2f }
    
    val range = maxValue - minValue
    val primary = MaterialTheme.colorScheme.primary

    val initialOffset = (1f - (value - minValue) / range) * trackHeightPx
    var dragOffset by remember { mutableFloatStateOf(initialOffset) }

    Column(
        modifier = modifier.width(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (value >= 0) "+${String.format("%.0f", value)}" else String.format("%.0f", value),
            style = MaterialTheme.typography.labelSmall,
            color = if (value != 0f) primary else SonaraTextTertiary,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier.width(32.dp).height(trackHeight),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(trackHeight)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(SonaraCardElevated)
                    .align(Alignment.Center)
            )

            val midPx = trackHeightPx / 2f
            val fillTopPx = if (dragOffset < midPx) dragOffset else midPx
            val fillHeightPx = if (dragOffset < midPx) midPx - dragOffset else dragOffset - midPx
            val fillTopDp = LocalDensity.current.run { fillTopPx.toDp() }
            val fillHeightDp = LocalDensity.current.run { fillHeightPx.toDp() }
            val activeColor = if (enabled) primary.copy(alpha = 0.7f) else SonaraTextTertiary.copy(alpha = 0.3f)

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(fillHeightDp)
                    .align(Alignment.TopCenter)
                    .offset(x = 0.dp, y = fillTopDp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(activeColor)
            )

            val thumbOffsetDp = LocalDensity.current.run { (dragOffset - halfThumbSizePx).toDp() }
            val thumbColor = if (enabled) primary else SonaraTextTertiary

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 0.dp, y = thumbOffsetDp)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .pointerInput(enabled) {
                        if (enabled) {
                            detectVerticalDragGestures { _, dragAmount ->
                                val newOffset = (dragOffset + dragAmount).coerceIn(0f, trackHeightPx)
                                dragOffset = newOffset
                                val newValue = maxValue - (newOffset / trackHeightPx) * range
                                onValueChange(newValue)
                            }
                        }
                    }
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SonaraTextTertiary,
            textAlign = TextAlign.Center
        )
    }
}
