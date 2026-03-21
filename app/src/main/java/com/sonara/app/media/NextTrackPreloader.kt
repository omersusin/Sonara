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
    private val _nextTrack = MutableStateFlow(PreloadResult())
    val nextTrack: StateFlow<PreloadResult> = _nextTrack.asStateFlow()
    private var preloadJob: Job? = null
    private var lastPreloadKey = ""

    /**
     * Try to detect & preload next track from media session queue.
     * Enhanced with detailed debug logging per tespitler.txt item 3.
     */
    fun tryPreload(controller: MediaController?) {
        if (controller == null) {
            SonaraLogger.ai("Preloader: controller=null, skipping")
            return
        }

        val queue = try { controller.queue } catch (e: Exception) {
            SonaraLogger.ai("Preloader: queue access failed: ${e.message}")
            null
        }

        val queueSize = queue?.size ?: 0
        SonaraLogger.ai("Preloader: queue size=$queueSize, pkg=${controller.packageName}")

        if (queue.isNullOrEmpty()) {
            SonaraLogger.ai("Preloader: no queue from player, skipping")
            return
        }

        val metadata = controller.metadata
        if (metadata == null) {
            SonaraLogger.ai("Preloader: no metadata, skipping")
            return
        }

        val currentTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
        val currentArtist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""

        val currentIndex = queue.indexOfFirst { item ->
            val desc = item.description
            desc.title?.toString() == currentTitle
        }

        SonaraLogger.ai("Preloader: currentIndex=$currentIndex, current=$currentTitle")

        if (currentIndex < 0 || currentIndex >= queue.size - 1) {
            SonaraLogger.ai("Preloader: no next track (index=$currentIndex, queueSize=$queueSize)")
            return
        }

        val nextItem = queue[currentIndex + 1]
        val nextTitle = nextItem.description.title?.toString() ?: return
        val nextArtist = nextItem.description.subtitle?.toString() ?: ""
        val key = "${nextTitle.lowercase()}::${nextArtist.lowercase()}"

        if (key == lastPreloadKey && _nextTrack.value.isReady) {
            SonaraLogger.ai("Preloader: already preloaded $nextTitle")
            return
        }
        lastPreloadKey = key

        preloadJob?.cancel()
        preloadJob = scope.launch {
            try {
                SonaraLogger.ai("Preloader: starting preload → $nextTitle - $nextArtist")
                val track = SonaraTrackInfo(nextTitle, nextArtist, "", 0, "")
                val prediction = pipeline.analyze(track)
                _nextTrack.value = PreloadResult(
                    title = nextTitle, artist = nextArtist,
                    prediction = prediction, isReady = true,
                    preloadedAt = System.currentTimeMillis()
                )
                SonaraLogger.ai("Preloader: ready → ${prediction.genre} for $nextTitle")
            } catch (e: Exception) {
                SonaraLogger.w("Preloader", "Preload failed: ${e.message}")
            }
        }
    }

    fun consumeIfMatch(title: String, artist: String): SonaraPrediction? {
        val preloaded = _nextTrack.value
        if (!preloaded.isReady) return null
        if (preloaded.title.lowercase().trim() == title.lowercase().trim() &&
            preloaded.artist.lowercase().trim() == artist.lowercase().trim()) {
            val pred = preloaded.prediction
            _nextTrack.value = PreloadResult()
            lastPreloadKey = ""
            SonaraLogger.ai("Preloader: CONSUMED preloaded prediction for $title")
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
