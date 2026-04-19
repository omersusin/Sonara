package com.sonara.app.ui.screens.insights

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material3.AssistChip
import androidx.compose.ui.res.painterResource
import com.sonara.app.R
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sonara.app.intelligence.events.BandsintownClient
import com.sonara.app.intelligence.events.BandsintownEvent
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.odesli.OdesliHelper
import com.sonara.app.intelligence.theaudiodb.AudioDbArtist
import com.sonara.app.intelligence.theaudiodb.TheAudioDbClient
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArtistDetailScreen(artistName: String, onBack: () -> Unit, onTrackClick: (String, String) -> Unit = { _, _ -> }) {
    val ctx = LocalContext.current
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    val app = SonaraApp.instance

    var detail by remember { mutableStateOf<DeezerImageResolver.ArtistDetail?>(null) }
    var audioDbArtist by remember { mutableStateOf<AudioDbArtist?>(null) }
    var loading by remember { mutableStateOf(true) }
    var artistTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var userPlayCount by remember { mutableStateOf("") }
    var platformLinks by remember { mutableStateOf<List<OdesliHelper.PlatformLink>>(emptyList()) }
    var userTopTracks by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var artistBio by remember { mutableStateOf("") }
    var artistListeners by remember { mutableStateOf("") }
    var upcomingEvents by remember { mutableStateOf<List<BandsintownEvent>>(emptyList()) }

    LaunchedEffect(artistName) {
        withContext(Dispatchers.IO) {
            detail = DeezerImageResolver.getArtistDetail(artistName)
            try { audioDbArtist = TheAudioDbClient.searchArtist(artistName) } catch (_: Exception) {}
            try { upcomingEvents = BandsintownClient.getUpcomingEvents(artistName) } catch (_: Exception) {}
            val apiKey = app.lastFmAuth.getActiveApiKey()
            val username = app.lastFmAuth.getConnectionInfo().username
            if (apiKey.isNotBlank()) {
                try {
                    val info = LastFmClient.api.getArtistInfo(artistName, apiKey, username)
                    info.artist?.let { a ->
                        artistListeners = a.stats?.listeners ?: ""
                        val raw = (a.bio?.content?.takeIf { it.isNotBlank() } ?: a.bio?.summary ?: "")
                        // Strip Last.fm footer link
                        artistBio = raw.substringBefore("<a href=\"https://www.last.fm").trim()
                    }
                } catch (_: Exception) {}
                try { artistTags = LastFmClient.api.getArtistTags(artistName, apiKey).toptags?.tag?.take(10)?.map { it.name }?.filter { it.isNotBlank() } ?: emptyList() } catch (_: Exception) {}
                if (username.isNotBlank()) {
                    try {
                        val top = LastFmClient.api.getUserTopArtists(username, apiKey, "overall", 50)
                        userPlayCount = top.topartists?.artist?.find { it.name.equals(artistName, ignoreCase = true) }?.playcount ?: ""
                    } catch (_: Exception) {}
                }
            }
            // User's top tracks by this artist
            val username2 = app.lastFmAuth.getConnectionInfo().username
            val apiKey2 = app.lastFmAuth.getActiveApiKey()
            if (username2.isNotBlank() && apiKey2.isNotBlank()) {
                try {
                    val topTr = LastFmClient.api.getUserTopTracks(username2, apiKey2, "overall", 100)
                    val byArtist = topTr.toptracks?.track?.filter {
                        it.artist?.name.equals(artistName, ignoreCase = true)
                    }?.take(10)?.map { it.name to it.playcount } ?: emptyList()
                    userTopTracks = byArtist
                } catch (_: Exception) {}
            }
            try { platformLinks = OdesliHelper.getArtistLinks(artistName) } catch (_: Exception) {}
            loading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(artistName) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (loading) { Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p) } }
        else {
            val d = detail
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    FluentCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val imageUrl = audioDbArtist?.strThumb?.takeIf { it.isNotBlank() } ?: d?.imageUrl
                            if (imageUrl?.isNotBlank() == true) {
                                AsyncImage(model = ImageRequest.Builder(ctx).data(imageUrl).crossfade(true).build(), contentDescription = artistName,
                                    modifier = Modifier.size(100.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Surface(Modifier.size(100.dp), shape = CircleShape, color = p.copy(0.15f)) {
                                    Box(contentAlignment = Alignment.Center) { Text(artistName.take(2).uppercase(), style = MaterialTheme.typography.headlineMedium, color = p) }
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(d?.name ?: artistName, style = MaterialTheme.typography.headlineSmall, color = SonaraTextPrimary)
                                if (d != null) {
                                    Text("${fmt.format(d.fans.toLong())} fans", style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary)
                                    Text("${d.albums} albums", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                                }
                                val adb = audioDbArtist
                                if (adb != null) {
                                    if (adb.strCountry?.isNotBlank() == true || adb.intFormedYear?.isNotBlank() == true) {
                                        Spacer(Modifier.height(4.dp))
                                        val meta = listOfNotNull(adb.strCountry, adb.intFormedYear?.let { "est. $it" }).joinToString(" · ")
                                        Text(meta, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                            }
                        }
                        // Social media links row
                        val adb = audioDbArtist
                        if (adb != null) {
                            val socialLinks = buildList {
                                adb.strTwitter?.let { add("Twitter" to it) }
                                adb.strFacebook?.let { add("Facebook" to it) }
                                adb.strInstagram?.let { add("Instagram" to it) }
                                adb.strWebsite?.let { add("Website" to it) }
                            }
                            if (socialLinks.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    socialLinks.forEach { (name, url) ->
                                        val fullUrl = if (url.startsWith("http")) url else "https://$url"
                                        AssistChip(
                                            onClick = { OdesliHelper.openLink(ctx, OdesliHelper.PlatformLink(name = name, url = fullUrl, key = name.lowercase())) },
                                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                            leadingIcon = {
                                                val iconRes = platformIconRes(name.lowercase())
                                                if (iconRes != null) {
                                                    Icon(painterResource(iconRes), null, Modifier.size(14.dp), tint = p)
                                                } else {
                                                    Icon(Icons.Rounded.Launch, null, Modifier.size(14.dp))
                                                }
                                            },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = p.copy(0.1f), labelColor = p),
                                            border = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // Bio card with expand/collapse
                val bioToShow = artistBio.ifBlank { audioDbArtist?.strBiographyEN ?: "" }
                if (bioToShow.isNotBlank()) {
                    item {
                        var bioExpanded by remember { mutableStateOf(false) }
                        val isLong = bioToShow.length > 300
                        FluentCard {
                            Text("About", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (!bioExpanded && isLong) bioToShow.take(300) + "…" else bioToShow,
                                style = MaterialTheme.typography.bodySmall,
                                color = SonaraTextSecondary
                            )
                            if (isLong) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (bioExpanded) "Show less" else "Show more",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = p,
                                    modifier = Modifier.clickable { bioExpanded = !bioExpanded }
                                )
                            }
                            if (artistListeners.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(try { "${fmt.format(artistListeners.toLong())} listeners on Last.fm" } catch (_: Exception) { "$artistListeners listeners" },
                                    style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                            }
                        }
                    }
                }
                if (userPlayCount.isNotBlank()) {
                    item {
                        FluentCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(try { fmt.format(userPlayCount.toLong()) } catch (_: Exception) { userPlayCount },
                                        style = MaterialTheme.typography.headlineMedium, color = p)
                                    Text("your plays", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                }
                            }
                        }
                    }
                }
                if (artistTags.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Genres & Tags", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary); Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                artistTags.forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = p.copy(0.1f), labelColor = p), border = null) }
                            }
                        }
                    }
                }
                if (d?.topTracks?.isNotEmpty() == true) {
                    item {
                        FluentCard {
                            Text("Top Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary); Spacer(Modifier.height(12.dp))
                            d.topTracks.forEachIndexed { i, track ->
                                Row(Modifier.fillMaxWidth().clickable { onTrackClick(track.title, artistName) }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(24.dp))
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
                if (userTopTracks.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Your Top Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary); Spacer(Modifier.height(10.dp))
                            userTopTracks.forEachIndexed { i, (title, plays) ->
                                Row(Modifier.fillMaxWidth().clickable { onTrackClick(title, artistName) }.padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(24.dp))
                                    Text(title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
                                    Text(try { fmt.format(plays.toLong()) } catch (_: Exception) { plays }, style = MaterialTheme.typography.labelMedium, color = p)
                                }
                                if (i < userTopTracks.lastIndex) HorizontalDivider(color = SonaraDivider.copy(0.2f))
                            }
                        }
                    }
                }
                if (platformLinks.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Listen on", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary); Spacer(Modifier.height(8.dp))
                            platformLinks.forEach { link ->
                                Row(Modifier.fillMaxWidth().clickable { OdesliHelper.openLink(ctx, link) }.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val iconRes = platformIconRes(link.key)
                                    if (iconRes != null) {
                                        Icon(painterResource(iconRes), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    } else {
                                        Icon(Icons.Rounded.Launch, null, Modifier.size(18.dp), tint = p)
                                    }
                                    Text(link.name, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                                }
                            }
                        }
                    }
                }
                if (upcomingEvents.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Upcoming Shows", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(10.dp))
                            upcomingEvents.take(5).forEachIndexed { i, event ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(event.url))) }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(event.venue.name, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                        Text(event.venue.displayLocation, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary, maxLines = 1)
                                    }
                                    Text(event.displayDate, style = MaterialTheme.typography.labelMedium, color = p)
                                }
                                if (i < upcomingEvents.take(5).lastIndex) HorizontalDivider(color = SonaraDivider.copy(0.3f))
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

fun platformIconRes(key: String): Int? = when (key) {
    "spotify" -> R.drawable.ic_spotify_24
    "appleMusic" -> R.drawable.ic_apple_24
    "youtubeMusic" -> R.drawable.ic_youtube_music_24
    "youtube" -> R.drawable.ic_youtube_24
    "deezer" -> R.drawable.ic_deezer_24
    "amazonMusic" -> R.drawable.ic_amazon_24
    "soundcloud" -> R.drawable.ic_soundcloud_24
    "tidal" -> R.drawable.ic_tidal_24
    "pandora" -> R.drawable.ic_pandora_24
    "napster" -> R.drawable.ic_napster_24
    "audiomack" -> R.drawable.ic_audiomack_24
    "anghami" -> R.drawable.ic_anghami_24
    "boomplay" -> R.drawable.ic_boomplay_24
    "yandex" -> R.drawable.ic_yandex_music_24
    "twitter" -> R.drawable.ic_twitter_24
    "facebook" -> R.drawable.ic_facebook_24
    "instagram" -> R.drawable.ic_instagram_24
    "website" -> R.drawable.ic_website_24
    else -> null
}
