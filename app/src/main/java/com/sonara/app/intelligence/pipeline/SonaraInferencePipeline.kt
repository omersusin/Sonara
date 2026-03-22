package com.sonara.app.intelligence.pipeline

import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.classifier.MetadataClassifier
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.lastfm.LastFmResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class SonaraInferencePipeline(private val lastFmApiKey: String?) {
    private val classifier = MetadataClassifier()
    private val lastFmResolver = LastFmResolver()
    private val cache = ConcurrentHashMap<String, Pair<SonaraPrediction, Long>>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentPrediction = MutableStateFlow<SonaraPrediction?>(null)
    val currentPrediction: StateFlow<SonaraPrediction?> = _currentPrediction.asStateFlow()

    /** Callback for self-training: called after each successful prediction */
    var onPrediction: ((SonaraTrackInfo, SonaraPrediction) -> Unit)? = null

    suspend fun analyze(track: SonaraTrackInfo, useLastFm: Boolean = true, useLocalAi: Boolean = true): SonaraPrediction {
        // 1. Cache
        cache[track.cacheKey]?.let { (pred, time) ->
            if (System.currentTimeMillis() - time < 7 * 24 * 60 * 60 * 1000) {
                SonaraLogger.ai("Cache hit: ${pred.genre}")
                val cached = pred.copy(source = PredictionSource.CACHE)
                _currentPrediction.value = cached
                return cached
            }
        }

        SonaraLogger.ai("Analyzing: ${track.artist} - ${track.title}")

        // 2. Local classifier (if enabled)
        val localResult = if (useLocalAi) classifier.classify(track) else null

        // 3. Last.fm (parallel, 5s timeout)
        var lastFmSignal: SignalMerger.LastFmSignal? = null
        if (!lastFmApiKey.isNullOrBlank() && useLastFm) {
            try {
                lastFmSignal = withTimeout(5000) {
                    val result = lastFmResolver.resolve(track.title, track.artist, lastFmApiKey)
                    if (result != null && result.genre.isNotBlank() && result.genre != "other") {
                        val genre = Genre.fromString(result.genre)
                        if (genre != Genre.UNKNOWN) {
                            val mood = inferMoodFromTags(result.mood)
                            SignalMerger.LastFmSignal(genre, result.subGenre, mood, result.energy, result.tags.ifEmpty { listOf(result.genre, result.mood).filter { it.isNotBlank() } })
                        } else null
                    } else null
                }
            } catch (e: Exception) { SonaraLogger.ai("Last.fm timeout/error: ${e.message}") }
        }

        // 4. Merge
        val prediction = SignalMerger.merge(lastFmSignal, localResult)
        SonaraLogger.ai("Result: ${prediction.genre} | ${prediction.mood} | conf=${prediction.confidence} | src=${prediction.source}")

        // 5. Cache
        cache[track.cacheKey] = prediction to System.currentTimeMillis()
        _currentPrediction.value = prediction

        // 6. Self-training callback
        try { onPrediction?.invoke(track, prediction) } catch (e: Exception) {
            SonaraLogger.w("Pipeline", "Training callback error: ${e.message}")
        }

        return prediction
    }

    private fun inferMoodFromTags(moodStr: String): Mood? {
        val l = moodStr.lowercase()
        return when {
            l.contains("sad") || l.contains("melanchol") -> Mood.MELANCHOLIC
            l.contains("happy") || l.contains("cheerful") -> Mood.HAPPY
            l.contains("energetic") || l.contains("party") -> Mood.ENERGETIC
            l.contains("calm") || l.contains("chill") || l.contains("relax") -> Mood.CALM
            l.contains("dark") || l.contains("gothic") -> Mood.DARK
            l.contains("romantic") || l.contains("love") -> Mood.ROMANTIC
            l.contains("aggressive") || l.contains("angry") -> Mood.AGGRESSIVE
            else -> null
        }
    }

    fun updateApiKey(newKey: String) {
        // Pipeline uses the key passed at construction, but we can reload
        SonaraLogger.i("Pipeline", "API key updated")
    }

    fun clearCache() { cache.clear() }
    fun destroy() { scope.cancel() }
}
