package com.sonara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MultiArtistAvatarRow(
    artists: List<String>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 28.dp,
    overlap: Dp = 10.dp,
    maxVisible: Int = 3
) {
    val visible = artists.take(maxVisible)
    if (visible.size <= 1) return

    val totalWidth = avatarSize + (overlap * (visible.size - 1))
    Box(modifier = modifier.size(width = totalWidth, height = avatarSize)) {
        visible.forEachIndexed { i, artistName ->
            ArtistAvatar(
                artistName = artistName,
                size = avatarSize,
                modifier = Modifier
                    .offset(x = overlap * i)
                    .align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
private fun ArtistAvatar(artistName: String, size: Dp, modifier: Modifier = Modifier) {
    val p = MaterialTheme.colorScheme.primary

    val imageUrl by produceState<String?>(initialValue = null, artistName) {
        value = withContext(Dispatchers.IO) {
            try {
                TheAudioDbClient.searchArtist(artistName)?.strThumb?.takeIf { it.isNotBlank() }
                    ?: DeezerImageResolver.getArtistDetail(artistName)?.imageUrl?.takeIf { it.isNotBlank() }
            } catch (_: Exception) { null }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(p.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = artistName,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = artistName.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = p
            )
        }
    }
}
