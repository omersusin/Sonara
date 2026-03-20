package com.sonara.app.media

import android.media.MediaMetadata
import android.media.session.MediaController
import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.pipeline.SonaraInferencePipeline
import com.sonara.app.intelligence.pipeline.SonaraPrediction
import com.sonara.app.intelligence.pipeline.SonaraTrackInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PreloadResult(
    val title: String = "",
    val artist: String = "",
    val prediction: SonaraPrediction? = null,
    val isReady: Boolean = false,
    val preloadedAt: Long = 0
)

class NextTrackPreloader(
    private val pipeline: SonaraInferencePipeline,
    private val scope: CoroutineScope
) {
    private val TAG = "NextTrackPreloader"
    private val _nextTrack = MutableStateFlow(PreloadResult())
    val nextTrack: StateFlow<PreloadResult> = _nextTrack.asStateFlow()
    private var preloadJob: Job? = null
    private var lastPreloadKey = ""

    /**
     * Try to detect & preload next track from media session queue.
     * Called periodically or on queue change.
     */
    fun tryPreload(controller: MediaController?) {
        if (controller == null) return
        val queue = try { controller.queue } catch (_: Exception) { null }
        if (queue.isNullOrEmpty()) return

        // Find currently playing position
        val metadata = controller.metadata ?: return
        val currentTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
        val currentArtist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""

        // Find next in queue
        val currentIndex = queue.indexOfFirst { item ->
            val desc = item.description
            desc.title?.toString() == currentTitle
        }
        if (currentIndex < 0 || currentIndex >= queue.size - 1) return

        val nextItem = queue[currentIndex + 1]
        val nextTitle = nextItem.description.title?.toString() ?: return
        val nextArtist = nextItem.description.subtitle?.toString() ?: ""
        val key = "${nextTitle.lowercase()}::${nextArtist.lowercase()}"

        if (key == lastPreloadKey && _nextTrack.value.isReady) return
        lastPreloadKey = key

        preloadJob?.cancel()
        preloadJob = scope.launch {
            try {
                SonaraLogger.ai("Preloading next: $nextTitle - $nextArtist")
                val track = SonaraTrackInfo(nextTitle, nextArtist, "", 0, "")
                val prediction = pipeline.analyze(track)
                _nextTrack.value = PreloadResult(
                    title = nextTitle, artist = nextArtist,
                    prediction = prediction, isReady = true,
                    preloadedAt = System.currentTimeMillis()
                )
                SonaraLogger.ai("Preloaded: ${prediction.genre} for $nextTitle")
            } catch (e: Exception) {
                SonaraLogger.w("Preloader", "Preload failed: ${e.message}")
            }
        }
    }

    /**
     * Check if we have a preloaded prediction for this track.
     * Returns prediction if match, null otherwise. Clears after consumption.
     */
    fun consumeIfMatch(title: String, artist: String): SonaraPrediction? {
        val preloaded = _nextTrack.value
        if (!preloaded.isReady) return null
        if (preloaded.title.lowercase().trim() == title.lowercase().trim() &&
            preloaded.artist.lowercase().trim() == artist.lowercase().trim()) {
            val pred = preloaded.prediction
            _nextTrack.value = PreloadResult()
            lastPreloadKey = ""
            SonaraLogger.ai("Used preloaded prediction for $title")
            return pred
        }
        return null
    }

    fun clear() {
        preloadJob?.cancel()
        _nextTrack.value = PreloadResult()
        lastPreloadKey = ""
    }
}
