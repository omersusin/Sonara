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
import com.sonara.app.ai.SonaraAi
import com.sonara.app.ai.SonaraAiState
import com.sonara.app.ui.components.FluentCard
import java.text.NumberFormat
import java.util.Locale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import android.content.Intent
import android.net.Uri
import com.sonara.app.ui.theme.*

@Composable
fun InsightsScreen(onArtistClick: (String) -> Unit = {}) {
    val vm: InsightsViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val art by vm.albumArt.collectAsState()
    val p = MaterialTheme.colorScheme.primary
    val aiState by (SonaraAi.getInstance()?.state ?: kotlinx.coroutines.flow.MutableStateFlow(SonaraAiState())).collectAsState()
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ═══ 1. HERO: Scrobble count + avg daily ═══
        item {
            if (s.lastFmConnected) {
                Column {
                    Text(try { fmt.format(s.totalScrobbles.toLong()) } catch (_: Exception) { s.totalScrobbles },
                        style = MaterialTheme.typography.displaySmall, color = p)
                    Text("scrobbles", style = MaterialTheme.typography.titleMedium, color = SonaraTextSecondary)
                    if (s.avgDailyScrobbles > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text("~${s.avgDailyScrobbles} per day", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                    }
                }
            } else {
                Text("Insights", style = MaterialTheme.typography.headlineLarge)
            }
        }

        // ═══ 2. PERIOD SELECTOR ═══
        if (s.lastFmConnected) {
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("7day" to "1 week", "1month" to "1 month", "3month" to "3 months", "6month" to "6 months", "12month" to "1 year", "overall" to "Overall").forEach { (id, label) ->
                        val sel = s.selectedPeriod == id
                        OutlinedButton(onClick = { vm.setPeriod(id) }, shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (sel) p else SonaraDivider.copy(0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (sel) p.copy(0.15f) else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (sel) p else SonaraTextTertiary)
                        ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }

        // ═══ 3. SNAPSHOT ROW (top artist + top track + stats) ═══
        item {
            FluentCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val topArtistName = s.topArtists.firstOrNull()?.first ?: "-"
                    val topTrackName = s.topTracks.firstOrNull()?.title ?: "-"
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(topArtistName, style = MaterialTheme.typography.titleSmall, color = p, maxLines = 1)
                        Text("#1 artist", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(topTrackName, style = MaterialTheme.typography.titleSmall, color = p, maxLines = 1)
                        Text("#1 track", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(try { fmt.format(s.totalArtists.toLong()) } catch (_: Exception) { s.totalArtists },
                            style = MaterialTheme.typography.titleSmall, color = p)
                        Text("artists", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                }
            }
        }

        // ═══ 4. NOW PLAYING ═══
        if (s.trackTitle.isNotEmpty()) { item { TrackCard(s, art, p) } }

        // ═══ 5. TOP ARTISTS ═══
        if (s.topArtists.isNotEmpty()) { item { TopArtistsCard(s, p, onArtistClick) } }

        // ═══ 6. TOP TRACKS ═══
        if (s.topTracks.isNotEmpty()) { item { TopTracksCard(s, p) } }

        // ═══ 7. RECENTLY PLAYED ═══
        if (s.recentTracks.isNotEmpty()) { item { RecentlyPlayedCard(s, p) } }

        // ═══ 8. GENRE DISTRIBUTION ═══
        if (s.genreDistribution.isNotEmpty()) { item { GenrePercentCard(s, p) } }

        // ═══ 9. AI CLASSIFICATION (compact) ═══
        if (s.trackTitle.isNotEmpty()) {
            item {
                FluentCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Memory, null, Modifier.size(16.dp), tint = p)
                        Text("AI Classification", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                        Spacer(Modifier.weight(1f))
                        Text("${s.apiAccuracy}%", style = MaterialTheme.typography.labelMedium, color = p)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.genre.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = p)
                            Text("genre", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.mood.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = p)
                            Text("mood", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(s.energy * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = p)
                            Text("energy", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.songsLearned.toString(), style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                            Text("learned", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.cacheSize.toString(), style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                            Text("cached", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.dataSource, style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                            Text("source", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}


private fun openInMusicApp(ctx: android.content.Context, query: String, type: String = "artist") {
    // Try music apps in order: Spotify, YouTube Music, Apple Music, generic search
    val intents = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$query")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/music/${java.net.URLEncoder.encode(query, "UTF-8")}"))
    )
    for (intent in intents) {
        try {
            if (intent.resolveActivity(ctx.packageManager) != null) { ctx.startActivity(intent); return }
        } catch (_: Exception) {}
    }
    // Fallback: generic web search
    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${java.net.URLEncoder.encode("$query $type", "UTF-8")}"))) } catch (_: Exception) {}
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
private fun ListeningStatsCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    FluentCard {
        if (s.lastFmConnected && s.lastFmUsername.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Public, null, tint = p, modifier = Modifier.size(20.dp))
                Text("Last.fm: ${s.lastFmUsername}", style = MaterialTheme.typography.titleSmall, color = p)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(try { fmt.format(s.totalScrobbles.toLong()) } catch (_: Exception) { s.totalScrobbles },
                        style = MaterialTheme.typography.headlineMedium, color = p)
                    Text("Scrobbles", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(try { fmt.format(s.totalArtists.toLong()) } catch (_: Exception) { s.totalArtists },
                        style = MaterialTheme.typography.headlineMedium, color = p)
                    Text("Artists", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.songsLearned.toString(), style = MaterialTheme.typography.headlineMedium, color = p)
                    Text("Learned", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Public, null, tint = SonaraTextTertiary, modifier = Modifier.size(20.dp))
                Column {
                    Text("Last.fm Not Connected", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                    Text("Connect in Settings for rich listening stats", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.songsLearned.toString(), style = MaterialTheme.typography.headlineMedium, color = p)
                    Text("Songs Learned", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.cacheSize.toString(), style = MaterialTheme.typography.headlineMedium, color = p)
                    Text("Cached", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${s.apiAccuracy}%", style = MaterialTheme.typography.headlineMedium, color = p)
                    Text("Accuracy", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                }
            }
        }
    }
}

@Composable
private fun TopArtistsCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color, onArtistClick: (String) -> Unit = {}) {
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    FluentCard {
        Text("Top Artists", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
        Spacer(Modifier.height(12.dp))
        val ctx = LocalContext.current
        s.topArtists.forEachIndexed { i, triple ->
            val name = triple.first; val plays = triple.second; val imageUrl = triple.third
            Row(Modifier.fillMaxWidth().clickable { onArtistClick(name) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(28.dp).background(if (i < 3) p.copy(alpha = 0.15f) else SonaraCardElevated, RoundedCornerShape(6.dp)),
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
            Row(Modifier.fillMaxWidth().clickable { openInMusicApp(ctx, "${track.artist} ${track.title}", "track") }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(28.dp).background(if (i < 3) p.copy(alpha = 0.15f) else SonaraCardElevated, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = if (i < 3) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                        color = if (i < 3) p else SonaraTextTertiary)
                }
                if (track.imageUrl.isNotBlank()) {
                    AsyncImage(model = ImageRequest.Builder(ctx).data(track.imageUrl).crossfade(true).build(),
                        contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(44.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MusicNote, null, tint = p.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
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
                Box(Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(6.dp)).background(SonaraCardElevated)) {
                    Box(Modifier.fillMaxWidth(count / total).height(20.dp).clip(RoundedCornerShape(6.dp)).background(p.copy(alpha = 0.7f)))
                    Text("$pct%", style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ArtistDetailDialog(d: DeezerImageResolver.ArtistDetail, p: androidx.compose.ui.graphics.Color, onDismiss: () -> Unit) {
    val ctx = LocalContext.current; val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    AlertDialog(onDismissRequest = onDismiss, containerColor = SonaraCard,
        title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (d.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(d.imageUrl).crossfade(true).build(), contentDescription = d.name, modifier = Modifier.size(56.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Text(d.name, style = MaterialTheme.typography.titleLarge, color = SonaraTextPrimary)
        } },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(fmt.format(d.fans.toLong()), style = MaterialTheme.typography.titleMedium, color = p); Text("Fans", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(d.albums.toString(), style = MaterialTheme.typography.titleMedium, color = p); Text("Albums", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary) }
            }
            if (d.topTracks.isNotEmpty()) { Text("Top Tracks", style = MaterialTheme.typography.titleSmall, color = SonaraTextSecondary)
                d.topTracks.forEachIndexed { i, t -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i+1}", style = MaterialTheme.typography.labelMedium, color = if (i<3) p else SonaraTextTertiary, modifier = Modifier.width(24.dp))
                    Text(t.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("${t.durationSec/60}:${"%02d".format(t.durationSec%60)}", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                } }
            }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = p) } })
}

@Composable
private fun RecentlyPlayedCard(s: InsightsUiState, p: androidx.compose.ui.graphics.Color) {
    val ctx = LocalContext.current
    FluentCard {
        Text("Recently Played", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary); Spacer(Modifier.height(10.dp))
        s.recentTracks.take(8).forEach { t ->
            Row(Modifier.fillMaxWidth().clickable { openInMusicApp(ctx, "${t.artist} ${t.title}", "track") }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (t.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(t.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                else Box(Modifier.size(36.dp).background(SonaraCardElevated, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.5f), modifier = Modifier.size(16.dp)) }
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
        Icon(icon, null, tint = if (on) SonaraTextSecondary else SonaraTextTertiary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = if (on) SonaraTextPrimary else SonaraTextTertiary)
    }
}

@Composable
private fun PDivider() { Row(Modifier.padding(start = 3.dp)) { Box(Modifier.size(width = 2.dp, height = 16.dp).background(SonaraDivider.copy(0.3f))) } }
