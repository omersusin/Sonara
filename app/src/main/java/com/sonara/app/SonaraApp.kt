package com.sonara.app

import android.app.Application
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.sonara.app.audio.engine.SafetyLimiter
import com.sonara.app.audio.engine.SmoothTransitionEngine
import com.sonara.app.autoeq.AutoEqManager
import com.sonara.app.autoeq.HeadphoneDetector
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.SonaraLogger
import com.sonara.app.data.models.SharedEqState
import com.sonara.app.data.preferences.SecureSecrets
import com.sonara.app.data.preferences.SonaraPreferences
import com.sonara.app.engine.classifier.AdaptiveGenreClassifier
import com.sonara.app.engine.eq.AudioSessionManager
import com.sonara.app.engine.eq.EqComposer
import com.sonara.app.intelligence.adaptive.AdaptiveLearningEngine
import com.sonara.app.intelligence.adaptive.PersonalizationEngine
import com.sonara.app.intelligence.gemini.GeminiInsightEngine
import com.sonara.app.intelligence.provider.InsightProviderManager
import com.sonara.app.intelligence.provider.InsightRequest
import com.sonara.app.intelligence.provider.InsightResult
import com.sonara.app.intelligence.lastfm.LastFmAuthManager
import com.sonara.app.intelligence.lastfm.ScrobbleWorker
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.intelligence.pipeline.*
import com.sonara.app.media.NextTrackPreloader
import com.sonara.app.preset.PresetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SonaraApp : Application() {
    lateinit var preferences: SonaraPreferences private set
    lateinit var secureSecrets: SecureSecrets private set
    lateinit var database: SonaraDatabase private set
    lateinit var presetRepository: PresetRepository private set
    lateinit var audioSessionManager: AudioSessionManager private set
    lateinit var eqComposer: EqComposer private set
    lateinit var adaptiveLearning: AdaptiveLearningEngine private set
    lateinit var adaptiveClassifier: AdaptiveGenreClassifier private set
    lateinit var inferencePipeline: SonaraInferencePipeline private set
    lateinit var nextTrackPreloader: NextTrackPreloader private set
    lateinit var smoothTransitionEngine: SmoothTransitionEngine private set
    lateinit var lastFmAuth: LastFmAuthManager private set
    lateinit var personalization: PersonalizationEngine private set

    // Madde 14 FIX: AutoEQ bileşenleri
    lateinit var autoEqManager: AutoEqManager private set
    lateinit var headphoneDetector: HeadphoneDetector private set

    // Madde 10 FIX: Gemini engine instance
    lateinit var geminiEngine: GeminiInsightEngine private set
    lateinit var insightManager: InsightProviderManager private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _eqState = MutableStateFlow(SharedEqState())
    val eqState: StateFlow<SharedEqState> = _eqState.asStateFlow()
    private val _currentRoute = MutableStateFlow(AudioRoute.UNKNOWN)
    val currentRoute: StateFlow<AudioRoute> = _currentRoute.asStateFlow()
    private val _currentProfile = MutableStateFlow(FinalEqProfile.neutral())
    val currentProfile: StateFlow<FinalEqProfile> = _currentProfile.asStateFlow()

    // Madde 10: Gemini insight state (NLS ve Dashboard'da kullanılır)
    private val _geminiInsight = MutableStateFlow<GeminiInsightEngine.GeminiInsight?>(null)
    val geminiInsight: StateFlow<GeminiInsightEngine.GeminiInsight?> = _geminiInsight.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        SonaraLogger.init(this)
        preferences = SonaraPreferences(this)
        secureSecrets = SecureSecrets(this)
        database = SonaraDatabase.get(this)
        presetRepository = PresetRepository(database.presetDao())

        audioSessionManager = AudioSessionManager(this)
        audioSessionManager.start()

        eqComposer = EqComposer()
        adaptiveLearning = AdaptiveLearningEngine(this)
        adaptiveClassifier = AdaptiveGenreClassifier(this)
        smoothTransitionEngine = SmoothTransitionEngine()
        appScope.launch { adaptiveLearning.load() }

        lastFmAuth = LastFmAuthManager(this)
        personalization = PersonalizationEngine(this)
        appScope.launch { personalization.load() }
        ScrobbleWorker.schedule(this)

        // Madde 14 FIX: AutoEQ başlat
        autoEqManager = AutoEqManager()
        headphoneDetector = HeadphoneDetector(this)
        headphoneDetector.start()
        appScope.launch {
            headphoneDetector.headphone.collect { hp ->
                val autoEqOn = preferences.autoEqEnabledFlow.first()
                autoEqManager.onHeadphoneChanged(hp, autoEqOn, this@SonaraApp)
                SonaraLogger.i("AutoEQ", "Headphone: ${hp.name} autoEQ=$autoEqOn active=${autoEqManager.state.value.isActive}")
            }
        }

        // Madde 10 FIX: Gemini engine
        val geminiKey = runBlocking { preferences.geminiApiKeyFlow.first() }.ifBlank { BuildConfig.GEMINI_API_KEY }
        geminiEngine = GeminiInsightEngine(geminiKey)
        insightManager = InsightProviderManager()
        insightManager.configureGemini(geminiEngine)

        // Configure OpenRouter/Groq from prefs
        runBlocking {
            val orKey = preferences.openRouterApiKeyFlow.first()
            val orModel = preferences.openRouterModelFlow.first()
            val grKey = preferences.groqApiKeyFlow.first()
            val grModel = preferences.groqModelFlow.first()
            val provider = preferences.aiProviderFlow.first()
            insightManager.configureOpenRouter(orKey, orModel)
            insightManager.configureGroq(grKey, grModel)
            insightManager.setPrimary(provider)
        }
        appScope.launch {
            preferences.geminiModelFlow.collect { m ->
                geminiEngine.model = when (m) {
                    "balanced" -> GeminiInsightEngine.GeminiModel.BALANCED
                    "strong" -> GeminiInsightEngine.GeminiModel.STRONG
                    else -> GeminiInsightEngine.GeminiModel.FAST
                }
            }
        }

        val apiKey = secureSecrets.getLastFmApiKey().takeIf { it.isNotBlank() }
            ?: runBlocking { preferences.lastFmApiKeyFlow.first() }

        inferencePipeline = SonaraInferencePipeline(apiKey.takeIf { it.isNotBlank() })
        nextTrackPreloader = NextTrackPreloader(inferencePipeline, appScope)

        inferencePipeline.onPrediction = { track, prediction ->
            if (prediction.source == PredictionSource.LASTFM || prediction.source == PredictionSource.MERGED) {
                adaptiveClassifier.train(prediction.genre.name.lowercase(), track.title, track.artist, track.album)
            }
            appScope.launch {
                try {
                    val info = com.sonara.app.data.models.TrackInfo(
                        title = track.title, artist = track.artist,
                        genre = prediction.genre.name, mood = prediction.mood.name,
                        energy = prediction.energy, confidence = prediction.confidence,
                        source = prediction.source.name
                    )
                    val cache = TrackCache(database.trackCacheDao())
                    cache.put(info)
                    if (cache.size() % 10 == 0) com.sonara.app.ai.SonaraAi.getInstance()?.cloudManager?.syncNow()
                } catch (_: Exception) {}
            }
        }

        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        detectRoute(am)
        am.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(a: Array<out AudioDeviceInfo>) {
                detectRoute(am)
                Handler(Looper.getMainLooper()).postDelayed({ audioSessionManager.reinitialize() }, 500)
            }
            override fun onAudioDevicesRemoved(r: Array<out AudioDeviceInfo>) {
                detectRoute(am)
                Handler(Looper.getMainLooper()).postDelayed({ audioSessionManager.reinitialize() }, 500)
            }
        }, Handler(Looper.getMainLooper()))

        SonaraLogger.i("App", "Sonara started. EQ: ${audioSessionManager.activeStrategy.value}")
        appScope.launch { presetRepository.initBuiltIns(); TrackCache(database.trackCacheDao()).cleanup() }
    }

    /**
     * SINGLE ENTRY: Prediction → EQ (reads ALL toggles)
     * Madde 11 FIX: lyricsModifier parametresi eklendi
     * Madde 14 FIX: AutoEQ correction bands uygulanıyor
     */
    fun applyFromPrediction(prediction: SonaraPrediction, lyricsModifier: FloatArray? = null) {
        val route = _currentRoute.value
        val userOffset = try {
            personalization.getPersonalOffset(prediction.genre, route) ?: adaptiveLearning.getOffset(prediction.genre, route)
        } catch (e: Exception) {
            SonaraLogger.w("App", "Offset error (safe fallback): ${e.message}")
            null
        }
        val profile = eqComposer.compose(prediction, route, userOffset, lyricsModifier)

        // ─── Madde 14 FIX: AutoEQ correction ───
        val autoEqEnabled = runBlocking { preferences.autoEqEnabledFlow.first() }
        val autoEqState = autoEqManager.state.value
        var correctedBands = profile.bands.copyOf()
        if (autoEqEnabled && autoEqState.isActive) {
            val correction = autoEqState.correctionBands
            for (i in correctedBands.indices) {
                correctedBands[i] = (correctedBands[i] + correction.getOrElse(i) { 0f }).coerceIn(-12f, 12f)
            }
            SonaraLogger.i("AutoEQ", "Applied correction: ${autoEqState.profile?.name}")
        }

        // ─── Safety Limiter (toggle-aware) ───
        val useSafety = runBlocking { preferences.safetyLimiterFlow.first() }
        val finalBands: FloatArray
        val finalPreamp: Float
        if (useSafety) {
            val (sb, sp) = SafetyLimiter.limit(correctedBands, profile.preamp)
            finalBands = sb; finalPreamp = sp
        } else {
            finalBands = correctedBands; finalPreamp = profile.preamp
        }
        val clipping = SafetyLimiter.wouldClip(correctedBands, profile.preamp)

        // ─── Smooth Transitions (toggle-aware) ───
        val useSmooth = runBlocking { preferences.smoothTransitionsFlow.first() }

        if (useSmooth) {
            val oldBands = _currentProfile.value.bands.copyOf()
            val adjustedTarget = FloatArray(finalBands.size) { (finalBands[it] + finalPreamp).coerceIn(-12f, 12f) }
            appScope.launch {
                smoothTransitionEngine.transition(oldBands, adjustedTarget) { step ->
                    audioSessionManager.applyBands(step)
                }
                audioSessionManager.applyEffects(profile.bassBoost, profile.virtualizer, profile.loudness)
            }
        } else {
            val adjusted = FloatArray(finalBands.size) { (finalBands[it] + finalPreamp).coerceIn(-12f, 12f) }
            audioSessionManager.applyBands(adjusted)
            audioSessionManager.applyEffects(profile.bassBoost, profile.virtualizer, profile.loudness)
        }

        _currentProfile.value = profile
        _eqState.update {
            it.copy(
                bands = finalBands.take(10).toFloatArray(),
                presetName = "AI: ${profile.prediction.genre.displayName}",
                isManualPreset = false,
                bassBoost = profile.bassBoost,
                virtualizer = profile.virtualizer,
                loudness = profile.loudness
            )
        }

        SonaraLogger.eq("Applied: ${profile.prediction.genre} smooth=$useSmooth safety=$useSafety clip=$clipping autoEQ=${autoEqEnabled && autoEqState.isActive} preamp=${"%.1f".format(finalPreamp)}")
    }

    fun applyProfile(profile: FinalEqProfile) {
        val useSafety = runBlocking { preferences.safetyLimiterFlow.first() }
        val (safeBands, safePreamp) = if (useSafety) SafetyLimiter.limit(profile.bands, profile.preamp) else profile.bands to profile.preamp
        val adjusted = FloatArray(safeBands.size) { (safeBands[it] + safePreamp).coerceIn(-12f, 12f) }
        audioSessionManager.applyBands(adjusted)
        audioSessionManager.applyEffects(profile.bassBoost, profile.virtualizer, profile.loudness)
        _currentProfile.value = profile
        _eqState.update {
            it.copy(bands = safeBands.take(10).toFloatArray(), presetName = "AI: ${profile.prediction.genre.displayName}",
                isManualPreset = false, bassBoost = profile.bassBoost, virtualizer = profile.virtualizer, loudness = profile.loudness)
        }
    }

    fun applyManualBands(bands: FloatArray) {
        audioSessionManager.applyBands(bands)
        val pred = _currentProfile.value.prediction
        _eqState.update { it.copy(bands = bands, presetName = "Custom", isManualPreset = true) }
        if (pred.confidence > 0f) {
            appScope.launch {
                adaptiveLearning.recordFeedback(pred.genre, _currentRoute.value, _currentProfile.value.bands, bands)
                personalization.recordAdjustment(pred.genre, _currentRoute.value, pred.subGenre, _currentProfile.value.bands, bands)
            }
        }
    }

    fun applyEq(bands: FloatArray, presetName: String = "Custom", manual: Boolean = true,
                bassBoost: Int = 0, virtualizer: Int = 0, loudness: Int = 0, preamp: Float = 0f) {
        val adj = if (preamp != 0f) FloatArray(bands.size) { (bands[it] + preamp).coerceIn(-12f, 12f) } else bands
        audioSessionManager.applyBands(adj)
        audioSessionManager.applyEffects(bassBoost, virtualizer, loudness)
        _eqState.update {
            it.copy(bands = bands, preamp = preamp, presetName = presetName, isManualPreset = manual,
                bassBoost = bassBoost, virtualizer = virtualizer, loudness = loudness)
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        audioSessionManager.setEnabled(enabled)
        _eqState.update { it.copy(isEnabled = enabled) }
    }

    fun resetToAi() {
        _eqState.update { it.copy(isManualPreset = false, presetName = "AI Auto") }
        appScope.launch {
            val np = com.sonara.app.service.SonaraNotificationListener.nowPlaying.value
            if (np.title.isNotBlank()) {
                val track = SonaraTrackInfo(np.title, np.artist, np.album, np.duration, np.packageName)
                val prediction = inferencePipeline.analyze(track)
                if (prediction.genre != Genre.UNKNOWN) applyFromPrediction(prediction)
            }
        }
    }

    fun saveCurrentAsPreset(name: String) {
        appScope.launch {
            val profile = _currentProfile.value
            val preset = com.sonara.app.preset.Preset(
                name = name, bands = com.sonara.app.preset.Preset.fromArray(profile.bands),
                preamp = profile.preamp, bassBoost = profile.bassBoost,
                virtualizer = profile.virtualizer, loudness = profile.loudness,
                category = "ai-generated", genre = profile.prediction.genre.name)
            presetRepository.save(preset)
            SonaraLogger.i("App", "Saved AI preset: $name")
        }
    }

    suspend fun loveTrack(title: String, artist: String, love: Boolean): Boolean {
        return try {
            val apiKey = secureSecrets.getLastFmApiKey().takeIf { it.isNotBlank() } ?: preferences.lastFmApiKeyFlow.first()
            val secret = secureSecrets.getLastFmSharedSecret().takeIf { it.isNotBlank() } ?: preferences.lastFmSharedSecretFlow.first()
            val sessionKey = secureSecrets.getLastFmSessionKey().takeIf { it.isNotBlank() } ?: preferences.lastFmSessionKeyFlow.first()
            SonaraLogger.i("Love", "key=set session=set")
            if (apiKey.isBlank()) { SonaraLogger.w("Love", "No API key"); return false }
            if (sessionKey.isBlank()) { SonaraLogger.w("Love", "No session key"); return false }
            val scrobbler = com.sonara.app.intelligence.lastfm.ScrobblingManager()
            val result = if (love) scrobbler.loveTrack(title, artist, apiKey, secret, sessionKey)
            else scrobbler.unloveTrack(title, artist, apiKey, secret, sessionKey)
            SonaraLogger.i("Love", "Result=$result for $title")
            result
        } catch (e: Exception) { SonaraLogger.e("Love", "Failed: ${e.message}"); false }
    }

    /**
     * Madde 10 FIX: Gemini insight'ı güncelle
     */
    fun updateGeminiInsight(insight: GeminiInsightEngine.GeminiInsight?) {
        _geminiInsight.value = insight
    }

    fun reloadPipeline() {
        val apiKey = lastFmAuth.getActiveApiKey().takeIf { it.isNotBlank() }
            ?: secureSecrets.getLastFmApiKey().takeIf { it.isNotBlank() }
            ?: runBlocking { preferences.lastFmApiKeyFlow.first() }
        inferencePipeline.destroy()
        inferencePipeline = SonaraInferencePipeline(apiKey.takeIf { it.isNotBlank() })
        inferencePipeline.onPrediction = { track, prediction ->
            if (prediction.source == PredictionSource.LASTFM || prediction.source == PredictionSource.MERGED) {
                adaptiveClassifier.train(prediction.genre.name.lowercase(), track.title, track.artist, track.album)
            }
            appScope.launch {
                try {
                    val info = com.sonara.app.data.models.TrackInfo(
                        title = track.title, artist = track.artist,
                        genre = prediction.genre.name, mood = prediction.mood.name,
                        energy = prediction.energy, confidence = prediction.confidence,
                        source = prediction.source.name
                    )
                    TrackCache(database.trackCacheDao()).put(info)
                } catch (_: Exception) {}
            }
        }
        nextTrackPreloader = NextTrackPreloader(inferencePipeline, appScope)
        SonaraLogger.i("App", "Pipeline rebuilt with key=${if (apiKey.isNullOrBlank()) "NONE" else "${apiKey.take(4)}***"}")
    }

    fun clearAllData() {
        appScope.launch {
            TrackCache(database.trackCacheDao()).clear()
            preferences.resetAll()
            secureSecrets.clearAll()
            database.presetDao().deleteAllCustom()
            inferencePipeline.clearCache()
            adaptiveLearning.load()
            SonaraLogger.i("App", "All data cleared")
        }
    }

    private fun detectRoute(am: AudioManager) {
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        _currentRoute.value = when {
            devices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } -> AudioRoute.BLUETOOTH
            devices.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET } -> AudioRoute.WIRED_HEADPHONES
            devices.any { it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE } -> AudioRoute.USB
            else -> AudioRoute.SPEAKER
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        audioSessionManager.stop()
        inferencePipeline.destroy()
        headphoneDetector.stop()
    }

    companion object { lateinit var instance: SonaraApp private set }
}
