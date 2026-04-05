@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sonara.app.ui.screens.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sonara.app.SonaraApp
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.components.DisplayLabelMapper
import com.sonara.app.ui.theme.SonaraBackground
import com.sonara.app.ui.theme.SonaraCard
import com.sonara.app.ui.theme.SonaraDivider
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextSecondary

@Composable
fun DebugPipelineScreen() {
    val np by SonaraNotificationListener.nowPlaying.collectAsState()
    val genre by SonaraNotificationListener.currentGenre.collectAsState()
    val mood by SonaraNotificationListener.currentMood.collectAsState()
    val energy by SonaraNotificationListener.currentEnergy.collectAsState()
    val confidence by SonaraNotificationListener.currentConfidence.collectAsState()
    val source by SonaraNotificationListener.currentSource.collectAsState()
    val eqState by SonaraApp.instance.eqState.collectAsState()
    val route by SonaraApp.instance.currentRoute.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().background(SonaraBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Pipeline Debug", style = MaterialTheme.typography.headlineMedium, color = SonaraTextPrimary)
            Spacer(Modifier.height(8.dp))
        }
        item { DebugRow("Detected Title", np.title.ifBlank { "—" }) }
        item { DebugRow("Detected Artist", np.artist.ifBlank { "—" }) }
        item { DebugRow("Package", np.packageName.ifBlank { "—" }) }
        item { DebugRow("Duration", if (np.duration > 0) "${np.duration / 1000}s" else "—") }
        item { Divider(Modifier.padding(vertical = 4.dp), color = SonaraDivider.copy(0.3f)) }
        item { DebugRow("Genre", DisplayLabelMapper.formatGenre(genre)) }
        item { DebugRow("Mood", DisplayLabelMapper.formatMood(mood)) }
        item { DebugRow("Energy", "%.0f%%".format(energy * 100)) }
        item { DebugRow("Confidence", "%.0f%%".format(confidence * 100)) }
        item { DebugRow("Source", DisplayLabelMapper.formatSource(source)) }
        item { Divider(Modifier.padding(vertical = 4.dp), color = SonaraDivider.copy(0.3f)) }
        item { DebugRow("Audio Route", route.displayName) }
        item { DebugRow("EQ Enabled", if (eqState.isEnabled) "YES" else "NO") }
        item { DebugRow("EQ Preset", eqState.presetName) }
        item { DebugRow("Manual Preset", if (eqState.isManualPreset) "YES" else "NO") }
        item { DebugRow("Bass Boost", "${eqState.bassBoost}") }
        item { DebugRow("Virtualizer", "${eqState.virtualizer}") }
        item { DebugRow("Loudness", "${eqState.loudness}") }
        item { DebugRow("Bands", eqState.bands.joinToString(", ") { "%.1f".format(it) }) }
        item { DebugRow("Playing", if (np.isPlaying) "YES" else "NO") }
        item { DebugRow("EQ Strategy", SonaraApp.instance.audioSessionManager.activeStrategy.value) }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(SonaraCard)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
    }
}
