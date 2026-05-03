package com.sonara.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
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
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SearchTab { TRACKS, ARTISTS, ALBUMS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchInsightsScreen(
    onBack: () -> Unit,
    onTrackClick: (String, String) -> Unit = { _, _ -> },
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String, String) -> Unit = { _, _ -> }
) {
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(SearchTab.TRACKS) }
    var loading by remember { mutableStateOf(false) }

    var trackResults by remember { mutableStateOf<List<com.sonara.app.intelligence.lastfm.SearchTrackItem>>(emptyList()) }
    var artistResults by remember { mutableStateOf<List<com.sonara.app.intelligence.lastfm.SearchArtistItem>>(emptyList()) }
    var albumResults by remember { mutableStateOf<List<com.sonara.app.intelligence.lastfm.SearchAlbumItem>>(emptyList()) }

    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(query, tab) {
        searchJob?.cancel()
        if (query.length < 2) {
            trackResults = emptyList(); artistResults = emptyList(); albumResults = emptyList()
            return@LaunchedEffect
        }
        searchJob = scope.launch {
            delay(400)
            loading = true
            val apiKey = app.lastFmAuth.getActiveApiKey()
            if (apiKey.isBlank()) { loading = false; return@launch }
            withContext(Dispatchers.IO) {
                try {
                    when (tab) {
                        SearchTab.TRACKS -> {
                            val resp = LastFmClient.api.searchTrack(query, apiKey, 20)
                            trackResults = resp.results?.trackmatches?.track ?: emptyList()
                        }
                        SearchTab.ARTISTS -> {
                            val resp = LastFmClient.api.searchArtist(query, apiKey, 20)
                            artistResults = resp.results?.artistmatches?.artist ?: emptyList()
                        }
                        SearchTab.ALBUMS -> {
                            val resp = LastFmClient.api.searchAlbum(query, apiKey, 20)
                            albumResults = resp.results?.albummatches?.album ?: emptyList()
                        }
                    }
                } catch (_: Exception) {}
            }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search Last.fm…", color = SonaraTextTertiary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p,
                            unfocusedBorderColor = SonaraDivider.copy(0.3f),
                            focusedContainerColor = SonaraCard,
                            unfocusedContainerColor = SonaraCard,
                            focusedTextColor = SonaraTextPrimary,
                            unfocusedTextColor = SonaraTextPrimary,
                            cursorColor = p
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab.ordinal, containerColor = MaterialTheme.colorScheme.surface) {
                SearchTab.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }, maxLines = 1, style = MaterialTheme.typography.labelMedium) })
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = p)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    when (tab) {
                        SearchTab.TRACKS -> items(trackResults) { t ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onTrackClick(t.name ?: "", t.artist ?: "") }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val imgUrl = t.imageUrl
                                if (!imgUrl.isNullOrBlank()) {
                                    AsyncImage(model = ImageRequest.Builder(ctx).data(imgUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.size(46.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.4f), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(t.name ?: "", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                    Text(t.artist ?: "", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                                }
                                if (!t.listeners.isNullOrBlank()) Text("${t.listeners} listeners", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                            }
                        }
                        SearchTab.ARTISTS -> items(artistResults) { a ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onArtistClick(a.name ?: "") }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val imgUrl = a.imageUrl
                                if (!imgUrl.isNullOrBlank()) {
                                    AsyncImage(model = ImageRequest.Builder(ctx).data(imgUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(46.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.size(46.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Person, null, tint = p.copy(0.4f), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(a.name ?: "", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                }
                                if (!a.listeners.isNullOrBlank()) Text("${a.listeners} listeners", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                            }
                        }
                        SearchTab.ALBUMS -> items(albumResults) { al ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onAlbumClick(al.name ?: "", al.artist ?: "") }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val imgUrl = al.imageUrl
                                if (!imgUrl.isNullOrBlank()) {
                                    AsyncImage(model = ImageRequest.Builder(ctx).data(imgUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.size(46.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Album, null, tint = p.copy(0.4f), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(al.name ?: "", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                    Text(al.artist ?: "", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
