package com.sonara.app.ui.screens.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Launch
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
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(artistName: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var detail by remember { mutableStateOf<DeezerImageResolver.ArtistDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    val p = MaterialTheme.colorScheme.primary

    LaunchedEffect(artistName) {
        scope.launch(Dispatchers.IO) {
            detail = DeezerImageResolver.getArtistDetail(artistName)
            loading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(artistName) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = p)
            }
        } else {
            val d = detail
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    FluentCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (d?.imageUrl?.isNotBlank() == true) {
                                AsyncImage(model = ImageRequest.Builder(ctx).data(d.imageUrl).crossfade(true).build(),
                                    contentDescription = artistName, modifier = Modifier.size(80.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop)
                            } else {
                                Surface(Modifier.size(80.dp), shape = CircleShape, color = p.copy(0.15f)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(artistName.take(2).uppercase(), style = MaterialTheme.typography.headlineMedium, color = p)
                                    }
                                }
                            }
                            Column {
                                Text(d?.name ?: artistName, style = MaterialTheme.typography.headlineMedium, color = SonaraTextPrimary)
                                if (d != null) {
                                    Text("${fmt.format(d.fans)} fans", style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary)
                                    Text("${d.albums} albums", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                                }
                            }
                        }
                    }
                }
                if (d?.topTracks?.isNotEmpty() == true) {
                    item {
                        FluentCard {
                            Text("Top Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(12.dp))
                            d.topTracks.forEachIndexed { i, track ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary,
                                        modifier = Modifier.width(24.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(track.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                        val mins = track.durationSec / 60; val secs = track.durationSec % 60
                                        Text("${mins}:${"%02d".format(secs)}", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                                    }
                                }
                                if (i < d.topTracks.lastIndex) HorizontalDivider(color = SonaraDivider.copy(0.3f))
                            }
                        }
                    }
                }
                item {
                    FluentCard {
                        Text("Open in...", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                        Spacer(Modifier.height(8.dp))
                        val links = listOf("Spotify" to "https://open.spotify.com/search/${artistName}",
                            "YouTube Music" to "https://music.youtube.com/search?q=${artistName}",
                            "Last.fm" to "https://www.last.fm/music/${artistName}")
                        links.forEach { (name, url) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(name, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                                IconButton(onClick = {
                                    ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                                }) { Icon(Icons.Rounded.Launch, "Open", tint = p) }
                            }
                        }
                    }
                }
            }
        }
    }
}
