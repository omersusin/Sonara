package com.sonara.app

import android.app.Application
import android.util.Log
import com.sonara.app.audio.engine.AudioEngine
import com.sonara.app.audio.engine.CompareManager
import com.sonara.app.audio.equalizer.TenBandEqualizer
import com.sonara.app.autoeq.HeadphoneDetector
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.models.SharedEqState
import com.sonara.app.data.preferences.SonaraPreferences
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
    lateinit var audioEngine: AudioEngine private set

    val trackResolver: TrackResolver by lazy {
        val plugins = mutableListOf<LocalInferencePlugin>(MetadataHeuristicPlugin())
        val wfp = WaveformGenrePlugin(this); wfp.init(); plugins.add(wfp)
        TrackResolver(LastFmResolver(), plugins, TrackCache(database.trackCacheDao()))
    }
    val headphoneDetector: HeadphoneDetector by lazy { HeadphoneDetector(this) }
    val compareManager: CompareManager by lazy { CompareManager(audioEngine) }
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
        audioEngine = AudioEngine(this)

        val initOk = audioEngine.init()
        SonaraLogger.i("App", "═══ Sonara started ═══")
        SonaraLogger.i("App", "Engine: ${if (audioEngine.isInitialized) "OK" else "FAILED"}")
        SonaraLogger.i("App", "════ AudioEngine init: $initOk ════")

        headphoneDetector.start()
        appScope.launch { presetRepository.initBuiltIns(); TrackCache(database.trackCacheDao()).cleanup() }
    }

    fun applyEq(
        bands: FloatArray,
        presetName: String = _eqState.value.presetName,
        manual: Boolean = _eqState.value.isManualPreset,
        bassBoost: Int = _eqState.value.bassBoost,
        virtualizer: Int = _eqState.value.virtualizer,
        loudness: Int = _eqState.value.loudness,
        preamp: Float = 0f
    ) {
        // ENSURE engine is initialized
        if (!audioEngine.isInitialized) {
            val reinit = audioEngine.init()
        SonaraLogger.i("App", "═══ Sonara started ═══")
        SonaraLogger.i("App", "Engine: ${if (audioEngine.isInitialized) "OK" else "FAILED"}")
            SonaraLogger.i("App", "Engine was dead, reinit=$reinit")
        }

        val finalBands = if (preamp != 0f) {
            FloatArray(bands.size) { i -> TenBandEqualizer.clamp(bands[i] + preamp) }
        } else bands

        // ACTUALLY APPLY TO HARDWARE
        audioEngine.applyBands(finalBands)
        audioEngine.applyBassBoost(bassBoost)
        audioEngine.applyVirtualizer(virtualizer)
        audioEngine.applyLoudness(loudness)

        SonaraLogger.i("App", "════ EQ APPLIED ════")
        SonaraLogger.i("App", "  Preset: $presetName")
        SonaraLogger.i("App", "  Bands: ${finalBands.take(5).map { "%.1f".format(it) }}...")
        SonaraLogger.i("App", "  Bass=$bassBoost Virt=$virtualizer Loud=$loudness")
        SonaraLogger.i("App", "  Engine initialized: ${audioEngine.isInitialized}")

        // Update shared state
        _eqState.update {
            it.copy(
                bands = finalBands.copyOf(),
                bassBoost = bassBoost,
                virtualizer = virtualizer,
                loudness = loudness,
                presetName = presetName,
                isManualPreset = manual
            )
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        audioEngine.setEnabled(enabled)
        _eqState.update { it.copy(isEnabled = enabled) }
        SonaraLogger.i("App", "EQ enabled=$enabled")
    }

    fun resetToAi() {
        _eqState.update { it.copy(isManualPreset = false, presetName = "AI Auto") }
        trackResolver.forceReResolve()
        SonaraLogger.i("App", "Reset to AI Auto")
    }

    override fun onTerminate() {
        super.onTerminate()
        headphoneDetector.stop()
        audioEngine.release()
    }

    companion object { lateinit var instance: SonaraApp private set }
}
