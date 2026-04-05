@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sonara.app.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.Environment
import java.io.File
import android.widget.Toast
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.SonaraApp
import com.sonara.app.data.BackupManager
import com.sonara.app.autoeq.AutoEqImporter
import com.sonara.app.preset.PresetExporter
import com.sonara.app.ui.components.ChipStatus
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.components.StatusChip
import com.sonara.app.ui.theme.*

@Composable
fun SettingsScreen(onOpenDebugLog: () -> Unit = {}, onOpenPipelineDebug: () -> Unit = {}) {
    val vm: SettingsViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val ctx = LocalContext.current

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 8.dp)) }

        if (!state.notificationListenerEnabled) {
            item { NotificationCard(ctx) }
        }

        item { SectionHeader("Last.fm Integration") }
        item { LastFmCard(state, vm, ctx) }


        item { SectionHeader("Appearance") }
        item { AppearanceCard(state, vm) }

        item { SectionHeader("Playback & EQ") }
        item { SoundEngineCard(state, vm) }

        item { SectionHeader("AI Sources") }
        item { AiSourcesCard(state, vm) }

        item { AdvancedCard(state, vm) }

        // Gemini merged into AI Sources

        // Theme merged into Appearance above

        item { PresetExportImportCard(vm) }

        item { SectionHeader("Community") }
        item { CommunityCard(state, vm) }

        item { SectionHeader("Data & Developer") }
        item { DataCard(state, vm) }
        item {
            FluentCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Debug Log", style = MaterialTheme.typography.titleMedium)
                        Text("View real-time app logs", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    }
                    OutlinedButton(onClick = onOpenDebugLog, shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Open") }
                }
            }
        }
        item {
            FluentCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Pipeline Debug", style = MaterialTheme.typography.titleMedium)
                        Text("Track detection, source, EQ state", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    }
                    OutlinedButton(onClick = onOpenPipelineDebug, shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Open") }
                }
            }
        }
        item { SectionHeader("About") }
        item { AboutCard(state, vm) }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun CommunityCard(state: SettingsUiState, vm: SettingsViewModel) {
    val p = MaterialTheme.colorScheme.primary
    FluentCard {
        var showCommunityHelp by remember { mutableStateOf(false) }
        if (showCommunityHelp) {
            AlertDialog(
                onDismissRequest = { showCommunityHelp = false },
                containerColor = SonaraCard,
                title = { Text("How Community Works") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Download", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                        Text("Downloads community-trained audio classification data to improve accuracy from day one.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        HorizontalDivider(color = SonaraDivider.copy(0.3f))
                        Text("Contribute", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                        Text("Shares anonymous audio feature vectors (NOT audio recordings) to help improve the model for everyone.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        HorizontalDivider(color = SonaraDivider.copy(0.3f))
                        Text("GitHub Token", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                        Text("Required for contributions. Create a fine-grained token at github.com/settings/tokens with repo:sonara-models read/write access.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    }
                },
                confirmButton = { TextButton(onClick = { showCommunityHelp = false }) { Text("Got it") } },
                shape = MaterialTheme.shapes.extraLarge
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Community", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { showCommunityHelp = true }) { Text("How it works?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Use community data", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                Text("Improves accuracy from day one", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
            Switch(checked = state.communityDownloadEnabled, onCheckedChange = { vm.setCommunityDownload(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = p))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Contribute to community", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                Text("Share anonymous audio data to help improve Sonara", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
            Switch(checked = state.communityUploadEnabled, onCheckedChange = { vm.setCommunityUpload(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = p))
        }
        if (state.communityUploadEnabled) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = SonaraDivider.copy(0.3f))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.communityPending.toString(), style = MaterialTheme.typography.titleLarge, color = p)
                    Text("Pending", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.communityTotalSent.toString(), style = MaterialTheme.typography.titleLarge, color = p)
                    Text("Total sent", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
            }
        }

            // GitHub PAT for community contributions
            Spacer(Modifier.height(12.dp))
            Text(
                text = "GitHub Token (for contributions)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            if (state.isGithubTokenSet) {
                Text(
                    text = "Token saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                OutlinedTextField(
                    value = state.githubTokenInput,
                    onValueChange = { vm.updateGithubTokenInput(it) },
                    label = { Text("Personal Access Token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { vm.saveGithubToken() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.githubTokenInput.isNotBlank()
                ) {
                    Text("Save Token")
                }
            }
    }
}

@Composable
private fun SectionHeader(t: String) {
    Text(t.uppercase(), style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp))
}

@Composable
private fun NotificationCard(ctx: Context) {
    FluentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Notifications, null, tint = SonaraWarning, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text("Notification Access", style = MaterialTheme.typography.titleMedium)
                Text("Required to detect playing music", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) { Text("Grant Permission") }
    }
}

@Composable
private fun LastFmCard(state: SettingsUiState, vm: SettingsViewModel, ctx: Context) {
    val p = MaterialTheme.colorScheme.primary
    FluentCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Last.fm", style = MaterialTheme.typography.titleMedium)
            StatusChip(
                if (state.lastFmConnected) "Connected" else "Not Connected",
                if (state.lastFmConnected) ChipStatus.Active else ChipStatus.Warning
            )
        }
        Spacer(Modifier.height(8.dp))
        when {
            state.lastFmConnected -> {
                Text("Connected as ${state.lastFmUsername}", style = MaterialTheme.typography.bodySmall, color = SonaraSuccess)
                Spacer(Modifier.height(4.dp))
                Text("Genre detection and scrobbling active.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.disconnectLastFm() }, shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(1.dp, SonaraError.copy(0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraError)
                    ) { Text("Disconnect") }
                    OutlinedButton(onClick = { vm.connectLastFm { intent -> ctx.startActivity(intent) } },
                        shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(1.dp, SonaraDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraTextSecondary)
                    ) { Text("Reconnect") }
                }
            }
            else -> {
                Text("Connect your Last.fm account for accurate genre detection and scrobbling.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { vm.connectLastFm { intent -> ctx.startActivity(intent) } },
                    Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, SonaraInfo),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraInfo)
                ) { Text("Connect Last.fm") }

                // --- API Key guide + inputs ---
                var showApiGuide by remember { mutableStateOf(false) }
                if (showApiGuide) {
                    AlertDialog(
                        onDismissRequest = { showApiGuide = false },
                        containerColor = SonaraCard,
                        title = { Text("How to get Last.fm API Key") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("1. Open last.fm/api/account/create", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                                Text("2. Application Name: Sonara", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                                Text("3. Leave Callback URL empty", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                                Text("4. Click Submit", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                                Text("5. Copy API Key and Shared Secret", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                                Text("6. Paste them below and tap Save Keys", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showApiGuide = false
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/api/account/create")))
                            }) { Text("Open Last.fm") }
                        },
                        dismissButton = { TextButton(onClick = { showApiGuide = false }) { Text("Close") } },
                        shape = MaterialTheme.shapes.extraLarge
                    )
                }
                if (!state.lastFmConnected) {
                    TextButton(onClick = { showApiGuide = true }) {
                        Text("How to get API keys?", color = p, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.apiKeyInput,
                        onValueChange = { vm.updateApiKeyInput(it) },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = MaterialTheme.shapes.extraSmall
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.sharedSecretInput,
                        onValueChange = { vm.updateSharedSecretInput(it) },
                        label = { Text("Shared Secret") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = MaterialTheme.shapes.extraSmall
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { vm.saveApiKey(); vm.saveSharedSecret() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.apiKeyInput.isNotBlank() && state.sharedSecretInput.isNotBlank()
                    ) {
                        Text("Save Keys")
                    }
                }

            }
        }
    }
}



@Composable
private fun AutoEqImportCard() {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<AutoEqImporter.ImportResult?>(null) }
    val clipboard = LocalClipboardManager.current
    val p = MaterialTheme.colorScheme.primary

    FluentCard {
        var showAutoEqHelp by remember { mutableStateOf(false) }
        if (showAutoEqHelp) {
            AlertDialog(
                onDismissRequest = { showAutoEqHelp = false },
                containerColor = SonaraCard,
                title = { Text("How AutoEQ Works") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AutoEQ compensates for your headphone's frequency response to deliver a flatter, more accurate sound.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        HorizontalDivider(color = SonaraDivider.copy(0.3f))
                        Text("How to get your profile:", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                        Text("1. Go to github.com/jaakkopasanen/AutoEq", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        Text("2. Find your headphone model", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        Text("3. Copy the GraphicEQ line from the README", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        Text("4. Paste it here and tap Parse -> Apply", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    }
                },
                confirmButton = { TextButton(onClick = { showAutoEqHelp = false }) { Text("Got it") } },
                shape = MaterialTheme.shapes.extraLarge
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Import AutoEQ Profile", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { showAutoEqHelp = true }) { Text("How to?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(4.dp))
        Text("Paste AutoEQ GraphicEQ data or 10 comma-separated gain values (dB)",
            style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = input, onValueChange = { input = it; result = null },
            placeholder = { Text("Paste EQ data here...", color = SonaraTextTertiary) },
            modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 8, shape = MaterialTheme.shapes.small,
            trailingIcon = { IconButton(onClick = { clipboard.getText()?.text?.let { input = it } }) { Icon(Icons.Rounded.ContentPaste, "Paste", tint = SonaraTextSecondary) } },
            colors = tfColors()
        )
        Spacer(Modifier.height(10.dp))
        if (result != null) {
            if (result!!.success) {
                StatusChip("Parsed successfully", ChipStatus.Active)
                Spacer(Modifier.height(4.dp))
                Text("Values: ${result!!.bands.joinToString(", ") { "%.1f".format(it) }}",
                    style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            } else {
                StatusChip("Error: ${result!!.error}", ChipStatus.Error)
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { result = AutoEqImporter.parseGraphicEq(input) },
                enabled = input.isNotBlank(), modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (input.isNotBlank()) p else SonaraDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = p)
            ) { Text("Parse") }
            OutlinedButton(
                onClick = { result?.let { r -> if (r.success) SonaraApp.instance.applyEq(bands = r.bands, presetName = "AutoEQ Import", manual = true) } },
                enabled = result?.success == true, modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (result?.success == true) SonaraSuccess else SonaraDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraSuccess)
            ) { Text("Apply") }
        }
    }
}



@Composable
private fun SoundEngineCard(s: SettingsUiState, vm: SettingsViewModel) {
    FluentCard {
        SwitchRow("AI Auto-adjust", "Automatically adjust EQ based on genre and mood", s.aiEnabled) { vm.setAiEnabled(it) }
        SettingsDivider()
        SwitchRow("AutoEQ", "Apply headphone correction", s.autoEqEnabled) { vm.setAutoEqEnabled(it) }
        SettingsDivider()
        SwitchRow("Auto Preset", "Auto-select preset based on genre", s.autoPreset) { vm.setAutoPreset(it) }
    }
}

@Composable
private fun AdvancedCard(s: SettingsUiState, vm: SettingsViewModel) {
    FluentCard {
        SwitchRow("Smooth Transitions", "Gradual EQ changes between tracks", s.smoothTransitions) { vm.setSmoothTransitions(it) }
        SettingsDivider()
        SwitchRow("Safety Limiter", "Prevent audio clipping", s.safetyLimiter) { vm.setSafetyLimiter(it) }
        SettingsDivider()
        SwitchRow("Scrobbling", "Send listening history to Last.fm", s.scrobblingEnabled) { vm.setScrobblingEnabled(it) }
        if (s.pendingScrobbles > 0) {
            Spacer(Modifier.height(4.dp))
            Text("Pending: ${s.pendingScrobbles} scrobbles queued", style = MaterialTheme.typography.bodySmall, color = SonaraWarning)
        }
        SettingsDivider()
        SwitchRow("Keep Notification", "Show notification when paused", s.keepNotificationPaused) { vm.setKeepNotificationPaused(it) }
    }
}

@Composable
private fun PresetExportImportCard(vm: SettingsViewModel) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showImportDialog by remember { mutableStateOf(false) }
    var importInput by remember { mutableStateOf("") }
    var importResult by remember { mutableStateOf("") }
    val p = MaterialTheme.colorScheme.primary

    FluentCard {
        Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Full backup: all settings, presets, and learning data",
            style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.exportFullBackup { json ->
                    val file = BackupManager.saveToFile(json)
                    if (file != null) {
                        Toast.makeText(ctx, "Saved to Downloads/${file.name}", Toast.LENGTH_LONG).show()
                    } else {
                        clipboard.setText(AnnotatedString(json))
                        Toast.makeText(ctx, "File failed, copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                } },
                modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, p),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = p)
            ) {
                Icon(Icons.Rounded.Upload, null, Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("Export")
            }
            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, p),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = p)
            ) {
                Icon(Icons.Rounded.Download, null, Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("Import")
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; importInput = ""; importResult = "" },
            containerColor = SonaraCard,
            title = { Text("Restore Backup") },
            text = {
                Column {
                    Text("Paste exported Sonara preset JSON:", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInput, onValueChange = { importInput = it; importResult = "" },
                        placeholder = { Text("Paste JSON here...", color = SonaraTextTertiary) },
                        modifier = Modifier.fillMaxWidth().height(150.dp), maxLines = 10,
                        shape = MaterialTheme.shapes.small,
                        trailingIcon = { IconButton(onClick = { clipboard.getText()?.text?.let { importInput = it } }) { Icon(Icons.Rounded.ContentPaste, "Paste", tint = SonaraTextSecondary) } },
                        colors = tfColors()
                    )
                    if (importResult.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(importResult, style = MaterialTheme.typography.bodySmall,
                            color = if (importResult.startsWith("✓")) SonaraSuccess else SonaraError)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.importFullBackup(importInput) { msg -> importResult = msg }
                }) { Text("Import", color = p) }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; importInput = ""; importResult = "" }) {
                    Text("Cancel", color = SonaraTextSecondary)
                }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

@Composable
private fun DataCard(s: SettingsUiState, vm: SettingsViewModel) {
    FluentCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Track Cache", style = MaterialTheme.typography.titleMedium)
                Text("${s.cacheSize} tracks | ${s.personalSamples} learned", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
            OutlinedButton(
                onClick = { vm.clearCache() }, shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, SonaraDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraError)
            ) { Text("Clear") }
        }
        SettingsDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Reset All", style = MaterialTheme.typography.titleMedium)
                Text("Restore defaults", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
            OutlinedButton(
                onClick = { vm.clearAllData() }, shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, SonaraError.copy(0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraError)
            ) { Text("Reset") }
        }
    }
}



@Composable
private fun AppearanceCard(s: SettingsUiState, vm: SettingsViewModel) {
    val p = MaterialTheme.colorScheme.primary
    FluentCard {
        Text("Accent Color", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(if (s.accentColor == AccentColor.Auto) "Wallpaper colors" else s.accentColor.displayName,
            style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(16.dp))
        val colors = AccentColor.entries.filter { it != AccentColor.Auto }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            colors.forEach { c ->
                val sel = c == s.accentColor
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .then(if (c == AccentColor.Auto) Modifier.background(Brush.sweepGradient(listOf(SonaraInfo, SonaraSuccess, SonaraBandLow, SonaraError, SonaraWarning, SonaraInfo))) else Modifier.background(c.primary))
                        .then(if (sel) Modifier.border(2.5.dp, SonaraTextPrimary, CircleShape) else Modifier.border(1.dp, SonaraDivider.copy(0.3f), CircleShape))
                        .clickable { vm.setAccentColor(c) },
                    contentAlignment = Alignment.Center
                ) { if (sel) Icon(Icons.Rounded.Check, null, tint = SonaraBackground, modifier = Modifier.size(18.dp)) }
            }
        }
        SettingsDivider()
        Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (id, label) ->
                val sel = s.themeMode == id
                OutlinedButton(onClick = { vm.setThemeMode(id) },
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, if (sel) p else SonaraDivider),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (sel) p else SonaraTextSecondary),
                    modifier = Modifier.weight(1f)
                ) { Text(label) }
            }
        }
        SettingsDivider()
        SwitchRow("AMOLED Mode", "Pure black background", s.amoledMode) { vm.setAmoledMode(it) }
        SettingsDivider()
        SwitchRow("Dynamic Colors", "Use wallpaper colors (Android 12+)", s.dynamicColors) { vm.setDynamicColors(it) }
        SettingsDivider()
        SwitchRow("High Contrast", "Increase text contrast", s.highContrast) { vm.setHighContrast(it) }
    }
}

@Composable
private fun AiSourcesCard(s: SettingsUiState, vm: SettingsViewModel) {
    FluentCard {
        var showAiSourcesHelp by remember { mutableStateOf(false) }
        if (showAiSourcesHelp) {
            AlertDialog(
                onDismissRequest = { showAiSourcesHelp = false },
                containerColor = SonaraCard,
                title = { Text("How AI Sources Work") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Last.fm", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                        Text("Fetches genre tags and metadata from the Last.fm database. Most accurate source for well-known tracks.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        HorizontalDivider(color = SonaraDivider.copy(0.3f))
                        Text("Local AI", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                        Text("On-device classification using artist/title patterns and audio features. Works offline.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        HorizontalDivider(color = SonaraDivider.copy(0.3f))
                        Text("Lyrics", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                        Text("Analyzes lyrics tone and language for mood detection. Adds emotional context to EQ choices.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        HorizontalDivider(color = SonaraDivider.copy(0.3f))
                        Text("Merged", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                        Text("When multiple sources agree, confidence is higher and the result is marked as Merged.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    }
                },
                confirmButton = { TextButton(onClick = { showAiSourcesHelp = false }) { Text("Got it") } },
                shape = MaterialTheme.shapes.extraLarge
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("AI Sources", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { showAiSourcesHelp = true }) { Text("How it works?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(4.dp))
        Text("Choose which sources AI uses for genre detection", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(8.dp))
        SwitchRow("Last.fm", "Online genre tags and metadata", s.sourceLastFm) { vm.setSourceLastFm(it) }
        SettingsDivider()
        SwitchRow("Local AI", "On-device title/artist classification", s.sourceLocalAi) { vm.setSourceLocalAi(it) }
        SettingsDivider()
        SwitchRow("Lyrics", "Lyrics-based tone and mood analysis", s.sourceLyrics) { vm.setSourceLyrics(it) }
        SettingsDivider()
        Text("AI Provider", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Primary provider for AI insights (with fallback)", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("gemini" to "Gemini", "openrouter" to "OpenRouter", "groq" to "Groq").forEach { (id, label) ->
                val sel = s.aiProvider == id
                val p2 = MaterialTheme.colorScheme.primary
                OutlinedButton(onClick = { vm.setAiProvider(id) },
                    modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, if (sel) p2 else SonaraDivider),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (sel) p2 else SonaraTextSecondary)
                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
            }
        }
                if (s.aiProvider == "gemini") {
            Spacer(Modifier.height(8.dp))
            val p2 = MaterialTheme.colorScheme.primary
            OutlinedTextField(value = s.geminiKeyInput, onValueChange = { vm.updateGeminiKeyInput(it) },
                placeholder = { Text(if (s.geminiApiKey.isNotBlank()) "..." else "Gemini API key", color = SonaraTextTertiary) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = MaterialTheme.shapes.extraSmall,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p2, cursorColor = p2))
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = { vm.saveGeminiKey() }, enabled = s.geminiKeyInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (s.geminiKeyInput.isNotBlank()) p2 else SonaraDivider)
            ) { Text("Save Key") }
            Spacer(Modifier.height(4.dp))
            Text("Model", style = MaterialTheme.typography.labelMedium, color = SonaraTextSecondary)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("fast" to "Fast", "balanced" to "Balanced", "strong" to "Strong").forEach { (id, label) ->
                    val sel = s.geminiModel == id
                    OutlinedButton(onClick = { vm.setGeminiModel(id) },
                        modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(1.dp, if (sel) p2 else SonaraDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (sel) p2 else SonaraTextSecondary)
                    ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
        if (s.aiProvider == "openrouter") {
            Spacer(Modifier.height(8.dp))
            val p2 = MaterialTheme.colorScheme.primary
            OutlinedTextField(value = s.openRouterKeyInput, onValueChange = { vm.updateOpenRouterKeyInput(it) },
                placeholder = { Text(if (s.openRouterApiKey.isNotBlank()) "...." else "OpenRouter API key", color = SonaraTextTertiary) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = MaterialTheme.shapes.extraSmall,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p2, cursorColor = p2))
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(value = s.openRouterModel, onValueChange = { vm.setOpenRouterModel(it) },
                label = { Text("Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = MaterialTheme.shapes.extraSmall,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p2, cursorColor = p2))
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = { vm.saveOpenRouterKey() }, enabled = s.openRouterKeyInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (s.openRouterKeyInput.isNotBlank()) p2 else SonaraDivider)
            ) { Text("Save Key") }
        }
        if (s.aiProvider == "groq") {
            Spacer(Modifier.height(8.dp))
            val p2 = MaterialTheme.colorScheme.primary
            OutlinedTextField(value = s.groqKeyInput, onValueChange = { vm.updateGroqKeyInput(it) },
                placeholder = { Text(if (s.groqApiKey.isNotBlank()) "...." else "Groq API key", color = SonaraTextTertiary) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = MaterialTheme.shapes.extraSmall,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p2, cursorColor = p2))
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(value = s.groqModel, onValueChange = { vm.setGroqModel(it) },
                label = { Text("Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = MaterialTheme.shapes.extraSmall,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p2, cursorColor = p2))
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = { vm.saveGroqKey() }, enabled = s.groqKeyInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (s.groqKeyInput.isNotBlank()) p2 else SonaraDivider)
            ) { Text("Save Key") }
        }
    }
}

@Composable
private fun AboutCard(state: SettingsUiState, vm: SettingsViewModel) {
    var tapCount by remember { mutableStateOf(0) }
    var devMode by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    FluentCard {
        Row(Modifier.fillMaxWidth().clickable {
            tapCount++
            if (tapCount >= 5 && !devMode) { devMode = true }
        }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Sonara", style = MaterialTheme.typography.titleMedium)
                Text("Personal Sound Engine", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
            Text("v1.0.0", style = MaterialTheme.typography.labelLarge, color = SonaraTextTertiary)
        }
        if (tapCount in 1..4) {
            Text("${5 - tapCount} more taps for developer options", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
        }
        if (devMode) {
            SettingsDivider()
            Text("Developer Options", style = MaterialTheme.typography.titleSmall, color = SonaraWarning)
            Spacer(Modifier.height(12.dp))
            // Community sync interval
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Sync every N songs", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                var intervalText by remember { mutableStateOf(state.syncInterval.toString()) }
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { v ->
                        intervalText = v.filter { it.isDigit() }.take(4)
                        val num = intervalText.toIntOrNull()
                        if (num != null && num > 0) vm.setSyncInterval(num)
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = MaterialTheme.shapes.extraSmall
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { vm.disconnectLastFm(); vm.clearAllData() },
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, SonaraError.copy(0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraError)
            ) { Text("Reset All Auth") }
        }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = p, checkedTrackColor = p.copy(0.3f),
                uncheckedThumbColor = SonaraTextTertiary, uncheckedTrackColor = SonaraCardElevated))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(vertical = 14.dp), 0.5.dp, SonaraDivider.copy(0.5f))
}

@Composable
private fun tfColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = SonaraDivider,
    focusedContainerColor = SonaraCardElevated, unfocusedContainerColor = SonaraCardElevated,
    cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = SonaraTextPrimary, unfocusedTextColor = SonaraTextPrimary
)
