package com.sonara.app.intelligence.media

import android.util.Log
import com.sonara.app.intelligence.pipeline.MediaType

object MediaSourceDetector {
    private const val TAG = "MediaSourceDetector"

    private val VIDEO_PACKAGES = setOf(
        "com.netflix.mediaclient",
        "com.google.android.youtube",
        "com.disney.disneyplus",
        "tv.twitch.android.app",
        "com.amazon.avod",
        "com.hbo.hbonow",
        "com.hulu.plus",
        "org.videolan.vlc",
        "com.mxtech.videoplayer.ad",
        "com.mxtech.videoplayer.pro"
    )

    private val MUSIC_PACKAGES = setOf(
        "com.google.android.apps.youtube.music",
        "com.spotify.music",
        "com.apple.android.music",
        "com.amazon.mp3",
        "deezer.android.app",
        "com.soundcloud.android",
        "com.tidal.android",
        "com.jrtstudio.AnotherMusicPlayer",
        "code.name.monkey.retromusic",
        "com.maxmpz.audioplayer"
    )

    private val PODCAST_PACKAGES = setOf(
        "com.google.android.apps.podcasts",
        "au.com.shiftyjelly.pocketcasts",
        "fm.castbox.audiobook.radio.podcast",
        "com.bambuna.podcastaddict",
        "com.podcast.podcasts"
    )

    fun detect(packageName: String): MediaType {
        val result = when {
            MUSIC_PACKAGES.contains(packageName) -> MediaType.MUSIC
            VIDEO_PACKAGES.contains(packageName) -> MediaType.VIDEO
            PODCAST_PACKAGES.contains(packageName) -> MediaType.PODCAST
            packageName.contains("music") -> MediaType.MUSIC
            packageName.contains("video") || packageName.contains("player") -> MediaType.VIDEO
            packageName.contains("podcast") -> MediaType.PODCAST
            packageName.contains("audiobook") -> MediaType.AUDIOBOOK
            else -> MediaType.UNKNOWN
        }
        Log.d(TAG, "detect($packageName) → $result")
        return result
    }
}
