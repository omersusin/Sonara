package com.sonara.app.ui.screens.dashboard

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import com.sonara.app.ai.models.FeedbackType
import com.sonara.app.ui.components.ChipStatus
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.components.MoodRing
import com.sonara.app.ui.components.NowPlayingBar
import com.sonara.app.ui.components.PermissionCard
import com.sonara.app.ui.components.SonaraVisualizer
import com.sonara.app.ui.components.StatusChip
import com.sonara.app.ui.components.VisualizerMode
import com.sonara.app.ui.components.VisualizerStateDetector
import com.sonara.app.ui.theme.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen() {
    val vm: DashboardViewModel = viewModel()
    val lyricsVm: LyricsViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val art by vm.albumArt.collectAsState()
    val aiState by vm.aiState.collectAsState()
    val vizData by vm.visualizerData.collectAsState()
    val lyricsState by lyricsVm.state.collectAsState()
    val p = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    val lc = LocalLifecycleOwner.current
    LaunchedEffect(lc) { lc.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { vm.checkNotificationListener() } }
    LaunchedEffect(s.savedMessage) { if (s.savedMessage.isNotBlank()) Toast.makeText(ctx, s.savedMessage, Toast.LENGTH_SHORT).show() }
    // Trigger lyrics load when track changes
    LaunchedEffect(s.title, s.artist) {
        if (s.hasTrack) lyricsVm.load(s.title, s.artist, "", s.duration)
        else lyricsVm.reset()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Sonara", style = MaterialTheme.typography.headlineLarge, color = p)
                    Text("Personal Sound Engine", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(if (s.eqActive) "EQ On" else "EQ Off", if (s.eqActive) ChipStatus.Active else ChipStatus.Inactive)
                    StatusChip(if (s.isAiEnabled) "AI On" else "AI Off", if (s.isAiEnabled) ChipStatus.Active else ChipStatus.Inactive, Icons.Rounded.AutoAwesome)
                }
            }
        }

        if (!s.notificationListenerEnabled) {
            item { PermissionCard(onGrant = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) }
        }

        if (s.hearTheDiffEnabled && !s.hasSeenHearTheDifference && s.hasTrack && s.eqActive) {
            item {
                HearTheDifferenceBanner(
                    onHoldStart = { vm.setEqTemporarilyDisabled(true) },
                    onHoldEnd = { vm.setEqTemporarilyDisabled(false) },
                    onDismiss = { vm.dismissHearTheDifference() }
                )
            }
        }

        item {
            NowPlayingBar(
                title = if (s.hasTrack) s.title else "No music playing",
                artist = s.artist,
                isPlaying = s.isPlaying,
                albumArt = art,
                duration = s.duration,
                position = s.position,
                positionTimestamp = s.positionTimestamp,
                lyricsState = lyricsState
            )
        }

        // Unified AI Analysis + MoodRing card
        if (s.hasTrack) {
            item {
                var feedbackSent by remember(s.title) { mutableStateOf(false) }
                var feedbackText by remember(s.title) { mutableStateOf("") }
                FluentCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        MoodRing(mood = s.mood, energy = s.energy, genre = s.genre, modifier = Modifier.size(112.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (aiState.isReady) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Hearing, null, Modifier.size(14.dp), tint = p)
                                    Text(aiState.status.display, style = MaterialTheme.typography.labelSmall, color = p)
                                }
                            }
                            if (s.sourceLabel != "None") StatusChip(
                                s.sourceLabel, ChipStatus.Active,
                                if (s.sourceLabel.contains("Last")) Icons.Rounded.Public else Icons.Rounded.Memory,
                                compact = true
                            )
                            Text("${s.genre} / ${s.mood}", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                            Text("Confidence ${(s.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                            if (s.songsLearned > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.School, null, Modifier.size(12.dp), tint = p)
                                    Text("${s.songsLearned} songs learned", style = MaterialTheme.typography.labelSmall, color = p)
                                }
                            }
                        }
                    }
                    if (s.geminiSummary.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(s.geminiSummary, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    }
                    val hasPills = s.bassBoost > 0 || s.virtualizer > 0
                    if (hasPills) {
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (s.bassBoost > 0) Pill("Bass", "${(s.bassBoost / 10f).toInt()}%", Modifier.weight(1f), p)
                            if (s.virtualizer > 0) Pill("Surround", "${(s.virtualizer / 10f).toInt()}%", Modifier.weight(1f), p)
                        }
                    }
                    if (aiState.isReady) {
                        if (!feedbackSent) {
                            if (s.legacyAnalysis) {
                                Spacer(Modifier.height(10.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FeedbackType.allOptions.forEach { fb ->
                                        AssistChip(onClick = { vm.onAiFeedback(fb.id); feedbackSent = true
                                            Toast.makeText(ctx, "${fb.emoji} ${fb.label}", Toast.LENGTH_SHORT).show() },
                                            label = { Text("${fb.emoji} ${fb.label}", style = MaterialTheme.typography.labelSmall) },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = SonaraCardElevated))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = feedbackText, onValueChange = { feedbackText = it },
                                    placeholder = { Text("Tell AI what you want...", color = SonaraTextTertiary, style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.weight(1f).height(44.dp), singleLine = true, textStyle = MaterialTheme.typography.bodySmall,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p, unfocusedBorderColor = SonaraDivider.copy(0.5f),
                                        cursorColor = p, focusedTextColor = SonaraTextPrimary, unfocusedTextColor = SonaraTextPrimary))
                                FilledTonalButton(onClick = { vm.onAiFeedback("custom:$feedbackText"); feedbackSent = true
                                    Toast.makeText(ctx, "Applying...", Toast.LENGTH_SHORT).show() },
                                    enabled = feedbackText.isNotBlank(),
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = p, contentColor = SonaraBackground)
                                ) { Text("Apply", style = MaterialTheme.typography.labelSmall) }
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            Text("✓ AI applied your request!", style = MaterialTheme.typography.labelSmall, color = SonaraSuccess)
                        }
                    }
                }
            }
        } else {
            item { FluentCard { Text("Play some music to start", style = MaterialTheme.typography.bodyLarge, color = SonaraTextSecondary) } }
        }

        item {
            FluentCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sound Profile", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                    Text(s.currentPresetName, style = MaterialTheme.typography.labelLarge, color = p)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    s.bands.copyOf().take(10).forEach { v ->
                        val n = ((v + 12f) / 24f).coerceIn(0.08f, 1f)
                        Box(Modifier.weight(1f).height((n * 36).dp).background(p.copy(alpha = 0.2f + n * 0.5f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (s.hasTrack && !s.isManualPreset) {
                        var showSaveDialog by remember { mutableStateOf(false) }
                        var saveName by remember { mutableStateOf(if (s.genre != "Unknown") "AI: ${s.genre} (${s.mood})" else "AI Preset") }
                        TextButton(onClick = { showSaveDialog = true }) {
                            Icon(Icons.Rounded.Save, null, Modifier.size(16.dp), tint = p)
                            Spacer(Modifier.width(4.dp))
                            Text("Save Preset", color = p)
                        }
                        if (showSaveDialog) {
                            AlertDialog(
                                onDismissRequest = { showSaveDialog = false },
                                containerColor = SonaraCard,
                                title = { Text("Save Preset") },
                                text = {
                                    OutlinedTextField(
                                        value = saveName,
                                        onValueChange = { saveName = it },
                                        label = { Text("Preset name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        if (saveName.isNotBlank()) {
                                            vm.saveCurrentAsPreset(saveName)
                                            showSaveDialog = false
                                        }
                                    }) { Text("Save", color = p) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showSaveDialog = false }) { Text("Cancel", color = SonaraTextSecondary) }
                                }
                            )
                        }
                    } else { Spacer(Modifier.width(1.dp)) }
                    TextButton(onClick = { vm.resetToAi() }) {
                        Icon(Icons.Rounded.RestartAlt, null, Modifier.size(16.dp), tint = p)
                        Spacer(Modifier.width(4.dp))
                        Text("Reset to AI", color = p)
                    }
                }
            }
        }

        item {
            val vizMode = VisualizerStateDetector.detect(hasAudioSession = s.eqActive, hasVisualizerPermission = false, isPlaying = s.isPlaying)
            Column {
                SonaraVisualizer(isPlaying = s.isPlaying, fftData = vizData)
                Spacer(Modifier.height(4.dp))
                Text("Visualizer: ${if (vizData != null) "Live" else vizMode.label}", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, modifier = Modifier.padding(start = 8.dp))
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun Pill(label: String, value: String, modifier: Modifier, p: androidx.compose.ui.graphics.Color) {
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = SonaraCardElevated) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            Text(value, style = MaterialTheme.typography.labelLarge, color = p)
        }
    }
}


