@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sonara.app.ui.screens.presets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.preset.Preset
import com.sonara.app.ui.components.EqCurve
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.SonaraCard
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.SonaraDivider
import com.sonara.app.ui.theme.SonaraError
import com.sonara.app.ui.theme.SonaraSuccess
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextSecondary
import com.sonara.app.ui.theme.SonaraTextTertiary

@Composable
fun PresetsScreen() {
    val vm: PresetsViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val p = MaterialTheme.colorScheme.primary

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Presets", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 8.dp)) }

        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.filterTabs.forEach { (key, label) ->
                    val sel = s.selectedFilter == key
                    FilterChip(selected = sel, onClick = { vm.setFilter(key) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = p.copy(0.15f), selectedLabelColor = p, containerColor = SonaraCard, labelColor = SonaraTextSecondary),
                        border = BorderStroke(1.dp, if (sel) p.copy(0.3f) else SonaraDivider.copy(0.3f)))
                }
            }
        }

        val filtered = vm.filteredPresets()
        if (filtered.isEmpty()) {
            item { FluentCard { Text("No presets found", style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary) } }
        } else {
            items(filtered, key = { it.id }) { preset ->
                val isActive = preset.name == s.activePresetName
                PresetItem(preset, isActive,
                    onApply = { vm.applyPreset(preset) },
                    onFav = { vm.toggleFavorite(preset) },
                    onDuplicate = { vm.duplicatePreset(preset) },
                    onDelete = { vm.deletePreset(preset) })
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PresetItem(preset: Preset, isActive: Boolean, onApply: () -> Unit, onFav: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    val p = MaterialTheme.colorScheme.primary
    val border = if (isActive) p.copy(0.6f) else SonaraDivider.copy(0.3f)
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "preset_press"
    )

    Surface(
        onClick = onApply,
        shape = MaterialTheme.shapes.medium,
        color = if (isActive) SonaraCardElevated else SonaraCard,
        border = BorderStroke(if (isActive) 1.5.dp else 0.6.dp, border),
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(onPress = { isPressed = true; try { awaitRelease() } finally { isPressed = false } })
            }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(preset.name, style = MaterialTheme.typography.titleMedium)
                        if (isActive) Icon(Icons.Rounded.Check, "Active", tint = SonaraSuccess, modifier = Modifier.size(16.dp))
                    }
                    Text(preset.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
                Row {
                    IconButton(onClick = onFav, Modifier.size(32.dp)) {
                        Icon(if (preset.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Fav",
                            tint = if (preset.isFavorite) SonaraError else SonaraTextTertiary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDuplicate, Modifier.size(32.dp)) { Icon(Icons.Rounded.ContentCopy, "Copy", tint = SonaraTextTertiary, modifier = Modifier.size(18.dp)) }
                    if (!preset.isBuiltIn) {
                        IconButton(onClick = onDelete, Modifier.size(32.dp)) { Icon(Icons.Rounded.Delete, "Del", tint = SonaraTextTertiary, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            EqCurve(bands = preset.bandsArray(), modifier = Modifier.height(50.dp))
            if (isActive) {
                Spacer(Modifier.height(4.dp))
                Text("Currently active — edit in EQ tab", style = MaterialTheme.typography.labelSmall, color = p)
            }
        }
    }
}
