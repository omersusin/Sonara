package com.sonara.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.*

enum class ChipStatus { Active, Warning, Error, Neutral }

@Composable
fun StatusChip(
    label: String,
    status: ChipStatus = ChipStatus.Neutral,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = when (status) {
            ChipStatus.Active  -> SonaraSuccess
            ChipStatus.Warning -> SonaraWarning
            ChipStatus.Error   -> SonaraError
            ChipStatus.Neutral -> SonaraTextTertiary
        },
        animationSpec = spring(Spring.DampingRatioNoBouncy),
        label = "chipDotColor"
    )

    val bgColor = when (status) {
        ChipStatus.Active  -> SonaraSuccess.copy(alpha = 0.12f)
        ChipStatus.Warning -> SonaraWarning.copy(alpha = 0.12f)
        ChipStatus.Error   -> SonaraError.copy(alpha = 0.12f)
        ChipStatus.Neutral -> SonaraSurfaceContainerHigh
    }

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)    // pill şekil
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, PillShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = SonaraTextSecondary
        )
    }
}
