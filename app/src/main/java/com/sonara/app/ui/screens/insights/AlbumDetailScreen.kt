package com.sonara.app.ui.screens.insights

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

data class AlbumTrackItem(
    val rank: Int,
    val title: String,
    val durationSec: Int,
    val userPlays: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumName: String,
    artistName: String,
    albumPlays: String,
    albumImageUrl: String,
    onBack: () -> Unit,
    onTrackClick: (String, String) -> Unit = { _, _ -> }
) {
    val ctx = LocalContext.current
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())

    var imageUrl by remember { mutableStateOf(albumImageUrl) }
    var tracks by remember { mutableStateOf<List<AlbumTrackItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var listeners by remember { mutableStateOf("") }
    var totalPlays by remember { mutableStateOf(albumPlays) }

    LaunchedEffect(albumName, artistName) {
        withContext(Dispatchers.IO) {
            val apiKey = app.lastFmAuth.getActiveApiKey()
            // Resolve better image if needed
            if (imageUrl.isBlank() || imageUrl.contains("2a96cbd8b46e")) {
                imageUrl = DeezerImageResolver.getTrackImageWithFallback(albumName, artistName) ?: imageUrl
            }
            if (apiKey.isNotBlank()) {
                try {
                    val info = LastFmClient.api.getAlbumInfo(artistName, albumName, apiKey)
                    info.album?.let { album ->
                        listeners = album.listeners
                        if (album.playcount.isNotBlank()) totalPlays = album.playcount
                        if (album.imageUrl?.isNotBlank() == true && !album.imageUrl!!.contains("2a96cbd8b46e")) {
                            imageUrl = album.imageUrl!!
                        }

                        // Build track list — cross-reference with user's top tracks for play counts
                        val username = app.lastFmAuth.getConnectionInfo().username
                        val userPlayMap = mutableMapOf<String, String>()
                        if (username.isNotBlank()) {
                            try {
                                val topTracks = LastFmClient.api.getUserTopTracks(username, apiKey, "overall", 500)
                                topTracks.toptracks?.track?.filter { it.artist?.name.equals(artistName, ignoreCase = true) }
                                    ?.forEach { t -> userPlayMap[t.name.lowercase()] = t.playcount }
                            } catch (_: Exception) {}
                        }

                        tracks = album.tracks?.track?.mapIndexed { idx, t ->
                            AlbumTrackItem(
                                rank = t.attr?.rank?.toIntOrNull() ?: (idx + 1),
                                title = t.name,
                                durationSec = t.duration.toIntOrNull() ?: 0,
                                userPlays = userPlayMap[t.name.lowercase()] ?: ""
                            )
                        }?.sortedBy { it.rank } ?: emptyList()
                    }
                } catch (_: Exception) {}
            }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(albumName, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = p)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Album header
                item {
                    FluentCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(ctx).data(imageUrl).crossfade(true).build(),
                                    contentDescription = albumName,
                                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(Modifier.size(100.dp), shape = RoundedCornerShape(12.dp), color = p.copy(0.1f)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Album, null, Modifier.size(40.dp), tint = p.copy(0.5f))
                                    }
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(albumName, style = MaterialTheme.typography.titleLarge, color = SonaraTextPrimary, maxLines = 2)
                                Spacer(Modifier.height(2.dp))
                                Text(artistName, style = MaterialTheme.typography.bodyMedium, color = p, maxLines = 1)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (totalPlays.isNotBlank()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(try { fmt.format(totalPlays.toLong()) } catch (_: Exception) { totalPlays },
                                                style = MaterialTheme.typography.titleMedium, color = p, fontWeight = FontWeight.Bold)
                                            Text("your plays", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                        }
                                    }
                                    if (listeners.isNotBlank()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(try { fmt.format(listeners.toLong()) } catch (_: Exception) { listeners },
                                                style = MaterialTheme.typography.titleMedium, color = SonaraTextSecondary, fontWeight = FontWeight.Bold)
                                            Text("listeners", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tracks
                if (tracks.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(10.dp))
                            tracks.forEachIndexed { i, track ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { onTrackClick(track.title, artistName) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "${track.rank}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (i < 3) p else SonaraTextTertiary,
                                        modifier = Modifier.width(26.dp)
                                    )
                                    Box(
                                        Modifier.size(36.dp).background(SonaraCardElevated, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.MusicNote, null, Modifier.size(16.dp), tint = p.copy(0.4f))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(track.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                        if (track.durationSec > 0) {
                                            val m = track.durationSec / 60; val s = track.durationSec % 60
                                            Text("$m:${"%02d".format(s)}", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                        }
                                    }
                                    if (track.userPlays.isNotBlank()) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                try { fmt.format(track.userPlays.toLong()) } catch (_: Exception) { track.userPlays },
                                                style = MaterialTheme.typography.labelMedium, color = p
                                            )
                                            Text("plays", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                        }
                                    }
                                }
                                if (i < tracks.lastIndex) HorizontalDivider(color = SonaraDivider.copy(0.12f))
                            }
                        }
                    }
                } else {
                    item {
                        FluentCard {
                            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                Text("No track info available", style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
