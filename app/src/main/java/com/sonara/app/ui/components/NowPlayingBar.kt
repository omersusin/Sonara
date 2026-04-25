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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.intelligence.artist.ArtistNameParser
import com.sonara.app.intelligence.lyrics.LrcParser
import com.sonara.app.intelligence.lyrics.LyricsAnimationStyle
import com.sonara.app.intelligence.lyrics.LyricsState
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.theme.*
import kotlin.math.abs
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
    lyricsAnimationStyle: LyricsAnimationStyle = LyricsAnimationStyle.KARAOKE,
    lyricsSyncOffsetMs: Int = 0,
    lyricsTextSizeSp: Float = 0f,
    onClick: (() -> Unit)? = null,
    isLoved: Boolean = false,
    onToggleLove: () -> Unit = {},
    lyricsShowTranslated: Boolean = false,
    lyricsTargetLanguage: String = "en"
) {
    val p = MaterialTheme.colorScheme.primary
    val hasTrack = title != "No music playing" && title.isNotBlank()
    var lyricsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(title) { lyricsExpanded = false }

    // Drag state: while dragging, freeze the ticker and show drag value
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    // Ticker: adaptive rate based on lyrics expansion and animation style
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val tickMs = when {
        !lyricsExpanded -> 250L
        lyricsAnimationStyle in listOf(LyricsAnimationStyle.KARAOKE,
            LyricsAnimationStyle.VIVIMUSIC, LyricsAnimationStyle.LYRICS_V2) -> 16L
        else -> 100L
    }
    LaunchedEffect(isPlaying, isDragging, lyricsExpanded, lyricsAnimationStyle) {
        while (isPlaying && !isDragging) { delay(tickMs); tick = System.currentTimeMillis() }
    }

    val estimatedPosition = when {
        isDragging -> (dragValue * duration).toLong()
        isPlaying && positionTimestamp > 0 -> position + (tick - positionTimestamp)
        else -> position
    }

    val progress = if (duration > 0) (estimatedPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val displayProgress by remember {
        derivedStateOf {
            if (isDragging) dragValue
            else if (duration > 0)
                (estimatedPosition.toFloat() / duration).coerceIn(0f, 1f)
            else 0f
        }
    }

    val displayArtist = remember(artist) { ArtistNameParser.formatForDisplay(artist) }
    val lyricsPosition = estimatedPosition + lyricsSyncOffsetMs

    val readyState = lyricsState as? LyricsState.Ready
    val activeLineIndex by remember {
        derivedStateOf {
            readyState?.lyrics?.lines?.let { LrcParser.activeLineIndex(it, lyricsPosition) } ?: -1
        }
    }
    val lyricsListState = rememberLazyListState()
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && lyricsExpanded) {
            val viewportCenter = lyricsListState.layoutInfo.viewportSize.height / 2
            lyricsListState.animateScrollToItem(
                index = activeLineIndex,
                scrollOffset = -viewportCenter + 40
            )
        }
    }

    val hasLyrics = lyricsState is LyricsState.Ready || lyricsState is LyricsState.Loading

    FluentCard(onClick = if (hasTrack) onClick else null) {

        // ── Top row: album art + title/artist ────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                    Icon(Icons.Rounded.MusicNote, null, tint = p, modifier = Modifier.size(28.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = SonaraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (displayArtist.isNotEmpty()) {
                    Text(
                        displayArtist,
                        style = MaterialTheme.typography.bodySmall,
                        color = SonaraTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!hasTrack && isPlaying) {
                Box(modifier = Modifier.size(8.dp).background(SonaraSuccess, CircleShape))
            }
        }

        // ── Lyrics panel (between top row and controls) ──────────────────
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
                            itemsIndexed(
                                items = lyrics.lines,
                                key = { idx, _ -> idx }
                            ) { idx, line ->
                                val isActive = idx == activeLineIndex
                                val activeWord = if (isActive && lyrics.hasWordTimestamps) {
                                    LrcParser.activeWordIndex(line, lyricsPosition)
                                } else -1
                                val distanceFromActive = kotlin.math.abs(idx - activeLineIndex)
                                SyncedLyricLine(
                                    line = line,
                                    isActive = isActive,
                                    activeWordIndex = activeWord,
                                    estimatedPositionMs = if (isActive) lyricsPosition else 0L,
                                    animationStyle = lyricsAnimationStyle,
                                    textSizeSp = lyricsTextSizeSp,
                                    distanceFromActive = distanceFromActive
                                )
                                val translatedLine = if (lyricsShowTranslated) {
                                    (lyricsState as? LyricsState.Ready)?.translatedLines?.getOrNull(idx)
                                } else null
                                if (translatedLine != null) {
                                    Text(
                                        text = translatedLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SonaraTextSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
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

        // ── Progress slider with elapsed / remaining times ────────────────
        if (hasTrack && duration > 0) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val displayMs = if (isDragging) (dragValue * duration).toLong() else estimatedPosition
                Text(
                    text = displayMs.toMmSs(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SonaraTextTertiary,
                    fontSize = 10.sp
                )
                Slider(
                    value = displayProgress,
                    onValueChange = { v ->
                        if (!isDragging) {
                            isDragging = true
                            dragValue = displayProgress
                        }
                        dragValue = v
                    },
                    onValueChangeFinished = {
                        SonaraNotificationListener.seekTo((dragValue * duration).toLong())
                        isDragging = false
                    },
                    modifier = Modifier.weight(1f).height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = p,
                        activeTrackColor = p,
                        inactiveTrackColor = SonaraDivider.copy(alpha = 0.35f)
                    )
                )
                val remainingMs = duration - displayMs
                Text(
                    text = "-${remainingMs.toMmSs()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SonaraTextTertiary,
                    fontSize = 10.sp
                )
            }
        }

        // ── Playback controls — always shown when hasTrack ────────────────
        if (hasTrack) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (hasLyrics) {
                    IconButton(
                        onClick = { lyricsExpanded = !lyricsExpanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (lyricsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (lyricsExpanded) p else SonaraTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { onToggleLove() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isLoved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Love",
                        tint = if (isLoved) Color(0xFFE91E63) else SonaraTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { SonaraNotificationListener.sendPrevious() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", tint = SonaraTextSecondary, modifier = Modifier.size(24.dp))
                }
                IconButton(
                    onClick = { SonaraNotificationListener.sendPlayPause() },
                    modifier = Modifier.size(44.dp).background(p.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (isPlaying) "Pause" else "Play",
                        tint = p,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(
                    onClick = { SonaraNotificationListener.sendNext() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, "Next", tint = SonaraTextSecondary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

private fun Long.toMmSs(): String {
    val clamped = coerceAtLeast(0L)
    val totalSec = clamped / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
