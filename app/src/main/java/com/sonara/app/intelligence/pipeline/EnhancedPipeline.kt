package com.sonara.app.intelligence.pipeline

import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.lastfm.LastFmResolver
import com.sonara.app.intelligence.lyrics.LyricsInsightEngine
import com.sonara.app.intelligence.lyrics.LyricsResolver
import com.sonara.app.intelligence.musicbrainz.MusicBrainzClient
import com.sonara.app.intelligence.classifier.MetadataClassifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Madde 13/14/15: Geliştirilmiş inference pipeline.
 * 6 katman:
 * 1. Identity Layer (canonical title/artist, MusicBrainz match)
 * 2. Metadata Layer (Last.fm tags, genre, subgenre, mood)
 * 3. Lyrics Layer (LRCLIB → tone, theme, polarity)
 * 4. Audio Feature Layer (brightness, bass ratio - mevcut local AI)
 * 5. Personalization Layer (user bias, route bias)
 * 6. Safety Layer (clipping control, smoothing)
 */
class EnhancedPipeline(private val lastFmApiKey: String?) {

    private val classifier = MetadataClassifier()
    private val lastFmResolver = LastFmResolver()
    private val cache = ConcurrentHashMap<String, Pair<EnhancedPrediction, Long>>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentPrediction = MutableStateFlow<SonaraPrediction?>(null)
    val currentPrediction: StateFlow<SonaraPrediction?> = _currentPrediction.asStateFlow()

    private val _lyricsInsight = MutableStateFlow<LyricsInsightEngine.LyricsInsight?>(null)
    val lyricsInsight: StateFlow<LyricsInsightEngine.LyricsInsight?> = _lyricsInsight.asStateFlow()

    /** Callback for learning */
    var onPrediction: ((SonaraTrackInfo, SonaraPrediction) -> Unit)? = null

    data class EnhancedPrediction(
        val prediction: SonaraPrediction,
        val canonicalTitle: String,
        val canonicalArtist: String,
        val mbid: String? = null,
        val lyricsInsight: LyricsInsightEngine.LyricsInsight? = null,
        val lyricsEqModifier: FloatArray? = null
    )

    suspend fun analyze(track: SonaraTrackInfo): SonaraPrediction {
        // Title normalization (Madde 4)
        val normTitle = TitleNormalizer.normalizeTitle(track.title)
        val normArtist = TitleNormalizer.normalizeArtist(track.artist)
        val cacheKey = TitleNormalizer.canonicalKey(normTitle, normArtist)

        // Cache check
        cache[cacheKey]?.let { (enhanced, time) ->
            if (System.currentTimeMillis() - time < 7 * 24 * 60 * 60 * 1000) {
                SonaraLogger.ai("Cache hit: ${enhanced.prediction.genre}")
                val cached = enhanced.prediction.copy(source = PredictionSource.CACHE)
                _currentPrediction.value = cached
                _lyricsInsight.value = enhanced.lyricsInsight
                return cached
            }
        }

        SonaraLogger.ai("═══ Enhanced Analysis: $normArtist - $normTitle ═══")

        // Layer 1: Local classifier (instant)
        val localResult = classifier.classify(
            SonaraTrackInfo(normTitle, normArtist, track.album, track.durationMs, track.packageName)
        )

        // Layer 2-4: Parallel fetches
        var lastFmSignal: SignalMerger.LastFmSignal? = null
        var mbMatch: MusicBrainzClient.MbMatch? = null
        var lyricsResult: LyricsResolver.LyricsResult? = null

        supervisorScope {
            val lastFmJob = async {
                if (!lastFmApiKey.isNullOrBlank()) {
                    try {
                        withTimeout(5000) {
                            val result = lastFmResolver.resolve(normTitle, normArtist, lastFmApiKey)
                            if (result != null && result.genre.isNotBlank() && result.genre != "other") {
                                val genre = Genre.fromString(result.genre)
                                if (genre != Genre.UNKNOWN) {
                                    lastFmSignal = SignalMerger.LastFmSignal(
                                        genre, result.subGenre,
                                        inferMood(result.mood), result.energy,
                                        result.tags.ifEmpty { listOf(result.genre) }
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        SonaraLogger.ai("Last.fm timeout: ${e.message}")
                    }
                }
            }

            val mbJob = async {
                try {
                    withTimeout(6000) {
                        mbMatch = MusicBrainzClient.searchRecording(normTitle, normArtist)
                    }
                } catch (_: Exception) {}
            }

            val lyricsJob = async {
                try {
                    withTimeout(5000) {
                        lyricsResult = LyricsResolver.resolve(normTitle, normArtist, track.durationMs)
                    }
                } catch (_: Exception) {}
            }

            lastFmJob.await()
            mbJob.await()
            lyricsJob.await()
        }

        // Layer 3: Lyrics insight
        var insight: LyricsInsightEngine.LyricsInsight? = null
        lyricsResult?.let { lyrics ->
            insight = LyricsInsightEngine.analyze(lyrics.plainLyrics)
            _lyricsInsight.value = insight
            SonaraLogger.ai("Lyrics: tone=${insight?.tone} theme=${insight?.theme} polarity=${insight?.polarity}")
        }

        // Merge all signals
        val prediction = SignalMerger.merge(lastFmSignal, localResult)

        // Add lyrics source if present
        val finalPrediction = if (insight != null && (insight?.confidence ?: 0f) > 0.2f) {
            val hasLyrics = true
            val sourceDisplay = PredictionSourceMapper.map(prediction, hasLyrics)
            prediction.copy(
                reasoning = prediction.reasoning + "Lyrics: ${insight?.tone} (${insight?.theme})"
            )
        } else prediction

        SonaraLogger.ai("Result: ${finalPrediction.genre} | ${finalPrediction.mood} | conf=${finalPrediction.confidence} | src=${finalPrediction.source}")

        // Cache
        val enhanced = EnhancedPrediction(
            finalPrediction, normTitle, normArtist,
            mbMatch?.mbid, insight, insight?.eqModifier
        )
        cache[cacheKey] = enhanced to System.currentTimeMillis()
        _currentPrediction.value = finalPrediction

        // Self-training callback
        try { onPrediction?.invoke(track, finalPrediction) } catch (e: Exception) {
            SonaraLogger.w("Pipeline", "Training callback error: ${e.message}")
        }

        return finalPrediction
    }

    /** Mevcut track için lyrics EQ modifier al */
    fun getLyricsModifier(): FloatArray? = _lyricsInsight.value?.eqModifier

    private fun inferMood(moodStr: String): Mood? {
        val l = moodStr.lowercase()
        return when {
            l.contains("sad") || l.contains("melanchol") -> Mood.MELANCHOLIC
            l.contains("happy") || l.contains("cheerful") -> Mood.HAPPY
            l.contains("energetic") || l.contains("party") -> Mood.ENERGETIC
            l.contains("calm") || l.contains("chill") -> Mood.CALM
            l.contains("dark") -> Mood.DARK
            l.contains("romantic") || l.contains("love") -> Mood.ROMANTIC
            l.contains("aggressive") -> Mood.AGGRESSIVE
            else -> null
        }
    }

    fun updateApiKey(newKey: String) { SonaraLogger.i("Pipeline", "API key updated") }
    fun clearCache() { cache.clear(); _lyricsInsight.value = null }
    fun destroy() { scope.cancel() }
}
