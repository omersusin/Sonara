package com.sonara.app.ui.screens.insights

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*

@Composable
fun InsightsScreen() {
    val viewModel: InsightsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val primary = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Insights", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(2.dp))
                Text("How Sonara processes your sound", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
            }
        }

        item { TrackCard(state, primary) }
        item { PipelineCard(state, primary) }
        item { ConfidenceCard(state, primary) }
        item { EnergyCard(state, primary) }
        item { StatsCard(state, primary) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TrackCard(state: InsightsUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(SonaraCardElevated, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, null, tint = primary, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(state.trackTitle.ifEmpty { "No track playing" }, style = MaterialTheme.typography.titleMedium)
                if (state.trackArtist.isNotEmpty()) {
                    Text(state.trackArtist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                }
            }
            if (state.isResolving) {
                Text("Analyzing...", style = MaterialTheme.typography.labelSmall, color = primary)
            }
        }
    }
}

@Composable
private fun PipelineCard(state: InsightsUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Sound Pipeline", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(14.dp))
        PipelineRow(Icons.Rounded.Album, "Data Source", state.dataSource, primary, state.dataSource != "None")
        PipelineDivider()
        PipelineRow(Icons.Rounded.AutoAwesome, "Genre", state.genre.replaceFirstChar { it.uppercase() }, primary, state.genre != "Unknown")
        PipelineDivider()
        PipelineRow(Icons.Rounded.Speed, "Mood", state.mood.replaceFirstChar { it.uppercase() }, primary, state.mood != "Unknown")
        PipelineDivider()
        PipelineRow(Icons.Rounded.Memory, "AI Engine", state.aiAdjustment, primary, state.isAiEnabled)
        PipelineDivider()
        PipelineRow(Icons.Rounded.Headphones, "Headphone",
            if (state.headphoneConnected) state.headphoneName else "Not connected",
            primary, state.headphoneConnected)
        PipelineDivider()
        PipelineRow(Icons.Rounded.Tune, "AutoEQ",
            when {
                state.autoEqActive -> "Active (${state.autoEqProfile})"
                !state.isAutoEqEnabled -> "Disabled"
                else -> "No profile"
            }, primary, state.autoEqActive)
        PipelineDivider()
        PipelineRow(Icons.Rounded.Tune, "Preset", state.activePreset, primary, true)
    }
}

@Composable
private fun PipelineRow(icon: ImageVector, label: String, value: String, primary: androidx.compose.ui.graphics.Color, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(
            if (isActive) primary.copy(alpha = 0.8f) else SonaraTextTertiary.copy(alpha = 0.3f), CircleShape
        ))
        Icon(icon, null, tint = if (isActive) SonaraTextSecondary else SonaraTextTertiary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = if (isActive) SonaraTextPrimary else SonaraTextTertiary)
    }
}

@Composable
private fun PipelineDivider() {
    Row(modifier = Modifier.padding(start = 3.dp)) {
        Box(modifier = Modifier.size(width = 2.dp, height = 16.dp).background(SonaraDivider.copy(alpha = 0.3f)))
    }
}

@Composable
private fun ConfidenceCard(state: InsightsUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Confidence", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
            Text("${(state.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = primary)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { state.confidence },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = primary, trackColor = SonaraCardElevated, strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                state.confidence >= 0.8f -> "High confidence — sound profile is well-tuned for this track"
                state.confidence >= 0.5f -> "Moderate — some adjustments may further improve quality"
                state.confidence > 0f -> "Low — limited data available, using best estimates"
                else -> "No analysis performed yet"
            },
            style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary
        )
    }
}

@Composable
private fun EnergyCard(state: InsightsUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Energy Level", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Calm", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            Text("${(state.energy * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = primary)
            Text("Energetic", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { state.energy },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = primary, trackColor = SonaraCardElevated, strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun StatsCard(state: InsightsUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Cache & Stats", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatItem("Cached Tracks", "${state.cacheSize}", Icons.Rounded.Cached, primary, Modifier.weight(1f))
            StatItem("AI Engine", if (state.isAiEnabled) "On" else "Off", Icons.Rounded.Memory, primary, Modifier.weight(1f))
            StatItem("AutoEQ", if (state.isAutoEqEnabled) "On" else "Off", Icons.Rounded.Headphones, primary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector, primary: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = SonaraTextTertiary, modifier = Modifier.size(18.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
    }
}
