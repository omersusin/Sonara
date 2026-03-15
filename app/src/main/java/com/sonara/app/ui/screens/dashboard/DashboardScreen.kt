package com.sonara.app.ui.screens.dashboard

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val vm: DashboardViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val art by vm.albumArt.collectAsState()
    val p = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    val lc = LocalLifecycleOwner.current

    LaunchedEffect(lc) { lc.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { vm.checkNotificationListener() } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Sonara", style = MaterialTheme.typography.headlineLarge, color = p); Spacer(Modifier.height(2.dp)); Text("Personal Sound Engine", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (s.eqActive) StatusChip("EQ Active", ChipStatus.Active)
                    StatusChip(if (s.isAiEnabled) "AI On" else "AI Off", if (s.isAiEnabled) ChipStatus.Active else ChipStatus.Inactive, Icons.Rounded.AutoAwesome)
                }
            }
        }
        if (!s.notificationListenerEnabled) { item { PermissionCard(onGrant = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) } }
        item { NowPlayingBar(if (s.hasTrack) s.title else "No music playing", s.artist, s.isPlaying, art) }

        item {
            FluentCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Intelligence", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary); Spacer(Modifier.height(4.dp))
                        if (s.isResolving) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = p); Text("Analyzing...", style = MaterialTheme.typography.bodyLarge) } }
                        else if (s.hasTrack && s.sourceLabel != "None") Text("${s.genre.replaceFirstChar { it.uppercase() }} · ${s.mood.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodyLarge)
                        else Text("Waiting for music...", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (s.hasTrack && s.sourceLabel != "None") StatusChip(s.sourceLabel, ChipStatus.Active, if (s.sourceLabel.contains("Last")) Icons.Rounded.Public else Icons.Rounded.Memory)
                    else StatusChip("Idle", ChipStatus.Inactive)
                }
                if (s.hasTrack && s.sourceLabel != "None") { Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pill("Energy", "${(s.energy * 100).toInt()}%", Modifier.weight(1f), p); Pill("Confidence", "${(s.confidence * 100).toInt()}%", Modifier.weight(1f), p) } }
            }
        }

        item {
            FluentCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val ic = if (s.headphoneConnected) { if (s.headphoneType.contains("BLUETOOTH")) Icons.Rounded.Bluetooth else Icons.Rounded.Headphones } else Icons.Rounded.HeadsetOff
                        Icon(ic, null, tint = if (s.headphoneConnected) p else SonaraTextTertiary, modifier = Modifier.size(20.dp))
                        Column { Text("Headphone", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary); Text(if (s.headphoneConnected) s.headphoneName else "No device", style = MaterialTheme.typography.bodyLarge) }
                    }
                    StatusChip(when { !s.headphoneConnected -> "No Device"; s.autoEqActive -> "AutoEQ"; else -> "Connected" },
                        when { !s.headphoneConnected -> ChipStatus.Inactive; s.autoEqActive -> ChipStatus.Active; else -> ChipStatus.Warning })
                }
            }
        }

        item {
            FluentCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sound Profile", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                    Text(s.currentPresetName, style = MaterialTheme.typography.labelLarge, color = p)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    s.bands.forEach { v -> val n = ((v + 12f) / 24f).coerceIn(0.08f, 1f); Box(Modifier.weight(1f).height((n * 36).dp).background(p.copy(alpha = 0.2f + n * 0.5f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))) }
                }
                if (s.aiReasoning.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text(s.aiReasoning, style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary, maxLines = 2) }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { vm.resetToAi() }) { Text("Reset to AI Auto", color = p) }
                }
            }
        }

        item { SonaraVisualizer(isPlaying = s.isPlaying) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun Pill(label: String, value: String, modifier: Modifier, p: androidx.compose.ui.graphics.Color) {
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = SonaraCardElevated) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary); Text(value, style = MaterialTheme.typography.labelLarge, color = p)
        }
    }
}
