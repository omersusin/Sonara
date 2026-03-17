package com.sonara.app

import android.app.Application
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sonara.app.audio.equalizer.TenBandEqualizer
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.SonaraLogger
import com.sonara.app.data.models.SharedEqState
import com.sonara.app.data.preferences.SonaraPreferences
import com.sonara.app.engine.classifier.AdaptiveGenreClassifier
import com.sonara.app.engine.eq.AudioSessionManager
import com.sonara.app.engine.eq.AudioSessionBridge
import com.sonara.app.intelligence.TrackResolver
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.intelligence.lastfm.LastFmResolver
import com.sonara.app.intelligence.local.LocalInferencePlugin
import com.sonara.app.intelligence.local.MetadataHeuristicPlugin
import com.sonara.app.intelligence.local.SmartMediaClassifier
import com.sonara.app.intelligence.local.WaveformGenrePlugin
import com.sonara.app.preset.PresetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SonaraApp : Application() {
    lateinit var preferences: SonaraPreferences private set
    lateinit var database: SonaraDatabase private set
    lateinit var presetRepository: PresetRepository private set

    // NEW: DynamicsProcessing-based EQ manager
    lateinit var audioSessionManager: AudioSessionManager private set

    // NEW: Self-training AI classifier
    lateinit var adaptiveClassifier: AdaptiveGenreClassifier private set

    // Legacy bridge (for NotificationListener integration)
    lateinit var sessionBridge: AudioSessionBridge private set

    val trackResolver: TrackResolver by lazy {
        val plugins = mutableListOf<LocalInferencePlugin>(MetadataHeuristicPlugin())
        val wfp = WaveformGenrePlugin(this); wfp.init(); plugins.add(wfp)
        TrackResolver(LastFmResolver(), plugins, TrackCache(database.trackCacheDao()))
    }
    val mediaClassifier: SmartMediaClassifier by lazy { SmartMediaClassifier() }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _eqState = MutableStateFlow(SharedEqState())
    val eqState: StateFlow<SharedEqState> = _eqState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = SonaraPreferences(this)
        database = SonaraDatabase.get(this)
        presetRepository = PresetRepository(database.presetDao())

        // Initialize new EQ engine
        audioSessionManager = AudioSessionManager(this)
        audioSessionManager.start()

        // Initialize self-training classifier
        adaptiveClassifier = AdaptiveGenreClassifier(this)

        // Legacy bridge
        sessionBridge = AudioSessionBridge(this)

        // Route change handling
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        am.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) {
                Handler(Looper.getMainLooper()).postDelayed({ audioSessionManager.reinitialize() }, 300)
            }
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) {
                Handler(Looper.getMainLooper()).postDelayed({ audioSessionManager.reinitialize() }, 300)
            }
        }, Handler(Looper.getMainLooper()))

        SonaraLogger.i("App", "Sonara started. Strategy: ${audioSessionManager.activeStrategy.value}")

        appScope.launch { presetRepository.initBuiltIns(); TrackCache(database.trackCacheDao()).cleanup() }
    }

    fun applyEq(
        bands: FloatArray, presetName: String = _eqState.value.presetName,
        manual: Boolean = _eqState.value.isManualPreset,
        bassBoost: Int = _eqState.value.bassBoost, virtualizer: Int = _eqState.value.virtualizer,
        loudness: Int = _eqState.value.loudness, preamp: Float = 0f
    ) {
        val finalBands = if (preamp != 0f) FloatArray(bands.size) { TenBandEqualizer.clamp(bands[it] + preamp) } else bands

        // Apply through new DynamicsProcessing-based manager
        audioSessionManager.applyBands(finalBands)

        _eqState.update { it.copy(bands = finalBands.copyOf(), bassBoost = bassBoost, virtualizer = virtualizer, loudness = loudness, presetName = presetName, isManualPreset = manual) }
        SonaraLogger.i("App", "EQ: $presetName [${finalBands.take(3).map { "%.1f".format(it) }}...] strategy=${audioSessionManager.activeStrategy.value}")

        // Self-training: if this was from AI, train the adaptive classifier
        if (!manual) {
            val state = sessionBridge.currentState
            if (state.title?.isNotBlank() == true && state.genre != "other") {
                appScope.launch {
                    adaptiveClassifier.train(state.genre, state.title ?: "", state.artist ?: "", state.album ?: "", weight = 1f)
                }
            }
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        audioSessionManager.setEnabled(enabled)
        _eqState.update { it.copy(isEnabled = enabled) }
    }

    fun resetToAi() {
        _eqState.update { it.copy(isManualPreset = false, presetName = "AI Auto") }
        trackResolver.forceReResolve()
    }

    override fun onTerminate() {
        super.onTerminate()
        audioSessionManager.stop()
        sessionBridge.release()
        adaptiveClassifier.save()
    }

    companion object { lateinit var instance: SonaraApp private set }
}
