package com.sonara.app.ui.screens.dashboard

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HeadsetOff
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.ui.components.ChipStatus
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.components.NowPlayingBar
import com.sonara.app.ui.components.PermissionCard
import com.sonara.app.ui.components.SonaraVisualizer
import com.sonara.app.ui.components.StatusChip
import com.sonara.app.ui.theme.*

@Composable
fun DashboardScreen() {
    val viewModel: DashboardViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderSection(state, primary) }

        if (!state.notificationListenerEnabled) {
            item {
                PermissionCard(onGrant = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                })
            }
        }

        item { NowPlayingBar(
            title = if (state.hasTrack) state.title else "No music playing",
            artist = state.artist,
            isPlaying = state.isPlaying
        ) }
        item { IntelligenceCard(state, primary) }
        item { HeadphoneCard(state, primary) }
        item { SoundProfileCard(state, primary) }
        item { SonaraVisualizer(isPlaying = state.isPlaying) }
        item { QuickActions() }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun HeaderSection(state: DashboardUiState, primary: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Sonara", style = MaterialTheme.typography.headlineLarge, color = primary)
            Spacer(Modifier.height(2.dp))
            Text("Personal Sound Engine", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
        }
        StatusChip(
            label = if (state.isAiEnabled) "AI Active" else "AI Off",
            status = if (state.isAiEnabled) ChipStatus.Active else ChipStatus.Inactive,
            icon = Icons.Rounded.AutoAwesome
        )
    }
}

@Composable
private fun IntelligenceCard(state: DashboardUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Intelligence", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                Spacer(Modifier.height(4.dp))
                if (state.isResolving) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = primary)
                        Text("Analyzing...", style = MaterialTheme.typography.bodyLarge)
                    }
                } else if (state.hasTrack && state.sourceLabel != "None") {
                    Text("${state.genre.replaceFirstChar { it.uppercase() }} · ${state.mood.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Waiting for music...", style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (state.hasTrack && state.sourceLabel != "None") {
                val icon = when {
                    state.sourceLabel.contains("Last") -> Icons.Rounded.Public
                    state.sourceLabel.contains("AI") -> Icons.Rounded.Memory
                    else -> Icons.Rounded.AutoAwesome
                }
                StatusChip(state.sourceLabel, ChipStatus.Active, icon)
            } else {
                StatusChip("Idle", ChipStatus.Inactive)
            }
        }

        if (state.hasTrack && state.sourceLabel != "None") {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("Energy", "${(state.energy * 100).toInt()}%", Modifier.weight(1f), primary)
                InfoPill("Confidence", "${(state.confidence * 100).toInt()}%", Modifier.weight(1f), primary)
            }
        }
    }
}

@Composable
private fun InfoPill(label: String, value: String, modifier: Modifier, primary: androidx.compose.ui.graphics.Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = SonaraCardElevated) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            Text(value, style = MaterialTheme.typography.labelLarge, color = primary)
        }
    }
}

@Composable
private fun HeadphoneCard(state: DashboardUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val icon = if (state.headphoneConnected) {
                    if (state.headphoneType.contains("BLUETOOTH")) Icons.Rounded.Bluetooth
                    else if (state.headphoneType.contains("USB")) Icons.Rounded.Usb
                    else Icons.Rounded.Headphones
                } else Icons.Rounded.HeadsetOff

                Icon(icon, null, tint = if (state.headphoneConnected) primary else SonaraTextTertiary, modifier = Modifier.size(20.dp))
                Column {
                    Text("Headphone", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                    Text(if (state.headphoneConnected) state.headphoneName else "No device connected", style = MaterialTheme.typography.bodyLarge)
                }
            }
            val aeqStatus = when {
                !state.headphoneConnected -> ChipStatus.Inactive
                state.autoEqActive -> ChipStatus.Active
                !state.isAutoEqEnabled -> ChipStatus.Inactive
                else -> ChipStatus.Warning
            }
            StatusChip(
                when {
                    !state.headphoneConnected -> "No Device"
                    state.autoEqActive -> "AutoEQ On"
                    !state.isAutoEqEnabled -> "AutoEQ Off"
                    else -> "No Profile"
                }, aeqStatus
            )
        }

        if (state.autoEqActive && state.autoEqProfile.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = SonaraCardElevated) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Profile: ${state.autoEqProfile}", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    Text("${(state.autoEqConfidence * 100).toInt()}% match", style = MaterialTheme.typography.labelSmall, color = primary)
                }
            }
        }
    }
}

@Composable
private fun SoundProfileCard(state: DashboardUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sound Profile", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                Text(state.currentPresetName, style = MaterialTheme.typography.labelLarge, color = primary)
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                state.bands.forEach { v ->
                    val normalized = ((v + 12f) / 24f).coerceIn(0.08f, 1f)
                    Box(
                        modifier = Modifier.weight(1f).height((normalized * 36).dp)
                            .background(primary.copy(alpha = 0.2f + normalized * 0.5f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                }
            }
            if (state.aiReasoning.isNotEmpty()) {
                Text(state.aiReasoning, style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary, maxLines = 2)
            }
        }
    }
}

@Composable
private fun QuickActions() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickActionBtn(Modifier.weight(1f), Icons.Rounded.SwapHoriz, "Compare")
        QuickActionBtn(Modifier.weight(1f), Icons.Rounded.Save, "Save")
        QuickActionBtn(Modifier.weight(1f), Icons.Rounded.Refresh, "Reset")
    }
}

@Composable
private fun QuickActionBtn(modifier: Modifier, icon: ImageVector, label: String) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = SonaraCard, border = BorderStroke(0.6.dp, SonaraDivider.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, label, tint = SonaraTextSecondary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = SonaraTextSecondary)
        }
    }
}
