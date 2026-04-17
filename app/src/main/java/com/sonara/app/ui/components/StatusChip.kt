/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
    Surface(shape = RoundedCornerShape(50), color = bgColor) {
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
