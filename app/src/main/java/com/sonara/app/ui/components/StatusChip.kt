package com.sonara.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.sonara.app.ui.theme.AppFullShape
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.SonaraError
import com.sonara.app.ui.theme.SonaraSuccess
import com.sonara.app.ui.theme.SonaraTextTertiary
import com.sonara.app.ui.theme.SonaraWarning

enum class ChipStatus { Active, Warning, Error, Inactive }

@Composable
fun StatusChip(
    label: String,
    status: ChipStatus = ChipStatus.Inactive,
    icon: ImageVector? = null,
    compact: Boolean = false
) {
    val bgColor: Color
    val textColor: Color
    when (status) {
        ChipStatus.Active -> { bgColor = SonaraSuccess.copy(alpha = 0.12f); textColor = SonaraSuccess }
        ChipStatus.Warning -> { bgColor = SonaraWarning.copy(alpha = 0.12f); textColor = SonaraWarning }
        ChipStatus.Error -> { bgColor = SonaraError.copy(alpha = 0.12f); textColor = SonaraError }
        ChipStatus.Inactive -> { bgColor = SonaraCardElevated; textColor = SonaraTextTertiary }
    }
    Surface(shape = AppFullShape, color = bgColor) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 3.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) 12.dp else 14.dp), tint = textColor)
            }
            Text(label, style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium, color = textColor)
        }
    }
}
