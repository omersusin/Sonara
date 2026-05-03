package com.sonara.app.ui.screens.insights

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.odesli.OdesliHelper
import com.sonara.app.ui.theme.SonaraCard
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.SonaraDivider
import com.sonara.app.ui.theme.SonaraSuccess
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun EqualizerBars(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val barCount = 3
    val heights = (0 until barCount).map { idx ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 800
                    0.2f at 0 using FastOutSlowInEasing
                    1f at (200 + idx * 100) using FastOutSlowInEasing
                    0.3f at (500 + idx * 80) using FastOutSlowInEasing
                    0.8f at 700 using FastOutSlowInEasing
                    0.2f at 800 using FastOutSlowInEasing
                }
            ),
            label = "bar$idx"
        ).value
    }
    Canvas(modifier = modifier.size(18.dp)) {
        val barW = size.width / (barCount * 2 - 1)
        heights.forEachIndexed { i, h ->
            val barH = size.height * h
            val x = i * barW * 2
            drawRect(
                color = color,
                topLeft = Offset(x, size.height - barH),
                size = androidx.compose.ui.geometry.Size(barW, barH)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val clipboard = LocalClipboardManager.current

    var extraTracks by remember { mutableStateOf<List<RecentTrackItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var oldestUts by remember { mutableLongStateOf(0L) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedMenuTrack by remember { mutableStateOf<RecentTrackItem?>(null) }
    var streamingDialogTrack by remember { mutableStateOf<RecentTrackItem?>(null) }
    var streamingLinks by remember { mutableStateOf<List<OdesliHelper.PlatformLink>>(emptyList()) }
    var streamingLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.refreshAllRecentTracks()
    }

    LaunchedEffect(s.recentTracks) {
        val min = s.recentTracks.filter { it.uts > 0 }.minOfOrNull { it.uts }
        if (min != null && (oldestUts == 0L || min < oldestUts)) oldestUts = min
    }

    val allTracks = s.recentTracks + extraTracks

    fun loadNextPage() {
        if (oldestUts <= 0L) return
        loading = true
        scope.launch {
            val newTracks = withContext(Dispatchers.IO) {
                try {
                    val apiKey = app.lastFmAuth.getActiveApiKey()
                    val username = app.lastFmAuth.getConnectionInfo().username
                    if (apiKey.isBlank() || username.isBlank()) return@withContext emptyList()
                    val resp = LastFmClient.api.getRecentTracksRange(username, apiKey, 0L, oldestUts - 1, 200, 1)
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
                                uts = t.date?.uts?.toLongOrNull() ?: 0L,
                                isLoved = t.loved == "1"
                            )
                        } ?: emptyList()
                } catch (_: Exception) { emptyList() }
            }
            if (newTracks.isEmpty()) {
                hasMore = false
            } else {
                extraTracks = extraTracks + newTracks
                val newMin = newTracks.filter { it.uts > 0 }.minOfOrNull { it.uts }
                if (newMin != null) oldestUts = newMin
            }
            loading = false
        }
    }

    if (streamingDialogTrack != null) {
        AlertDialog(
            onDismissRequest = { streamingDialogTrack = null; streamingLinks = emptyList() },
            title = { Text(streamingDialogTrack!!.title, style = MaterialTheme.typography.titleMedium) },
            text = {
                if (streamingLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = p, modifier = Modifier.size(32.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        streamingLinks.forEach { link ->
                            TextButton(onClick = {
                                OdesliHelper.openLink(ctx, link)
                                streamingDialogTrack = null
                                streamingLinks = emptyList()
                            }) {
                                Text(link.name, color = p)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { streamingDialogTrack = null; streamingLinks = emptyList() }) { Text("Close") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently Played") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val displayTracks = if (searchQuery.isBlank()) allTracks
            else allTracks.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }

        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search…", style = MaterialTheme.typography.bodyMedium, color = SonaraTextTertiary) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = SonaraTextTertiary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, "Clear", Modifier.size(16.dp), tint = SonaraTextTertiary)
                        }
                    }
                },
                singleLine = true,
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

            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    scope.launch {
                        vm.refreshAllRecentTracks()
                        extraTracks = emptyList()
                        hasMore = true
                        oldestUts = 0L
                        refreshing = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(displayTracks) { i, t ->
                        Box {
                            Row(
                                Modifier.fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onTrackClick(t.title, t.artist) },
                                        onLongClick = { expandedMenuTrack = t }
                                    )
                                    .padding(vertical = 6.dp),
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
                                    EqualizerBars(color = SonaraSuccess)
                                } else {
                                    if (!t.isNowPlaying) {
                                        IconButton(
                                            onClick = { vm.toggleLove(t.title, t.artist) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                if (t.isLoved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = if (t.isLoved) "Unlove" else "Love",
                                                tint = if (t.isLoved) p else SonaraTextTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(relativeTime(t.uts), style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                }
                            }

                            DropdownMenu(
                                expanded = expandedMenuTrack == t,
                                onDismissRequest = { expandedMenuTrack = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Copy "${t.title} — ${t.artist}"") },
                                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        clipboard.setText(AnnotatedString("${t.title} — ${t.artist}"))
                                        expandedMenuTrack = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Open in streaming app") },
                                    leadingIcon = { Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        expandedMenuTrack = null
                                        streamingDialogTrack = t
                                        streamingLoading = true
                                        scope.launch {
                                            streamingLinks = withContext(Dispatchers.IO) {
                                                try { OdesliHelper.getLinks(t.title, t.artist) } catch (_: Exception) { emptyList() }
                                            }
                                            streamingLoading = false
                                        }
                                    }
                                )
                            }
                        }

                        if (i < displayTracks.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.1f)))
                    }

                    if (hasMore && searchQuery.isBlank()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                if (loading) {
                                    CircularProgressIndicator(color = p, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Load more", style = MaterialTheme.typography.labelMedium, color = p, modifier = Modifier.combinedClickable(onClick = { loadNextPage() }))
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
