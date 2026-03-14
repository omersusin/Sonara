package com.sonara.app.ui.screens.presets

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.preset.Preset
import com.sonara.app.ui.components.EqCurve
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*

@Composable
fun PresetsScreen() {
    val viewModel: PresetsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val primary = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Presets", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.filterTabs.forEach { (key, label) ->
                    val selected = state.selectedFilter == key
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setFilter(key) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primary.copy(alpha = 0.15f),
                            selectedLabelColor = primary,
                            containerColor = SonaraCard,
                            labelColor = SonaraTextSecondary
                        ),
                        border = if (selected) BorderStroke(1.dp, primary.copy(alpha = 0.3f))
                                 else BorderStroke(1.dp, SonaraDivider.copy(alpha = 0.3f))
                    )
                }
            }
        }

        val filtered = viewModel.filteredPresets()
        if (filtered.isEmpty()) {
            item {
                FluentCard {
                    Text("No presets found", style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary)
                }
            }
        } else {
            items(filtered, key = { it.id }) { preset ->
                PresetItem(
                    preset = preset,
                    isSelected = preset.id == state.selectedPresetId,
                    onSelect = { viewModel.selectPreset(preset.id) },
                    onFav = { viewModel.toggleFavorite(preset) },
                    onDuplicate = { viewModel.duplicatePreset(preset) },
                    onDelete = { viewModel.deletePreset(preset) }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PresetItem(
    preset: Preset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onFav: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = if (isSelected) primary.copy(alpha = 0.5f) else SonaraDivider.copy(alpha = 0.3f)

    androidx.compose.material3.Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) SonaraCardElevated else SonaraCard,
        border = BorderStroke(if (isSelected) 1.dp else 0.6.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(preset.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        preset.category.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = SonaraTextTertiary
                    )
                }
                Row {
                    IconButton(onClick = onFav, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (preset.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (preset.isFavorite) SonaraError else SonaraTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.ContentCopy, "Duplicate", tint = SonaraTextTertiary, modifier = Modifier.size(18.dp))
                    }
                    if (!preset.isBuiltIn) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Delete, "Delete", tint = SonaraTextTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            EqCurve(bands = preset.bandsArray(), modifier = Modifier.height(50.dp))
        }
    }
}
