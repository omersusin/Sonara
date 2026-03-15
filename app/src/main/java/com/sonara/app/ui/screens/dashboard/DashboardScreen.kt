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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
    val albumArt by viewModel.albumArt.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.checkNotificationListener()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Sonara", style = MaterialTheme.typography.headlineLarge, color = primary)
                    Spacer(Modifier.height(2.dp))
                    Text("Personal Sound Engine", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.eqActive) StatusChip("EQ Active", ChipStatus.Active)
                    StatusChip(if (state.isAiEnabled) "AI On" else "AI Off",
                        if (state.isAiEnabled) ChipStatus.Active else ChipStatus.Inactive, Icons.Rounded.AutoAwesome)
                }
            }
        }

        if (!state.notificationListenerEnabled) {
            item { PermissionCard(onGrant = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) }
        }

        item { NowPlayingBar(
            title = if (state.hasTrack) state.title else "No music playing",
            artist = state.artist, isPlaying = state.isPlaying, albumArt = albumArt
        ) }

        item {
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
                        } else Text("Waiting for music...", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (state.hasTrack && state.sourceLabel != "None") {
                        val icon = if (state.sourceLabel.contains("Last")) Icons.Rounded.Public else Icons.Rounded.Memory
                        StatusChip(state.sourceLabel, ChipStatus.Active, icon)
                    } else StatusChip("Idle", ChipStatus.Inactive)
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

        item {
            FluentCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val hpIcon = if (state.headphoneConnected) { if (state.headphoneType.contains("BLUETOOTH")) Icons.Rounded.Bluetooth else Icons.Rounded.Headphones } else Icons.Rounded.HeadsetOff
                        Icon(hpIcon, null, tint = if (state.headphoneConnected) primary else SonaraTextTertiary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Headphone", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                            Text(if (state.headphoneConnected) state.headphoneName else "No device", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    StatusChip(
                        when { !state.headphoneConnected -> "No Device"; state.autoEqActive -> "AutoEQ"; !state.isAutoEqEnabled -> "Off"; else -> "No Profile" },
                        when { !state.headphoneConnected -> ChipStatus.Inactive; state.autoEqActive -> ChipStatus.Active; else -> ChipStatus.Inactive }
                    )
                }
            }
        }

        item {
            FluentCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sound Profile", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                    Text(state.currentPresetName, style = MaterialTheme.typography.labelLarge, color = primary)
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    state.bands.forEach { v ->
                        val n = ((v + 12f) / 24f).coerceIn(0.08f, 1f)
                        Box(modifier = Modifier.weight(1f).height((n * 36).dp).background(primary.copy(alpha = 0.2f + n * 0.5f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                    }
                }
                if (state.aiReasoning.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text(state.aiReasoning, style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary, maxLines = 2) }
            }
        }

        item { SonaraVisualizer(isPlaying = state.isPlaying) }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickBtn(Modifier.weight(1f), Icons.Rounded.SwapHoriz, "Compare")
                QuickBtn(Modifier.weight(1f), Icons.Rounded.Save, "Save")
                QuickBtn(Modifier.weight(1f), Icons.Rounded.Refresh, "Reset")
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
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
private fun QuickBtn(modifier: Modifier, icon: ImageVector, label: String) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = SonaraCard, border = BorderStroke(0.6.dp, SonaraDivider.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, label, tint = SonaraTextSecondary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = SonaraTextSecondary)
        }
    }
}
