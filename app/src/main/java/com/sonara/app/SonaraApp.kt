package com.sonara.app

import android.app.Application
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.SonaraLogger
import com.sonara.app.data.models.SharedEqState
import com.sonara.app.data.preferences.SonaraPreferences
import com.sonara.app.engine.eq.AudioSessionManager
import com.sonara.app.engine.eq.EqComposer
import com.sonara.app.intelligence.adaptive.AdaptiveLearningEngine
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.intelligence.pipeline.*
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
    lateinit var database: SonaraDatabase private set
    lateinit var presetRepository: PresetRepository private set
    lateinit var audioSessionManager: AudioSessionManager private set
    lateinit var eqComposer: EqComposer private set
    lateinit var adaptiveLearning: AdaptiveLearningEngine private set
    lateinit var inferencePipeline: SonaraInferencePipeline private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _eqState = MutableStateFlow(SharedEqState())
    val eqState: StateFlow<SharedEqState> = _eqState.asStateFlow()
    private val _currentRoute = MutableStateFlow(AudioRoute.UNKNOWN)
    val currentRoute: StateFlow<AudioRoute> = _currentRoute.asStateFlow()
    private val _currentProfile = MutableStateFlow(FinalEqProfile.neutral())
    val currentProfile: StateFlow<FinalEqProfile> = _currentProfile.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = SonaraPreferences(this)
        database = SonaraDatabase.get(this)
        presetRepository = PresetRepository(database.presetDao())

        audioSessionManager = AudioSessionManager(this)
        audioSessionManager.start()

        eqComposer = EqComposer()
        adaptiveLearning = AdaptiveLearningEngine(this)
        appScope.launch { adaptiveLearning.load() }

        val apiKey = runBlocking { preferences.lastFmApiKeyFlow.first() }
        inferencePipeline = SonaraInferencePipeline(apiKey.takeIf { it.isNotBlank() })

        // Route monitoring
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        detectRoute(am)
        am.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(a: Array<out AudioDeviceInfo>) { detectRoute(am); Handler(Looper.getMainLooper()).postDelayed({ audioSessionManager.reinitialize() }, 500) }
            override fun onAudioDevicesRemoved(r: Array<out AudioDeviceInfo>) { detectRoute(am); Handler(Looper.getMainLooper()).postDelayed({ audioSessionManager.reinitialize() }, 500) }
        }, Handler(Looper.getMainLooper()))

        SonaraLogger.i("App", "Sonara started. EQ: ${audioSessionManager.activeStrategy.value}")
        appScope.launch { presetRepository.initBuiltIns(); TrackCache(database.trackCacheDao()).cleanup() }
    }

    // TEK GİRİŞ NOKTASI: Prediction → EQ
    fun applyFromPrediction(prediction: SonaraPrediction) {
        val route = _currentRoute.value
        val userOffset = adaptiveLearning.getOffset(prediction.genre, route)
        val profile = eqComposer.compose(prediction, route, userOffset)
        applyProfile(profile)
    }

    fun applyProfile(profile: FinalEqProfile) {
        val adjustedBands = FloatArray(profile.bands.size) { profile.bands[it] + profile.preamp }
        audioSessionManager.applyBands(adjustedBands)
        _currentProfile.value = profile
        _eqState.update { it.copy(bands = profile.bands, presetName = "AI: ${profile.prediction.genre.displayName}", isManualPreset = false, bassBoost = profile.bassBoost, virtualizer = profile.virtualizer, loudness = profile.loudness) }
        SonaraLogger.eq("Applied: ${profile.prediction.genre} bands=${adjustedBands.take(5).map { "%.1f".format(it) }}")
    }

    fun applyManualBands(bands: FloatArray) {
        audioSessionManager.applyBands(bands)
        val pred = _currentProfile.value.prediction
        _eqState.update { it.copy(bands = bands, presetName = "Custom", isManualPreset = true) }
        if (pred.confidence > 0f) {
            appScope.launch { adaptiveLearning.recordFeedback(pred.genre, _currentRoute.value, _currentProfile.value.bands, bands) }
        }
    }

    // Legacy compatibility
    fun applyEq(bands: FloatArray, presetName: String = "Custom", manual: Boolean = true, bassBoost: Int = 0, virtualizer: Int = 0, loudness: Int = 0, preamp: Float = 0f) {
        val adj = if (preamp != 0f) FloatArray(bands.size) { (bands[it] + preamp).coerceIn(-12f, 12f) } else bands
        audioSessionManager.applyBands(adj)
        _eqState.update { it.copy(bands = adj, presetName = presetName, isManualPreset = manual, bassBoost = bassBoost, virtualizer = virtualizer, loudness = loudness) }
    }

    fun setEqEnabled(enabled: Boolean) { audioSessionManager.setEnabled(enabled); _eqState.update { it.copy(isEnabled = enabled) } }
    fun resetToAi() { _eqState.update { it.copy(isManualPreset = false, presetName = "AI Auto") } }

    private fun detectRoute(am: AudioManager) {
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        _currentRoute.value = when {
            devices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } -> AudioRoute.BLUETOOTH
            devices.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET } -> AudioRoute.WIRED_HEADPHONES
            devices.any { it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE } -> AudioRoute.USB
            else -> AudioRoute.SPEAKER
        }
    }

    override fun onTerminate() { super.onTerminate(); audioSessionManager.stop(); inferencePipeline.destroy() }
    companion object { lateinit var instance: SonaraApp private set }
}
