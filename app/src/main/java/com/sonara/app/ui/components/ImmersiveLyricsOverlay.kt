package com.sonara.app.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.intelligence.lyrics.LrcParser
import com.sonara.app.intelligence.lyrics.LyricsAnimationStyle
import com.sonara.app.intelligence.lyrics.LyricsState
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.screens.share.LyricsShareScreen
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun ImmersiveLyricsOverlay(
    title: String,
    artist: String,
    albumArt: Bitmap?,
    isPlaying: Boolean,
    duration: Long,
    position: Long,
    positionTimestamp: Long,
    lyricsState: LyricsState,
    lyricsSyncOffsetMs: Int = 0,
    lyricsAnimationStyle: LyricsAnimationStyle = LyricsAnimationStyle.KARAOKE,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    lyricsAutoScroll: Boolean = true,
    lyricsLineSpacing: Float = 1.3f,
    lyricsBlurInactive: Boolean = true,
    lyricsTextAlignment: String = "center",
    lyricsTextSizeSp: Float = 0f
) {
    val p = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    var showShareScreen by remember { mutableStateOf(false) }

    BackHandler { if (showShareScreen) showShareScreen = false else onDismiss() }

    // Drag-to-dismiss
    var totalDragDelta by remember { mutableFloatStateOf(0f) }

    // Ticker
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) { delay(16L); tick = System.currentTimeMillis() }
    }

    val estimatedPosition = when {
        isPlaying && positionTimestamp > 0 -> position + (tick - positionTimestamp)
        else -> position
    }

    val lyricsPosition = estimatedPosition + lyricsSyncOffsetMs
    val progress = if (duration > 0) (estimatedPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    val readyState = lyricsState as? LyricsState.Ready
    val activeLineIndex = readyState?.lyrics?.lines
        ?.let { LrcParser.activeLineIndex(it, lyricsPosition) } ?: -1

    val listState = rememberLazyListState()

    // Snap to current line instantly when overlay opens (no animation, avoids layout-not-ready race)
    LaunchedEffect(Unit) {
        if (activeLineIndex >= 0) {
            // Wait for the LazyColumn to be laid out before scrolling
            var h = listState.layoutInfo.viewportSize.height
            while (h == 0) { delay(16L); h = listState.layoutInfo.viewportSize.height }
            listState.scrollToItem(
                index = (activeLineIndex - 1).coerceAtLeast(0),
                scrollOffset = -(h / 2) + 60
            )
        }
    }

    // Animated scroll to keep active line centered as song progresses
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && lyricsAutoScroll) {
            // Wait until the LazyColumn is laid out — viewportSize.height is 0 on the first frame,
            // which makes the scrollOffset positive and pushes the active line above the viewport.
            var h = listState.layoutInfo.viewportSize.height
            while (h == 0) { delay(16L); h = listState.layoutInfo.viewportSize.height }
            listState.animateScrollToItem(
                index = activeLineIndex,
                scrollOffset = -(h / 2) + 60
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, delta -> totalDragDelta += delta.y + delta.x },
                    onDragEnd = {
                        if (totalDragDelta > 300f) onDismiss()
                        totalDragDelta = 0f
                    },
                    onDragCancel = { totalDragDelta = 0f }
                )
            }
    ) {
        // Blurred album art background
        if (albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(24.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)))
        }

        // Dark overlay
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)))

        // Content
        Column(Modifier.fillMaxSize()) {

            // TopBar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.ArrowBack, "Close", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
                    if (artist.isNotBlank()) {
                        Text(artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f), maxLines = 1)
                    }
                }
                IconButton(onClick = { showShareScreen = true }) {
                    Icon(Icons.Rounded.Share, "Share lyrics", tint = Color.White.copy(0.7f))
                }
            }

            // Lyrics scroll
            when (lyricsState) {
                is LyricsState.Ready -> {
                    val lines = lyricsState.lyrics.lines
                    if (lines.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            itemsIndexed(lines, key = { idx, _ -> idx }) { idx, line ->
                                val isActive = idx == activeLineIndex
                                val activeWord = if (isActive && lyricsState.lyrics.hasWordTimestamps) {
                                    LrcParser.activeWordIndex(line, lyricsPosition)
                                } else -1
                                val distanceFromActive = abs(idx - activeLineIndex)
                                SyncedLyricLine(
                                    line = line,
                                    isActive = isActive,
                                    activeWordIndex = activeWord,
                                    estimatedPositionMs = if (isActive) lyricsPosition else 0L,
                                    animationStyle = lyricsAnimationStyle,
                                    distanceFromActive = distanceFromActive,
                                    lyricsLineSpacing = lyricsLineSpacing,
                                    lyricsBlurInactive = lyricsBlurInactive,
                                    lyricsPosition = lyricsTextAlignment,
                                    textSizeSp = lyricsTextSizeSp,
                                    modifier = Modifier.clickable {
                                        val seekMs = (line.startMs - lyricsSyncOffsetMs).coerceAtLeast(0L)
                                        SonaraNotificationListener.seekTo(seekMs)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                            }
                        }
                    } else {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No synced lyrics available", color = Color.White.copy(0.5f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                else -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No lyrics", color = Color.White.copy(0.5f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Progress + controls
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                var isDragging by remember { mutableStateOf(false) }
                var dragValue by remember { mutableFloatStateOf(progress) }
                val displayProgress = if (isDragging) dragValue else progress

                Slider(
                    value = displayProgress,
                    onValueChange = { isDragging = true; dragValue = it },
                    onValueChangeFinished = {
                        SonaraNotificationListener.seekTo((dragValue * duration).toLong())
                        isDragging = false
                    },
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(0.9f),
                        inactiveTrackColor = Color.White.copy(0.25f)
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(estimatedPosition.toMmSs(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f), fontSize = 10.sp)
                    Text(duration.toMmSs(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f), fontSize = 10.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(56.dp).background(Color.White.copy(0.15f), CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
        // CROSS-09: Share screen overlay
        if (showShareScreen) {
            val readyLines = (lyricsState as? LyricsState.Ready)?.lyrics?.lines ?: emptyList()
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                LyricsShareScreen(
                    title = title,
                    artist = artist,
                    lines = readyLines,
                    onBack = { showShareScreen = false }
                )
            }
        }
    }
}

private fun Long.toMmSs(): String {
    val clamped = coerceAtLeast(0L)
    val totalSec = clamped / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
