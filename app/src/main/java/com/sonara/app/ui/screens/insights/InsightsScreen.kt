package com.sonara.app.ui.screens.insights

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.TextButton
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.graphics.nativeCanvas
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    onArtistClick: (String) -> Unit = {},
    onTrackClick: (String, String) -> Unit = { _, _ -> },
    onSeeAllArtists: () -> Unit = {},
    onSeeAllTracks: () -> Unit = {},
    onSeeAllAlbums: () -> Unit = {},
    onSeeAllRecentTracks: () -> Unit = {},
    onSeeAllGenres: () -> Unit = {},
    onSeeAllListeningActivity: () -> Unit = {},
    onSeeAllLovedTracks: () -> Unit = {},
    onAlbumClick: (name: String, artist: String, plays: String, imageUrl: String) -> Unit = { _, _, _, _ -> },
    onConnectLastFm: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onGlobalChartsClick: () -> Unit = {},
    onCountryChartsClick: () -> Unit = {},
    onFriendClick: (FriendItem) -> Unit = {}
) {
    val vm: InsightsViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val art by vm.albumArt.collectAsState()
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Search icon row
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search", tint = p)
                }
            }
        }

        // ═══ HERO STATS ═══
        item {
            if (s.lastFmConnected) {
                FluentCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(try { fmt.format(s.totalScrobbles.toLong()) } catch (_: Exception) { s.totalScrobbles },
                            style = MaterialTheme.typography.displaySmall, color = p, fontWeight = FontWeight.Bold)
                        Text("scrobbles", style = MaterialTheme.typography.titleMedium, color = SonaraTextSecondary)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatColumn(if (s.listeningHours > 24) "${s.listeningHours / 24}d ${s.listeningHours % 24}h" else "${s.listeningHours}h", "listening", p)
                            StatColumn(try { fmt.format(s.totalArtists.toLong()) } catch (_: Exception) { s.totalArtists }, "artists", p)
                            StatColumn(try { fmt.format(s.trackCount.toLong()) } catch (_: Exception) { s.trackCount }, "tracks", p)
                            StatColumn("~${s.avgDailyScrobbles}", "per day", p)
                        }
                        if (s.scrobblesToday > 0) {
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.15f)))
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                StatColumn("${s.scrobblesToday}", "today", p)
                            }
                        }
                        if (s.streakDays > 0 || s.peakHour >= 0 || s.discoveryRate > 0) {
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.2f)))
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                if (s.streakDays > 0) StatColumn("${s.streakDays}d", "streak", p)
                                if (s.peakHour >= 0) {
                                    val h = s.peakHour
                                    StatColumn("${if (h % 12 == 0) 12 else h % 12}${if (h < 12) "am" else "pm"}", "peak hour", p)
                                }
                                if (s.discoveryRate > 0) StatColumn("${s.discoveryRate}%", "variety", p)
                            }
                        }
                        // ═══ MEMBERSHIP MILESTONE ═══
                        if (s.registeredUnix > 0) {
                            val daysSince = remember(s.registeredUnix) {
                                ((System.currentTimeMillis() / 1000 - s.registeredUnix) / 86400).toInt()
                            }
                            val regYear = remember(s.registeredUnix) {
                                java.util.Calendar.getInstance().apply { timeInMillis = s.registeredUnix * 1000 }
                                    .get(java.util.Calendar.YEAR)
                            }
                            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.15f)))
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.Cake, contentDescription = null, tint = p, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    buildString {
                                        append("Day ")
                                        append(when {
                                            daysSince < 30 -> "$daysSince of your music journey"
                                            daysSince < 365 -> "${daysSince / 30} months of listening"
                                            else -> "${daysSince / 365} years of listening"
                                        })
                                        if (currentYear - regYear >= 1) append(" · since $regYear")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SonaraTextTertiary
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Insights",
                            style = MaterialTheme.typography.headlineLarge,
                            color = SonaraTextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Connect Last.fm to unlock listening stats,\ntop artists, albums, and more.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SonaraTextTertiary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = onConnectLastFm,
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.5.dp, p),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = p)
                        ) {
                            Text(
                                "Connect Last.fm",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // ═══ PERIOD SELECTOR — tab bar style ═══
        if (s.lastFmConnected) {
            item {
                TabPeriodRow(s.selectedPeriod, p, { vm.setPeriod(it) }, { from, to -> vm.setCustomPeriod(from, to) })
            }
        }

        // ═══ NOW PLAYING ═══
        if (s.trackTitle.isNotEmpty()) {
            item {
                FluentCard(modifier = Modifier.clickable { onTrackClick(s.trackTitle, s.trackArtist) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (art != null) Image(art!!.asImageBitmap(), "Art", Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        else Box(Modifier.size(52.dp).background(SonaraCardElevated, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = p, modifier = Modifier.size(24.dp)) }
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

        // ═══ TOP ARTISTS — Asymmetric collage ═══
        if (s.topArtists.size >= 3) {
            item { SectionHeader("Top Artists") { onSeeAllArtists() } }
            item {
                val ctx = LocalContext.current
                Row(Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // #1 large left
                    val a1 = s.topArtists[0]
                    Box(Modifier.weight(0.6f).fillMaxSize().clip(RoundedCornerShape(16.dp)).clickable { onArtistClick(a1.first) }) {
                        if (a1.third.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(a1.third).crossfade(true).build(), contentDescription = a1.first, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Box(Modifier.fillMaxSize().background(SonaraCardElevated))
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                            Text("#1", style = MaterialTheme.typography.labelSmall, color = p)
                            Text(a1.first, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                            Text(try { "${fmt.format(a1.second.toLong())} plays" } catch (_: Exception) { "${a1.second} plays" }, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                        }
                    }
                    // #2 and #3 stacked right
                    Column(Modifier.weight(0.4f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2).forEach { idx ->
                            if (idx < s.topArtists.size) {
                                val a = s.topArtists[idx]
                                Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onArtistClick(a.first) }) {
                                    if (a.third.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(a.third).crossfade(true).build(), contentDescription = a.first, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else Box(Modifier.fillMaxSize().background(SonaraCardElevated))
                                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
                                    Column(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                                        Text("#${idx + 1}", style = MaterialTheme.typography.labelSmall, color = p)
                                        Text(a.first, style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Remaining artists as horizontal scroll
            if (s.topArtists.size > 3) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(s.topArtists.drop(3)) { a ->
                            val ctx = LocalContext.current
                            Column(Modifier.width(80.dp).clickable { onArtistClick(a.first) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                if (a.third.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(a.third).crossfade(true).build(), contentDescription = a.first, modifier = Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                else Box(Modifier.size(64.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) { Text(a.first.take(1), style = MaterialTheme.typography.titleSmall, color = p) }
                                Text(a.first, style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary, maxLines = 1)
                            }
                        }
                    }
                }
            }
        } else if (s.topArtists.isNotEmpty()) {
            item { SectionHeader("Top Artists") { onSeeAllArtists() } }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(s.topArtists) { a ->
                        val ctx = LocalContext.current
                        Column(Modifier.width(100.dp).clickable { onArtistClick(a.first) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            if (a.third.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(a.third).crossfade(true).build(), contentDescription = a.first, modifier = Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            else Box(Modifier.size(80.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) { Text(a.first.take(1), style = MaterialTheme.typography.headlineSmall, color = p) }
                            Spacer(Modifier.height(4.dp))
                            Text(a.first, style = MaterialTheme.typography.bodySmall, color = SonaraTextPrimary, maxLines = 1)
                            Text(try { "${fmt.format(a.second.toLong())}" } catch (_: Exception) { a.second }, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                    }
                }
            }
        }

        // ═══ TOP TRACKS ═══
        if (s.topTracks.isNotEmpty()) {
            item {
                FluentCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Top Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary); Text("See all >", style = MaterialTheme.typography.labelMedium, color = p, modifier = Modifier.clickable { onSeeAllTracks() }) }; Spacer(Modifier.height(10.dp))
                    val ctx = LocalContext.current
                    s.topTracks.forEachIndexed { i, track ->
                        Row(Modifier.fillMaxWidth().clickable { onTrackClick(track.title, track.artist) }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${i+1}", style = MaterialTheme.typography.labelLarge, color = if (i<3) p else SonaraTextTertiary, modifier = Modifier.width(22.dp))
                            if (track.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(track.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            else Box(Modifier.size(42.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.4f), modifier = Modifier.size(16.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                Text(track.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                            }
                            Text(try { fmt.format(track.plays.toLong()) } catch (_: Exception) { track.plays }, style = MaterialTheme.typography.labelMedium, color = p)
                        }
                        if (i < s.topTracks.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.12f)))
                    }
                }
            }
        }

        // ═══ TOP ALBUMS ═══
        if (s.topAlbums.isNotEmpty()) {
            item { SectionHeader("Top Albums") { onSeeAllAlbums() } }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(s.topAlbums) { album ->
                        val ctx = LocalContext.current
                        Column(Modifier.width(120.dp).clickable { onAlbumClick(album.name, album.artist, album.plays, album.imageUrl) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            if (album.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(album.imageUrl).crossfade(true).build(), contentDescription = album.name,
                                modifier = Modifier.size(110.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                            else Box(Modifier.size(110.dp).background(SonaraCardElevated, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Album, null, tint = p.copy(0.3f), modifier = Modifier.size(32.dp)) }
                            Spacer(Modifier.height(6.dp))
                            Text(album.name, style = MaterialTheme.typography.bodySmall, color = SonaraTextPrimary, maxLines = 1)
                            Text(album.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                            Text(try { "${fmt.format(album.plays.toLong())} plays" } catch (_: Exception) { "${album.plays} plays" }, style = MaterialTheme.typography.labelSmall, color = p)
                        }
                    }
                }
            }
        }

        // ═══ GLOBAL CHARTS + COUNTRY BUTTONS ═══
        if (s.lastFmConnected) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onGlobalChartsClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, p.copy(0.5f))
                    ) {
                        Icon(Icons.Rounded.Public, null, Modifier.size(16.dp).padding(end = 4.dp))
                        Text("Global Charts", style = MaterialTheme.typography.labelMedium, color = p)
                    }
                    OutlinedButton(
                        onClick = onCountryChartsClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, p.copy(0.5f))
                    ) {
                        Text("Country Top", style = MaterialTheme.typography.labelMedium, color = p)
                    }
                }
            }
        }

        // ═══ GENRE DISTRIBUTION ═══
        if (s.genreDistribution.isNotEmpty()) {
            item { SectionHeader("Your Genres") { onSeeAllGenres() } }
            item {
                FluentCard {
                    val sorted = s.genreDistribution.entries.sortedByDescending { it.value }.take(7)
                    val total = sorted.sumOf { it.value }.toFloat().coerceAtLeast(1f)
                    val maxVal = sorted.firstOrNull()?.value?.toFloat() ?: 1f
                    sorted.forEach { (genre, count) ->
                        val pct = (count / total * 100).toInt()
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(genre, style = MaterialTheme.typography.labelMedium, color = SonaraTextPrimary, modifier = Modifier.width(80.dp), maxLines = 1)
                            Box(Modifier.weight(1f).height(22.dp).clip(RoundedCornerShape(6.dp)).background(SonaraCardElevated)) {
                                Box(Modifier.fillMaxWidth(count / maxVal).height(22.dp).clip(RoundedCornerShape(6.dp)).background(p.copy(alpha = 0.6f)))
                                Text("$pct%", style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        // ═══ LISTENING HEATMAP ═══
        if (s.heatmap.isNotEmpty()) {
            item { ListeningHeatmap(s.heatmap, p) }
        }

        // ═══ TAG CLOUD ═══
        if (s.genreDistribution.size >= 4) {
            item { TagCloudCard(s.genreDistribution, p) }
        }

        // ═══ LISTENING ACTIVITY (weekly bar chart) ═══
        if (s.weeklyActivity.isNotEmpty() && s.weeklyActivity.any { it.second > 0 }) {
            item { SectionHeader("Listening Activity") { onSeeAllListeningActivity() } }
            item {
                FluentCard {
                    Spacer(Modifier.height(2.dp))
                    val maxCount = s.weeklyActivity.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                    Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        s.weeklyActivity.forEach { (day, count) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                                Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                Spacer(Modifier.height(2.dp))
                                val h = if (maxCount > 0) (count / maxCount * 60).dp else 4.dp
                                Box(Modifier.width(24.dp).height(h.coerceAtLeast(4.dp)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(p.copy(alpha = 0.7f)))
                                Spacer(Modifier.height(4.dp))
                                Text(day, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                            }
                        }
                    }
                }
            }
        }

        // ═══ RECENTLY PLAYED ═══
        if (s.recentTracks.isNotEmpty()) {
            item { SectionHeader("Recently Played") { onSeeAllRecentTracks() } }
            item {
                FluentCard {
                    val ctx = LocalContext.current
                    s.recentTracks.take(5).forEach { t ->
                        Row(Modifier.fillMaxWidth().clickable { onTrackClick(t.title, t.artist) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (t.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(t.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                            else Box(Modifier.size(36.dp).background(SonaraCardElevated, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.4f), modifier = Modifier.size(14.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.bodySmall, color = SonaraTextPrimary, maxLines = 1)
                                Text(t.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                            }
                            if (t.isNowPlaying) Box(Modifier.size(8.dp).background(SonaraSuccess, CircleShape))
                            else Text(relativeTime(t.uts), style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                    }
                }
            }
        }

        // ═══ SONARA AI ═══
        if (s.songsLearned > 0 || s.trackTitle.isNotEmpty()) {
            item {
                FluentCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp), tint = p)
                        Text("Sonara AI", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (s.genre != "Unknown") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatColumn(s.genre.replaceFirstChar { it.uppercase() }, "genre", p)
                            StatColumn(s.mood.replaceFirstChar { it.uppercase() }, "mood", p)
                            StatColumn("${(s.energy * 100).toInt()}%", "energy", p)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatColumn(s.songsLearned.toString(), "learned", p)
                        StatColumn(s.cacheSize.toString(), "cached", p)
                        StatColumn("${s.apiAccuracy}%", "accuracy", p)
                    }
                    if (s.dataSource != "None") {
                        Spacer(Modifier.height(6.dp))
                        Text("Source: ${s.dataSource}", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                }
            }
        }

        // ═══ SÜRPRİZ KEŞFET ═══
        if (s.lastFmConnected && (s.topTracks.isNotEmpty() || s.topArtists.isNotEmpty())) {
            item {
                FluentCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Discover", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Text("A random pick from your collection", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                        OutlinedButton(
                            onClick = { vm.rollSurprise() },
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.dp, p),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = p)
                        ) {
                            Text("Surprise me", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    when (s.surpriseType) {
                        "track" -> s.surpriseTrack?.let { track ->
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.2f)))
                            Spacer(Modifier.height(10.dp))
                            val ctx = LocalContext.current
                            Row(
                                Modifier.fillMaxWidth().clickable { onTrackClick(track.title, track.artist) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (track.imageUrl.isNotBlank()) {
                                    AsyncImage(model = ImageRequest.Builder(ctx).data(track.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.size(48.dp).background(SonaraCardElevated, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.4f), modifier = Modifier.size(20.dp)) }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                    Text(track.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                                }
                                Text(
                                    try { "${fmt.format(track.plays.toLong())} plays" } catch (_: Exception) { "${track.plays} plays" },
                                    style = MaterialTheme.typography.labelSmall, color = p
                                )
                            }
                        }
                        "artist" -> s.surpriseArtist?.let { a ->
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.2f)))
                            Spacer(Modifier.height(10.dp))
                            val ctx = LocalContext.current
                            Row(
                                Modifier.fillMaxWidth().clickable { onArtistClick(a.first) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (a.third.isNotBlank()) {
                                    AsyncImage(model = ImageRequest.Builder(ctx).data(a.third).crossfade(true).build(), contentDescription = a.first, modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.size(48.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) { Text(a.first.take(1), style = MaterialTheme.typography.titleMedium, color = p) }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(a.first, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                    Text(
                                        try { "${fmt.format(a.second.toLong())} plays" } catch (_: Exception) { "${a.second} plays" },
                                        style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // INSIGHT-01: Loved Tracks
        if (s.lastFmConnected) {
            item {
                FluentCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Loved Tracks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        if (s.lovedTracks.isNotEmpty()) TextButton(onClick = onSeeAllLovedTracks) { Text("See All") }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (s.lovedTracksLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    } else {
                        s.lovedTracks.take(5).forEach { t ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                    Text(t.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                Text(t.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // INSIGHT-02: Top Genres
        if (s.topGenres.isNotEmpty()) {
            item {
                FluentCard {
                    Text("Top Genres", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    val maxCount = s.topGenres.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                    s.topGenres.take(8).forEach { (name, count) ->
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { count.toFloat() / maxCount },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(0.12f)
                            )
                        }
                    }
                }
            }
        }

        // INSIGHT-03: Weekly Artist Chart
        if (s.weeklyArtists.isNotEmpty()) {
            item {
                FluentCard {
                    Text("This Week's Top Artists", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    s.weeklyArtists.take(5).forEachIndexed { i, (name, plays, _) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("#${i + 1}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
                            Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
                            Text("$plays plays", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (i < 4) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                    }
                }
            }
        }

        // INSIGHT-04: Weekly Activity Bar Chart
        if (s.dailyScrobbleChart.isNotEmpty() && s.dailyScrobbleChart.any { it.second > 0 }) {
            item {
                FluentCard {
                    Text("Weekly Activity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(16.dp))
                    val maxVal = s.dailyScrobbleChart.maxOf { it.second }.coerceAtLeast(1)
                    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        s.dailyScrobbleChart.forEach { (day, count) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val fraction = count.toFloat() / maxVal
                                Box(Modifier.width(28.dp).height(120.dp * fraction)
                                    .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                                Spacer(Modifier.height(4.dp))
                                Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$count", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // INSIGHT-05: Friends
        if (s.friends.isNotEmpty()) {
            item {
                FluentCard {
                    Text("Friends", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(s.friends.take(10)) { f ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(72.dp).clickable { onFriendClick(f) }
                            ) {
                                AsyncImage(model = f.imageUrl, contentDescription = f.name,
                                    modifier = Modifier.size(48.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop)
                                Spacer(Modifier.height(4.dp))
                                Text(f.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${f.playcount} plays", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // INSIGHT-07: Track Details
        if (s.trackListeners.isNotBlank() || s.trackPlaycount.isNotBlank()) {
            item {
                FluentCard {
                    Text("Track Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Listeners" to s.trackListeners, "Plays" to s.trackPlaycount, "Duration" to s.trackDuration)
                            .filter { it.second.isNotBlank() }
                            .forEach { (label, value) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                    }
                    if (s.trackTags.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            s.trackTags.take(6).forEach { tag ->
                                SuggestionChip(onClick = {}, label = { Text(tag, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
internal fun TagCloudCard(genres: Map<String, Int>, p: Color) {
    if (genres.isEmpty()) return
    val sorted = remember(genres) {
        genres.entries.sortedByDescending { it.value }.take(30)
    }
    val maxCount = sorted.firstOrNull()?.value?.toFloat() ?: 1f
    val minTextSp = 10f
    val maxTextSp = 38f

    FluentCard {
        Text("Genre Cloud", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
        Spacer(Modifier.height(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.minDimension / 2f
                val density = drawContext.density
                val placed = mutableListOf<Rect>()

                for ((genre, count) in sorted) {
                    val fraction = count / maxCount
                    val textSp = minTextSp + fraction * (maxTextSp - minTextSp)
                    val textPx = textSp * density.density
                    val alpha = 0.35f + 0.65f * fraction
                    val color = p.copy(alpha = alpha)
                    val textWidth = genre.length * textPx * 0.6f
                    val textHeight = textPx * 1.2f

                    var placedOk = false
                    var angle = 0f
                    var spiralR = 0f
                    val step = 0.25f
                    while (spiralR < radius * 0.85f && !placedOk) {
                        val tx = cx + spiralR * kotlin.math.cos(angle) - textWidth / 2f
                        val ty = cy + spiralR * kotlin.math.sin(angle) - textHeight / 2f
                        val candidate = Rect(tx, ty, tx + textWidth, ty + textHeight)
                        val farX = (tx + textWidth / 2f - cx)
                        val farY = (ty + textHeight / 2f - cy)
                        val inCircle = kotlin.math.sqrt(farX * farX + farY * farY) + textWidth / 2f < radius * 0.88f
                        val overlaps = placed.any { it.overlaps(candidate) }
                        if (inCircle && !overlaps) {
                            drawIntoCanvas { c ->
                                c.nativeCanvas.drawText(
                                    genre, tx, ty + textHeight * 0.85f,
                                    Paint().apply {
                                        this.color = color.toArgb()
                                        textSize = textPx
                                        isAntiAlias = true
                                        typeface = if (fraction > 0.5f) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                                    }
                                )
                            }
                            placed.add(candidate)
                            placedOk = true
                        }
                        angle += step
                        spiralR = angle * radius * 0.045f
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Based on Last.fm tag data", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
        Text("See all >", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onSeeAll() })
    }
}

internal fun relativeTime(uts: Long): String {
    if (uts <= 0) return ""
    val now = System.currentTimeMillis() / 1000
    val diff = now - uts
    return when {
        diff < 60 -> "now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        diff < 2592000 -> "${diff / 604800}w ago"
        else -> "${diff / 2592000}mo ago"
    }
}

@Composable
private fun StatColumn(value: String, label: String, p: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = p, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
    }
}

@Composable
private fun ListeningHeatmap(heatmap: Map<String, Int>, p: Color) {
    val weeksCount = 10
    val maxCount = remember(heatmap) { heatmap.values.maxOrNull() ?: 1 }
    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }
    val grid = remember(heatmap) {
        val start = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -(weeksCount * 7 - 1))
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        Array(7) { dayOfWeek ->
            IntArray(weeksCount) { week ->
                val c = start.clone() as java.util.Calendar
                c.add(java.util.Calendar.DAY_OF_YEAR, week * 7 + dayOfWeek)
                heatmap[sdf.format(c.time)] ?: 0
            }
        }
    }
    FluentCard {
        Text("Listening Heatmap", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
        Spacer(Modifier.height(8.dp))
        val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            (0..6).forEach { dayOfWeek ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(dayLabels[dayOfWeek], style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, modifier = Modifier.width(12.dp))
                    (0 until weeksCount).forEach { week ->
                        val count = grid[dayOfWeek][week]
                        val alpha = if (count == 0) 0.07f else (count.toFloat() / maxCount).coerceIn(0.2f, 1f)
                        Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(2.dp)).background(p.copy(alpha = alpha)))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            Spacer(Modifier.width(4.dp))
            listOf(0.07f, 0.3f, 0.55f, 0.78f, 1f).forEach { alpha ->
                Box(Modifier.padding(horizontal = 1.dp).size(10.dp).clip(RoundedCornerShape(2.dp)).background(p.copy(alpha = alpha)))
            }
            Spacer(Modifier.width(4.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
        }
    }
}
