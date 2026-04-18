package com.sonara.app.ui.screens.insights

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.SonaraDivider
import com.sonara.app.ui.theme.SonaraSuccess
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentTracksScreen(
    onBack: () -> Unit,
    onTrackClick: (String, String) -> Unit = { _, _ -> }
) {
    val activity = LocalContext.current as ComponentActivity
    val vm: InsightsViewModel = viewModel(viewModelStoreOwner = activity)
    val s by vm.uiState.collectAsState()
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Tracks shown in this screen — may grow with load-more
    var extraTracks by remember { mutableStateOf<List<RecentTrackItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }

    // On open: refresh to get freshest data
    LaunchedEffect(Unit) {
        vm.refreshAllRecentTracks()
    }

    val allTracks = s.recentTracks + extraTracks

    fun loadNextPage() {
        loading = true
        val nextPage = currentPage + 1
        scope.launch {
            val newTracks = withContext(Dispatchers.IO) {
                try {
                    val apiKey = app.lastFmAuth.getActiveApiKey()
                    val username = app.lastFmAuth.getConnectionInfo().username
                    if (apiKey.isBlank() || username.isBlank()) return@withContext emptyList()
                    val resp = LastFmClient.api.getRecentTracks(username, apiKey, 200, nextPage)
                    resp.recenttracks?.track
                        ?.filter { it.date != null }
                        ?.map { t ->
                            RecentTrackItem(
                                title = t.name, artist = t.artist?.text ?: "",
                                album = t.album?.text ?: "",
                                imageUrl = t.imageUrl?.takeIf { !it.contains("2a96cbd8b46e") }
                                    ?: DeezerImageResolver.getTrackImageWithFallback(t.name, t.artist?.text ?: "") ?: "",
                                isNowPlaying = false,
                                date = t.date?.text ?: "",
                                uts = t.date?.uts?.toLongOrNull() ?: 0L
                            )
                        } ?: emptyList<RecentTrackItem>()
                } catch (_: Exception) { emptyList() }
            }
            if (newTracks.isEmpty()) hasMore = false
            else { extraTracks = extraTracks + newTracks; currentPage = nextPage }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently Played (${allTracks.size})") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            itemsIndexed(allTracks) { i, t ->
                Row(
                    Modifier.fillMaxWidth().clickable { onTrackClick(t.title, t.artist) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(28.dp))
                    if (t.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx).data(t.imageUrl).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.size(46.dp).background(SonaraCardElevated, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.4f), modifier = Modifier.size(18.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                        Text(t.artist, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                        if (t.album.isNotBlank()) Text(t.album, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary.copy(alpha = 0.6f), maxLines = 1)
                    }
                    if (t.isNowPlaying) {
                        Box(Modifier.size(8.dp).background(SonaraSuccess, CircleShape))
                    } else {
                        Text(relativeTime(t.uts), style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                }
                if (i < allTracks.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.1f)))
            }

            if (hasMore) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        if (loading) {
                            CircularProgressIndicator(color = p, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Load more", style = MaterialTheme.typography.labelMedium, color = p, modifier = Modifier.clickable { loadNextPage() })
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
