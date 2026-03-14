package com.sonara.app.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.components.ChipStatus
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.components.NowPlayingBar
import com.sonara.app.ui.components.StatusChip
import com.sonara.app.ui.theme.*

@Composable
fun DashboardScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sonara", style = MaterialTheme.typography.headlineLarge, color = SonaraPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Personal Sound Engine", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                }
                StatusChip(label = "AI Ready", status = ChipStatus.Active, icon = Icons.Rounded.AutoAwesome)
            }
        }

        item { NowPlayingBar() }

        item {
            FluentCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Intelligence", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Waiting for music...", style = MaterialTheme.typography.bodyLarge, color = SonaraTextPrimary)
                    }
                    StatusChip("Idle", ChipStatus.Inactive)
                }
            }
        }

        item {
            FluentCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Headphones, contentDescription = null, tint = SonaraTextSecondary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Headphone", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                            Text("No device connected", style = MaterialTheme.typography.bodyLarge, color = SonaraTextPrimary)
                        }
                    }
                    StatusChip("AutoEQ Off", ChipStatus.Inactive)
                }
            }
        }

        item {
            FluentCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sound Profile", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                        Text("Flat", style = MaterialTheme.typography.labelLarge, color = SonaraPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val bars = listOf(18f, 20f, 22f, 24f, 26f, 26f, 24f, 22f, 20f, 18f)
                        bars.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(h.dp)
                                    .background(SonaraPrimary.copy(alpha = 0.35f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionBtn(Modifier.weight(1f), Icons.Rounded.SwapHoriz, "Compare")
                QuickActionBtn(Modifier.weight(1f), Icons.Rounded.Save, "Save")
                QuickActionBtn(Modifier.weight(1f), Icons.Rounded.Refresh, "Reset")
            }
        }
    }
}

@Composable
private fun QuickActionBtn(modifier: Modifier = Modifier, icon: ImageVector, label: String) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = SonaraCard,
        border = BorderStroke(0.6.dp, SonaraDivider.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = SonaraTextSecondary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = SonaraTextSecondary)
        }
    }
}
