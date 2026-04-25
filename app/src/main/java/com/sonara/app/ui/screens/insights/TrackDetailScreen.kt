package com.sonara.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.lastfm.LastFmSimilarTrack
import com.sonara.app.intelligence.odesli.OdesliHelper
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrackDetailScreen(
    trackTitle: String,
    trackArtist: String,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onTrackClick: (String, String) -> Unit = { _, _ -> }
) {
    val ctx = LocalContext.current
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    val app = SonaraApp.instance

    var artworkUrl by remember { mutableStateOf("") }
    var listeners by remember { mutableStateOf("") }
    var globalPlaycount by remember { mutableStateOf("") }
    var userPlaycount by remember { mutableStateOf("") }
    var durationMs by remember { mutableStateOf(0L) }
    var albumName by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var platformLinks by remember { mutableStateOf<List<OdesliHelper.PlatformLink>>(emptyList()) }
    var similarTracks by remember { mutableStateOf<List<LastFmSimilarTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val genre by SonaraNotificationListener.currentGenre.collectAsState()
    val mood by SonaraNotificationListener.currentMood.collectAsState()
    val energy by SonaraNotificationListener.currentEnergy.collectAsState()
    val np by SonaraNotificationListener.nowPlaying.collectAsState()
    val isCurrentTrack = np.title == trackTitle && np.artist == trackArtist

    LaunchedEffect(trackTitle, trackArtist) {
        withContext(Dispatchers.IO) {
            // Resolve artwork — try Last.fm first (accurate album attribution), fall back to Deezer
            artworkUrl = DeezerImageResolver.resolveTrackArtwork(trackTitle, trackArtist, app.lastFmAuth.getActiveApiKey())
                ?.replace("cover_medium", "cover_big")?.replace("100x100bb", "600x600bb") ?: ""

            val apiKey = app.lastFmAuth.getActiveApiKey()
            if (apiKey.isNotBlank()) {
                try {
                    val info = LastFmClient.api.getTrackInfo(trackTitle, trackArtist, apiKey)
                    info.track?.let { t ->
                        listeners = t.listeners ?: ""
                        globalPlaycount = t.playcount ?: ""
                        durationMs = t.duration?.toLongOrNull() ?: 0L
                        albumName = t.album?.title ?: ""
                        if (artworkUrl.isBlank()) {
                            artworkUrl = t.album?.imageUrl ?: ""
                        }
                        val trackTags = t.toptags?.tag?.map { it.name }?.filter { it.isNotBlank() } ?: emptyList()
                        if (trackTags.isNotEmpty()) tags = trackTags.take(8)
                    }
                } catch (_: Exception) {}

                if (tags.isEmpty()) {
                    try {
                        tags = LastFmClient.api.getArtistTags(trackArtist, apiKey)
                            .toptags?.tag?.take(8)?.map { it.name }?.filter { it.isNotBlank() } ?: emptyList()
                    } catch (_: Exception) {}
                }

                try {
                    val sim = LastFmClient.api.getSimilarTracks(trackTitle, trackArtist, apiKey, 8)
                    val raw = sim.similartracks?.track ?: emptyList()
                    // Enrich images
                    val enriched = raw.map { t ->
                        val img = t.imageUrl?.takeIf { !it.contains("2a96cbd8b46e") }
                            ?: DeezerImageResolver.getTrackImageWithFallback(t.name, t.artist?.name ?: "") ?: ""
                        t.copy(image = if (img.isNotBlank()) listOf(
                            com.sonara.app.intelligence.lastfm.LastFmImage(img, "large")
                        ) else t.image)
                    }
                    similarTracks = enriched
                } catch (_: Exception) {}

                val username = app.lastFmAuth.getConnectionInfo().username
                if (username.isNotBlank()) {
                    try {
                        val topTracks = LastFmClient.api.getUserTopTracks(username, apiKey, "overall", 200)
                        userPlaycount = topTracks.toptracks?.track?.find {
                            it.name.equals(trackTitle, ignoreCase = true) &&
                                it.artist?.name.equals(trackArtist, ignoreCase = true)
                        }?.playcount ?: ""
                    } catch (_: Exception) {}
                }
            }

            try { platformLinks = OdesliHelper.getLinks(trackTitle, trackArtist) } catch (_: Exception) {}
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trackTitle, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p) }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Album art ─────────────────────────────────────────────────────
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (artworkUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx).data(artworkUrl).crossfade(true).build(),
                                contentDescription = trackTitle,
                                modifier = Modifier.size(260.dp).clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(Modifier.size(260.dp), shape = RoundedCornerShape(24.dp), color = SonaraCardElevated) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, Modifier.size(72.dp), tint = p.copy(0.3f)) }
                            }
                        }
                    }
                }

                // ── Title / artist / album ─────────────────────────────────────────
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(trackTitle, style = MaterialTheme.typography.headlineSmall, color = SonaraTextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            trackArtist,
                            style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.Underline),
                            color = p,
                            modifier = Modifier.clickable { onArtistClick(trackArtist) }
                        )
                        if (albumName.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(albumName, style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary)
                        }
                    }
                }

                // ── Stats card ────────────────────────────────────────────────────
                val hasStats = userPlaycount.isNotBlank() || globalPlaycount.isNotBlank() || listeners.isNotBlank() || durationMs > 0
                if (hasStats) {
                    item {
                        FluentCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                if (userPlaycount.isNotBlank()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            try { fmt.format(userPlaycount.toLong()) } catch (_: Exception) { userPlaycount },
                                            style = MaterialTheme.typography.titleLarge, color = p, fontWeight = FontWeight.Bold
                                        )
                                        Text("your plays", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                                if (listeners.isNotBlank()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            try { fmt.format(listeners.toLong()) } catch (_: Exception) { listeners },
                                            style = MaterialTheme.typography.titleLarge, color = SonaraTextSecondary, fontWeight = FontWeight.Bold
                                        )
                                        Text("listeners", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                                if (globalPlaycount.isNotBlank()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            try { fmt.format(globalPlaycount.toLong()) } catch (_: Exception) { globalPlaycount },
                                            style = MaterialTheme.typography.titleLarge, color = SonaraTextSecondary, fontWeight = FontWeight.Bold
                                        )
                                        Text("scrobbles", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                                if (durationMs > 0) {
                                    val mins = durationMs / 60000
                                    val secs = (durationMs % 60000) / 1000
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "$mins:${"%02d".format(secs)}",
                                            style = MaterialTheme.typography.titleLarge, color = SonaraTextSecondary, fontWeight = FontWeight.Bold
                                        )
                                        Text("duration", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Sonara AI (if currently playing) ─────────────────────────────
                if (isCurrentTrack && genre.isNotBlank()) {
                    item {
                        FluentCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp), tint = p)
                                Text("Sonara AI", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(genre.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = p, fontWeight = FontWeight.Bold)
                                    Text("genre", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(mood.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = p, fontWeight = FontWeight.Bold)
                                    Text("mood", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${(energy * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = p, fontWeight = FontWeight.Bold)
                                    Text("energy", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                }
                            }
                        }
                    }
                }

                // ── Tags ──────────────────────────────────────────────────────────
                if (tags.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Tags", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                tags.forEach { tag ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = p.copy(0.1f), labelColor = p),
                                        border = null
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Similar Tracks ────────────────────────────────────────────────
                if (similarTracks.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Similar Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(10.dp))
                            similarTracks.forEachIndexed { i, track ->
                                val imgUrl = track.imageUrl ?: ""
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { onTrackClick(track.name, track.artist?.name ?: trackArtist) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (imgUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(ctx).data(imgUrl).crossfade(true).build(),
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(Modifier.size(40.dp).background(SonaraCardElevated, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.MusicNote, null, Modifier.size(16.dp), tint = p.copy(0.4f))
                                        }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(track.name, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                        Text(track.artist?.name ?: "", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, maxLines = 1)
                                    }
                                }
                                if (i < similarTracks.lastIndex) HorizontalDivider(color = SonaraDivider.copy(0.12f))
                            }
                        }
                    }
                }

                // ── Listen on ─────────────────────────────────────────────────────
                if (platformLinks.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Listen on", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(8.dp))
                            platformLinks.forEach { link ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { OdesliHelper.openLink(ctx, link) }.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val iconRes = platformIconRes(link.key)
                                    if (iconRes != null) Icon(painterResource(iconRes), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    else Icon(Icons.Rounded.Launch, null, Modifier.size(18.dp), tint = p)
                                    Text(link.name, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
