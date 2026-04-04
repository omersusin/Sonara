package com.sonara.app.ai

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.sonara.app.ai.classifier.EmbeddedPrototypes
import com.sonara.app.ai.classifier.KnnClassifier
import com.sonara.app.ai.cloud.CloudLearningManager
import com.sonara.app.ai.eq.SmartEqGenerator
import com.sonara.app.ai.explanation.ExplanationBuilder
import com.sonara.app.ai.extraction.AudioCapture
import com.sonara.app.ai.extraction.FeatureExtractor
import com.sonara.app.ai.bridge.AudioSessionTracker
import com.sonara.app.ai.models.*
import com.sonara.app.ai.personalization.Personalizer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SonaraAi private constructor(
    private val context: Context,
    private val dao: TrainingExampleDao
) {
    companion object {
        private const val TAG = "SonaraAi"
        private const val LISTEN_MS = 6000L
        private const val RE_ANALYZE_MS = 45000L
        @Volatile private var INSTANCE: SonaraAi? = null

        fun create(context: Context, dao: TrainingExampleDao): SonaraAi {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SonaraAi(context.applicationContext, dao).also { INSTANCE = it }
            }
        }
        fun getInstance(): SonaraAi? = INSTANCE
    }

    private val capture = AudioCapture()
    private val extractor = FeatureExtractor()
    private val classifier = KnnClassifier(dao)
    private val eqGen = SmartEqGenerator()
    private val personalizer = Personalizer(context)
    val cloudManager = CloudLearningManager(context, classifier, dao)

    private val _state = MutableStateFlow(SonaraAiState())
    val state: StateFlow<SonaraAiState> = _state.asStateFlow()

    private val _visualizerData = MutableStateFlow<FloatArray?>(null)
    val visualizerData: StateFlow<FloatArray?> = _visualizerData.asStateFlow()

    private var currentFeatures: AudioFeatureVector? = null
    private var analysisJob: Job? = null
    private var reAnalysisJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun initialize() {
        Log.d(TAG, "Initializing...")
        if (dao.getCount() == 0) { classifier.loadPrototypes(EmbeddedPrototypes.getAll()) }
        classifier.refreshCache()
        cloudManager.scheduleSync()
        _state.value = _state.value.copy(learnedCount = classifier.getLearnedCount(), isReady = true)
        Log.d(TAG, "Ready. ${dao.getCount()} examples")
    }

    fun onTrackChanged(title: String, artist: String, albumArt: String? = null, audioSessionId: Int? = null) {
        Log.d(TAG, "Track: $title by $artist (session: $audioSessionId)")
        analysisJob?.cancel(); reAnalysisJob?.cancel(); currentFeatures = null
        _state.value = SonaraAiState(title = title, artist = artist, albumArt = albumArt,
            status = AiStatus.LISTENING, isReady = true, learnedCount = _state.value.learnedCount, route = _state.value.route)
        analysisJob = scope.launch { analyze(title, artist, audioSessionId) }
    }

    fun onSessionChanged(sessionId: Int) { Log.d(TAG, "Session changed: $sessionId"); capture.attach(sessionId) }

    fun onFeedback(type: String) {
        val current = _state.value.result ?: return
        scope.launch {
            if (type == "perfect") {
                val f = currentFeatures ?: return@launch
                classifier.learn(f, current.primaryGenre.lowercase(), current.mood, current.energy, "user_confirmed", _state.value.title, _state.value.artist)
                cloudManager.addContribution(f, current.primaryGenre.lowercase(), current.mood, current.energy, "confirmed")
            } else if (type.startsWith("custom:")) {
                val text = type.removePrefix("custom:").trim()
                Log.d(TAG, "Custom AI feedback: '$text'")
                try {
                    val app = com.sonara.app.SonaraApp.instance
                    val request = com.sonara.app.intelligence.provider.InsightRequest(
                        title = _state.value.title, artist = _state.value.artist,
                        genre = current.primaryGenre, subGenre = null,
                        tags = current.genres.keys.toList(), lyricalTone = text,
                        energy = current.energy, confidence = current.confidence,
                        currentEqBands = current.eqBands)
                    val result = app.insightManager.getInsight(request)
                    if (result.success) Log.d(TAG, "AI response: ${result.summary}")
                } catch (e: Exception) { Log.w(TAG, "AI call failed: ${e.message}") }
                val mapped = mapCustomFeedback(text.lowercase())
                personalizer.recordFeedback(mapped, current, _state.value.route)
                regenerateEq(current)
            } else {
                personalizer.recordFeedback(type, current, _state.value.route)
                regenerateEq(current)
            }
            _state.value = _state.value.copy(learnedCount = classifier.getLearnedCount())
        }
    }

    private fun mapCustomFeedback(text: String): String = when {
        text.contains("bass") && (text.contains("more") || text.contains("fazla")) -> "more_bass"
        text.contains("bass") && (text.contains("less") || text.contains("az")) -> "too_bassy"
        text.contains("treble") || text.contains("tiz") || text.contains("bright") -> "too_bright"
        text.contains("vocal") || text.contains("vokal") -> "more_vocal"
        text.contains("warm") || text.contains("sıcak") -> "prefer_warmer"
        text.contains("clear") || text.contains("temiz") -> "prefer_clearer"
        text.contains("harsh") || text.contains("sert") -> "too_harsh"
        text.contains("thin") || text.contains("ince") -> "too_thin"
        text.contains("muddy") || text.contains("bulanık") -> "too_muddy"
        text.contains("flat") || text.contains("düz") -> "too_flat"
        else -> "prefer_clearer"
    }

    fun onGenreCorrection(correctedGenre: String) {
        val f = currentFeatures ?: return; val current = _state.value.result ?: return
        scope.launch {
            classifier.learn(f, correctedGenre, current.mood, current.energy, "user_corrected", _state.value.title, _state.value.artist)
            classifier.refreshCache()
            cloudManager.addContribution(f, correctedGenre.lowercase(), current.mood, current.energy, "corrected")
            val updated = current.copy(genres = mapOf(correctedGenre.lowercase() to 1.0f))
            _state.value = _state.value.copy(result = updated)
            regenerateEq(updated)
            _state.value = _state.value.copy(learnedCount = classifier.getLearnedCount())
        }
    }

    fun onRouteChanged(routeType: String) {
        _state.value = _state.value.copy(route = routeType)
        _state.value.result?.let { scope.launch { regenerateEq(it) } }
    }

    fun setEqEnabled(enabled: Boolean) { _state.value = _state.value.copy(eqEnabled = enabled) }

    fun release() {
        _visualizerData.value = null
        analysisJob?.cancel(); reAnalysisJob?.cancel(); capture.release(); scope.cancel() }


    private fun hasRecordPermission(): Boolean {
        val granted = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "RECORD_AUDIO permission: $granted")
        return granted
    }

    private suspend fun tryAttachAudio(sessionId: Int?): Boolean {
        if (!hasRecordPermission()) {
            Log.w(TAG, "Cannot attach — RECORD_AUDIO permission not granted")
            return false
        }
        // Strategy 1: provided session ID
        if (sessionId != null && sessionId > 0) {
            Log.d(TAG, "Trying provided session: $sessionId")
            if (capture.attach(sessionId)) return true
        }
        // Strategy 2: global output mix (session 0)
        Log.d(TAG, "Trying global output mix (session 0)")
        if (capture.attach(0)) return true
        // Strategy 3: wait for AudioSessionTracker
        Log.d(TAG, "Waiting 5s for session from AudioSessionTracker...")
        delay(5000)
        val tracked = AudioSessionTracker.get()
        if (tracked > 0) {
            Log.d(TAG, "Tracker provided session: $tracked")
            if (capture.attach(tracked)) return true
        }
        Log.w(TAG, "All audio attach strategies failed")
        return false
    }

    private fun parseFFTMagnitudes(fftBytes: ByteArray): FloatArray {
        // FFT format: [DC, real0, imag0, real1, imag1, ...]
        val n = (fftBytes.size - 1) / 2
        if (n <= 0) return FloatArray(0)
        val magnitudes = FloatArray(n)
        var maxMag = 1f
        for (i in 0 until n) {
            val real = fftBytes[1 + 2 * i].toFloat()
            val imag = fftBytes[2 + 2 * i].toFloat()
            val mag = kotlin.math.sqrt(real * real + imag * imag)
            magnitudes[i] = mag
            if (mag > maxMag) maxMag = mag
        }
        // Normalize to 0-1
        for (i in magnitudes.indices) {
            magnitudes[i] = magnitudes[i] / maxMag
        }
        return magnitudes
    }

    fun updateVisualizerFromCapture() {
        val frames = capture.getFFTFrames()
        if (frames.isEmpty()) return
        // Smooth: average last 4 frames to reduce jitter
        val recent = frames.takeLast(4).map { parseFFTMagnitudes(it) }.filter { it.isNotEmpty() }
        if (recent.isEmpty()) return
        val n = recent.first().size
        val avg = FloatArray(n)
        for (frame in recent) { for (i in 0 until minOf(n, frame.size)) avg[i] += frame[i] }
        for (i in avg.indices) avg[i] /= recent.size
        _visualizerData.value = avg
    }



    private suspend fun analyze(title: String, artist: String, sessionId: Int?) {
        Log.d(TAG, "analyze() start — title=$title, artist=$artist, sessionId=$sessionId")
        if (tryAttachAudio(sessionId)) {
            capture.clearBuffers(); _state.value = _state.value.copy(status = AiStatus.LISTENING)

            // Start visualizer data feed immediately (during listen + after)
            val vizJob = scope.launch {
                while (true) {
                    updateVisualizerFromCapture()
                    delay(50) // ~20fps
                }
            }

            delay(LISTEN_MS); _state.value = _state.value.copy(status = AiStatus.ANALYZING)
            val features = extractor.extract(capture.getFFTFrames(), capture.getWaveFrames())
            if (features != null) {
                currentFeatures = features; val result = classifier.classify(features)
                val eq = eqGen.generate(result, _state.value.route)
                val pEq = personalizer.applyPersonalization(eq, result, _state.value.route)
                val explanation = ExplanationBuilder.build(result, pEq, title, artist)
                val finalResult = result.copy(eqBands = pEq.bands, eqPreamp = pEq.preamp, isSpectralEq = pEq.isSpectralBased, explanation = explanation)
                _state.value = _state.value.copy(result = finalResult, status = AiStatus.COMPLETE)
                startReAnalysis()
            } else { _state.value = _state.value.copy(status = AiStatus.LIMITED) }
        } else { _state.value = _state.value.copy(status = AiStatus.UNAVAILABLE) }
    }

    private fun startReAnalysis() {
        reAnalysisJob?.cancel()
        reAnalysisJob = scope.launch {
            while (isActive) {
                delay(RE_ANALYZE_MS)
                val features = extractor.extract(capture.getFFTFrames(), capture.getWaveFrames())
                if (features != null) {
                    currentFeatures = features; val result = classifier.classify(features)
                    val eq = eqGen.generate(result, _state.value.route)
                    val pEq = personalizer.applyPersonalization(eq, result, _state.value.route)
                    val explanation = ExplanationBuilder.build(result, pEq, _state.value.title, _state.value.artist)
                    val finalResult = result.copy(eqBands = pEq.bands, eqPreamp = pEq.preamp, isSpectralEq = pEq.isSpectralBased, explanation = explanation)
                    _state.value = _state.value.copy(result = finalResult)
                }
            }
        }
    }

    private suspend fun regenerateEq(result: SonaraAiResult) {
        val eq = eqGen.generate(result, _state.value.route)
        val pEq = personalizer.applyPersonalization(eq, result, _state.value.route)
        val explanation = ExplanationBuilder.build(result, pEq, _state.value.title, _state.value.artist)
        val updated = result.copy(eqBands = pEq.bands, eqPreamp = pEq.preamp, isSpectralEq = pEq.isSpectralBased, explanation = explanation)
        _state.value = _state.value.copy(result = updated)
    }
}

data class SonaraAiState(
    val title: String = "", val artist: String = "", val albumArt: String? = null,
    val result: SonaraAiResult? = null, val status: AiStatus = AiStatus.IDLE,
    val route: String = "unknown", val eqEnabled: Boolean = true,
    val isReady: Boolean = false, val learnedCount: Int = 0
)

enum class AiStatus(val display: String) {
    IDLE("Ready"), LISTENING("Listening..."), ANALYZING("Analyzing audio..."),
    COMPLETE("Analysis complete"), LIMITED("Using track info"), UNAVAILABLE("No audio source")
}
