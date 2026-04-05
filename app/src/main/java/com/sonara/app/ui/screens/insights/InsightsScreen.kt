@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sonara.app.ui.screens.insights

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.sonara.app.ai.SonaraAi
import com.sonara.app.ai.SonaraAiState
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.AppFullShape
import com.sonara.app.ui.theme.SonaraCard
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.SonaraDivider
import com.sonara.app.ui.theme.SonaraSuccess
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextSecondary
import com.sonara.app.ui.theme.SonaraTextTertiary
import com.sonara.app.ui.theme.SonaraWarning
import com.sonara.app.ui.theme.SonaraInfo
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InsightsScreen() {
    val vm: InsightsViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val art by vm.albumArt.collectAsState()
    val p = MaterialTheme.colorScheme.primary
    val aiState by (SonaraAi.getInstance()?.state ?: kotlinx.coroutines.flow.MutableStateFlow(SonaraAiState())).collectAsState()
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        if (s.lastFmConnected) {
            item {
                Column {
                    Text(try { fmt.format(s.totalScrobbles.toLong()) } catch (_: Exception) { s.totalScrobbles },
                        style = MaterialTheme.typography.displaySmall, color = p)
                    Text("scrobbles", style = MaterialTheme.typography.titleMedium, color = SonaraTextSecondary)
                }
            }
        } else {
            item { Text("Insights", style = MaterialTheme.typography.headlineLarge) }
        }

        if (s.lastFmConnected) {
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("7day" to "1 week", "1month" to "1 month", "3month" to "3 months", "6month" to "6 months", "12month" to "1 year", "overall" to "Overall").forEach { (id, label) ->
                        val sel = s.selectedPeriod == id
                        OutlinedButton(onClick = { vm.setPeriod(id) }, shape = AppFullShape,
                            border = BorderStroke(1.dp, if (sel) p else SonaraDivider.copy(0.4f)),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = if (sel) p.copy(0.15f) else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (sel) p else SonaraTextTertiary)
                        ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }

        item {
            FluentCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(try { fmt.format(s.totalArtists.toLong()) } catch (_: Exception) { s.totalArtists },
                            style = MaterialTheme.typography.headlineSmall, color = p)
                        Text("artists", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.songsLearned.toString(), style = MaterialTheme.typography.headlineSmall, color = p)
                        Text("learned", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.cacheSize.toString(), style = MaterialTheme.typography.headlineSmall, color = p)
                        Text("cached", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${s.apiAccuracy}%", style = MaterialTheme.typography.headlineSmall, color = p)
                        Text("accuracy", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                }
            }
        }

        if (s.trackTitle.isNotEmpty()) { item { TrackCard(s, art, p) } }
        if (s.topArtists.isNotEmpty()) { item { TopArtistsCard(s, p) } }
        if (s.topTracks.isNotEmpty()) { item { TopTracksCard(s, p) } }
        if (s.recentTracks.isNotEmpty()) { item { RecentlyPlayedCard(s, p) } }
        if (s.genreDistribution.isNotEmpty()) { item { GenrePercentCard(s, p) } }
        if (s.weeklyTracks.isNotEmpty()) { item { WeeklyCard(s, p) } }

        if (s.trackTitle.isNotEmpty()) {
            item {
                FluentCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("${s.genre.replaceFirstChar { it.uppercase() }} · ${s.mood.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                            Text("Energy ${(s.energy * 100).toInt()}% · Confidence ${(s.confidence * 100).toInt()}% · ${s.dataSource}",
                                style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                    }
                }
            }
        }

        item { PipelineCard(s, p) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TrackCard(s: InsightsUiState, art: Bitmap?, p: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (art != null) Image(art.asImageBitmap(), "Art", Modifier.size(48.dp).clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
            else Box(Modifier.size(48.dp).background(SonaraCardElevated, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(Icons.Rounded.MusicNote, null, tint = p, modifier = Modifier.size(24.dp)) }
            Column(Modifier.weight(1f)) { Text(s.trackTitle.ifEmpty { "No track playing" }, style = MaterialTheme.typography.titleMedium); if (s.trackArtist.isNotEmpty()) Text(s.trackArtist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
            if (s.isPlaying) Box(Modifier.size(8.dp).background(SonaraSuccess, CircleShape))
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
                Box(Modifier.weight(1f).height(16.dp).clip(MaterialTheme.shapes.extraSmall).background(SonaraCardElevated)) {
                    Box(Modifier.fillMaxWidth(count / max).height(16.dp).clip(MaterialTheme.shapes.extraSmall).background(p.copy(alpha = 0.6f)))
                }
                Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary, modifier = Modifier.width(30.dp).padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun BarRow(label: String, value: Float, p: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary, modifier = Modifier.width(90.dp))
        LinearProgressIndicator(progress = { value }, Modifier.weight(1f).height(6.dp).clip(MaterialTheme.shapes.extraSmall), color = p, trackColor = SonaraCardElevated, strokeCap = StrokeCap.Round)
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
private fun TopArtistsCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    FluentCard {
        Text("Top Artists", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
        Spacer(Modifier.height(12.dp))
        val ctx = LocalContext.current
        s.topArtists.forEachIndexed { i, triple ->
            val name = triple.first; val plays = triple.second; val imageUrl = triple.third
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(28.dp).background(if (i < 3) p.copy(alpha = 0.15f) else SonaraCardElevated, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = if (i < 3) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                        color = if (i < 3) p else SonaraTextTertiary)
                }
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(imageUrl).crossfade(true).build(),
                        contentDescription = name,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(44.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleSmall, color = p)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                    Text(try { "${fmt.format(plays.toLong())} plays" } catch (_: Exception) { "$plays plays" },
                        style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                }
            }
            if (i < s.topArtists.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.2f)))
        }
    }
}

@Composable
private fun TopTracksCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    FluentCard {
        Text("Top Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
        Spacer(Modifier.height(12.dp))
        val ctx = LocalContext.current
        s.topTracks.forEachIndexed { i, track ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(28.dp).background(if (i < 3) p.copy(alpha = 0.15f) else SonaraCardElevated, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = if (i < 3) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                        color = if (i < 3) p else SonaraTextTertiary)
                }
                if (track.imageUrl.isNotBlank()) {
                    AsyncImage(model = ImageRequest.Builder(ctx).data(track.imageUrl).crossfade(true).build(),
                        contentDescription = null, modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(44.dp).background(SonaraCardElevated, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.Rounded.MusicNote, null, tint = p.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary, maxLines = 1)
                }
                Text(try { fmt.format(track.plays.toLong()) } catch (_: Exception) { track.plays },
                    style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            }
            if (i < s.topTracks.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.2f)))
        }
    }
}

@Composable
private fun WeeklyCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("This Week", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(10.dp))
        s.weeklyTracks.forEachIndexed { i, (title, artist, plays) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                    Text(artist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary, maxLines = 1)
                }
                Text(plays, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            }
        }
    }
}

