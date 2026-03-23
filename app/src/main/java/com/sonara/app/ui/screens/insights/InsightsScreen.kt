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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.School
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
    val vm: InsightsViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val art by vm.albumArt.collectAsState()
    val p = MaterialTheme.colorScheme.primary


    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Column(Modifier.padding(vertical = 8.dp)) { Text("Insights", style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(2.dp)); Text("How Sonara processes your sound", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary) } }
        item { TrackCard(s, art, p) }
        item { WhyCard(s, p) }
        item { DetectionCard(s, p) }
        item { AnalysisCard(s, p) }
        item { LearningCard(s, p) }
        item { GenreCard(s, p) }
        item { PipelineCard(s, p) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TrackCard(s: InsightsUiState, art: Bitmap?, p: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (art != null) Image(art.asImageBitmap(), "Art", Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            else Box(Modifier.size(48.dp).background(SonaraCardElevated, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = p, modifier = Modifier.size(24.dp)) }
            Column(Modifier.weight(1f)) { Text(s.trackTitle.ifEmpty { "No track playing" }, style = MaterialTheme.typography.titleMedium); if (s.trackArtist.isNotEmpty()) Text(s.trackArtist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
            if (s.isPlaying) Box(Modifier.size(8.dp).background(SonaraSuccess, CircleShape))
        }
    }
}

@Composable
private fun LearningCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.School, null, tint = p, modifier = Modifier.size(20.dp))
            Text("AI Learning", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBox(s.songsLearned.toString(), "Songs Learned", p)
            StatBox(s.songsViaLastFm.toString(), "Via Last.fm", p)
            StatBox(s.songsViaLocal.toString(), "Via Local AI", p)
            StatBox("${s.apiAccuracy}%", "API Accuracy", p)
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, p: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = p)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
    }
}

@Composable
private fun GenreCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    if (s.genreDistribution.isEmpty()) return
    FluentCard {
        Text("Genre Distribution", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))
        val sorted = s.genreDistribution.entries.sortedByDescending { it.value }.take(8)
        val max = sorted.firstOrNull()?.value?.toFloat() ?: 1f
        sorted.forEach { (genre, count) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(genre, style = MaterialTheme.typography.labelSmall, color = SonaraTextSecondary, modifier = Modifier.width(80.dp))
                Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(SonaraCardElevated)) {
                    Box(Modifier.fillMaxWidth(count / max).height(16.dp).clip(RoundedCornerShape(4.dp)).background(p.copy(alpha = 0.6f)))
                }
                Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary, modifier = Modifier.width(30.dp).padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun WhyCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Why This Result", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(8.dp))
        val isUnknown = s.genre == "Unknown" || s.confidence < 0.1f
        if (isUnknown) {
            Text("No Match", style = MaterialTheme.typography.titleMedium, color = SonaraWarning)
            Spacer(Modifier.height(4.dp))
            val reasons = mutableListOf<String>()
            if (s.dataSource.contains("Local", ignoreCase = true) || s.dataSource == "None") reasons.add("Last.fm could not find this track or artist")
            if (s.confidence < 0.1f) reasons.add("Local AI confidence too low to classify")
            if (reasons.isEmpty()) reasons.add("No source returned a strong genre signal")
            reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
            Spacer(Modifier.height(8.dp))
            Text("EQ is set to flat (no changes applied).", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
        } else {
            Text(s.genre.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = p)
            Spacer(Modifier.height(4.dp))
            val parts = mutableListOf<String>()
            if (s.dataSource.contains("Last.fm", ignoreCase = true)) parts.add("Last.fm matched genre tags for this track/artist")
            if (s.dataSource.contains("Local", ignoreCase = true)) parts.add("Local AI identified the artist pattern")
            if (s.dataSource.contains("Lyrics", ignoreCase = true)) parts.add("Lyrics analysis contributed mood data")
            if (s.dataSource.contains("Merged", ignoreCase = true)) parts.add("Multiple sources combined for higher confidence")
            if (parts.isEmpty()) parts.add("Detected via ${s.dataSource}")
            parts.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
            Spacer(Modifier.height(8.dp))
            Text("Confidence: ${(s.confidence * 100).toInt()}% — ${if (s.confidence > 0.7f) "High" else if (s.confidence > 0.4f) "Medium" else "Low"}", style = MaterialTheme.typography.bodySmall, color = if (s.confidence > 0.7f) SonaraSuccess else SonaraWarning)
        }
    }
}

@Composable
private fun DetectionCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Detection", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.genre.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge, color = p)
                Text("Genre", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.mood.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge, color = p)
                Text("Mood", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            }
        }
    }
}

@Composable
private fun AnalysisCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Analysis Details", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))
        BarRow("Energy", s.energy, p)
        Spacer(Modifier.height(8.dp))
        BarRow("Confidence", s.confidence, p)
    }
}

@Composable
private fun BarRow(label: String, value: Float, p: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary, modifier = Modifier.width(90.dp))
        LinearProgressIndicator(progress = { value }, Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)), color = p, trackColor = SonaraCardElevated, strokeCap = StrokeCap.Round)
        Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = p, modifier = Modifier.width(40.dp).padding(start = 8.dp))
    }
}

@Composable
private fun PipelineCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Pipeline", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(12.dp))
        PRow(Icons.Rounded.Album, "Source", s.dataSource, p, s.dataSource != "None")
        PDivider()
        PRow(Icons.Rounded.Memory, "AI", if (s.isAiEnabled) "Active" else "Off", p, s.isAiEnabled)
        PDivider()
        PRow(Icons.Rounded.Headphones, "Headphone", if (s.headphoneConnected) s.headphoneName else "None", p, s.headphoneConnected)
        PDivider()
        PRow(Icons.Rounded.GraphicEq, "EQ", if (s.eqActive) "Active" else "Off", p, s.eqActive)
        PDivider()
        PRow(Icons.Rounded.Cached, "Cache", "${s.cacheSize} tracks", p, s.cacheSize > 0)
        PDivider()
        PRow(Icons.Rounded.AutoAwesome, "AI Provider", s.route.ifBlank { "None" }, p, s.route.isNotBlank() && s.route != "Unknown")
    }
}

@Composable
private fun PRow(icon: ImageVector, label: String, value: String, p: androidx.compose.ui.graphics.Color, on: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(if (on) p.copy(0.8f) else SonaraTextTertiary.copy(0.3f), CircleShape))
        Icon(icon, null, tint = if (on) SonaraTextSecondary else SonaraTextTertiary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = if (on) SonaraTextPrimary else SonaraTextTertiary)
    }
}

@Composable
private fun PDivider() { Row(Modifier.padding(start = 3.dp)) { Box(Modifier.size(width = 2.dp, height = 16.dp).background(SonaraDivider.copy(0.3f))) } }