@Composable
private fun HearTheDifferenceBanner(
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onDismiss: () -> Unit
) {
    var isHolding by remember { mutableStateOf(false) }
    val p = MaterialTheme.colorScheme.primary

    FluentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = Icons.Default.Hearing, contentDescription = null, tint = p, modifier = Modifier.size(20.dp))
            Text("Hear the Difference", style = MaterialTheme.typography.titleSmall, color = p)
        }
        Spacer(Modifier.height(8.dp))
        Text("Hold to hear original audio without EQ.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                color = if (isHolding) p.copy(alpha = 0.2f) else SonaraCardElevated) {
                Text(if (isHolding) "Playing Original" else "Original",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isHolding) p else SonaraTextTertiary,
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                color = if (!isHolding) p.copy(alpha = 0.2f) else SonaraCardElevated) {
                Text(if (!isHolding) "Enhanced" else "Sonara EQ",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (!isHolding) p else SonaraTextTertiary,
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }

        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(p.copy(alpha = 0.15f))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    isHolding = true; onHoldStart()
                    try { awaitRelease() } finally { isHolding = false; onHoldEnd() }
                })
            }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(if (isHolding) "Release to hear enhanced" else "Hold to hear original",
                style = MaterialTheme.typography.labelMedium, color = p)
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Got it", color = SonaraTextSecondary) }
        }
    }
}
