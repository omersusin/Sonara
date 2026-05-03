package com.sonara.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.lastfm.ChartArtistItem
import com.sonara.app.intelligence.lastfm.ChartTrackItem
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

private enum class ChartsTab { ARTISTS, TRACKS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalChartsScreen(
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onTrackClick: (String, String) -> Unit = { _, _ -> }
) {
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())

    var tab by remember { mutableStateOf(ChartsTab.ARTISTS) }
    var artists by remember { mutableStateOf<List<ChartArtistItem>>(emptyList()) }
    var tracks by remember { mutableStateOf<List<ChartTrackItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        val apiKey = app.lastFmAuth.getActiveApiKey()
        if (apiKey.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val artistsResp = LastFmClient.api.getChartTopArtists(apiKey, 50)
                    artists = artistsResp.artists?.artist ?: emptyList()
                } catch (_: Exception) {}
                try {
                    val tracksResp = LastFmClient.api.getChartTopTracks(apiKey, 50)
                    tracks = tracksResp.tracks?.track ?: emptyList()
                } catch (_: Exception) {}
            }
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Charts") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab.ordinal, containerColor = MaterialTheme.colorScheme.surface) {
                ChartsTab.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }, maxLines = 1, style = MaterialTheme.typography.labelMedium) })
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p) }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    when (tab) {
                        ChartsTab.ARTISTS -> itemsIndexed(artists) { i, a ->
                            var imgUrl by remember(a.name) { mutableStateOf(a.imageUrl ?: "") }
                            LaunchedEffect(a.name) {
                                if (imgUrl.isBlank() && !a.name.isNullOrBlank()) {
                                    val r = withContext(Dispatchers.IO) { DeezerImageResolver.getArtistImageWithFallback(a.name) ?: "" }
                                    if (r.isNotBlank()) imgUrl = r
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth().clickable { onArtistClick(a.name ?: "") }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(28.dp))
                                if (imgUrl.isNotBlank()) {
                                    AsyncImage(model = ImageRequest.Builder(ctx).data(imgUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(46.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.size(46.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Person, null, tint = p.copy(0.4f), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(a.name ?: "", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                    if (!a.listeners.isNullOrBlank()) Text(try { "${fmt.format(a.listeners.toLong())} listeners" } catch (_: Exception) { "${a.listeners} listeners" }, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                }
                            }
                            if (i < artists.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 86.dp).height(0.5.dp).background(SonaraDivider.copy(0.1f)))
                        }
                        ChartsTab.TRACKS -> itemsIndexed(tracks) { i, t ->
                            var imgUrl by remember(t.name, t.artist?.name) { mutableStateOf(t.imageUrl ?: "") }
                            LaunchedEffect(t.name, t.artist?.name) {
                                if (imgUrl.isBlank() && !t.name.isNullOrBlank()) {
                                    val r = withContext(Dispatchers.IO) { DeezerImageResolver.getTrackImageWithFallback(t.name, t.artist?.name ?: "") ?: "" }
                                    if (r.isNotBlank()) imgUrl = r
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth().clickable { onTrackClick(t.name ?: "", t.artist?.name ?: "") }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(28.dp))
                                if (imgUrl.isNotBlank()) {
                                    AsyncImage(model = ImageRequest.Builder(ctx).data(imgUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.size(46.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.4f), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(t.name ?: "", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                    Text(t.artist?.name ?: "", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                                }
                                if (!t.listeners.isNullOrBlank()) Text(try { fmt.format(t.listeners.toLong()) } catch (_: Exception) { t.listeners ?: "" }, style = MaterialTheme.typography.labelMedium, color = p)
                            }
                            if (i < tracks.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 86.dp).height(0.5.dp).background(SonaraDivider.copy(0.1f)))
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
