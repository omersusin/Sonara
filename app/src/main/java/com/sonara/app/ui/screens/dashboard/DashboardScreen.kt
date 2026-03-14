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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HeadsetOff
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.data.models.ConnectionType
import com.sonara.app.intelligence.ResolveSource
import com.sonara.app.ui.components.ChipStatus
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.components.NowPlayingBar
import com.sonara.app.ui.components.SonaraVisualizer
import com.sonara.app.ui.components.StatusChip
import com.sonara.app.ui.theme.*

@Composable
fun DashboardScreen() {
    val viewModel: DashboardViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val primary = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderSection(state, primary) }
        item { NowPlayingBar(
            title = state.nowPlaying.displayTitle,
            artist = state.nowPlaying.displayArtist,
            isPlaying = state.nowPlaying.isPlaying
        ) }
        item { IntelligenceCard(state, primary) }
        item { HeadphoneCard(state, primary) }
        item { SoundProfileCard(state, primary) }
        item { SonaraVisualizer(isPlaying = state.nowPlaying.isPlaying) }
        item { QuickActions(primary) }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Intelligence", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                Spacer(Modifier.height(4.dp))
                if (state.isResolving) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = primary)
                        Text("Analyzing...", style = MaterialTheme.typography.bodyLarge, color = SonaraTextPrimary)
                    }
                } else if (state.hasTrackInfo) {
                    Text(
                        "${state.genre.replaceFirstChar { it.uppercase() }} · ${state.mood.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SonaraTextPrimary
                    )
                } else {
                    Text("Waiting for music...", style = MaterialTheme.typography.bodyLarge, color = SonaraTextPrimary)
                }
            }
            if (state.hasTrackInfo) {
                val sourceIcon = when (state.resolveResult.source) {
                    ResolveSource.LASTFM, ResolveSource.LASTFM_ARTIST -> Icons.Rounded.Public
                    ResolveSource.LOCAL_AI -> Icons.Rounded.Memory
                    else -> Icons.Rounded.AutoAwesome
                }
                StatusChip(state.sourceLabel, ChipStatus.Active, sourceIcon)
            } else {
                StatusChip("Idle", ChipStatus.Inactive)
            }
        }

        if (state.hasTrackInfo) {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("Energy", "${(state.energy * 100).toInt()}%", Modifier.weight(1f), primary)
                InfoPill("Confidence", "${(state.confidence * 100).toInt()}%", Modifier.weight(1f), primary)
            }
        }
    }
}

@Composable
private fun InfoPill(label: String, value: String, modifier: Modifier = Modifier, primary: androidx.compose.ui.graphics.Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = SonaraCardElevated
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            Text(value, style = MaterialTheme.typography.labelLarge, color = primary)
        }
    }
}

@Composable
private fun HeadphoneCard(state: DashboardUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val hpIcon = if (state.headphone.isConnected) {
                    when (state.headphone.type) {
                        ConnectionType.BLUETOOTH_A2DP, ConnectionType.BLUETOOTH_LE -> Icons.Rounded.Bluetooth
                        ConnectionType.WIRED -> Icons.Rounded.Cable
                        ConnectionType.USB -> Icons.Rounded.Usb
                        else -> Icons.Rounded.Headphones
                    }
                } else Icons.Rounded.HeadsetOff

                Icon(hpIcon, null, tint = if (state.headphone.isConnected) primary else SonaraTextTertiary, modifier = Modifier.size(20.dp))
                Column {
                    Text("Headphone", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                    Text(
                        if (state.headphone.isConnected) state.headphone.name else "No device connected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SonaraTextPrimary
                    )
                }
            }
            val aeqStatus = when {
                !state.headphone.isConnected -> ChipStatus.Inactive
                state.autoEqState.isActive -> ChipStatus.Active
                !state.isAutoEqEnabled -> ChipStatus.Inactive
                else -> ChipStatus.Warning
            }
            val aeqLabel = when {
                !state.headphone.isConnected -> "No Device"
                state.autoEqState.isActive -> "AutoEQ On"
                !state.isAutoEqEnabled -> "AutoEQ Off"
                else -> "No Profile"
            }
            StatusChip(aeqLabel, aeqStatus)
        }

        if (state.autoEqState.isActive && state.autoEqState.profile != null) {
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = SonaraCardElevated) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Profile: ${state.autoEqState.profile.name}", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    Text("${(state.autoEqState.profile.matchConfidence * 100).toInt()}% match", style = MaterialTheme.typography.labelSmall, color = primary)
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
                state.bands.forEachIndexed { i, v ->
                    val normalized = ((v + 12f) / 24f).coerceIn(0.08f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((normalized * 36).dp)
                            .background(
                                primary.copy(alpha = 0.2f + normalized * 0.5f),
                                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(primary: androidx.compose.ui.graphics.Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickActionBtn(Modifier.weight(1f), Icons.Rounded.SwapHoriz, "Compare")
        QuickActionBtn(Modifier.weight(1f), Icons.Rounded.Save, "Save")
        QuickActionBtn(Modifier.weight(1f), Icons.Rounded.Refresh, "Reset")
    }
}

@Composable
private fun QuickActionBtn(modifier: Modifier, icon: ImageVector, label: String) {
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
            Icon(icon, label, tint = SonaraTextSecondary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = SonaraTextSecondary)
        }
    }
}
