package com.sonara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.theaudiodb.TheAudioDbClient
import com.sonara.app.ui.theme.SonaraDivider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders side-by-side circular avatars for multiple artists.
 * Single-artist display is left to the caller (returns early when size <= 1).
 *
 * | Artists | Size  | Gap  | Shown          |
 * |---------|-------|------|----------------|
 * | 2       | 44dp  | 8dp  | 2 circles      |
 * | 3       | 36dp  | 6dp  | 3 circles      |
 * | 4+      | 32dp  | 6dp  | 3 circles + +N |
 */
@Composable
fun MultiArtistAvatarRow(
    artists: List<String>,
    modifier: Modifier = Modifier,
    onArtistClick: (String) -> Unit = {}
) {
    if (artists.size <= 1) return

    val (avatarSize, gap, maxShow) = when {
        artists.size == 2 -> Triple(44.dp, 8.dp, 2)
        artists.size == 3 -> Triple(36.dp, 6.dp, 3)
        else              -> Triple(32.dp, 6.dp, 3)
    }
    val overflow = (artists.size - maxShow).coerceAtLeast(0)
    val visible  = artists.take(maxShow)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visible.forEach { name ->
            ArtistAvatarCircle(
                artistName = name,
                size       = avatarSize,
                onClick    = { onArtistClick(name) }
            )
        }
        if (overflow > 0) {
            val p = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(p.copy(0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "+$overflow",
                    style = MaterialTheme.typography.labelSmall,
                    color = p
                )
            }
        }
    }
}

@Composable
fun ArtistAvatarCircle(
    artistName: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = MaterialTheme.colorScheme.primary
    val imageUrl by produceState<String?>(null, artistName) {
        value = withContext(Dispatchers.IO) {
            try {
                DeezerImageResolver.getArtistDetail(artistName)
                    ?.imageUrl?.takeIf { it.isNotBlank() }
                    ?: TheAudioDbClient.searchArtist(artistName)
                        ?.strThumb?.takeIf { it.isNotBlank() }
            } catch (_: Exception) { null }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, SonaraDivider.copy(0.3f), CircleShape)
            .background(p.copy(0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model              = imageUrl,
                contentDescription = artistName,
                modifier           = Modifier.size(size).clip(CircleShape),
                contentScale       = ContentScale.Crop
            )
        } else {
            Text(
                text  = artistName.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = p
            )
        }
    }
}
