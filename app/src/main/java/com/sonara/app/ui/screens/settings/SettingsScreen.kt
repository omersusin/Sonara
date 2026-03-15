package com.sonara.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.Notifications
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.SonaraApp
import com.sonara.app.autoeq.AutoEqImporter
import com.sonara.app.ui.components.ChipStatus
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.components.StatusChip
import com.sonara.app.ui.theme.*

@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val ctx = LocalContext.current
    val p = MaterialTheme.colorScheme.primary

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 8.dp)) }

        if (!state.notificationListenerEnabled) {
            item {
                FluentCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Notifications, null, tint = SonaraWarning, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) { Text("Notification Access", style = MaterialTheme.typography.titleMedium); Text("Required to detect playing music", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = p)) { Text("Grant Permission") }
                }
            }
        }

        item { SectionHeader("Last.fm Integration") }
        item { LastFmCard(state, vm) }

        item { SectionHeader("AutoEQ Import") }
        item { AutoEqImportCard() }

        item { SectionHeader("Appearance") }
        item { AccentColorCard(state.accentColor) { vm.setAccentColor(it) } }

        item { SectionHeader("Sound Engine") }
        item { SoundEngineCard(state, vm) }

        item { SectionHeader("Advanced") }
        item { AdvancedCard(state, vm) }

        item { SectionHeader("Data") }
        item { DataCard(state, vm) }

        item { SectionHeader("About") }
        item { FluentCard { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Sonara", style = MaterialTheme.typography.titleMedium); Text("Personal Sound Engine", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }; Text("v1.0.0", style = MaterialTheme.typography.labelLarge, color = SonaraTextTertiary) } } }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable private fun SectionHeader(t: String) { Text(t.uppercase(), style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)) }

@Composable
private fun LastFmCard(state: SettingsUiState, vm: SettingsViewModel) {
    val ctx = LocalContext.current
    val p = MaterialTheme.colorScheme.primary

    FluentCard {
        // API Key section
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Last.fm API Key", style = MaterialTheme.typography.titleMedium)
            StatusChip(if (state.isApiKeySet) "Active" else "Required", if (state.isApiKeySet) ChipStatus.Active else ChipStatus.Warning)
        }

        Spacer(Modifier.height(8.dp))

        if (state.isApiKeySet) {
            Text("Genre detection via Last.fm is active.", style = MaterialTheme.typography.bodySmall, color = SonaraSuccess)
        } else {
            Text("Required for accurate genre detection via Last.fm. Without this key, Sonara falls back to less accurate local analysis.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            Spacer(Modifier.height(12.dp))
            Text("How to get your free API key:", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
            Spacer(Modifier.height(6.dp))
            val steps = listOf(
                "Go to last.fm/api/account/create",
                "Create a Last.fm account (or log in)",
                "Fill in Application name: Sonara",
                "Leave other fields empty, click Submit",
                "Copy the API Key shown on the next page",
                "Paste it below and tap Save"
            )
            steps.forEachIndexed { i, step ->
                Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/api/account/create"))) }) {
                Icon(Icons.Rounded.Launch, null, Modifier.size(16.dp), tint = p)
                Spacer(Modifier.size(6.dp))
                Text("Open Last.fm API Page", color = p)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("API Key", style = MaterialTheme.typography.labelMedium, color = SonaraTextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = state.apiKeyInput, onValueChange = { vm.updateApiKeyInput(it) },
            placeholder = { Text(if (state.isApiKeySet) "••••••••••••" else "Paste your API key here", color = SonaraTextTertiary) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.small,
            colors = tfColors())
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = { vm.saveApiKey() }, enabled = state.apiKeyInput.isNotBlank(), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (state.apiKeyInput.isNotBlank()) MaterialTheme.colorScheme.primary else SonaraDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text("Save API Key") }
        }

        Div()

        // Shared Secret section
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Shared Secret", style = MaterialTheme.typography.titleMedium)
            StatusChip(if (state.isSharedSecretSet) "Set" else "Optional", if (state.isSharedSecretSet) ChipStatus.Active else ChipStatus.Inactive)
        }
        Spacer(Modifier.height(4.dp))
        Text("Found on the same Last.fm API page alongside your API Key. Required for scrobbling your listening history.", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(10.dp))
        Text("Shared Secret", style = MaterialTheme.typography.labelMedium, color = SonaraTextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = state.sharedSecretInput, onValueChange = { vm.updateSharedSecretInput(it) },
            placeholder = { Text(if (state.isSharedSecretSet) "••••••••••••" else "Paste shared secret here", color = SonaraTextTertiary) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.small,
            colors = tfColors())
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = { vm.saveSharedSecret() }, enabled = state.sharedSecretInput.isNotBlank(), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (state.sharedSecretInput.isNotBlank()) MaterialTheme.colorScheme.primary else SonaraDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text("Save Shared Secret") }
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
        Text("Paste AutoEQ GraphicEQ data or 10 comma-separated gain values (dB)", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = input, onValueChange = { input = it; result = null },
            placeholder = { Text("Paste EQ data here...", color = SonaraTextTertiary) },
            modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 8, shape = MaterialTheme.shapes.small,
            trailingIcon = { IconButton(onClick = { clipboard.getText()?.text?.let { input = it } }) { Icon(Icons.Rounded.ContentPaste, "Paste", tint = SonaraTextSecondary) } },
            colors = tfColors())
        Spacer(Modifier.height(10.dp))
        if (result != null) {
            val r = result!!
            if (r.success) {
                StatusChip("Parsed successfully", ChipStatus.Active)
                Spacer(Modifier.height(4.dp))
                Text("Values: ${r.bands.joinToString(", ") { "%.1f".format(it) }}", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            } else StatusChip("Error: ${r.error}", ChipStatus.Error)
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { result = AutoEqImporter.parseGraphicEq(input) }, enabled = input.isNotBlank(),
                Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (input.isNotBlank()) p else SonaraDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = p)) { Text("Parse") }
            OutlinedButton(onClick = {
                result?.let { r -> if (r.success) SonaraApp.instance.applyEq(bands = r.bands, presetName = "AutoEQ Import", manual = true) }
            }, enabled = result?.success == true,
                Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (result?.success == true) SonaraSuccess else SonaraDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraSuccess)) { Text("Apply") }
        }
    }
}

@Composable
private fun AccentColorCard(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    FluentCard {
        Text("Accent Color", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(if (selected == AccentColor.Auto) "Wallpaper colors" else selected.displayName, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(16.dp))
        val colors = if (Build.VERSION.SDK_INT >= 31) AccentColor.entries else AccentColor.entries.filter { it != AccentColor.Auto }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            colors.forEach { c ->
                val sel = c == selected
                Box(Modifier.size(38.dp).clip(CircleShape)
                    .then(if (c == AccentColor.Auto) Modifier.background(Brush.sweepGradient(listOf(SonaraInfo, SonaraSuccess, SonaraBandLow, SonaraError, SonaraWarning, SonaraInfo))) else Modifier.background(c.primary))
                    .then(if (sel) Modifier.border(2.5.dp, SonaraTextPrimary, CircleShape) else Modifier.border(1.dp, SonaraDivider.copy(0.3f), CircleShape))
                    .clickable { onSelect(c) }, contentAlignment = Alignment.Center
                ) { if (sel) Icon(Icons.Rounded.Check, null, tint = SonaraBackground, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable private fun SoundEngineCard(s: SettingsUiState, vm: SettingsViewModel) {
    FluentCard { SR("AI Auto-adjust", "Automatically adjust EQ based on detected genre and mood", s.aiEnabled) { vm.setAiEnabled(it) }; Div(); SR("AutoEQ", "Apply headphone correction", s.autoEqEnabled) { vm.setAutoEqEnabled(it) }; Div(); SR("Auto Preset", "Auto-select preset based on genre", s.autoPreset) { vm.setAutoPreset(it) } }
}

@Composable private fun AdvancedCard(s: SettingsUiState, vm: SettingsViewModel) {
    FluentCard { SR("Smooth Transitions", "Gradual EQ changes between tracks", s.smoothTransitions) { vm.setSmoothTransitions(it) }; Div(); SR("Safety Limiter", "Prevent audio clipping", s.safetyLimiter) { vm.setSafetyLimiter(it) }; Div(); SR("Scrobbling", "Send history to Last.fm", s.scrobblingEnabled) { vm.setScrobblingEnabled(it) } }
}

@Composable private fun DataCard(s: SettingsUiState, vm: SettingsViewModel) {
    FluentCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Track Cache", style = MaterialTheme.typography.titleMedium); Text("${s.cacheSize} tracks", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
            OutlinedButton(onClick = { vm.clearCache() }, shape = MaterialTheme.shapes.extraLarge, border = BorderStroke(1.dp, SonaraDivider), colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraError)) { Text("Clear") }
        }
        Div()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Reset All", style = MaterialTheme.typography.titleMedium); Text("Restore defaults", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
            OutlinedButton(onClick = { vm.clearAllData() }, shape = MaterialTheme.shapes.extraLarge, border = BorderStroke(1.dp, SonaraError.copy(0.5f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraError)) { Text("Reset") }
        }
    }
}

@Composable private fun SR(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(desc, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = p, checkedTrackColor = p.copy(0.3f), uncheckedThumbColor = SonaraTextTertiary, uncheckedTrackColor = SonaraCardElevated))
    }
}

@Composable private fun Div() { HorizontalDivider(Modifier.padding(vertical = 14.dp), 0.5.dp, SonaraDivider.copy(0.5f)) }

@Composable
private fun tfColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = SonaraDivider,
    focusedContainerColor = SonaraCardElevated, unfocusedContainerColor = SonaraCardElevated,
    cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = SonaraTextPrimary, unfocusedTextColor = SonaraTextPrimary
)
