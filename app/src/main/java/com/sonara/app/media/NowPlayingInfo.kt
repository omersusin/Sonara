package com.sonara.app.media

data class NowPlayingInfo(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val packageName: String = "",
    val isPlaying: Boolean = false,
    val duration: Long = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
) {
    val hasTrack: Boolean get() = title.isNotBlank()
    val displayTitle: String get() = title.ifBlank { "No music playing" }
    val displayArtist: String get() = artist.ifBlank { "" }
}
