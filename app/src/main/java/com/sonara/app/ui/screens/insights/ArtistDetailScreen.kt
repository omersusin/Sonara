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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.events.BandsintownClient
import com.sonara.app.intelligence.events.BandsintownEvent
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.lastfm.LastFmSimilarArtist
import com.sonara.app.intelligence.musicbrainz.MusicBrainzClient
import com.sonara.app.intelligence.odesli.OdesliHelper
import com.sonara.app.intelligence.theaudiodb.AudioDbAlbum
import com.sonara.app.intelligence.theaudiodb.AudioDbArtist
import com.sonara.app.intelligence.theaudiodb.TheAudioDbClient
import com.sonara.app.intelligence.artist.ArtistNameParser
import com.sonara.app.intelligence.artist.TrackTitleCleaner
import com.sonara.app.ui.components.ArtistAvatarCircle
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.components.MultiArtistAvatarRow
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    trackTitle: String = "",
    onBack: () -> Unit,
    onTrackClick: (String, String) -> Unit = { _, _ -> },
    onAlbumClick: (name: String, artist: String, plays: String, imageUrl: String) -> Unit = { _, _, _, _ -> },
    onArtistClick: (String) -> Unit = {},
    onSeeAllDiscography: (String) -> Unit = {},
    onSeeAllSimilar: (String) -> Unit = {}
) {
    val ctx = LocalContext.current
    val p = MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    val app = SonaraApp.instance

    // Merge artists from the artist field + featured artists extracted from the track title
    val allArtists = remember(artistName, trackTitle) {
        val fromArtist = ArtistNameParser.resolve(artistName)
        val fromTitle = if (trackTitle.isNotBlank()) TrackTitleCleaner.clean(trackTitle).featuredArtists else emptyList()
        (fromArtist + fromTitle).distinct()
    }

    var detail by remember { mutableStateOf<DeezerImageResolver.ArtistDetail?>(null) }
    var audioDbArtist by remember { mutableStateOf<AudioDbArtist?>(null) }
    var discography by remember { mutableStateOf<List<AudioDbAlbum>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var artistTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var userPlayCount by remember { mutableStateOf("") }
    var globalPlayCount by remember { mutableStateOf("") }
    var platformLinks by remember { mutableStateOf<List<OdesliHelper.PlatformLink>>(emptyList()) }
    var userTopTracks by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var deezerTopTracks by remember { mutableStateOf<List<DeezerImageResolver.TrackItem>>(emptyList()) }
    var artistBio by remember { mutableStateOf("") }
    var artistListeners by remember { mutableStateOf("") }
    var upcomingEvents by remember { mutableStateOf<List<BandsintownEvent>>(emptyList()) }
    var similarArtists by remember { mutableStateOf<List<LastFmSimilarArtist>>(emptyList()) }
    var mbUrls by remember { mutableStateOf<MusicBrainzClient.MbArtistUrls?>(null) }
    val discographyArtUrls = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(artistName) {
        withContext(Dispatchers.IO) {
            detail = DeezerImageResolver.getArtistDetail(artistName)
            deezerTopTracks = detail?.topTracks ?: emptyList<DeezerImageResolver.TrackItem>()

            try { audioDbArtist = TheAudioDbClient.searchArtist(artistName) } catch (_: Exception) {}
            try { mbUrls = MusicBrainzClient.searchArtistUrls(artistName) } catch (_: Exception) {}
            try { upcomingEvents = BandsintownClient.getUpcomingEvents(artistName) } catch (_: Exception) {}
            try {
                discography = TheAudioDbClient.getDiscography(artistName)
                    .sortedByDescending { it.intYearReleased ?: 0 }
            } catch (_: Exception) {}

            val apiKey = app.lastFmAuth.getActiveApiKey()
            val username = app.lastFmAuth.getConnectionInfo().username
            if (apiKey.isNotBlank()) {
                try {
                    val info = LastFmClient.api.getArtistInfo(artistName, apiKey, username)
                    info.artist?.let { a ->
                        artistListeners = a.stats?.listeners ?: ""
                        globalPlayCount = a.stats?.playcount ?: ""
                        val raw = a.bio?.content?.takeIf { it.isNotBlank() } ?: a.bio?.summary ?: ""
                        artistBio = raw.substringBefore("<a href=\"https://www.last.fm").trim()
                    }
                } catch (_: Exception) {}
                try {
                    artistTags = LastFmClient.api.getArtistTags(artistName, apiKey)
                        .toptags?.tag?.take(10)?.map { it.name }?.filter { it.isNotBlank() } ?: emptyList()
                } catch (_: Exception) {}
                try {
                    val sim = LastFmClient.api.getSimilarArtists(artistName, apiKey, 8)
                    val raw = sim.similarartists?.artist ?: emptyList()
                    val enriched = raw.map { a ->
                        val img = a.imageUrl?.takeIf { !it.contains("2a96cbd8b46e") }
                            ?: DeezerImageResolver.getArtistImageWithFallback(a.name) ?: ""
                        a.copy(image = if (img.isNotBlank()) listOf(
                            com.sonara.app.intelligence.lastfm.LastFmImage(img, "large")
                        ) else a.image)
                    }
                    similarArtists = enriched
                } catch (_: Exception) {}
                if (username.isNotBlank()) {
                    try {
                        val top = LastFmClient.api.getUserTopArtists(username, apiKey, "overall", 50)
                        userPlayCount = top.topartists?.artist
                            ?.find { it.name.equals(artistName, ignoreCase = true) }?.playcount ?: ""
                    } catch (_: Exception) {}
                }
            }

            if (username.isNotBlank() && apiKey.isNotBlank()) {
                try {
                    val topTr = LastFmClient.api.getUserTopTracks(username, apiKey, "overall", 200)
                    userTopTracks = topTr.toptracks?.track
                        ?.filter { it.artist?.name.equals(artistName, ignoreCase = true) }
                        ?.take(15)
                        ?.map { t ->
                            val img = t.imageUrl?.takeIf { !it.contains("2a96cbd8b46e") } ?: ""
                            Triple(t.name, t.playcount, img)
                        } ?: emptyList()
                } catch (_: Exception) {}
            }

            try { platformLinks = OdesliHelper.getArtistLinks(artistName) } catch (_: Exception) {}
            loading = false
        }
    }

    // Sequential art resolution for the inline discography preview.
    // Key = strAlbum (unique per discography). We skip getAlbumById because
    // discography.php often returns the same idAlbum for every entry in a
    // catalog, which causes all albums to resolve to the same (wrong) artwork.
    // searchAlbum queries by name and returns the correct per-album result.
    LaunchedEffect(discography) {
        if (discography.isEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            for (album in discography) {
                val key = album.strAlbum
                if (discographyArtUrls.containsKey(key)) continue
                // Tier 1: inline art embedded in the discography response (often absent)
                val inline = album.strThumbHQ ?: album.strThumb
                if (!inline.isNullOrBlank()) {
                    discographyArtUrls[key] = inline
                    continue
                }
                // Tier 2: name-based search — unique query per album, avoids stale IDs
                try {
                    val found = TheAudioDbClient.searchAlbum(artistName, album.strAlbum)
                    val url = found?.strThumbHQ ?: found?.strThumb
                    if (!url.isNullOrBlank()) discographyArtUrls[key] = url
                } catch (_: Exception) {}
                delay(350L)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artistName) },
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
            val d = detail
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Artist header ─────────────────────────────────────────────────
                item {
                    FluentCard {
                        if (allArtists.size > 1) {
                            // ── Multi-artist header ──────────────────────────────────────
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                MultiArtistAvatarRow(
                                    artists = allArtists,
                                    onArtistClick = onArtistClick
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    allArtists.forEach { name ->
                                        Text(
                                            name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SonaraTextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable { onArtistClick(name) }
                                        )
                                    }
                                    val adb = audioDbArtist
                                    val meta = listOfNotNull(
                                        adb?.strCountry?.takeIf { it.isNotBlank() },
                                        adb?.intFormedYear?.let { "est. $it" }
                                    ).joinToString(" · ")
                                    if (meta.isNotBlank()) {
                                        Text(meta, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                            }
                        } else {
                            // ── Single-artist header (unchanged) ─────────────────────────
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val imageUrl = audioDbArtist?.strThumb?.takeIf { it.isNotBlank() }
                                    ?: d?.imageUrl?.takeIf { it.isNotBlank() }
                                if (!imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(ctx).data(imageUrl).crossfade(true).build(),
                                        contentDescription = artistName,
                                        modifier = Modifier.size(96.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(Modifier.size(96.dp), shape = CircleShape, color = p.copy(0.15f)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(artistName.take(2).uppercase(), style = MaterialTheme.typography.headlineMedium, color = p)
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(d?.name ?: artistName, style = MaterialTheme.typography.headlineSmall, color = SonaraTextPrimary, fontWeight = FontWeight.Bold)
                                    val adb = audioDbArtist
                                    val meta = listOfNotNull(
                                        adb?.strCountry?.takeIf { it.isNotBlank() },
                                        adb?.intFormedYear?.let { "est. $it" }
                                    ).joinToString(" · ")
                                    if (meta.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(meta, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                            }
                        }

                        // Stats row
                        val hasStats = userPlayCount.isNotBlank() || artistListeners.isNotBlank() || globalPlayCount.isNotBlank() || (d?.fans ?: 0) > 0
                        if (hasStats) {
                            Spacer(Modifier.height(14.dp))
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.2f)))
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                if (userPlayCount.isNotBlank()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            try { fmt.format(userPlayCount.toLong()) } catch (_: Exception) { userPlayCount },
                                            style = MaterialTheme.typography.titleMedium, color = p, fontWeight = FontWeight.Bold
                                        )
                                        Text("your plays", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                                if (artistListeners.isNotBlank()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            try { fmt.format(artistListeners.toLong()) } catch (_: Exception) { artistListeners },
                                            style = MaterialTheme.typography.titleMedium, color = SonaraTextSecondary, fontWeight = FontWeight.Bold
                                        )
                                        Text("listeners", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                                if (globalPlayCount.isNotBlank()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            try { fmt.format(globalPlayCount.toLong()) } catch (_: Exception) { globalPlayCount },
                                            style = MaterialTheme.typography.titleMedium, color = SonaraTextSecondary, fontWeight = FontWeight.Bold
                                        )
                                        Text("scrobbles", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                                if ((d?.fans ?: 0) > 0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            try { fmt.format(d!!.fans.toLong()) } catch (_: Exception) { "${d?.fans}" },
                                            style = MaterialTheme.typography.titleMedium, color = SonaraTextSecondary, fontWeight = FontWeight.Bold
                                        )
                                        Text("fans", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                            }
                        }

                        // Social links — TheAudioDB primary, MusicBrainz fallback
                        val adb = audioDbArtist
                        val mb = mbUrls
                        val socialLinks = buildList {
                            val twitterUrl = adb?.strTwitter?.takeIf { it.isNotBlank() && it != "1" }
                                ?.let { buildSocialUrl("twitter.com", it) }
                                ?: mb?.twitter
                            twitterUrl?.let { add("twitter" to it) }

                            val fbUrl = adb?.strFacebook?.takeIf { it.isNotBlank() && it != "1" }
                                ?.let { buildSocialUrl("facebook.com", it) }
                                ?: mb?.facebook
                            fbUrl?.let { add("facebook" to it) }

                            val igUrl = adb?.strInstagram?.takeIf { it.isNotBlank() && it != "1" }
                                ?.let { buildSocialUrl("instagram.com", it) }
                                ?: mb?.instagram
                            igUrl?.let { add("instagram" to it) }

                            val webUrl = adb?.strWebsite?.takeIf { it.isNotBlank() && it != "1" }
                                ?.let { if (it.startsWith("http")) it else "https://$it" }
                                ?: mb?.website
                            webUrl?.let { add("website" to it) }

                            mb?.youtube?.let { add("youtube" to it) }
                            mb?.soundcloud?.let { add("soundcloud" to it) }
                        }
                        if (socialLinks.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                socialLinks.forEach { (key, url) ->
                                    val displayName = key.replaceFirstChar { it.uppercase() }
                                    AssistChip(
                                        onClick = { OdesliHelper.openLink(ctx, OdesliHelper.PlatformLink(name = displayName, url = url, key = key)) },
                                        label = { Text(displayName, style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = {
                                            val iconRes = platformIconRes(key)
                                            if (iconRes != null) Icon(painterResource(iconRes), null, Modifier.size(14.dp), tint = p)
                                            else Icon(Icons.Rounded.Launch, null, Modifier.size(14.dp))
                                        },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = p.copy(0.1f), labelColor = p),
                                        border = null
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Bio ───────────────────────────────────────────────────────────
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
                                style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary
                            )
                            if (isLong) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (bioExpanded) "Show less" else "Show more",
                                    style = MaterialTheme.typography.labelMedium, color = p,
                                    modifier = Modifier.clickable { bioExpanded = !bioExpanded }
                                )
                            }
                        }
                    }
                }

                // ── Genres & Tags ─────────────────────────────────────────────────
                if (artistTags.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Genres & Tags", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                artistTags.forEach { tag ->
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

                // ── Top Tracks (Deezer) ───────────────────────────────────────────
                if (deezerTopTracks.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Top Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(12.dp))
                            deezerTopTracks.forEachIndexed { i, track ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { onTrackClick(track.title, artistName) }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(24.dp))
                                    if (!track.albumArt.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(ctx).data(track.albumArt).crossfade(true).build(),
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
                                        Text(track.title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                        if (track.durationSec > 0) {
                                            Text("${track.durationSec / 60}:${"%02d".format(track.durationSec % 60)}", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                        }
                                    }
                                }
                                if (i < deezerTopTracks.lastIndex) HorizontalDivider(color = SonaraDivider.copy(0.12f))
                            }
                        }
                    }
                }

                // ── Your Top Tracks ───────────────────────────────────────────────
                if (userTopTracks.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Your Top Tracks", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(10.dp))
                            userTopTracks.forEachIndexed { i, (title, plays, imgUrl) ->
                                var resolvedImg by remember(title) { mutableStateOf(imgUrl) }
                                LaunchedEffect(title) {
                                    if (resolvedImg.isBlank() || resolvedImg.contains("2a96cbd8b46e")) {
                                        val r = withContext(Dispatchers.IO) { DeezerImageResolver.getTrackImageWithFallback(title, artistName) ?: "" }
                                        if (r.isNotBlank()) resolvedImg = r
                                    }
                                }
                                Row(
                                    Modifier.fillMaxWidth().clickable { onTrackClick(title, artistName) }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(24.dp))
                                    if (resolvedImg.isNotBlank() && !resolvedImg.contains("2a96cbd8b46e")) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(ctx).data(resolvedImg).crossfade(true).build(),
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(Modifier.size(40.dp).background(SonaraCardElevated, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.MusicNote, null, Modifier.size(16.dp), tint = p.copy(0.4f))
                                        }
                                    }
                                    Text(title, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            try { fmt.format(plays.toLong()) } catch (_: Exception) { plays },
                                            style = MaterialTheme.typography.labelMedium, color = p
                                        )
                                        Text("plays", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    }
                                }
                                if (i < userTopTracks.lastIndex) HorizontalDivider(color = SonaraDivider.copy(0.12f))
                            }
                        }
                    }
                }

                // ── Discography ───────────────────────────────────────────────────
                if (discography.isNotEmpty()) {
                    item {
                        FluentCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Discography", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                                Text("See all >", style = MaterialTheme.typography.labelMedium, color = p, modifier = Modifier.clickable { onSeeAllDiscography(artistName) })
                            }
                            Spacer(Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(discography) { album ->
                                    val artUrl = discographyArtUrls[album.strAlbum] ?: ""
                                    Column(
                                        modifier = Modifier.width(96.dp)
                                            .clickable { onAlbumClick(album.strAlbum, artistName, "", artUrl) },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (artUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(ctx).data(artUrl).crossfade(true).build(),
                                                contentDescription = album.strAlbum,
                                                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(Modifier.size(96.dp).background(SonaraCardElevated, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Album, null, Modifier.size(30.dp), tint = p.copy(0.4f))
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(album.strAlbum, style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary, maxLines = 2, textAlign = TextAlign.Center)
                                        if (album.intYearReleased != null) {
                                            Text("${album.intYearReleased}", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Similar Artists ───────────────────────────────────────────────
                if (similarArtists.isNotEmpty()) {
                    item {
                        FluentCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Similar Artists", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                                Text("See all >", style = MaterialTheme.typography.labelMedium, color = p, modifier = Modifier.clickable { onSeeAllSimilar(artistName) })
                            }
                            Spacer(Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(similarArtists) { artist ->
                                    SimilarArtistItem(artist) { onArtistClick(artist.name) }
                                }
                            }
                        }
                    }
                }

                // ── Upcoming Shows ────────────────────────────────────────────────
                if (upcomingEvents.isNotEmpty()) {
                    item {
                        FluentCard {
                            Text("Upcoming Shows", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Spacer(Modifier.height(10.dp))
                            upcomingEvents.take(5).forEachIndexed { i, event ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(event.url)))
                                        }
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
                                if (i < upcomingEvents.take(5).lastIndex) HorizontalDivider(color = SonaraDivider.copy(0.12f))
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

@Composable
private fun SimilarArtistItem(artist: LastFmSimilarArtist, onClick: () -> Unit) {
    val parsedNames = remember(artist.name) { ArtistNameParser.resolve(artist.name) }
    Column(
        modifier = Modifier.width(72.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (parsedNames.size > 1) {
            MultiArtistAvatarRow(
                artists = parsedNames,
                modifier = Modifier.height(44.dp),
                onArtistClick = {}
            )
        } else {
            ArtistAvatarCircle(
                artistName = parsedNames.firstOrNull() ?: artist.name,
                size = 64.dp,
                onClick = {}
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelSmall,
            color = SonaraTextPrimary,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
        val matchPct = artist.match.toFloatOrNull() ?: 0f
        if (matchPct > 0f) {
            Text(
                text = "${(matchPct * 100).toInt()}% match",
                style = MaterialTheme.typography.labelSmall,
                color = SonaraTextTertiary,
                fontSize = 10.sp
            )
        }
    }
}

private fun buildSocialUrl(domain: String, raw: String): String {
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
    val cleaned = raw
        .removePrefix("www.$domain/")
        .removePrefix("$domain/")
        .removePrefix("@")
        .trim('/')
    return "https://www.$domain/$cleaned"
}

fun platformIconRes(key: String): Int? = when (key.lowercase()) {
    "spotify" -> com.sonara.app.R.drawable.ic_spotify_24
    "applemusic", "apple_music", "applemusic" -> com.sonara.app.R.drawable.ic_apple_24
    "youtubemusic", "youtube_music", "youtubemusic" -> com.sonara.app.R.drawable.ic_youtube_music_24
    "youtube" -> com.sonara.app.R.drawable.ic_youtube_24
    "deezer" -> com.sonara.app.R.drawable.ic_deezer_24
    "amazonmusic", "amazon_music", "amazonmusic" -> com.sonara.app.R.drawable.ic_amazon_24
    "soundcloud" -> com.sonara.app.R.drawable.ic_soundcloud_24
    "tidal" -> com.sonara.app.R.drawable.ic_tidal_24
    "pandora" -> com.sonara.app.R.drawable.ic_pandora_24
    "napster" -> com.sonara.app.R.drawable.ic_napster_24
    "audiomack" -> com.sonara.app.R.drawable.ic_audiomack_24
    "anghami" -> com.sonara.app.R.drawable.ic_anghami_24
    "boomplay" -> com.sonara.app.R.drawable.ic_boomplay_24
    "yandex", "yandexmusic" -> com.sonara.app.R.drawable.ic_yandex_music_24
    "twitter", "x" -> com.sonara.app.R.drawable.ic_twitter_24
    "facebook" -> com.sonara.app.R.drawable.ic_facebook_24
    "instagram" -> com.sonara.app.R.drawable.ic_instagram_24
    "website" -> com.sonara.app.R.drawable.ic_website_24
    else -> null
}
