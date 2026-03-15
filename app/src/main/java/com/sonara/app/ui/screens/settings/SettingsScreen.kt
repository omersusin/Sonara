package com.sonara.app.ui.screens.settings

import android.content.Intent
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
import com.sonara.app.autoeq.AutoEqImporter
import com.sonara.app.ui.components.ChipStatus
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.components.StatusChip
import com.sonara.app.ui.theme.*

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 8.dp)) }

        if (!state.notificationListenerEnabled) {
            item {
                FluentCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Notifications, null, tint = SonaraWarning, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Notification Access", style = MaterialTheme.typography.titleMedium)
                            Text("Required to detect playing music", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Grant Permission") }
                }
            }
        }

        item { SectionHeader("Last.fm Integration") }
        item { LastFmCard(state, viewModel) }

        item { SectionHeader("AutoEQ Import") }
        item { AutoEqImportCard() }

        item { SectionHeader("Appearance") }
        item { AccentColorCard(state.accentColor) { viewModel.setAccentColor(it) } }

        item { SectionHeader("Sound Engine") }
        item { SoundEngineCard(state, viewModel) }

        item { SectionHeader("Advanced") }
        item { AdvancedCard(state, viewModel) }

        item { SectionHeader("Data") }
        item { DataManagementCard(state, viewModel) }

        item { SectionHeader("About") }
        item { AboutCard() }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp))
}

@Composable
private fun AutoEqImportCard() {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<AutoEqImporter.ImportResult?>(null) }
    val clipboard = LocalClipboardManager.current

    FluentCard {
        Text("Import AutoEQ Profile", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Paste AutoEQ GraphicEQ data or 10 comma-separated gain values (dB)", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = input, onValueChange = { input = it; result = null },
            placeholder = { Text("Paste EQ data here...", color = SonaraTextTertiary) },
            modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 8,
            shape = MaterialTheme.shapes.small,
            trailingIcon = {
                IconButton(onClick = {
                    clipboard.getText()?.text?.let { input = it }
                }) { Icon(Icons.Rounded.ContentPaste, "Paste", tint = SonaraTextSecondary) }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = SonaraDivider,
                focusedContainerColor = SonaraCardElevated, unfocusedContainerColor = SonaraCardElevated,
                cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = SonaraTextPrimary, unfocusedTextColor = SonaraTextPrimary
            )
        )

        Spacer(Modifier.height(10.dp))

        if (result != null) {
            val r = result!!
            if (r.success) {
                StatusChip("Parsed: ${r.bands.map { String.format("%.1f", it) }}", ChipStatus.Active)
                Spacer(Modifier.height(8.dp))
                Text("Values: ${r.bands.joinToString(", ") { String.format("%.1f", it) }}", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            } else {
                StatusChip("Error: ${r.error}", ChipStatus.Error)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { result = AutoEqImporter.parseGraphicEq(input) },
                enabled = input.isNotBlank(),
                modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (input.isNotBlank()) MaterialTheme.colorScheme.primary else SonaraDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) { Text("Parse") }

            OutlinedButton(
                onClick = {
                    result?.let { r ->
                        if (r.success) {
                            val app = com.sonara.app.SonaraApp.instance
                            app.applyEqBands(r.bands)
                        }
                    }
                },
                enabled = result?.success == true,
                modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, if (result?.success == true) SonaraSuccess else SonaraDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraSuccess)
            ) { Text("Apply") }
        }
    }
}

@Composable
private fun LastFmCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    FluentCard {
        KeyInputSection("API Key", state.isApiKeySet,
            if (state.isApiKeySet) "Genre detection via Last.fm is active." else "Enter your Last.fm API key.",
            state.apiKeyInput, { viewModel.updateApiKeyInput(it) }, "Enter API key...", "Save API Key") { viewModel.saveApiKey() }
        SettingsDivider()
        KeyInputSection("Shared Secret", state.isSharedSecretSet,
            if (state.isSharedSecretSet) "Scrobbling is ready." else "Required for scrobbling.",
            state.sharedSecretInput, { viewModel.updateSharedSecretInput(it) }, "Enter shared secret...", "Save Secret") { viewModel.saveSharedSecret() }
    }
}

