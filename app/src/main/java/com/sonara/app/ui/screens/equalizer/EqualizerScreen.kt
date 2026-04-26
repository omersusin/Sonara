package com.sonara.app.ui.screens.equalizer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.audio.equalizer.TenBandEqualizer
import com.sonara.app.engine.effects.EffectsChain
import com.sonara.app.ui.components.BandSlider
import com.sonara.app.ui.components.EqCurve
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.SonaraApp
import com.sonara.app.autoeq.WaveletAutoEqLoader
import com.sonara.app.autoeq.AutoEqDatabase
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SurroundSound
import com.sonara.app.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen() {
    val vm: EqualizerViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val p = MaterialTheme.colorScheme.primary
    var showSave by remember { mutableStateOf(false) }
    var showPresetMenu by remember { mutableStateOf(false) }
    var activeSoundProfile by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Equalizer", style = MaterialTheme.typography.headlineLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showSave = true }) { Icon(Icons.Rounded.Save, "Save", tint = SonaraTextSecondary) }
                    IconButton(onClick = { vm.resetBands() }) { Icon(Icons.Rounded.Refresh, "Reset", tint = SonaraTextSecondary) }
                    Switch(checked = s.isEnabled, onCheckedChange = { vm.setEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = p, checkedTrackColor = p.copy(0.3f), uncheckedThumbColor = SonaraTextTertiary, uncheckedTrackColor = SonaraCardElevated))
                }
            }
        }

        // Preset dropdown
        item {
            FluentCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Presets", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                    Box {
                        Surface(
                            onClick = { showPresetMenu = true },
                            shape = MaterialTheme.shapes.small,
                            color = SonaraCardElevated,
                            border = BorderStroke(1.dp, SonaraDivider.copy(0.3f))
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(s.currentPresetName, style = MaterialTheme.typography.bodyMedium, color = p)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Rounded.ArrowDropDown, null, tint = p)
                            }
                        }
                        DropdownMenu(expanded = showPresetMenu, onDismissRequest = { showPresetMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("AI Auto", color = if (s.currentPresetName.startsWith("AI")) p else SonaraTextPrimary) },
                                onClick = { vm.resetToAi(); showPresetMenu = false }
                            )
                            HorizontalDivider(color = SonaraDivider.copy(0.3f))
                            s.availablePresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.name, color = if (preset.name == s.currentPresetName) p else SonaraTextPrimary) },
                                    onClick = { vm.applyPreset(preset); showPresetMenu = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sound Profiles — quick presets
        item {
            val profiles = listOf(
                Triple("Flat",     Icons.Rounded.GraphicEq,    { vm.resetBands(); vm.setBassBoost(0); vm.setVirtualizer(0); vm.setLoudness(0) }),
                Triple("Bass Boost", Icons.Rounded.GraphicEq,  { vm.setBand(0, 4f); vm.setBand(1, 3.5f); vm.setBand(2, 2f); vm.setBassBoost(600); vm.setVirtualizer(0) }),
                Triple("Spatial",  Icons.Rounded.SurroundSound, { vm.setVirtualizer(800); vm.setBassBoost(200); vm.setLoudness(400); vm.setBand(3, 1f); vm.setBand(4, 1.5f) }),
                Triple("Vocal",    Icons.Rounded.GraphicEq,    { vm.setBand(3, 2f); vm.setBand(4, 3f); vm.setBand(5, 2.5f); vm.setBand(6, 1.5f); vm.setBassBoost(0); vm.setVirtualizer(0) }),
                Triple("Treble",   Icons.Rounded.GraphicEq,    { vm.setBand(6, 2.5f); vm.setBand(7, 3f); vm.setBand(8, 3.5f); vm.setBand(9, 3f); vm.setBassBoost(0) }),
                Triple("Night",    Icons.Rounded.AutoAwesome,  { vm.setBand(0, 1f); vm.setBand(1, 0.5f); vm.setBand(6, -1f); vm.setBand(7, -2f); vm.setBand(8, -3f); vm.setLoudness(800); vm.setVirtualizer(200) })
            )
            FluentCard {
                Text("Sound Profiles", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profiles) { (name, icon, apply) ->
                        FilterChip(
                            selected = activeSoundProfile == name,
                            onClick = { activeSoundProfile = name; apply() },
                            label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = p.copy(0.2f),
                                selectedLabelColor = p,
                                selectedLeadingIconColor = p,
                                containerColor = SonaraCardElevated,
                                labelColor = SonaraTextSecondary,
                                iconColor = SonaraTextTertiary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = activeSoundProfile == name,
                                borderColor = SonaraDivider.copy(0.3f),
                                selectedBorderColor = p.copy(0.4f)
                            )
                        )
                    }
                }
            }
        }

        item { FluentCard { EqCurve(bands = s.bands) } }

        item { FluentCard {
            Text("Bands", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary); Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                s.bands.take(10).forEachIndexed { i, v -> BandSlider(v, { vm.setBand(i, it) }, TenBandEqualizer.LABELS[i], enabled = s.isEnabled) }
            }
        } }

        item { FluentCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Preamp", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                Text("${if (s.preamp >= 0) "+" else ""}${"%.1f".format(s.preamp)} dB", style = MaterialTheme.typography.labelLarge, color = if (s.preamp != 0f) p else SonaraTextTertiary)
            }; Spacer(Modifier.height(4.dp))
            Slider(s.preamp, { vm.setPreamp(it) }, valueRange = -6f..6f, enabled = s.isEnabled,
                colors = SliderDefaults.colors(thumbColor = p, activeTrackColor = p, inactiveTrackColor = SonaraCardElevated))
        } }

        // ═══ AutoEQ Headphone Correction (only when headphones connected) ═══
        if (SonaraApp.instance.autoEqManager.state.value.headphone.isConnected) {
        item {
            var autoEqExpanded by remember { mutableStateOf(false) }
            var searchQuery by remember { mutableStateOf("") }
            var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val autoEqState by SonaraApp.instance.autoEqManager.state.collectAsState()

            FluentCard {
                Row(Modifier.fillMaxWidth().clickable { autoEqExpanded = !autoEqExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Headphones, null, tint = if (autoEqState.isActive) p else SonaraTextTertiary, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Headphone EQ", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                            Text(if (autoEqState.isActive) "Active: ${autoEqState.profile?.name ?: "Unknown"}" else "Tap to select headphone",
                                style = MaterialTheme.typography.bodySmall, color = if (autoEqState.isActive) p else SonaraTextTertiary)
                        }
                    }
                    Text("${WaveletAutoEqLoader.profileCount(ctx) + AutoEqDatabase.profileCount()} profiles",
                        style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
                if (autoEqExpanded) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { q ->
                            searchQuery = q
                            searchResults = if (q.length >= 2) WaveletAutoEqLoader.searchProfiles(q, ctx, 15) else emptyList()
                        },
                        placeholder = { Text("Search headphone...", color = SonaraTextTertiary) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = SonaraTextTertiary, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p, unfocusedBorderColor = SonaraDivider.copy(0.5f),
                            cursorColor = p, focusedTextColor = SonaraTextPrimary, unfocusedTextColor = SonaraTextPrimary)
                    )
                    if (searchResults.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        searchResults.forEach { name ->
                            Row(Modifier.fillMaxWidth().clickable {
                                SonaraApp.instance.autoEqManager.setManualProfile(name, ctx)
                                autoEqExpanded = false; searchQuery = ""
                            }.padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(name, style = MaterialTheme.typography.bodySmall, color = SonaraTextPrimary, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    if (autoEqState.isActive) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { SonaraApp.instance.autoEqManager.disable() }) {
                            Text("Disable Correction", color = SonaraError, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        } // headphone connected check

        item { FluentCard {
            Text("Effects", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary); Spacer(Modifier.height(12.dp))
            EffRow("Bass Boost", s.bassBoost, { vm.setBassBoost(it) }, s.isEnabled, p, 1000f) { "${(it / 10f).roundToInt()}%" }
            Spacer(Modifier.height(8.dp))
            EffRow("Stereo Width", s.virtualizer, { vm.setVirtualizer(it) }, s.isEnabled, p, 1000f) { "${(it / 10f).roundToInt()}%" }
            Spacer(Modifier.height(8.dp))
            EffRow("Loudness", s.loudness, { vm.setLoudness(it) }, s.isEnabled, p, 3000f) { "${"%.1f".format(it / 100f)} dB" }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Room", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                Text(EffectsChain.reverbName(s.reverb), style = MaterialTheme.typography.labelMedium, color = if (s.reverb > 0) p else SonaraTextTertiary)
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items((0..6).toList()) { preset ->
                    FilterChip(
                        selected = s.reverb == preset,
                        enabled = s.isEnabled,
                        onClick = { vm.setReverb(preset) },
                        label = { Text(EffectsChain.reverbName(preset), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = p.copy(0.2f), selectedLabelColor = p,
                            containerColor = SonaraCardElevated, labelColor = SonaraTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = s.isEnabled, selected = s.reverb == preset,
                            borderColor = SonaraDivider.copy(0.3f), selectedBorderColor = p.copy(0.4f)
                        )
                    )
                }
            }
        } }

        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showSave) { SaveDialog({ showSave = false }) { name -> vm.saveCurrentAsPreset(name); showSave = false } }
}

@Composable
private fun EffRow(label: String, value: Int, onChange: (Int) -> Unit, enabled: Boolean, p: androidx.compose.ui.graphics.Color, max: Float, format: (Int) -> String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
            Text(format(value), style = MaterialTheme.typography.labelMedium, color = if (value > 0) p else SonaraTextTertiary)
        }
        Slider(value.toFloat(), { onChange(it.toInt()) }, valueRange = 0f..max, enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = p, activeTrackColor = p, inactiveTrackColor = SonaraCardElevated))
    }
}

@Composable
private fun SaveDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Save Preset") },
        text = { OutlinedTextField(name, { name = it }, placeholder = { Text("Preset name", color = SonaraTextTertiary) }, singleLine = true, shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = SonaraDivider, focusedContainerColor = SonaraCardElevated, unfocusedContainerColor = SonaraCardElevated, cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = SonaraTextPrimary, unfocusedTextColor = SonaraTextPrimary)) },
        confirmButton = { TextButton({ if (name.isNotBlank()) onSave(name) }) { Text("Save", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = SonaraTextSecondary) } })
}
