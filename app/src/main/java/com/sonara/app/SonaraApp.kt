package com.sonara.app

import android.app.Application
import android.util.Log
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.SonaraLogger
import com.sonara.app.data.models.SharedEqState
import com.sonara.app.data.preferences.SonaraPreferences
import com.sonara.app.engine.eq.AudioSessionBridge
import com.sonara.app.engine.eq.EqSessionController
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

    // New engine
    lateinit var sessionBridge: AudioSessionBridge private set

    // Legacy compatibility — ViewModels use these
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

        // Initialize new engine
        sessionBridge = AudioSessionBridge(this)
        SonaraLogger.i("App", "Sonara started - new engine active")

        // Bridge callback -> update shared state for UI
        sessionBridge.onTrackAnalyzed = { state ->
            _eqState.update {
                it.copy(
                    bands = FloatArray(state.appliedBands.size) { i -> state.appliedBands[i] / 100f },
                    presetName = "AI: ${state.genre.replaceFirstChar { c -> c.uppercase() }}",
                    isManualPreset = false
                )
            }
            SonaraLogger.ai("Track analyzed: ${state.genre}/${state.mood} c=${state.confidence} r=${state.route}")
        }

        // Restore AI weights
        appScope.launch {
            sessionBridge.learner.restoreWeights()
            presetRepository.initBuiltIns()
            TrackCache(database.trackCacheDao()).cleanup()
        }
    }

    // Called by ViewModels when user changes EQ
    fun applyEq(
        bands: FloatArray, presetName: String = _eqState.value.presetName,
        manual: Boolean = _eqState.value.isManualPreset,
        bassBoost: Int = _eqState.value.bassBoost, virtualizer: Int = _eqState.value.virtualizer,
        loudness: Int = _eqState.value.loudness, preamp: Float = 0f
    ) {
        // Convert 10-band float dB to 5-band short millibel for new engine
        val shortBands = convertTo5Band(bands, preamp)
        sessionBridge.eqController.applyBands(shortBands)

        _eqState.update {
            it.copy(bands = bands.copyOf(), bassBoost = bassBoost, virtualizer = virtualizer,
                loudness = loudness, presetName = presetName, isManualPreset = manual)
        }

        // Learn from manual changes
        if (manual) {
            val state = sessionBridge.currentState
            appScope.launch {
                sessionBridge.learner.onBandsManuallyAdjusted(
                    genre = state.genre, mood = state.mood,
                    route = state.route, bands = shortBands, energy = state.energy
                )
            }
        }

        SonaraLogger.eq("EQ applied: $presetName manual=$manual")
    }

    fun setEqEnabled(enabled: Boolean) {
        sessionBridge.eqController.setEnabled(enabled)
        _eqState.update { it.copy(isEnabled = enabled) }
    }

    fun resetToAi() {
        _eqState.update { it.copy(isManualPreset = false, presetName = "AI Auto") }
        trackResolver.forceReResolve()
    }

    private fun convertTo5Band(tenBands: FloatArray, preamp: Float): ShortArray {
        // 10-band (31,62,125,250,500,1k,2k,4k,8k,16k) -> 5-band (60,230,910,3.6k,14k)
        // Average neighboring bands and convert dB to millibel
        val b = FloatArray(tenBands.size) { (tenBands[it] + preamp).coerceIn(-12f, 12f) }
        return shortArrayOf(
            ((b.getOrElse(0) { 0f } + b.getOrElse(1) { 0f }) / 2f * 100).toInt().toShort(),  // 60Hz
            ((b.getOrElse(2) { 0f } + b.getOrElse(3) { 0f }) / 2f * 100).toInt().toShort(),  // 230Hz
            ((b.getOrElse(4) { 0f } + b.getOrElse(5) { 0f }) / 2f * 100).toInt().toShort(),  // 910Hz
            ((b.getOrElse(6) { 0f } + b.getOrElse(7) { 0f }) / 2f * 100).toInt().toShort(),  // 3.6kHz
            ((b.getOrElse(8) { 0f } + b.getOrElse(9) { 0f }) / 2f * 100).toInt().toShort()   // 14kHz
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        sessionBridge.release()
    }

    companion object { lateinit var instance: SonaraApp private set }
}
