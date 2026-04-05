package com.sonara.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.AppFullShape
import com.sonara.app.ui.theme.M3ECircle
import com.sonara.app.ui.theme.M3EPill
import com.sonara.app.ui.theme.MorphPolygonShape
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.SonaraError
import com.sonara.app.ui.theme.SonaraSuccess
import com.sonara.app.ui.theme.SonaraTextTertiary
import com.sonara.app.ui.theme.SonaraWarning
import com.sonara.app.ui.theme.m3eMorph

enum class ChipStatus { Active, Warning, Error, Inactive }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusChip(
    label: String,
    status: ChipStatus = ChipStatus.Inactive,
    icon: ImageVector? = null,
    compact: Boolean = false,
    morphOnAppear: Boolean = true
) {
    val bgColor: Color
    val textColor: Color
    when (status) {
        ChipStatus.Active -> { bgColor = SonaraSuccess.copy(alpha = 0.12f); textColor = SonaraSuccess }
        ChipStatus.Warning -> { bgColor = SonaraWarning.copy(alpha = 0.12f); textColor = SonaraWarning }
        ChipStatus.Error -> { bgColor = SonaraError.copy(alpha = 0.12f); textColor = SonaraError }
        ChipStatus.Inactive -> { bgColor = SonaraCardElevated; textColor = SonaraTextTertiary }
    }

    var morphTarget by remember { mutableFloatStateOf(if (morphOnAppear) 0f else 1f) }
    val morphProgress by animateFloatAsState(
        targetValue = morphTarget,
        animationSpec = tween(durationMillis = 400),
        label = "chip_morph"
    )
    morphTarget = 1f

    val morph = remember { m3eMorph(M3ECircle, M3EPill) }

    Surface(
        shape = if (morphOnAppear && morphProgress < 1f) MorphPolygonShape(morph, morphProgress) else AppFullShape,
        color = bgColor,
        modifier = Modifier.graphicsLayer {
            if (morphOnAppear && morphProgress < 1f) {
                clip = true
                shape = MorphPolygonShape(morph, morphProgress)
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 3.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 12.dp else 14.dp),
                    tint = textColor
                )
            }
            Text(
                label,
                style = if (compact) MaterialTheme.typography.labelSmall
                else MaterialTheme.typography.labelMedium,
                color = textColor
            )
        }
    }
}
