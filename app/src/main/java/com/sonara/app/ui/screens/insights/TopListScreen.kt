package com.sonara.app.ui.screens.insights

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

private enum class ViewMode(val label: String, val cols: Int) {
    LIST("List", 1),
    GRID2("2 Grid", 2),
    GRID3("3 Grid", 3),
    GRID4("4 Grid", 4),
    GRID5("5 Grid", 5)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopArtistsListScreen(onBack: () -> Unit, onArtistClick: (String) -> Unit) {
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    val ctx = LocalContext.current
    var period by rememberSaveable { mutableStateOf("overall") }
    var artists by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var viewMode by rememberSaveable { mutableStateOf(ViewMode.LIST) }
    var showViewMenu by remember { mutableStateOf(false) }

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
        topBar = {
            TopAppBar(
                title = { Text("Top Artists") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                actions = {
                    Box {
                        IconButton(onClick = { showViewMenu = true }) {
                            Icon(if (viewMode == ViewMode.LIST) Icons.Rounded.ViewList else Icons.Rounded.GridView, "View mode", tint = p)
                        }
                        DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }) {
                            ViewMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label, color = if (viewMode == mode) p else SonaraTextPrimary) },
                                    onClick = { viewMode = mode; showViewMenu = false }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabPeriodRow(period, p) { period = it }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p) }
            } else if (viewMode == ViewMode.LIST) {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    itemsIndexed(artists) { i, a ->
                        Row(Modifier.fillMaxWidth().clickable { onArtistClick(a.first) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${i+1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(28.dp))
                            if (a.third.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(a.third).crossfade(true).build(), contentDescription = a.first, modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            else Box(Modifier.size(50.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) { Text(a.first.take(1), style = MaterialTheme.typography.titleSmall, color = p) }
                            Column(Modifier.weight(1f)) {
                                Text(a.first, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                Text(try { "${fmt.format(a.second.toLong())} plays" } catch (_: Exception) { "${a.second} plays" }, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                            }
                        }
                        if (i < artists.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 90.dp).height(0.5.dp).background(SonaraDivider.copy(0.12f)))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(viewMode.cols),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(artists) { i, a ->
                        Column(Modifier.clickable { onArtistClick(a.first) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                if (a.third.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(a.third).crossfade(true).build(), contentDescription = a.first, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape), contentScale = ContentScale.Crop)
                                else Box(Modifier.fillMaxWidth().aspectRatio(1f).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) { Text(a.first.take(1), style = if (viewMode.cols <= 2) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium, color = p) }
                                Box(Modifier.align(Alignment.TopEnd).size(18.dp).background(p, CircleShape), contentAlignment = Alignment.Center) {
                                    Text("${i+1}", style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(a.first, style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            if (viewMode.cols <= 3) Text(try { fmt.format(a.second.toLong()) } catch (_: Exception) { a.second }, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, textAlign = TextAlign.Center)
                        }
                    }
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
    var period by rememberSaveable { mutableStateOf("overall") }
    var tracks by remember { mutableStateOf<List<TopTrackItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var viewMode by rememberSaveable { mutableStateOf(ViewMode.LIST) }
    var showViewMenu by remember { mutableStateOf(false) }

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
        topBar = {
            TopAppBar(
                title = { Text("Top Tracks") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                actions = {
                    Box {
                        IconButton(onClick = { showViewMenu = true }) {
                            Icon(if (viewMode == ViewMode.LIST) Icons.Rounded.ViewList else Icons.Rounded.GridView, "View mode", tint = p)
                        }
                        DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }) {
                            ViewMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label, color = if (viewMode == mode) p else SonaraTextPrimary) },
                                    onClick = { viewMode = mode; showViewMenu = false }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabPeriodRow(period, p) { period = it }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p) }
            } else if (viewMode == ViewMode.LIST) {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    itemsIndexed(tracks) { i, track ->
                        Row(Modifier.fillMaxWidth().clickable { onTrackClick(track.title, track.artist) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${i+1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(28.dp))
                            if (track.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(track.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            else Box(Modifier.size(46.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.4f), modifier = Modifier.size(18.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                Text(track.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                            }
                            Text(try { fmt.format(track.plays.toLong()) } catch (_: Exception) { track.plays }, style = MaterialTheme.typography.labelMedium, color = p)
                        }
                        if (i < tracks.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 84.dp).height(0.5.dp).background(SonaraDivider.copy(0.12f)))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(viewMode.cols),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tracks) { i, track ->
                        Column(Modifier.clickable { onTrackClick(track.title, track.artist) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                if (track.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(track.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(if (viewMode.cols <= 2) 12.dp else 8.dp)), contentScale = ContentScale.Crop)
                                else Box(Modifier.fillMaxWidth().aspectRatio(1f).background(SonaraCardElevated, RoundedCornerShape(if (viewMode.cols <= 2) 12.dp else 8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.3f)) }
                                Box(Modifier.align(Alignment.TopStart).padding(4.dp).size(18.dp).background(p.copy(0.85f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                    Text("${i+1}", style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.75f), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(track.title, style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            if (viewMode.cols <= 3) {
                                Text(track.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                Text(try { "${fmt.format(track.plays.toLong())} plays" } catch (_: Exception) { track.plays }, style = MaterialTheme.typography.labelSmall, color = p, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAlbumsListScreen(
    onBack: () -> Unit,
    onAlbumClick: (name: String, artist: String, plays: String, imageUrl: String) -> Unit = { _, _, _, _ -> }
) {
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    val ctx = LocalContext.current
    var period by rememberSaveable { mutableStateOf("overall") }
    var albums by remember { mutableStateOf<List<TopAlbumItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var viewMode by rememberSaveable { mutableStateOf(ViewMode.LIST) }
    var showViewMenu by remember { mutableStateOf(false) }

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
        topBar = {
            TopAppBar(
                title = { Text("Top Albums") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                actions = {
                    Box {
                        IconButton(onClick = { showViewMenu = true }) {
                            Icon(if (viewMode == ViewMode.LIST) Icons.Rounded.ViewList else Icons.Rounded.GridView, "View mode", tint = p)
                        }
                        DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }) {
                            ViewMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label, color = if (viewMode == mode) p else SonaraTextPrimary) },
                                    onClick = { viewMode = mode; showViewMenu = false }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabPeriodRow(period, p) { period = it }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p) }
            } else if (viewMode == ViewMode.LIST) {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    itemsIndexed(albums) { i, album ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { onAlbumClick(album.name, album.artist, album.plays, album.imageUrl) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("${i+1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(28.dp))
                            if (album.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(album.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            else Box(Modifier.size(50.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Album, null, tint = p.copy(0.4f), modifier = Modifier.size(20.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(album.name, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                Text(album.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                            }
                            Text(try { "${fmt.format(album.plays.toLong())}" } catch (_: Exception) { album.plays }, style = MaterialTheme.typography.labelMedium, color = p)
                        }
                        if (i < albums.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 90.dp).height(0.5.dp).background(SonaraDivider.copy(0.12f)))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(viewMode.cols),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(albums) { i, album ->
                        Column(
                            Modifier.clickable { onAlbumClick(album.name, album.artist, album.plays, album.imageUrl) }.padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box {
                                if (album.imageUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(album.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(if (viewMode.cols <= 2) 12.dp else 8.dp)), contentScale = ContentScale.Crop)
                                else Box(Modifier.fillMaxWidth().aspectRatio(1f).background(SonaraCardElevated, RoundedCornerShape(if (viewMode.cols <= 2) 12.dp else 8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Album, null, tint = p.copy(0.3f)) }
                                Box(Modifier.align(Alignment.TopStart).padding(4.dp).size(18.dp).background(p.copy(0.85f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                    Text("${i+1}", style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.75f), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(album.name, style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            if (viewMode.cols <= 3) {
                                Text(album.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                Text(try { "${fmt.format(album.plays.toLong())} plays" } catch (_: Exception) { album.plays }, style = MaterialTheme.typography.labelSmall, color = p, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** stats.fm-style tab bar period selector with optional custom date range */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabPeriodRow(
    current: String,
    p: Color,
    onSelect: (String) -> Unit,
    onCustomRange: ((Long, Long) -> Unit)? = null
) {
    val periods = listOf("7day" to "1W", "1month" to "1M", "3month" to "3M", "6month" to "6M", "12month" to "1Y", "overall" to "All")
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangeState = rememberDateRangePickerState()

    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            periods.forEach { (id, label) ->
                val sel = current == id
                Box(
                    Modifier.weight(1f).clickable { onSelect(id) }.padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (sel) p else SonaraTextTertiary,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Box(Modifier.fillMaxWidth().height(2.dp).background(if (sel) p else Color.Transparent, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                    }
                }
            }
        }
        if (onCustomRange != null) {
            IconButton(onClick = { showDatePicker = true }, modifier = Modifier.size(40.dp).align(Alignment.CenterVertically)) {
                Icon(Icons.Rounded.DateRange, "Custom date range", tint = if (current == "custom") p else SonaraTextTertiary, modifier = Modifier.size(18.dp))
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.2f)))

    if (showDatePicker && onCustomRange != null) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val from = dateRangeState.selectedStartDateMillis
                    val to = dateRangeState.selectedEndDateMillis
                    if (from != null && to != null) {
                        onCustomRange(from / 1000, to / 1000)
                    }
                    showDatePicker = false
                }) { Text("OK", color = p) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DateRangePicker(state = dateRangeState, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PeriodRow(current: String, p: Color, onSelect: (String) -> Unit) = TabPeriodRow(current, p, onSelect)
