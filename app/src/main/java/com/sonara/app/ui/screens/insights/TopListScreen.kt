/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sonara.app.ui.screens.insights

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopArtistsListScreen(onBack: () -> Unit, onArtistClick: (String) -> Unit) {
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    val ctx = LocalContext.current
    var period by remember { mutableStateOf("overall") }
    var artists by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(period) {
        loading = true
        withContext(Dispatchers.IO) {
            val apiKey = app.lastFmAuth.getActiveApiKey()
            val username = app.lastFmAuth.getConnectionInfo().username
            if (apiKey.isNotBlank() && username.isNotBlank()) {
                try {
                    val resp = LastFmClient.api.getUserTopArtists(username, apiKey, period, 50)
                    val list = resp.topartists?.artist?.map { Triple(it.name, it.playcount, it.imageUrl ?: "") } ?: emptyList()
                    val enriched = list.map { (n, pl, img) -> Triple(n, pl, if (img.isNotBlank() && !img.contains("2a96cbd8b46e")) img else DeezerImageResolver.getArtistImageWithFallback(n) ?: "") }
                    artists = enriched
                } catch (_: Exception) {}
            }
            loading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Top Artists") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { PeriodRow(period, p) { period = it } }
            if (loading) { item { Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p) } } }
            else {
                itemsIndexed(artists) { i, a ->
                    Row(Modifier.fillMaxWidth().clickable { onArtistClick(a.first) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${i+1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(28.dp))
                        if (a.third.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(a.third).crossfade(true).build(), contentDescription = a.first, modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        else Box(Modifier.size(48.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) { Text(a.first.take(1), style = MaterialTheme.typography.titleSmall, color = p) }
                        Column(Modifier.weight(1f)) {
                            Text(a.first, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                            Text(try { "${fmt.format(a.second.toLong())} plays" } catch (_: Exception) { "${a.second} plays" }, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                    }
                    if (i < artists.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.12f)))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopTracksListScreen(onBack: () -> Unit, onTrackClick: (String, String) -> Unit) {
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    val ctx = LocalContext.current
    var period by remember { mutableStateOf("overall") }
    var tracks by remember { mutableStateOf<List<TopTrackItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(period) {
        loading = true
        withContext(Dispatchers.IO) {
            val apiKey = app.lastFmAuth.getActiveApiKey()
            val username = app.lastFmAuth.getConnectionInfo().username
            if (apiKey.isNotBlank() && username.isNotBlank()) {
                try {
                    val resp = LastFmClient.api.getUserTopTracks(username, apiKey, period, 50)
                    val list = resp.toptracks?.track?.map { TopTrackItem(it.name, it.artist?.name ?: "", it.playcount, it.imageUrl ?: "") } ?: emptyList()
                    val enriched = list.map { t -> t.copy(imageUrl = if (t.imageUrl.isNotBlank()) t.imageUrl else DeezerImageResolver.getTrackImageWithFallback(t.title, t.artist) ?: "") }
                    tracks = enriched
                } catch (_: Exception) {}
            }
            loading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Top Tracks") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { PeriodRow(period, p) { period = it } }
            if (loading) { item { Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p) } } }
            else {
                itemsIndexed(tracks) { i, track ->
                    Row(Modifier.fillMaxWidth().clickable { onTrackClick(track.title, track.artist) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${i+1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(28.dp))
                        if (track.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(track.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        else Box(Modifier.size(44.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.4f), modifier = Modifier.size(16.dp)) }
                        Column(Modifier.weight(1f)) { Text(track.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1); Text(track.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1) }
                        Text(try { fmt.format(track.plays.toLong()) } catch (_: Exception) { track.plays }, style = MaterialTheme.typography.labelMedium, color = p)
                    }
                    if (i < tracks.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.12f)))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAlbumsListScreen(onBack: () -> Unit) {
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    val ctx = LocalContext.current
    var period by remember { mutableStateOf("overall") }
    var albums by remember { mutableStateOf<List<TopAlbumItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(period) {
        loading = true
        withContext(Dispatchers.IO) {
            val apiKey = app.lastFmAuth.getActiveApiKey()
            val username = app.lastFmAuth.getConnectionInfo().username
            if (apiKey.isNotBlank() && username.isNotBlank()) {
                try {
                    val resp = LastFmClient.api.getUserTopAlbums(username, apiKey, period, 50)
                    val list = resp.topalbums?.album?.map { TopAlbumItem(it.name, it.artist?.name ?: "", it.playcount, it.imageUrl ?: "") } ?: emptyList()
                    val enriched = list.map { a -> a.copy(imageUrl = if (a.imageUrl.isNotBlank() && !a.imageUrl.contains("2a96cbd8b46e")) a.imageUrl else DeezerImageResolver.getTrackImageWithFallback(a.name, a.artist) ?: "") }
                    albums = enriched
                } catch (_: Exception) {}
            }
            loading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Top Albums") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { PeriodRow(period, p) { period = it } }
            if (loading) { item { Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p) } } }
            else {
                itemsIndexed(albums) { i, album ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${i+1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(28.dp))
                        if (album.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(album.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        else Box(Modifier.size(48.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Album, null, tint = p.copy(0.4f), modifier = Modifier.size(18.dp)) }
                        Column(Modifier.weight(1f)) { Text(album.name, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1); Text(album.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1) }
                        Text(try { "${fmt.format(album.plays.toLong())}" } catch (_: Exception) { album.plays }, style = MaterialTheme.typography.labelMedium, color = p)
                    }
                    if (i < albums.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.12f)))
                }
            }
        }
    }
}

@Composable
fun PeriodRow(current: String, p: Color, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("7day" to "1W", "1month" to "1M", "3month" to "3M", "6month" to "6M", "12month" to "1Y", "overall" to "All").forEach { (id, label) ->
            val sel = current == id
            OutlinedButton(onClick = { onSelect(id) }, shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (sel) p else SonaraDivider.copy(0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (sel) p.copy(0.15f) else Color.Transparent, contentColor = if (sel) p else SonaraTextTertiary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) { Text(label, style = MaterialTheme.typography.labelLarge) }
        }
    }
}
