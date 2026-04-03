package com.sonara.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.theme.*

@Composable
fun NowPlayingBar(
    title: String = "No music playing",
    artist: String = "",
    isPlaying: Boolean = false,
    albumArt: Bitmap? = null
) {
    val p = MaterialTheme.colorScheme.primary
    val hasTrack = title != "No music playing" && title.isNotBlank()

    FluentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Album art
            if (albumArt != null) {
                Image(bitmap = albumArt.asImageBitmap(), contentDescription = "Album Art",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.size(48.dp).background(SonaraCardElevated, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, tint = p, modifier = Modifier.size(24.dp))
                }
            }

            // Title + artist
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (artist.isNotEmpty()) Text(artist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Transport controls
            if (hasTrack) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            tint = p, modifier = Modifier.size(22.dp)
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
    }
}
