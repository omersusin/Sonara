package com.sonara.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonara.app.intelligence.artist.ArtistNameParser
import com.sonara.app.intelligence.lyrics.LrcParser
import com.sonara.app.intelligence.lyrics.LyricsState
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun NowPlayingBar(
    title: String = "No music playing",
    artist: String = "",
    isPlaying: Boolean = false,
    albumArt: Bitmap? = null,
    duration: Long = 0,
    position: Long = 0,
    positionTimestamp: Long = 0,
    lyricsState: LyricsState = LyricsState.Idle,
    onClick: (() -> Unit)? = null
) {
    val p = MaterialTheme.colorScheme.primary
    val hasTrack = title != "No music playing" && title.isNotBlank()
    var lyricsExpanded by remember { mutableStateOf(false) }

    // Reset expand when track changes
    LaunchedEffect(title) { lyricsExpanded = false }

    // Ticker for live progress (250ms)
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) { delay(250L); tick = System.currentTimeMillis() }
    }

    val estimatedPosition = if (isPlaying && positionTimestamp > 0) {
        position + (tick - positionTimestamp)
    } else position

    val progress = if (duration > 0) (estimatedPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    // Multi-artist display
    val displayArtist = remember(artist) { ArtistNameParser.formatForDisplay(artist) }
    val artistCount = remember(artist) { ArtistNameParser.resolve(artist).size }

    // Synced lyrics active line
    val readyState = lyricsState as? LyricsState.Ready
    val activeLineIndex = remember(estimatedPosition, readyState) {
        readyState?.lyrics?.lines?.let { LrcParser.activeLineIndex(it, estimatedPosition) } ?: -1
    }
    val lyricsListState = rememberLazyListState()
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 1 && lyricsExpanded) {
            lyricsListState.animateScrollToItem((activeLineIndex - 1).coerceAtLeast(0))
        }
    }

    val hasLyrics = lyricsState is LyricsState.Ready || lyricsState is LyricsState.Loading

    FluentCard(onClick = if (hasTrack) onClick else null) {
        // ── Main row ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Album art
            if (albumArt != null) {
                Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(56.dp).background(SonaraCardElevated, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = p, modifier = Modifier.size(26.dp))
                }
            }

            // Track + artist + progress
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = SonaraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (displayArtist.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (artistCount > 1) {
                            MultiArtistAvatarRow(
                                artistCount = artistCount,
                                avatarSize = 16.dp,
                                overlap = 6.dp,
                                maxVisible = 3
                            )
                        }
                        Text(
                            displayArtist,
                            style = MaterialTheme.typography.bodySmall,
                            color = SonaraTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (hasTrack && duration > 0) {
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                        color = p,
                        trackColor = SonaraDivider.copy(alpha = 0.3f)
                    )
                }
            }

            // Controls
            if (hasTrack) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lyrics toggle
                    if (hasLyrics) {
                        IconButton(
                            onClick = { lyricsExpanded = !lyricsExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (lyricsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.Lyrics,
                                contentDescription = "Lyrics",
                                tint = if (lyricsExpanded) p else SonaraTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = { SonaraNotificationListener.sendPrevious() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", tint = SonaraTextSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { SonaraNotificationListener.sendPlayPause() },
                        modifier = Modifier.size(36.dp).background(p.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = p,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { SonaraNotificationListener.sendNext() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.SkipNext, "Next", tint = SonaraTextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                if (isPlaying) Box(modifier = Modifier.size(8.dp).background(SonaraSuccess, CircleShape))
            }
        }

        // ── Lyrics panel ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = lyricsExpanded && hasTrack,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            when (lyricsState) {
                is LyricsState.Loading -> {
                    Box(
                        Modifier.fillMaxWidth().height(80.dp).padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = p, strokeWidth = 2.dp)
                    }
                }
                is LyricsState.Ready -> {
                    val lyrics = lyricsState.lyrics
                    if (lyrics.lines.isNotEmpty()) {
                        LazyColumn(
                            state = lyricsListState,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(lyrics.lines) { idx, line ->
                                val isActive = idx == activeLineIndex
                                val activeWord = if (isActive && lyrics.hasWordTimestamps) {
                                    LrcParser.activeWordIndex(line, estimatedPosition)
                                } else -1
                                SyncedLyricLine(line = line, isActive = isActive, activeWordIndex = activeWord)
                            }
                        }
                    } else if (lyricsState.plain != null) {
                        Text(
                            text = lyricsState.plain,
                            style = MaterialTheme.typography.bodySmall,
                            color = SonaraTextSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .heightIn(max = 220.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is LyricsState.NotFound -> {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No lyrics found", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                    }
                }
                else -> {}
            }
        }
    }
}
