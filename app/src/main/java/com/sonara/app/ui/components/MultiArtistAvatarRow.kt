package com.sonara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.SonaraCardElevated

/**
 * Shows stacked avatar circles for multi-artist tracks.
 * Each circle overlaps the previous by [overlap]dp.
 * Placeholder shows a person icon; swap with AsyncImage when artist images are available.
 */
@Composable
fun MultiArtistAvatarRow(
    artistCount: Int,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 28.dp,
    overlap: Dp = 10.dp,
    maxVisible: Int = 3
) {
    val count = minOf(artistCount, maxVisible)
    if (count <= 1) return

    val totalWidth = avatarSize + (overlap * (count - 1))
    Box(modifier = modifier.size(width = totalWidth, height = avatarSize)) {
        repeat(count) { i ->
            AvatarPlaceholder(
                size = avatarSize,
                modifier = Modifier
                    .offset(x = overlap * i)
                    .align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
private fun AvatarPlaceholder(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SonaraCardElevated),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
