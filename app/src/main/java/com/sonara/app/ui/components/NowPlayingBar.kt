package com.sonara.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.sonara.app.intelligence.artist.ArtistNameParser
import com.sonara.app.intelligence.lyrics.LrcParser
import com.sonara.app.intelligence.lyrics.LyricsAnimationStyle
import com.sonara.app.intelligence.lyrics.LyricsState
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.theme.*
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
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
    lyricsTargetLanguage: String = "en",
    playerPackage: String = "",
    onImmersiveRequest: () -> Unit = {},
    lyricsAutoScroll: Boolean = true,
    lyricsLineSpacing: Float = 1.3f,
    lyricsBlurInactive: Boolean = true,
    lyricsTextAlignment: String = "center",
    onSearchCorrection: ((String, String) -> Unit)? = null
) {
    val p = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hasTrack = title != "No music playing" && title.isNotBlank()
    var lyricsExpanded by remember { mutableStateOf(false) }

    // FEAT-10: Derive expressive accent from album art via Palette
    var expressiveAccent by remember { mutableStateOf(Color.Unspecified) }
    LaunchedEffect(albumArt) {
        expressiveAccent = if (albumArt != null) {
            withContext(Dispatchers.Default) {
                try {
                    val palette = Palette.from(albumArt).generate()
                    val rgb = palette.vibrantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                    if (rgb != null) Color(rgb) else Color.Unspecified
                } catch (_: Exception) { Color.Unspecified }
            }
        } else Color.Unspecified
    }

    // CROSS-02: Lyrics correction dialog state
    var showLyricsCorrection by remember { mutableStateOf(false) }
    var corrTitle by remember(title) { mutableStateOf(title) }
    var corrArtist by remember(artist) { mutableStateOf(artist) }

    val lyricsListState = rememberLazyListState()
    // CROSS-11: Scroll lock — user scroll temporarily overrides auto-scroll
    var hasUserScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(lyricsListState.isScrollInProgress) {
        if (lyricsListState.isScrollInProgress) {
            hasUserScrolled = true
            kotlinx.coroutines.delay(3000L)
            hasUserScrolled = false
        }
    }

    LaunchedEffect(title) {
        lyricsExpanded = false
        lyricsListState.scrollToItem(0)
    }

    // Drag state
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

    val displayProgress = if (isDragging) dragValue
        else if (duration > 0) (estimatedPosition.toFloat() / duration).coerceIn(0f, 1f)
        else 0f

    val displayArtist = remember(artist) { ArtistNameParser.formatForDisplay(artist) }
    val lyricsPosition = estimatedPosition + lyricsSyncOffsetMs

    // Resolve source app label from package name
    val sourceAppName = remember(playerPackage) {
        if (playerPackage.isBlank()) ""
        else try {
            ctx.packageManager.getApplicationLabel(
                ctx.packageManager.getApplicationInfo(playerPackage, 0)
            ).toString()
        } catch (_: Exception) { "" }
    }

    val readyState = lyricsState as? LyricsState.Ready
    val activeLineIndex = readyState?.lyrics?.lines
        ?.let { LrcParser.activeLineIndex(it, lyricsPosition) } ?: -1

    // Snap to active line when lyrics panel is first opened
    LaunchedEffect(lyricsExpanded) {
        if (lyricsExpanded && activeLineIndex >= 0) {
            var h = lyricsListState.layoutInfo.viewportSize.height
            while (h == 0) { delay(16L); h = lyricsListState.layoutInfo.viewportSize.height }
            lyricsListState.scrollToItem(
                index = (activeLineIndex - 1).coerceAtLeast(0),
                scrollOffset = -(h / 2) + 40
            )
        }
    }

    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && lyricsExpanded && lyricsAutoScroll && !hasUserScrolled) {
            val viewportCenter = lyricsListState.layoutInfo.viewportSize.height / 2
            lyricsListState.animateScrollToItem(
                index = activeLineIndex,
                scrollOffset = -viewportCenter + 40
            )
        }
    }

    val hasLyrics = lyricsState is LyricsState.Ready || lyricsState is LyricsState.Loading

    FluentCard(onClick = if (hasTrack) onClick else null) {

        // ── Top row: album art + title/artist + heart ────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(52.dp).background(SonaraCardElevated, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = p, modifier = Modifier.size(26.dp))
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
                val subtitleText = when {
                    displayArtist.isNotBlank() && sourceAppName.isNotBlank() -> "$displayArtist · $sourceAppName"
                    displayArtist.isNotBlank() -> displayArtist
                    else -> ""
                }
                if (subtitleText.isNotEmpty()) {
                    Text(
                        subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = SonaraTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Heart icon in top row
            if (hasTrack) {
                IconButton(onClick = onToggleLove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isLoved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Love",
                        tint = if (isLoved) Color(0xFFE91E63) else SonaraTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── Lyrics panel ─────────────────────────────────────────────────────
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = p, strokeWidth = 2.dp)
                            val providerName = (lyricsState as? LyricsState.Loading)?.providerName
                            if (!providerName.isNullOrBlank()) {
                                Text(
                                    text = "Trying $providerName…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SonaraTextTertiary
                                )
                            }
                        }
                    }
                }
                is LyricsState.Ready -> {
                    val lyrics = lyricsState.lyrics
                    if (lyrics.lines.isNotEmpty()) {
                        // Lyrics action row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (onSearchCorrection != null) {
                                IconButton(onClick = { showLyricsCorrection = true }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Edit, "Fix lyrics", tint = SonaraTextTertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        LazyColumn(
                            state = lyricsListState,
                            userScrollEnabled = !isPlaying || hasUserScrolled,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).padding(top = 6.dp),
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
                                val distanceFromActive = abs(idx - activeLineIndex)
                                // CROSS-01: Instrumental gap detection
                                val nextLineStartMs = lyrics.lines.getOrNull(idx + 1)?.startMs
                                val gapMs = if (nextLineStartMs != null) nextLineStartMs - line.startMs else Long.MAX_VALUE
                                val isInstrumental = line.text.isBlank() || (isActive && gapMs > 3000L && nextLineStartMs != null)
                                val instrumentalProg = if (isInstrumental && isActive && nextLineStartMs != null && gapMs > 0) {
                                    ((lyricsPosition - line.startMs).toFloat() / gapMs).coerceIn(0f, 1f)
                                } else 0f
                                SyncedLyricLine(
                                    line = line,
                                    isActive = isActive,
                                    activeWordIndex = activeWord,
                                    estimatedPositionMs = if (isActive) lyricsPosition else 0L,
                                    animationStyle = lyricsAnimationStyle,
                                    textSizeSp = lyricsTextSizeSp,
                                    distanceFromActive = distanceFromActive,
                                    lyricsLineSpacing = lyricsLineSpacing,
                                    lyricsBlurInactive = lyricsBlurInactive,
                                    lyricsPosition = lyricsTextAlignment,
                                    isInstrumental = isInstrumental,
                                    instrumentalProgress = { instrumentalProg },
                                    accentColor = expressiveAccent,
                                    modifier = Modifier.clickable {
                                        val seekMs = (line.startMs - lyricsSyncOffsetMs).coerceAtLeast(0L)
                                        SonaraNotificationListener.seekTo(seekMs)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                                val translatedLine = if (lyricsShowTranslated) {
                                    (lyricsState as? LyricsState.Ready)?.translatedLines?.getOrNull(idx)
                                } else null
                                if (translatedLine != null) {
                                    Text(
                                        text = translatedLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SonaraTextSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
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
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(max = 240.dp),
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

        // ── Progress slider ───────────────────────────────────────────────────
        if (hasTrack && duration > 0) {
            Spacer(Modifier.height(8.dp))
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
                        if (!isDragging) { isDragging = true; dragValue = displayProgress }
                        dragValue = v
                    },
                    onValueChangeFinished = {
                        SonaraNotificationListener.seekTo((dragValue * duration).toLong())
                        isDragging = false
                    },
                    modifier = Modifier.weight(1f).height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = p,
                        activeTrackColor = p.copy(alpha = 0.9f),
                        inactiveTrackColor = SonaraDivider.copy(alpha = 0.25f)
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

        // ── Playback controls ─────────────────────────────────────────────────
        if (hasTrack) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                IconButton(
                    onClick = { SonaraNotificationListener.sendPrevious() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", tint = SonaraTextSecondary, modifier = Modifier.size(24.dp))
                }

                // Pill-shaped play/pause
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(36.dp)
                        .background(p.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .combinedClickable(onClick = { SonaraNotificationListener.sendPlayPause() }),
                    contentAlignment = Alignment.Center
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

            // Lyrics toggle below controls
            if (hasLyrics) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .combinedClickable(
                                onClick = { lyricsExpanded = !lyricsExpanded },
                                onLongClick = { onImmersiveRequest() }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (lyricsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (lyricsExpanded) p else SonaraTextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    // CROSS-02: Lyrics correction dialog
    if (showLyricsCorrection) {
        AlertDialog(
            onDismissRequest = { showLyricsCorrection = false },
            title = { Text("Fix Lyrics", style = MaterialTheme.typography.titleLarge, color = SonaraTextPrimary) },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "If lyrics are wrong, correct the title or artist and search again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonaraTextSecondary
                    )
                    OutlinedTextField(
                        value = corrTitle, onValueChange = { corrTitle = it },
                        label = { Text("Song title") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = corrArtist, onValueChange = { corrArtist = it },
                        label = { Text("Artist") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLyricsCorrection = false
                        onSearchCorrection?.invoke(corrTitle, corrArtist)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = p)
                ) { Text("Search") }
            },
            dismissButton = {
                TextButton(onClick = { showLyricsCorrection = false }) {
                    Text("Cancel", color = SonaraTextSecondary)
                }
            }
        )
    }
}

private fun Long.toMmSs(): String {
    val clamped = coerceAtLeast(0L)
    val totalSec = clamped / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
