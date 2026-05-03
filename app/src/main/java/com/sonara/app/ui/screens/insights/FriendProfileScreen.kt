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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.lastfm.LastFmTopArtist
import com.sonara.app.intelligence.lastfm.LastFmTopTrack
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendProfileScreen(
    username: String,
    realname: String,
    playcount: String,
    avatarUrl: String,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onTrackClick: (String, String) -> Unit = { _, _ -> }
) {
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())

    var topArtists by remember { mutableStateOf<List<LastFmTopArtist>>(emptyList()) }
    var topTracks by remember { mutableStateOf<List<LastFmTopTrack>>(emptyList()) }
    var totalScrobbles by remember { mutableStateOf(playcount) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(username) {
        loading = true
        val apiKey = app.lastFmAuth.getActiveApiKey()
        if (apiKey.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val info = LastFmClient.api.getUserInfo(username, apiKey)
                    totalScrobbles = info.user?.playcount ?: playcount
                } catch (_: Exception) {}
                try {
                    val artists = LastFmClient.api.getUserTopArtists(username, apiKey, "overall", 10)
                    topArtists = artists.topartists?.artist ?: emptyList()
                } catch (_: Exception) {}
                try {
                    val tracks = LastFmClient.api.getUserTopTracks(username, apiKey, "overall", 10)
                    topTracks = tracks.toptracks?.track ?: emptyList()
                } catch (_: Exception) {}
            }
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (realname.isNotBlank()) realname else username) },
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
                // Profile header
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (avatarUrl.isNotBlank()) {
                            AsyncImage(model = ImageRequest.Builder(ctx).data(avatarUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.size(80.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, null, tint = p, modifier = Modifier.size(36.dp))
                            }
                        }
                        Column {
                            Text(if (realname.isNotBlank()) realname else username, style = MaterialTheme.typography.headlineSmall, color = SonaraTextPrimary, fontWeight = FontWeight.Bold)
                            if (realname.isNotBlank()) Text("@$username", style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary)
                            Text(try { "${fmt.format(totalScrobbles.toLong())} scrobbles" } catch (_: Exception) { "$totalScrobbles scrobbles" }, style = MaterialTheme.typography.labelMedium, color = p)
                        }
                    }
                }

                // Top Artists
                if (topArtists.isNotEmpty()) {
                    item { Text("Top Artists", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary) }
                    itemsIndexed(topArtists) { i, a ->
                        var imgUrl by remember(a.name) { mutableStateOf(a.imageUrl?.takeIf { !it.contains("2a96cbd8b46e") } ?: "") }
                        LaunchedEffect(a.name) {
                            if (imgUrl.isBlank()) {
                                val r = withContext(Dispatchers.IO) { DeezerImageResolver.getArtistImageWithFallback(a.name) ?: "" }
                                if (r.isNotBlank()) imgUrl = r
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable { onArtistClick(a.name) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(24.dp))
                            if (imgUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(imgUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            else Box(Modifier.size(42.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) { Text(a.name.take(1), style = MaterialTheme.typography.labelMedium, color = p) }
                            Text(a.name, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(try { "${fmt.format(a.playcount.toLong())} plays" } catch (_: Exception) { "${a.playcount} plays" }, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                    }
                }

                // Top Tracks
                if (topTracks.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Top Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                    }
                    itemsIndexed(topTracks) { i, t ->
                        var imgUrl by remember(t.name, t.artist?.name) { mutableStateOf(t.imageUrl?.takeIf { !it.contains("2a96cbd8b46e") } ?: "") }
                        LaunchedEffect(t.name, t.artist?.name) {
                            if (imgUrl.isBlank()) {
                                val r = withContext(Dispatchers.IO) { DeezerImageResolver.getTrackImageWithFallback(t.name, t.artist?.name ?: "") ?: "" }
                                if (r.isNotBlank()) imgUrl = r
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable { onTrackClick(t.name, t.artist?.name ?: "") }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(24.dp))
                            if (imgUrl.isNotBlank()) AsyncImage(model = ImageRequest.Builder(ctx).data(imgUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                            else Box(Modifier.size(42.dp).background(SonaraCardElevated, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.4f), modifier = Modifier.size(16.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(t.name, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                Text(t.artist?.name ?: "", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                            }
                            Text(try { fmt.format(t.playcount.toLong()) } catch (_: Exception) { t.playcount }, style = MaterialTheme.typography.labelMedium, color = p)
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
