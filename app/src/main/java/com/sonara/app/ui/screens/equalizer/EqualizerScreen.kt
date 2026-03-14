package com.sonara.app.ui.screens.equalizer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.sonara.app.ui.components.BandSlider
import com.sonara.app.ui.components.EqCurve
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*

@Composable
fun EqualizerScreen() {
    val viewModel: EqualizerViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    var showSaveDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header(state, viewModel, primary, onSave = { showSaveDialog = true }) }
        item { PresetQuickPicker(state, viewModel, primary) }
        item { CurveCard(state) }
        item { BandsCard(state, viewModel) }
        item { PreampCard(state, viewModel, primary) }
        item { EffectsCard(state, viewModel, primary) }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showSaveDialog) {
        SavePresetDialog(onDismiss = { showSaveDialog = false }, onSave = { name -> viewModel.saveCurrentAsPreset(name); showSaveDialog = false })
    }
}

@Composable
private fun Header(state: EqualizerUiState, vm: EqualizerViewModel, primary: androidx.compose.ui.graphics.Color, onSave: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Equalizer", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(2.dp))
            Text(state.currentPresetName, style = MaterialTheme.typography.bodySmall, color = primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onSave) { Icon(Icons.Rounded.Save, "Save", tint = SonaraTextSecondary) }
            IconButton(onClick = { vm.resetBands() }) { Icon(Icons.Rounded.Refresh, "Reset", tint = SonaraTextSecondary) }
            Switch(checked = state.isEnabled, onCheckedChange = { vm.setEnabled(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = primary, checkedTrackColor = primary.copy(alpha = 0.3f), uncheckedThumbColor = SonaraTextTertiary, uncheckedTrackColor = SonaraCardElevated))
        }
    }
}

@Composable
private fun PresetQuickPicker(state: EqualizerUiState, vm: EqualizerViewModel, primary: androidx.compose.ui.graphics.Color) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.availablePresets.take(15)) { preset ->
            val selected = preset.name == state.currentPresetName
            FilterChip(
                selected = selected,
                onClick = { vm.applyPreset(preset) },
                label = { Text(preset.name, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = primary.copy(alpha = 0.15f), selectedLabelColor = primary,
                    containerColor = SonaraCard, labelColor = SonaraTextSecondary
                ),
                border = if (selected) BorderStroke(1.dp, primary.copy(alpha = 0.3f))
                         else BorderStroke(1.dp, SonaraDivider.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun CurveCard(state: EqualizerUiState) {
    FluentCard { EqCurve(bands = state.bands) }
}

@Composable
private fun BandsCard(state: EqualizerUiState, vm: EqualizerViewModel) {
    FluentCard {
        Text("Bands", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            state.bands.forEachIndexed { i, value ->
                BandSlider(value = value, onValueChange = { vm.setBand(i, it) }, label = TenBandEqualizer.LABELS[i], enabled = state.isEnabled)
            }
        }
    }
}

@Composable
private fun PreampCard(state: EqualizerUiState, vm: EqualizerViewModel, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Preamp", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
            Text("${if (state.preamp >= 0) "+" else ""}${String.format("%.1f", state.preamp)} dB",
                style = MaterialTheme.typography.labelLarge, color = if (state.preamp != 0f) primary else SonaraTextTertiary)
        }
        Spacer(Modifier.height(4.dp))
        Slider(value = state.preamp, onValueChange = { vm.setPreamp(it) }, valueRange = -12f..12f, enabled = state.isEnabled,
            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary, inactiveTrackColor = SonaraCardElevated))
    }
}

@Composable
private fun EffectsCard(state: EqualizerUiState, vm: EqualizerViewModel, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Effects", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))
        EffectRow("Bass Boost", state.bassBoost, { vm.setBassBoost(it) }, state.isEnabled, primary)
        Spacer(Modifier.height(8.dp))
        EffectRow("Virtualizer", state.virtualizer, { vm.setVirtualizer(it) }, state.isEnabled, primary)
        Spacer(Modifier.height(8.dp))
        EffectRow("Loudness", state.loudness, { vm.setLoudness(it) }, state.isEnabled, primary)
    }
}

@Composable
private fun EffectRow(label: String, value: Int, onValueChange: (Int) -> Unit, enabled: Boolean, primary: androidx.compose.ui.graphics.Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
            Text("${(value / 10f).toInt()}%", style = MaterialTheme.typography.labelMedium, color = if (value > 0) primary else SonaraTextTertiary)
        }
        Slider(value = value.toFloat(), onValueChange = { onValueChange(it.toInt()) }, valueRange = 0f..1000f, enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary, inactiveTrackColor = SonaraCardElevated))
    }
}

@Composable
private fun SavePresetDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SonaraCard,
        title = { Text("Save Preset") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                placeholder = { Text("Preset name", color = SonaraTextTertiary) },
                singleLine = true, shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = SonaraDivider,
                    focusedContainerColor = SonaraCardElevated, unfocusedContainerColor = SonaraCardElevated,
                    cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = SonaraTextPrimary, unfocusedTextColor = SonaraTextPrimary))
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name) }) { Text("Save", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SonaraTextSecondary) } }
    )
}