@Composable
private fun KeyInputSection(title: String, isSet: Boolean, desc: String, value: String, onChange: (String) -> Unit, hint: String, btnText: String, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        StatusChip(if (isSet) "Set" else "Not Set", if (isSet) ChipStatus.Active else ChipStatus.Inactive)
    }
    Spacer(Modifier.height(4.dp)); Text(desc, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(value = value, onValueChange = onChange, placeholder = { Text(hint, color = SonaraTextTertiary) },
        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = SonaraDivider,
            focusedContainerColor = SonaraCardElevated, unfocusedContainerColor = SonaraCardElevated,
            cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = SonaraTextPrimary, unfocusedTextColor = SonaraTextPrimary))
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        OutlinedButton(onClick = onSave, enabled = value.isNotBlank(), shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, if (value.isNotBlank()) MaterialTheme.colorScheme.primary else SonaraDivider),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text(btnText) }
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
                    .then(if (c == AccentColor.Auto) Modifier.background(Brush.sweepGradient(listOf(SonaraInfo, SonaraSuccess, SonaraBandLow, SonaraError, SonaraWarning, SonaraInfo)))
                          else Modifier.background(c.primary))
                    .then(if (sel) Modifier.border(2.5.dp, SonaraTextPrimary, CircleShape) else Modifier.border(1.dp, SonaraDivider.copy(alpha = 0.3f), CircleShape))
                    .clickable { onSelect(c) }, contentAlignment = Alignment.Center
                ) { if (sel) Icon(Icons.Rounded.Check, null, tint = SonaraBackground, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun SoundEngineCard(state: SettingsUiState, vm: SettingsViewModel) {
    FluentCard {
        SwitchRow("AI Auto-adjust", "Auto EQ based on genre/mood. Disabled when manual preset is active.", state.aiEnabled) { vm.setAiEnabled(it) }
        SettingsDivider()
        SwitchRow("AutoEQ", "Apply headphone correction", state.autoEqEnabled) { vm.setAutoEqEnabled(it) }
        SettingsDivider()
        SwitchRow("Auto Preset", "Auto-select preset based on genre", state.autoPreset) { vm.setAutoPreset(it) }
    }
}

@Composable
private fun AdvancedCard(state: SettingsUiState, vm: SettingsViewModel) {
    FluentCard {
        SwitchRow("Smooth Transitions", "Gradual EQ changes between tracks", state.smoothTransitions) { vm.setSmoothTransitions(it) }
        SettingsDivider()
        SwitchRow("Safety Limiter", "Prevent audio clipping", state.safetyLimiter) { vm.setSafetyLimiter(it) }
        SettingsDivider()
        SwitchRow("Scrobbling", "Send history to Last.fm", state.scrobblingEnabled) { vm.setScrobblingEnabled(it) }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(desc, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = p, checkedTrackColor = p.copy(alpha = 0.3f), uncheckedThumbColor = SonaraTextTertiary, uncheckedTrackColor = SonaraCardElevated))
    }
}

@Composable private fun SettingsDivider() { HorizontalDivider(Modifier.padding(vertical = 14.dp), 0.5.dp, SonaraDivider.copy(alpha = 0.5f)) }

@Composable
private fun DataManagementCard(state: SettingsUiState, vm: SettingsViewModel) {
    FluentCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Track Cache", style = MaterialTheme.typography.titleMedium); Text("${state.cacheSize} tracks", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
            OutlinedButton(onClick = { vm.clearCache() }, shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, SonaraDivider), colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraError)) { Text("Clear") }
        }
        SettingsDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Reset All", style = MaterialTheme.typography.titleMedium); Text("Restore defaults", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
            OutlinedButton(onClick = { vm.clearAllData() }, shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, SonaraError.copy(alpha = 0.5f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = SonaraError)) { Text("Reset") }
        }
    }
}

@Composable
private fun AboutCard() {
    FluentCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Sonara", style = MaterialTheme.typography.titleMedium); Text("Personal Sound Engine", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
            Text("v1.0.0", style = MaterialTheme.typography.labelLarge, color = SonaraTextTertiary)
        }
    }
}
