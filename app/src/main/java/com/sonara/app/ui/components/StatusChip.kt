package com.sonara.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.*

enum class ChipStatus { Active, Warning, Error, Inactive }

@Composable
fun StatusChip(
    label: String,
    status: ChipStatus = ChipStatus.Inactive,
    icon: ImageVector? = null
) {
    val bgColor: Color
    val textColor: Color
    when (status) {
        ChipStatus.Active -> { bgColor = SonaraSuccess.copy(alpha = 0.12f); textColor = SonaraSuccess }
        ChipStatus.Warning -> { bgColor = SonaraWarning.copy(alpha = 0.12f); textColor = SonaraWarning }
        ChipStatus.Error -> { bgColor = SonaraError.copy(alpha = 0.12f); textColor = SonaraError }
        ChipStatus.Inactive -> { bgColor = SonaraCardElevated; textColor = SonaraTextTertiary }
    }
    Surface(shape = RoundedCornerShape(50), color = bgColor) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = textColor)
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = textColor)
        }
    }
}
