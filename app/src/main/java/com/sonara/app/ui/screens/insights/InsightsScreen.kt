package com.sonara.app.ui.screens.insights

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*

@Composable
fun InsightsScreen() {
    val viewModel: InsightsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val albumArt by viewModel.albumArt.collectAsState()
    val primary = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) { viewModel.refreshCache() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Column(Modifier.padding(vertical = 8.dp)) { Text("Insights", style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(2.dp)); Text("How Sonara processes your sound", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary) } }

        item { TrackCard(state, albumArt, primary) }
        item { PipelineCard(state, primary) }
        item { ConfidenceCard(state, primary) }
        item { EnergyCard(state, primary) }
        item { StatsCard(state, primary) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TrackCard(state: InsightsUiState, art: Bitmap?, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (art != null) {
                Image(bitmap = art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.size(48.dp).background(SonaraCardElevated, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = primary, modifier = Modifier.size(24.dp)) }
            }
            Column(Modifier.weight(1f)) {
                Text(state.trackTitle.ifEmpty { "No track playing" }, style = MaterialTheme.typography.titleMedium)
                if (state.trackArtist.isNotEmpty()) Text(state.trackArtist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
            if (state.isPlaying) { Box(Modifier.size(8.dp).background(SonaraSuccess, CircleShape)) }
        }
    }
}

@Composable
private fun PipelineCard(state: InsightsUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Sound Pipeline", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary); Spacer(Modifier.height(14.dp))
        PRow(Icons.Rounded.Album, "Data Source", state.dataSource, primary, state.dataSource != "None")
        PDivider()
        PRow(Icons.Rounded.AutoAwesome, "Genre", state.genre.replaceFirstChar { it.uppercase() }, primary, state.genre != "Unknown")
        PDivider()
        PRow(Icons.Rounded.Speed, "Mood", state.mood.replaceFirstChar { it.uppercase() }, primary, state.mood != "Unknown")
        PDivider()
        PRow(Icons.Rounded.Memory, "AI Engine", state.aiAdjustment, primary, state.isAiEnabled)
        PDivider()
        PRow(Icons.Rounded.Headphones, "Headphone", if (state.headphoneConnected) state.headphoneName else "Not connected", primary, state.headphoneConnected)
        PDivider()
        PRow(Icons.Rounded.Tune, "AutoEQ", if (state.autoEqActive) "Active" else if (!state.isAutoEqEnabled) "Disabled" else "No profile", primary, state.autoEqActive)
        PDivider()
        PRow(Icons.Rounded.GraphicEq, "EQ Session", if (state.eqSessionActive) "Active" else "Waiting", primary, state.eqSessionActive)
    }
}

@Composable
private fun PRow(icon: ImageVector, label: String, value: String, primary: androidx.compose.ui.graphics.Color, active: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(if (active) primary.copy(alpha = 0.8f) else SonaraTextTertiary.copy(alpha = 0.3f), CircleShape))
        Icon(icon, null, tint = if (active) SonaraTextSecondary else SonaraTextTertiary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = if (active) SonaraTextPrimary else SonaraTextTertiary)
    }
}

@Composable
private fun PDivider() { Row(Modifier.padding(start = 3.dp)) { Box(Modifier.size(width = 2.dp, height = 16.dp).background(SonaraDivider.copy(alpha = 0.3f))) } }

@Composable
private fun ConfidenceCard(state: InsightsUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Confidence", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary); Text("${(state.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = primary) }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { state.confidence }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = primary, trackColor = SonaraCardElevated, strokeCap = StrokeCap.Round)
    }
}

@Composable
private fun EnergyCard(state: InsightsUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Energy Level", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary); Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Calm", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary); Text("Energetic", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary) }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { state.energy }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = primary, trackColor = SonaraCardElevated, strokeCap = StrokeCap.Round)
    }
}

@Composable
private fun StatsCard(state: InsightsUiState, primary: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Stats", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary); Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatI("Cache", "${state.cacheSize}", Icons.Rounded.Cached, primary, Modifier.weight(1f))
            StatI("AI", if (state.isAiEnabled) "On" else "Off", Icons.Rounded.Memory, primary, Modifier.weight(1f))
            StatI("EQ", if (state.eqSessionActive) "Active" else "Off", Icons.Rounded.GraphicEq, primary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatI(label: String, value: String, icon: ImageVector, primary: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { Icon(icon, null, tint = SonaraTextTertiary, modifier = Modifier.size(18.dp)); Text(value, style = MaterialTheme.typography.labelLarge, color = primary); Text(label, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary) }
}
