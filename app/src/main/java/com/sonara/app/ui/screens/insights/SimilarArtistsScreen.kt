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
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.lastfm.LastFmImage
import com.sonara.app.intelligence.lastfm.LastFmSimilarArtist
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimilarArtistsScreen(
    artistName: String,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {}
) {
    val ctx = LocalContext.current
    val p = MaterialTheme.colorScheme.primary
    val app = SonaraApp.instance
    var artists by remember { mutableStateOf<List<LastFmSimilarArtist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(artistName) {
        withContext(Dispatchers.IO) {
            try {
                val apiKey = app.lastFmAuth.getActiveApiKey()
                if (apiKey.isNotBlank()) {
                    val resp = LastFmClient.api.getSimilarArtists(artistName, apiKey, 20)
                    val raw = resp.similarartists?.artist ?: emptyList()
                    artists = raw.map { a ->
                        val img = a.imageUrl?.takeIf { !it.contains("2a96cbd8b46e") }
                            ?: DeezerImageResolver.getArtistImageWithFallback(a.name) ?: ""
                        a.copy(image = if (img.isNotBlank()) listOf(LastFmImage(img, "large")) else a.image)
                    }
                }
            } catch (_: Exception) {}
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Similar to $artistName") },
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
        } else if (artists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No similar artists found", style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(artists) { i, artist ->
                    val imgUrl = artist.imageUrl ?: ""
                    Row(
                        Modifier.fillMaxWidth().clickable { onArtistClick(artist.name) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(26.dp))
                        if (imgUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx).data(imgUrl).crossfade(true).build(),
                                contentDescription = artist.name,
                                modifier = Modifier.size(52.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(Modifier.size(52.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, null, Modifier.size(24.dp), tint = p.copy(0.4f))
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(artist.name, style = MaterialTheme.typography.bodyLarge, color = SonaraTextPrimary)
                            val matchPct = (artist.match.toFloatOrNull() ?: 0f) * 100
                            if (matchPct > 0) {
                                Text("${matchPct.toInt()}% match", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                            }
                        }
                        // Match bar
                        val match = (artist.match.toFloatOrNull() ?: 0f).coerceIn(0f, 1f)
                        if (match > 0) {
                            Box(Modifier.width(60.dp).height(4.dp).background(SonaraCardElevated, RoundedCornerShape(2.dp))) {
                                Box(Modifier.fillMaxWidth(match).height(4.dp).background(p.copy(0.7f), RoundedCornerShape(2.dp)))
                            }
                        }
                    }
                    if (i < artists.lastIndex) {
                        HorizontalDivider(color = SonaraDivider.copy(0.08f))
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
