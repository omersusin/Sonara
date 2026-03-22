package com.sonara.app.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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

        item { SectionHeader("AutoEQ Import") }
        item { AutoEqImportCard() }

        item { SectionHeader("Appearance") }
        item { AppearanceCard(state, vm) }

        item { SectionHeader("Sound Engine") }
        item { SoundEngineCard(state, vm) }

        item { SectionHeader("AI Sources") }
        item { AiSourcesCard(state, vm) }

        item { SectionHeader("Advanced") }
        item { AdvancedCard(state, vm) }

        // Gemini merged into AI Sources

        // Theme merged into Appearance above

        item { SectionHeader("Presets") }
        item { PresetExportImportCard(vm) }

        item { SectionHeader("Data") }
        item { DataCard(state, vm) }

        item { SectionHeader("Developer") }
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
        item { AboutCard() }

        item { Spacer(Modifier.height(16.dp)) }
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
                when {
                    state.lastFmConnected -> "Connected"
                    state.isApiKeySet -> "Active"
                    else -> "Not Set"
                },
                when {
                    state.lastFmConnected -> ChipStatus.Active
                    state.isApiKeySet -> ChipStatus.Active
                    else -> ChipStatus.Warning
                }
            )
        }
        Spacer(Modifier.height(8.dp))

        when {
            state.lastFmConnected -> {
                Text("Connected as ${state.lastFmUsername}", style = MaterialTheme.typography.bodySmall, color = SonaraSuccess)
                Spacer(Modifier.height(4.dp))
                Text("Genre detection and scrobbling active.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { vm.disconnectLastFm() },
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, SonaraError.copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraError)
                ) { Text("Disconnect") }
            }
            state.isApiKeySet -> {
                Text("Genre detection via Last.fm is active.", style = MaterialTheme.typography.bodySmall, color = SonaraSuccess)
                if (!state.isSharedSecretSet) {
                    Spacer(Modifier.height(4.dp))
                    Text("Add shared secret for scrobbling support.", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                }
            }
            else -> {
                Text("Required for accurate genre detection.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                Spacer(Modifier.height(12.dp))
                Text("How to get your free API key:", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Go to last.fm/api/account/create",
                    "Create a Last.fm account (or log in)",
                    "Fill in Application name: Sonara",
                    "Leave other fields empty, click Submit",
                    "Copy API Key and Shared Secret",
                    "Paste them below and tap Save"
                ).forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/api/account/create"))) }) {
                    Icon(Icons.Rounded.Launch, null, Modifier.size(16.dp), tint = p)
                    Spacer(Modifier.size(6.dp))
                    Text("Open Last.fm API Page", color = p)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.connectLastFm { intent -> ctx.startActivity(intent) } },
                    Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, SonaraInfo),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraInfo)
                ) { Text("Quick Connect (OAuth)") }
            }
        }

        if (!state.lastFmConnected) {
            SettingsDivider()
            Text("API Key", style = MaterialTheme.typography.labelMedium, color = SonaraTextSecondary)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.apiKeyInput, onValueChange = { vm.updateApiKeyInput(it) },
                placeholder = { Text(if (state.isApiKeySet) "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022" else "Paste your API key", color = SonaraTextTertiary) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.small, colors = tfColors()
            )
            Spacer(Modifier.height(8.dp))
            Text("Shared Secret", style = MaterialTheme.typography.labelMedium, color = SonaraTextSecondary)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.sharedSecretInput, onValueChange = { vm.updateSharedSecretInput(it) },
                placeholder = { Text(if (state.isSharedSecretSet) "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022" else "Paste shared secret", color = SonaraTextTertiary) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.small, colors = tfColors()
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = { vm.saveApiKey(); vm.saveSharedSecret() },
                    enabled = state.apiKeyInput.isNotBlank() || state.sharedSecretInput.isNotBlank(),
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, if (state.apiKeyInput.isNotBlank() || state.sharedSecretInput.isNotBlank()) p else SonaraDivider),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = p)
                ) { Text("Save") }
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
        Text("Import AutoEQ Profile", style = MaterialTheme.typography.titleMedium)
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
        Text("Preset Management", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Export your custom presets to share or backup, import presets from JSON",
            style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.exportPresets { json ->
                    clipboard.setText(AnnotatedString(json))
                    Toast.makeText(ctx, "Presets copied to clipboard", Toast.LENGTH_SHORT).show()
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
            title = { Text("Import Presets") },
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
                    vm.importPresets(importInput) { msg -> importResult = msg }
                }) { Text("Import", color = p) }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; importInput = ""; importResult = "" }) {
                    Text("Cancel", color = SonaraTextSecondary)
                }
            }
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
        Text("AI Sources", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Choose which sources AI uses for genre detection", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(8.dp))
        SwitchRow("Last.fm", "Online genre tags and metadata", s.sourceLastFm) { vm.setSourceLastFm(it) }
        SettingsDivider()
        SwitchRow("Local AI", "On-device title/artist classification", s.sourceLocalAi) { vm.setSourceLocalAi(it) }
        SettingsDivider()
        SwitchRow("Lyrics", "Lyrics-based tone and mood analysis", s.sourceLyrics) { vm.setSourceLyrics(it) }
        SettingsDivider()
        SwitchRow("Gemini Insights", "AI-powered track analysis", s.geminiEnabled) { vm.setGeminiEnabled(it) }
        if (s.geminiEnabled) {
            Spacer(Modifier.height(8.dp))
            val p = MaterialTheme.colorScheme.primary
            OutlinedTextField(
                value = s.geminiKeyInput, onValueChange = { vm.updateGeminiKeyInput(it) },
                placeholder = { Text(if (s.geminiApiKey.isNotBlank()) "••••••••" else "Gemini API key", color = SonaraTextTertiary) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p, cursorColor = p)
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.saveGeminiKey() }, enabled = s.geminiKeyInput.isNotBlank(),
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, if (s.geminiKeyInput.isNotBlank()) p else SonaraDivider)
                ) { Text("Save Key") }
                listOf("fast" to "Fast", "balanced" to "Balanced", "strong" to "Strong").forEach { (id, label) ->
                    val sel = s.geminiModel == id
                    OutlinedButton(onClick = { vm.setGeminiModel(id) },
                        shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(1.dp, if (sel) p else SonaraDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (sel) p else SonaraTextSecondary)
                    ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable
private fun AboutCard() {
    FluentCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Sonara", style = MaterialTheme.typography.titleMedium)
                Text("Personal Sound Engine", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
            Text("v1.0.0", style = MaterialTheme.typography.labelLarge, color = SonaraTextTertiary)
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
