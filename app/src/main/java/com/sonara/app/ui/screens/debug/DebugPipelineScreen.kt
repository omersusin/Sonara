package com.sonara.app.ui.screens.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sonara.app.SonaraApp
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.components.DisplayLabelMapper
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Madde 4: Debug ekranı — pipeline durumunu detaylı gösterir.
 * Detected title, Canonical title, Source, Confidence, EQ applied?
 */
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
    val scope = rememberCoroutineScope()
    var demoResults by remember { mutableStateOf<List<String>>(emptyList()) }

    LazyColumn(
        Modifier.fillMaxSize().background(SonaraBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Pipeline Debug", style = MaterialTheme.typography.headlineMedium, color = SonaraTextPrimary)
            Spacer(Modifier.height(8.dp))
        }
        item {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val db = com.sonara.app.SonaraApp.instance.database
                            val demo = com.sonara.app.ai.demo.AiDemo(ctx, db.trainingExampleDao())
                            demoResults = listOf("Running AI Demo…")
                            demo.runFullDemo()
                            demoResults = listOf("AI Demo complete — see Logcat for results")
                        } catch (e: Exception) {
                            demoResults = listOf("Error: ${e.message}")
                        }
                    }
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Run AI Demo", color = com.sonara.app.ui.theme.SonaraBackground) }
        }
        for (result in demoResults) {
            item { DebugRow("AI Demo", result) }
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
            .clip(RoundedCornerShape(8.dp))
            .background(SonaraCard)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
    }
}
