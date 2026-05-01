package com.sonara.app.intelligence.lyrics

import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * LyricsPreloadManager — eagerly fetches lyrics for the upcoming track
 * so that they are already in the in-memory LRU cache when the track starts.
 *
 * Call [preload] when the player signals the next track is about to play.
 * The result is stored in [LyricsHelper]'s internal memCache, so [LyricsHelper.getLyrics]
 * will return immediately on the next call for the same title/artist.
 */
object LyricsPreloadManager {

    private const val TAG = "LyricsPreloadManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null

    /**
     * Starts an async prefetch for [title]/[artist].
     * Any in-flight prefetch for a different track is cancelled first.
     */
    fun preload(
        title: String,
        artist: String,
        album: String = "",
        durationMs: Long = 0L
    ) {
        if (title.isBlank()) return
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                SonaraLogger.d(TAG, "Preloading lyrics for \"$title\" by $artist")
                LyricsHelper.getLyrics(title, artist, album, durationMs)
            } catch (e: Exception) {
                SonaraLogger.w(TAG, "Preload failed for \"$title\": ${e.message}")
            }
        }
    }

    /** Cancels any in-flight prefetch. */
    fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }
}
