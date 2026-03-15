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
import androidx.compose.material.icons.rounded.Warning
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
import com.sonara.app.ui.components.ChipStatus
import com.sonara.app.ui.components.EqCurve
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.components.StatusChip
import com.sonara.app.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen() {
    val vm: EqualizerViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val p = MaterialTheme.colorScheme.primary
    var showSave by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Equalizer", style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(2.dp)); Text(s.currentPresetName, style = MaterialTheme.typography.bodySmall, color = p) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showSave = true }) { Icon(Icons.Rounded.Save, "Save", tint = SonaraTextSecondary) }
                    IconButton(onClick = { vm.resetBands() }) { Icon(Icons.Rounded.Refresh, "Reset", tint = SonaraTextSecondary) }
                    Switch(checked = s.isEnabled, onCheckedChange = { vm.setEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = p, checkedTrackColor = p.copy(0.3f), uncheckedThumbColor = SonaraTextTertiary, uncheckedTrackColor = SonaraCardElevated))
                }
            }
        }

        if (s.isClipping) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { StatusChip("⚠ Clipping detected — levels may be reduced", ChipStatus.Warning) } }
        }

        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.availablePresets.take(15)) { preset ->
                val sel = preset.name == s.currentPresetName
                FilterChip(sel, onClick = { vm.applyPreset(preset) }, label = { Text(preset.name, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = p.copy(0.15f), selectedLabelColor = p, containerColor = SonaraCard, labelColor = SonaraTextSecondary),
                    border = BorderStroke(1.dp, if (sel) p.copy(0.3f) else SonaraDivider.copy(0.3f)))
            }
        } }

        item { FluentCard { EqCurve(bands = s.bands) } }

        item { FluentCard {
            Text("Bands", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary); Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                s.bands.forEachIndexed { i, v -> BandSlider(v, { vm.setBand(i, it) }, TenBandEqualizer.LABELS[i], enabled = s.isEnabled) }
            }
        } }

        item { FluentCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Preamp", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                Text("${if (s.preamp >= 0) "+" else ""}${"%.1f".format(s.preamp)} dB", style = MaterialTheme.typography.labelLarge, color = if (s.preamp != 0f) p else SonaraTextTertiary)
            }; Spacer(Modifier.height(4.dp))
            Slider(s.preamp, { vm.setPreamp(it) }, valueRange = -12f..12f, enabled = s.isEnabled,
                colors = SliderDefaults.colors(thumbColor = p, activeTrackColor = p, inactiveTrackColor = SonaraCardElevated))
        } }

        item { FluentCard {
            Text("Effects", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary); Spacer(Modifier.height(12.dp))
            EffRow("Bass Boost", s.bassBoost, { vm.setBassBoost(it) }, s.isEnabled, p, 1000f, "%") { "${(it / 10f).roundToInt()}%" }
            Spacer(Modifier.height(8.dp))
            EffRow("Virtualizer", s.virtualizer, { vm.setVirtualizer(it) }, s.isEnabled, p, 1000f, "%") { "${(it / 10f).roundToInt()}%" }
            Spacer(Modifier.height(8.dp))
            EffRow("Loudness", s.loudness, { vm.setLoudness(it) }, s.isEnabled, p, 3000f, "dB") { "${"%.1f".format(it / 100f)} dB" }
        } }

        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showSave) { SaveDialog({ showSave = false }) { name -> vm.saveCurrentAsPreset(name); showSave = false } }
}

@Composable
private fun EffRow(label: String, value: Int, onChange: (Int) -> Unit, enabled: Boolean, p: androidx.compose.ui.graphics.Color, max: Float, unit: String, format: (Int) -> String) {
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
    AlertDialog(onDismiss, containerColor = SonaraCard, title = { Text("Save Preset") },
        text = { OutlinedTextField(name, { name = it }, placeholder = { Text("Preset name", color = SonaraTextTertiary) }, singleLine = true, shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = SonaraDivider, focusedContainerColor = SonaraCardElevated, unfocusedContainerColor = SonaraCardElevated, cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = SonaraTextPrimary, unfocusedTextColor = SonaraTextPrimary)) },
        confirmButton = { TextButton({ if (name.isNotBlank()) onSave(name) }) { Text("Save", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = SonaraTextSecondary) } })
}
