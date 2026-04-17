/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sonara.app.intelligence.local

data class MediaSource(val type: MediaType, val appName: String, val packageName: String)

enum class MediaType { MUSIC, VIDEO, PODCAST, AUDIOBOOK, GAME, CALL, UNKNOWN }

data class VideoEqPreset(val name: String, val bands: FloatArray, val bassBoost: Int, val virtualizer: Int, val loudness: Int, val reasoning: String) {
    override fun equals(other: Any?): Boolean { if (this === other) return true; if (other !is VideoEqPreset) return false; return name == other.name && bands.contentEquals(other.bands) }
    override fun hashCode() = name.hashCode()
}

object MediaSourceDetector {
    private val pkgMap = mapOf(
        "com.spotify.music" to MediaType.MUSIC, "com.google.android.apps.youtube.music" to MediaType.MUSIC,
        "com.apple.android.music" to MediaType.MUSIC, "deezer.android.app" to MediaType.MUSIC,
        "com.amazon.mp3" to MediaType.MUSIC, "com.aspiro.tidal" to MediaType.MUSIC,
        "com.soundcloud.android" to MediaType.MUSIC, "com.sec.android.app.music" to MediaType.MUSIC,
        "com.miui.player" to MediaType.MUSIC, "com.maxmpz.audioplayer" to MediaType.MUSIC,
        "org.videolan.vlc" to MediaType.MUSIC, "code.name.monkey.retromusic" to MediaType.MUSIC,
        "com.anghami" to MediaType.MUSIC,

        "com.google.android.youtube" to MediaType.VIDEO, "app.revanced.android.youtube" to MediaType.VIDEO,
        "com.netflix.mediaclient" to MediaType.VIDEO, "com.disney.disneyplus" to MediaType.VIDEO,
        "com.amazon.avod.thirdpartyclient" to MediaType.VIDEO, "com.hbo.hbonow" to MediaType.VIDEO,
        "com.twitch.android.app" to MediaType.VIDEO, "com.mxtech.videoplayer" to MediaType.VIDEO,
        "tv.blutv.android" to MediaType.VIDEO, "com.exxen.exxen" to MediaType.VIDEO,
        "tr.gov.trt.tabii" to MediaType.VIDEO, "tr.com.puhutv" to MediaType.VIDEO,

        "com.google.android.apps.podcasts" to MediaType.PODCAST, "com.bambuna.podcastaddict" to MediaType.PODCAST,
        "au.com.shiftyjelly.pocketcasts" to MediaType.PODCAST,
        "com.audible.application" to MediaType.AUDIOBOOK
    )

    fun detect(pkg: String): MediaSource {
        val type = pkgMap[pkg]
        if (type != null) return MediaSource(type, pkg.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg, pkg)
        val lower = pkg.lowercase()
        val guessed = when {
            lower.contains("music") || lower.contains("player") || lower.contains("audio") -> MediaType.MUSIC
            lower.contains("video") || lower.contains("movie") || lower.contains("tv") -> MediaType.VIDEO
            lower.contains("podcast") || lower.contains("radio") -> MediaType.PODCAST
            lower.contains("book") -> MediaType.AUDIOBOOK
            else -> MediaType.UNKNOWN
        }
        return MediaSource(guessed, pkg, pkg)
    }

    fun suggestEqForMediaType(type: MediaType) = when (type) {
        MediaType.VIDEO -> VideoEqPreset("Video/Film", floatArrayOf(-2f,-1f,0f,1.5f,3f,3.5f,3f,2f,1f,0f), 100, 400, 500, "Dialog clarity with surround for movies")
        MediaType.PODCAST -> VideoEqPreset("Podcast", floatArrayOf(-3f,-2f,-0.5f,2f,4f,5f,4f,2f,0f,-1.5f), 0, 0, 800, "Voice-optimized with maximum clarity")
        MediaType.AUDIOBOOK -> VideoEqPreset("Audiobook", floatArrayOf(-3f,-2f,-1f,1.5f,3.5f,4f,3f,1.5f,0f,-2f), 0, 0, 600, "Warm voice for long listening")
        MediaType.GAME -> VideoEqPreset("Gaming", floatArrayOf(3f,2.5f,1f,0f,-0.5f,0f,1.5f,3f,4f,3.5f), 250, 500, 300, "Positional audio with bass impact")
        MediaType.CALL -> VideoEqPreset("Voice Call", floatArrayOf(-4f,-3f,-1f,2f,4f,5f,4.5f,2f,-1f,-3f), 0, 0, 1000, "Maximum voice intelligibility")
        else -> VideoEqPreset("Default", FloatArray(10), 0, 0, 0, "Flat — unknown source")
    }
}
