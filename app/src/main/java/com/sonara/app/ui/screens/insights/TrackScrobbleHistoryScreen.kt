package com.sonara.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScrobbleHistoryScreen(
    trackTitle: String,
    trackArtist: String,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val p = MaterialTheme.colorScheme.primary
    val app = SonaraApp.instance
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())

    data class ScrobbleEntry(val date: String, val uts: Long, val imageUrl: String)

    var scrobbles by remember { mutableStateOf<List<ScrobbleEntry>>(emptyList()) }
    var totalScrobbleCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var firstScrobbleLabel by remember { mutableStateOf("") }

    LaunchedEffect(trackTitle, trackArtist) {
        withContext(Dispatchers.IO) {
            try {
                val username = app.lastFmAuth.getConnectionInfo().username
                val apiKey = app.lastFmAuth.getActiveApiKey()
                if (username.isBlank() || apiKey.isBlank()) { isLoading = false; return@withContext }

                val resp = LastFmClient.api.getUserTrackScrobbles(username, trackArtist, trackTitle, apiKey, limit = 200)
                val apiTotal = resp.recenttracks?.attr?.total?.toIntOrNull()
                val sdf = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
                val entries = resp.recenttracks?.track
                    ?.filter { it.date != null }
                    ?.map { t ->
                        val uts = t.date?.uts?.toLongOrNull() ?: 0L
                        val cal = Calendar.getInstance().apply { timeInMillis = uts * 1000 }
                        ScrobbleEntry(
                            date = sdf.format(cal.time),
                            uts = uts,
                            imageUrl = t.imageUrl?.takeIf { !it.contains("2a96cbd8b46e") } ?: ""
                        )
                    } ?: emptyList()

                totalScrobbleCount = apiTotal ?: entries.size

                val oldest = entries.minByOrNull { it.uts }
                if (oldest != null && oldest.uts > 0) {
                    val now = System.currentTimeMillis() / 1000
                    val days = (now - oldest.uts) / 86400
                    firstScrobbleLabel = when {
                        days < 1 -> "today"
                        days < 7 -> "$days days ago"
                        days < 30 -> "${days / 7} weeks ago"
                        days < 365 -> "${days / 30} months ago"
                        else -> "${days / 365} years ago"
                    }
                }

                scrobbles = entries
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trackTitle, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FluentCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                fmt.format(totalScrobbleCount.toLong()),
                                style = MaterialTheme.typography.headlineMedium,
                                color = p,
                                fontWeight = FontWeight.Bold
                            )
                            Text("total scrobbles", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        }
                        if (firstScrobbleLabel.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Cake, contentDescription = null, tint = p, modifier = Modifier.size(28.dp))
                                Text("First: $firstScrobbleLabel", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = p)
                    }
                }
            } else {
                items(scrobbles) { entry ->
                    FluentCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (entry.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(ctx).data(entry.imageUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                                )
                            } else {
                                Box(
                                    Modifier.size(36.dp).background(SonaraCardElevated, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.MusicNote, null, tint = p.copy(0.4f), modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(entry.date, style = MaterialTheme.typography.bodySmall, color = SonaraTextPrimary)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
