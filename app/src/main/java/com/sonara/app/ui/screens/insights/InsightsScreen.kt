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
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
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

        item {
            FluentCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).background(SonaraCardElevated, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, tint = primary, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            state.trackTitle.ifEmpty { "No track playing" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.trackArtist.isNotEmpty()) {
                            Text(state.trackArtist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        }
                    }
                }
            }
        }

        item {
            FluentCard {
                Text("Sound Pipeline", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                Spacer(Modifier.height(14.dp))
                PipelineRow(Icons.Rounded.Album, "Data Source", state.dataSource, primary)
                PipelineDivider()
                PipelineRow(Icons.Rounded.AutoAwesome, "Genre", state.genre, primary)
                PipelineDivider()
                PipelineRow(Icons.Rounded.Memory, "AI Adjustment", state.aiAdjustment, primary)
                PipelineDivider()
                PipelineRow(Icons.Rounded.Headphones, "Headphone",
                    state.headphoneName.ifEmpty { "Not connected" }, primary)
                PipelineDivider()
                PipelineRow(Icons.Rounded.Tune, "Active Preset", state.activePreset, primary)
            }
        }

        item {
            FluentCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Confidence", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                    Text(
                        "${(state.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.confidence },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = primary,
                    trackColor = SonaraCardElevated,
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        state.confidence >= 0.8f -> "High confidence — sound profile is well-tuned"
                        state.confidence >= 0.5f -> "Moderate confidence — some adjustments may improve quality"
                        state.confidence > 0f -> "Low confidence — limited data available"
                        else -> "No analysis performed yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SonaraTextTertiary
                )
            }
        }

        item {
            FluentCard {
                Text("Energy Level", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Calm", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    Text("Energetic", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { state.energy },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = primary,
                    trackColor = SonaraCardElevated,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PipelineRow(icon: ImageVector, label: String, value: String, primary: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(8.dp).background(primary.copy(alpha = 0.6f), CircleShape)
        )
        Icon(icon, null, tint = SonaraTextSecondary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = SonaraTextPrimary)
    }
}

@Composable
private fun PipelineDivider() {
    Row(modifier = Modifier.padding(start = 3.dp)) {
        Box(modifier = Modifier.size(width = 2.dp, height = 16.dp).background(SonaraDivider.copy(alpha = 0.3f)))
    }
}
