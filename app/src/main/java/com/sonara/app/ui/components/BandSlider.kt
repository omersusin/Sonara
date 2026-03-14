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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.*

@Composable
fun BandSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minValue: Float = -12f,
    maxValue: Float = 12f,
    enabled: Boolean = true
) {
    val trackHeight = 140.dp
    val trackHeightPx = with(LocalDensity.current) { trackHeight.toPx() }
    val range = maxValue - minValue
    val primary = MaterialTheme.colorScheme.primary

    var dragOffset by remember(value) {
        mutableFloatStateOf((1f - (value - minValue) / range) * trackHeightPx)
    }

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
                    .clip(RoundedCornerShape(2.dp))
                    .background(SonaraCardElevated)
                    .align(Alignment.Center)
            )

            val midY = trackHeightPx / 2f
            val thumbY = dragOffset
            val fillTop: Float
            val fillHeight: Float

            if (thumbY < midY) {
                fillTop = thumbY
                fillHeight = midY - thumbY
            } else {
                fillTop = midY
                fillHeight = thumbY - midY
            }

            val fillTopDp = with(LocalDensity.current) { fillTop.toDp() }
            val fillHeightDp = with(LocalDensity.current) { fillHeight.toDp() }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(fillHeightDp)
                    .offset { IntOffset(0, fillTopDp.roundToPx()) }
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (enabled) primary.copy(alpha = 0.7f) else SonaraTextTertiary.copy(alpha = 0.3f))
                    .align(Alignment.TopCenter)
            )

            val thumbDp = with(LocalDensity.current) { dragOffset.toDp() }
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbDp.roundToPx() - with(density) { 7.dp.roundToPx() }) }
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (enabled) primary else SonaraTextTertiary)
                    .align(Alignment.TopCenter)
                    .then(
                        if (enabled) Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                val newOffset = (dragOffset + dragAmount).coerceIn(0f, trackHeightPx)
                                dragOffset = newOffset
                                val newValue = maxValue - (newOffset / trackHeightPx) * range
                                onValueChange(newValue)
                            }
                        } else Modifier
                    )
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
