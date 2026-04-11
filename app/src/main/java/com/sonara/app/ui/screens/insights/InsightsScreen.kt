package com.sonara.app.ui.screens.insights

import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*
import androidx.compose.foundation.BorderStroke
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InsightsScreen(
    onArtistClick: (String) -> Unit = {},
    onTrackClick: (String, String) -> Unit = { _, _ -> }
) {
    val vm: InsightsViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val art by vm.albumArt.collectAsState()
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ═══ HERO STATS ═══
        item {
            if (s.lastFmConnected) {
                FluentCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(try { fmt.format(s.totalScrobbles.toLong()) } catch (_: Exception) { s.totalScrobbles },
                            style = MaterialTheme.typography.displaySmall, color = p)
                        Text("scrobbles", style = MaterialTheme.typography.titleMedium, color = SonaraTextSecondary)
                        if (s.avgDailyScrobbles > 0) {
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("~${s.avgDailyScrobbles}/day", style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary)
                                Text(try { "${fmt.format(s.totalArtists.toLong())} artists" } catch (_: Exception) { "${s.totalArtists} artists" },
                                    style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary)
                            }
                        }
                    }
                }
            } else {
                Text("Insights", style = MaterialTheme.typography.headlineLarge, color = SonaraTextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Connect Last.fm in Settings for listening stats", style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary)
            }
        }

        // ═══ PERIOD SELECTOR ═══
        if (s.lastFmConnected) {
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("7day" to "1W", "1month" to "1M", "3month" to "3M", "6month" to "6M", "12month" to "1Y", "overall" to "All").forEach { (id, label) ->
                        val sel = s.selectedPeriod == id
                        OutlinedButton(onClick = { vm.setPeriod(id) }, shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (sel) p else SonaraDivider.copy(0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (sel) p.copy(0.15f) else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (sel) p else SonaraTextTertiary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) { Text(label, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }

        // ═══ NOW PLAYING ═══
        if (s.trackTitle.isNotEmpty()) {
            item {
                FluentCard(modifier = Modifier.clickable { onTrackClick(s.trackTitle, s.trackArtist) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (art != null) Image(art!!.asImageBitmap(), "Art", Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        else Box(Modifier.size(52.dp).background(SonaraCardElevated, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MusicNote, null, tint = p, modifier = Modifier.size(24.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(s.trackTitle, style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary, maxLines = 1)
                            if (s.trackArtist.isNotEmpty()) Text(s.trackArtist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary, maxLines = 1)
                        }
                        if (s.isPlaying) Box(Modifier.size(10.dp).background(SonaraSuccess, CircleShape))
                        if (s.genre != "Unknown") Text(s.genre, style = MaterialTheme.typography.labelSmall, color = p)
                    }
                }
            }
        }

        // ═══ TOP ARTISTS (horizontal scroll with big images) ═══
        if (s.topArtists.isNotEmpty()) {
            item { Text("Top Artists", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary, modifier = Modifier.padding(top = 4.dp)) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.topArtists) { triple ->
                        val name = triple.first; val plays = triple.second; val imageUrl = triple.third
                        val ctx = LocalContext.current
                        FluentCard(modifier = Modifier.width(130.dp).clickable { onArtistClick(name) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (imageUrl.isNotBlank()) {
                                    AsyncImage(model = ImageRequest.Builder(ctx).data(imageUrl).crossfade(true).build(), contentDescription = name,
                                        modifier = Modifier.size(90.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.size(90.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = p) }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(name, style = MaterialTheme.typography.bodySmall, color = SonaraTextPrimary, maxLines = 1)
                                Text(try { "${fmt.format(plays.toLong())} plays" } catch (_: Exception) { "$plays plays" },
                                    style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                            }
                        }
                    }
                }
            }
        }

        // ═══ TOP TRACKS ═══
        if (s.topTracks.isNotEmpty()) {
            item {
                FluentCard {
                    Text("Top Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary); Spacer(Modifier.height(12.dp))
                    val ctx = LocalContext.current
                    s.topTracks.forEachIndexed { i, track ->
                        Row(Modifier.fillMaxWidth().clickable { onTrackClick(track.title, track.artist) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(28.dp).background(if (i < 3) p.copy(0.15f) else SonaraCardElevated, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary) }
                            if (track.imageUrl.isNotBlank()) {
                                AsyncImage(model = ImageRequest.Builder(ctx).data(track.imageUrl).crossfade(true).build(), contentDescription = null,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            } else Box(Modifier.size(44.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.5f), modifier = Modifier.size(18.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary, maxLines = 1) }
                            Text(try { fmt.format(track.plays.toLong()) } catch (_: Exception) { track.plays },
                                style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                        if (i < s.topTracks.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.15f)))
                    }
                }
            }
        }

        // ═══ GENRE DISTRIBUTION (horizontal bars) ═══
        if (s.genreDistribution.isNotEmpty()) {
            item {
                FluentCard {
                    Text("Your Genres", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary); Spacer(Modifier.height(12.dp))
                    val sorted = s.genreDistribution.entries.sortedByDescending { it.value }.take(7)
                    val total = sorted.sumOf { it.value }.toFloat().coerceAtLeast(1f)
                    val maxVal = sorted.firstOrNull()?.value?.toFloat() ?: 1f
                    sorted.forEach { (genre, count) ->
                        val pct = (count / total * 100).toInt()
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(genre, style = MaterialTheme.typography.labelMedium, color = SonaraTextPrimary, modifier = Modifier.width(80.dp), maxLines = 1)
                            Box(Modifier.weight(1f).height(22.dp).clip(RoundedCornerShape(6.dp)).background(SonaraCardElevated)) {
                                Box(Modifier.fillMaxWidth(count / maxVal).height(22.dp).clip(RoundedCornerShape(6.dp)).background(p.copy(alpha = 0.6f)))
                                Text("$pct%", style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary,
                                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        // ═══ RECENTLY PLAYED ═══
        if (s.recentTracks.isNotEmpty()) {
            item {
                FluentCard {
                    Text("Recently Played", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary); Spacer(Modifier.height(10.dp))
                    val ctx = LocalContext.current
                    s.recentTracks.take(8).forEach { t ->
                        Row(Modifier.fillMaxWidth().clickable { onTrackClick(t.title, t.artist) }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (t.imageUrl.isNotBlank()) {
                                AsyncImage(model = ImageRequest.Builder(ctx).data(t.imageUrl).crossfade(true).build(), contentDescription = null,
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                            } else Box(Modifier.size(38.dp).background(SonaraCardElevated, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.5f), modifier = Modifier.size(16.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.bodySmall, color = SonaraTextPrimary, maxLines = 1)
                                Text(t.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1) }
                            if (t.isNowPlaying) Box(Modifier.size(8.dp).background(SonaraSuccess, CircleShape))
                            else Text(t.date.takeLast(11).take(6), style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                    }
                }
            }
        }

        // ═══ AI STATUS ═══
        if (s.trackTitle.isNotEmpty()) {
            item {
                FluentCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp), tint = p)
                        Text("Sonara AI", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary) }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.genre.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = p)
                            Text("genre", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.mood.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = p)
                            Text("mood", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(s.energy * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = p)
                            Text("energy", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.songsLearned.toString(), style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                            Text("learned", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.cacheSize.toString(), style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                            Text("cached", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.dataSource, style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                            Text("source", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary) }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
