package com.sonara.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.theaudiodb.AudioDbAlbum
import com.sonara.app.intelligence.theaudiodb.TheAudioDbClient
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDiscographyScreen(
    artistName: String,
    onBack: () -> Unit,
    onAlbumClick: (name: String, artist: String, plays: String, imageUrl: String) -> Unit = { _, _, _, _ -> }
) {
    val ctx = LocalContext.current
    val p = MaterialTheme.colorScheme.primary
    val app = SonaraApp.instance
    var albums by remember { mutableStateOf<List<AudioDbAlbum>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(artistName) {
        withContext(Dispatchers.IO) {
            try {
                albums = TheAudioDbClient.getDiscography(artistName).sortedByDescending { it.intYearReleased ?: 0 }
            } catch (_: Exception) {}
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discography") },
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
        } else if (albums.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No albums found", style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(albums) { album ->
                    var artUrl by remember(album.idAlbum) {
                        mutableStateOf(album.strThumbHQ ?: album.strThumb ?: "")
                    }

                    // Lazy fallback: TheAudioDB searchAlbum → Last.fm
                    LaunchedEffect(album.idAlbum) {
                        if (artUrl.isBlank()) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val full = TheAudioDbClient.searchAlbum(artistName, album.strAlbum)
                                    val url = full?.strThumbHQ ?: full?.strThumb
                                    if (!url.isNullOrBlank()) { artUrl = url; return@withContext }
                                } catch (_: Exception) {}
                                try {
                                    val apiKey = app.lastFmAuth.getActiveApiKey()
                                    if (apiKey.isNotBlank()) {
                                        val info = LastFmClient.api.getAlbumInfo(artistName, album.strAlbum, apiKey)
                                        val url = info.album?.imageUrl
                                        if (!url.isNullOrBlank() && !url.contains("2a96cbd8b46e")) artUrl = url
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlbumClick(album.strAlbum, artistName, "", artUrl) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (artUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx).data(artUrl).crossfade(true).build(),
                                contentDescription = album.strAlbum,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f).background(SonaraCardElevated, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Album, null, Modifier.size(40.dp), tint = p.copy(0.4f))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(album.strAlbum, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 2, textAlign = TextAlign.Center)
                        if (album.intYearReleased != null) {
                            Text("${album.intYearReleased}", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                    }
                }
            }
        }
    }
}