@Composable
private fun GenrePercentCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    FluentCard {
        Text("Your Genres", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
        Spacer(Modifier.height(12.dp))
        val sorted = s.genreDistribution.entries.sortedByDescending { it.value }.take(6)
        val total = sorted.sumOf { it.value }.toFloat().coerceAtLeast(1f)
        sorted.forEach { (genre, count) ->
            val pct = (count / total * 100).toInt()
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(genre, style = MaterialTheme.typography.bodySmall, color = SonaraTextPrimary, modifier = Modifier.width(72.dp))
                Box(Modifier.weight(1f).height(20.dp).clip(MaterialTheme.shapes.small).background(SonaraCardElevated)) {
                    Box(Modifier.fillMaxWidth(count / total).height(20.dp).clip(MaterialTheme.shapes.small).background(p.copy(alpha = 0.7f)))
                    Text("$pct%", style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    val ctx = LocalContext.current
    FluentCard {
        Text("Recently Played", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary); Spacer(Modifier.height(10.dp))
        s.recentTracks.take(8).forEach { t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (t.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(t.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(36.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                else Box(Modifier.size(36.dp).background(SonaraCardElevated, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.5f), modifier = Modifier.size(16.dp)) }
                Column(Modifier.weight(1f)) { Text(t.title, style = MaterialTheme.typography.bodySmall, color = SonaraTextPrimary, maxLines = 1); Text(t.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1) }
                if (t.isNowPlaying) Box(Modifier.size(8.dp).background(SonaraSuccess, CircleShape))
                else Text(t.date.takeLast(11).take(6), style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            }
        }
    }
}

@Composable
private fun PRow(icon: ImageVector, label: String, value: String, p: androidx.compose.ui.graphics.Color, on: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(if (on) p.copy(0.8f) else SonaraTextTertiary.copy(0.3f), CircleShape))
        androidx.compose.material3.Icon(icon, null, tint = if (on) SonaraTextSecondary else SonaraTextTertiary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = if (on) SonaraTextPrimary else SonaraTextTertiary)
    }
}

@Composable
private fun PDivider() { Row(Modifier.padding(start = 3.dp)) { Box(Modifier.size(width = 2.dp, height = 16.dp).background(SonaraDivider.copy(0.3f))) } }
