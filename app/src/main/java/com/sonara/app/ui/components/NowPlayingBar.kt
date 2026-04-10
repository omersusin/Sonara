package com.sonara.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.theme.*

@Composable
fun NowPlayingBar(
    title: String    = "No music playing",
    artist: String   = "",
    isPlaying: Boolean = false,
    albumArt: Bitmap?  = null
) {
    val primary   = MaterialTheme.colorScheme.primary
    val hasTrack  = title != "No music playing" && title.isNotBlank()

    // MD3 Expressive: NowPlaying card daha büyük radius, surface container rengi
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = NowPlayingShape,  // 20 dp özel şekil
        color          = SonaraSurfaceContainerHigh,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Albüm kapağı ──────────────────────────────────
            if (albumArt != null) {
                Image(
                    bitmap             = albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier           = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(
                    modifier       = Modifier
                        .size(52.dp)
                        .background(SonaraSurfaceContainerHighest, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote, null,
                        tint     = primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // ── Başlık + sanatçı ──────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = title,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = SonaraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (artist.isNotEmpty()) {
                    Text(
                        text     = artist,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = SonaraTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ── Transport kontroller ──────────────────────────
            AnimatedVisibility(
                visible  = hasTrack,
                enter    = fadeIn() + expandHorizontally(),
                exit     = fadeOut() + shrinkHorizontally()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Önceki
                    IconButton(
                        onClick  = { SonaraNotificationListener.sendPrevious() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SkipPrevious, "Previous",
                            tint     = SonaraTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Oynat/Durdur — MD3 Expressive: FilledIconButton (daha canlı)
                    FilledIconButton(
                        onClick  = { SonaraNotificationListener.sendPlayPause() },
                        modifier = Modifier.size(42.dp),
                        colors   = IconButtonDefaults.filledIconButtonColors(
                            containerColor = primary,
                            contentColor   = SonaraBackground
                        )
                    ) {
                        Icon(
                            imageVector        = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier           = Modifier.size(24.dp)
                        )
                    }

                    // Sonraki
                    IconButton(
                        onClick  = { SonaraNotificationListener.sendNext() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext, "Next",
                            tint     = SonaraTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Müzik çalmıyorsa küçük yeşil nokta
            if (!hasTrack && isPlaying) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(SonaraSuccess, PillShape)
                )
            }
        }
    }
}
