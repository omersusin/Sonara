package com.sonara.app.ui.screens.equalizer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.audio.equalizer.TenBandEqualizer
import com.sonara.app.preset.Preset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EqualizerUiState(
    val bands: FloatArray = TenBandEqualizer.defaultBands(),
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val reverb: Int = 0,
    val isEnabled: Boolean = true,
    val currentPresetName: String = "Flat",
    val availablePresets: List<Preset> = emptyList(),
    val eqActive: Boolean = false,
    val eqStrategy: String = "none",
    val isClipping: Boolean = false,
    val isSimpleMode: Boolean = false,
    val simpleBass: Float = 0f,
    val simpleMids: Float = 0f,
    val simpleTreble: Float = 0f,
    val isAbComparing: Boolean = false,
    val perAppProfiles: Map<String, String> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqualizerUiState) return false
        return bands.contentEquals(other.bands) && preamp == other.preamp &&
            bassBoost == other.bassBoost && virtualizer == other.virtualizer &&
            loudness == other.loudness && reverb == other.reverb && isEnabled == other.isEnabled &&
            currentPresetName == other.currentPresetName &&
            availablePresets.size == other.availablePresets.size &&
            eqStrategy == other.eqStrategy && isClipping == other.isClipping &&
            isSimpleMode == other.isSimpleMode && isAbComparing == other.isAbComparing
    }
    override fun hashCode() = bands.contentHashCode()
}

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private var applyJob: Job? = null
    private var abSnapshotBands: FloatArray? = null

    private val _uiState = MutableStateFlow(EqualizerUiState(
        eqActive = app.audioSessionManager.isInitialized,
        eqStrategy = app.audioSessionManager.activeStrategy.value
    ))
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            app.eqState.collect { eq ->
                _uiState.update {
                    it.copy(bands = if (eq.bands.size > 10) eq.bands.take(10).toFloatArray() else eq.bands,
                        preamp = eq.preamp, bassBoost = eq.bassBoost, virtualizer = eq.virtualizer,
                        loudness = eq.loudness, reverb = eq.reverb, currentPresetName = eq.presetName, isEnabled = eq.isEnabled)
                }
            }
        }
        viewModelScope.launch {
            app.presetRepository.allPresets().collect { p ->
                _uiState.update { it.copy(availablePresets = p) }
            }
        }
        viewModelScope.launch {
            app.audioSessionManager.activeStrategy.collect { s ->
                _uiState.update { it.copy(eqStrategy = s, eqActive = s != "none") }
            }
        }
        viewModelScope.launch {
            app.preferences.perAppEqMapFlow.collect { map ->
                _uiState.update { it.copy(perAppProfiles = map) }
            }
        }
    }

    private fun c() = _uiState.value

    private fun debouncedApply(bands: FloatArray, name: String, bass: Int, virt: Int, loud: Int, preamp: Float = 0f, reverb: Int = c().reverb) {
        applyJob?.cancel()
        applyJob = viewModelScope.launch {
            delay(80)
            app.applyEq(bands, name, manual = true, bass, virt, loud, preamp, instant = true, reverb = reverb)
        }
    }

    fun setBand(i: Int, v: Float) {
        val bands = c().bands.copyOf()
        bands[i] = TenBandEqualizer.clamp(if (v in -0.5f..0.5f) 0f else v)
        val clipping = com.sonara.app.audio.engine.SafetyLimiter.wouldClip(bands, c().preamp)
        _uiState.update { it.copy(bands = bands, currentPresetName = "Custom", isClipping = clipping) }
        debouncedApply(bands, "Custom", c().bassBoost, c().virtualizer, c().loudness, c().preamp)
    }

    fun setPreamp(v: Float) {
        val cl = TenBandEqualizer.clamp(v)
        val clipping = com.sonara.app.audio.engine.SafetyLimiter.wouldClip(c().bands, cl)
        _uiState.update { it.copy(preamp = cl, isClipping = clipping) }
        debouncedApply(c().bands, c().currentPresetName, c().bassBoost, c().virtualizer, c().loudness, cl)
    }

    fun setBassBoost(v: Int) {
        _uiState.update { it.copy(bassBoost = v.coerceIn(0, 1000)) }
        debouncedApply(c().bands, c().currentPresetName, v.coerceIn(0, 1000), c().virtualizer, c().loudness)
    }

    fun setVirtualizer(v: Int) {
        _uiState.update { it.copy(virtualizer = v.coerceIn(0, 1000)) }
        debouncedApply(c().bands, c().currentPresetName, c().bassBoost, v.coerceIn(0, 1000), c().loudness)
    }

    fun setLoudness(v: Int) {
        _uiState.update { it.copy(loudness = v.coerceIn(0, 3000)) }
        debouncedApply(c().bands, c().currentPresetName, c().bassBoost, c().virtualizer, v.coerceIn(0, 3000))
    }

    fun setReverb(v: Int) {
        val clamped = v.coerceIn(0, 6)
        _uiState.update { it.copy(reverb = clamped) }
        debouncedApply(c().bands, c().currentPresetName, c().bassBoost, c().virtualizer, c().loudness, c().preamp, clamped)
    }

    fun setEnabled(on: Boolean) { app.setEqEnabled(on) }

    fun setBypass(bypass: Boolean) {
        if (bypass) {
            app.applyEq(FloatArray(10), "Bypass", manual = false, 0, 0, 0, reverb = 0)
        } else {
            val s = c()
            app.applyEq(s.bands, s.currentPresetName, manual = false, s.bassBoost, s.virtualizer, s.loudness, s.preamp, reverb = s.reverb)
        }
    }

    fun resetBands() {
        applyJob?.cancel()
        app.applyEq(FloatArray(10), "Flat", false, 0, 0, 0, reverb = 0)
        _uiState.update { it.copy(preamp = 0f, reverb = 0) }
    }

    /** Reset to AI and re-trigger analysis for current track */
    fun resetToAi() {
        app.resetToAi()
        // Force re-analysis of current track
        viewModelScope.launch {
            app.inferencePipeline.clearCache()
            val np = com.sonara.app.service.SonaraNotificationListener.nowPlaying.value
            if (np.title.isNotBlank()) {
                val track = com.sonara.app.intelligence.pipeline.SonaraTrackInfo(
                    np.title, np.artist, np.album, np.duration, np.packageName
                )
                val prediction = app.inferencePipeline.analyze(track)
                if (prediction.genre != com.sonara.app.intelligence.pipeline.Genre.UNKNOWN) {
                    app.applyFromPrediction(prediction)
                }
            }
        }
    }

    // EQ-02: Simple mode toggle
    fun setSimpleMode(on: Boolean) { _uiState.update { it.copy(isSimpleMode = on) } }

    // EQ-09: Organic blend — adjacent bands receive a tapered coefficient
    fun setSimpleBass(v: Float) {
        val bands = c().bands.copyOf()
        bands[0] = TenBandEqualizer.clamp(v)
        bands[1] = TenBandEqualizer.clamp(v * 0.85f)
        bands[2] = TenBandEqualizer.clamp(v * 0.55f)
        bands[3] = TenBandEqualizer.clamp((bands[3] + v * 0.2f).coerceIn(TenBandEqualizer.MIN_LEVEL, TenBandEqualizer.MAX_LEVEL))
        val clipping = com.sonara.app.audio.engine.SafetyLimiter.wouldClip(bands, c().preamp)
        _uiState.update { it.copy(bands = bands, simpleBass = v, currentPresetName = "Custom", isClipping = clipping) }
        debouncedApply(bands, "Custom", c().bassBoost, c().virtualizer, c().loudness, c().preamp)
    }

    fun setSimpleMids(v: Float) {
        val bands = c().bands.copyOf()
        bands[2] = TenBandEqualizer.clamp((bands[2] + v * 0.25f).coerceIn(TenBandEqualizer.MIN_LEVEL, TenBandEqualizer.MAX_LEVEL))
        bands[3] = TenBandEqualizer.clamp(v * 0.8f)
        bands[4] = TenBandEqualizer.clamp(v)
        bands[5] = TenBandEqualizer.clamp(v * 0.8f)
        bands[6] = TenBandEqualizer.clamp((bands[6] + v * 0.3f).coerceIn(TenBandEqualizer.MIN_LEVEL, TenBandEqualizer.MAX_LEVEL))
        val clipping = com.sonara.app.audio.engine.SafetyLimiter.wouldClip(bands, c().preamp)
        _uiState.update { it.copy(bands = bands, simpleMids = v, currentPresetName = "Custom", isClipping = clipping) }
        debouncedApply(bands, "Custom", c().bassBoost, c().virtualizer, c().loudness, c().preamp)
    }

    fun setSimpleTreble(v: Float) {
        val bands = c().bands.copyOf()
        bands[5] = TenBandEqualizer.clamp((bands[5] + v * 0.2f).coerceIn(TenBandEqualizer.MIN_LEVEL, TenBandEqualizer.MAX_LEVEL))
        bands[6] = TenBandEqualizer.clamp(v * 0.7f)
        bands[7] = TenBandEqualizer.clamp(v * 0.9f)
        bands[8] = TenBandEqualizer.clamp(v)
        bands[9] = TenBandEqualizer.clamp(v * 0.85f)
        val clipping = com.sonara.app.audio.engine.SafetyLimiter.wouldClip(bands, c().preamp)
        _uiState.update { it.copy(bands = bands, simpleTreble = v, currentPresetName = "Custom", isClipping = clipping) }
        debouncedApply(bands, "Custom", c().bassBoost, c().virtualizer, c().loudness, c().preamp)
    }

    // EQ-06: A/B compare — snapshot current bands, then restore on toggle off
    fun setAbCompare(on: Boolean) {
        if (on) {
            abSnapshotBands = c().bands.copyOf()
            app.applyEq(FloatArray(10), "A/B Flat", manual = false, 0, 0, 0, reverb = 0)
        } else {
            abSnapshotBands?.let { snap ->
                val s = c()
                app.applyEq(snap, s.currentPresetName, manual = false, s.bassBoost, s.virtualizer, s.loudness, s.preamp, reverb = s.reverb)
            }
            abSnapshotBands = null
        }
        _uiState.update { it.copy(isAbComparing = on) }
    }

    // EQ-08: Apply a single band value in milli-dB (used by Dolby/Dirac/Harman profiles)
    fun setBandRaw(band: Int, valueMDb: Int) {
        val dB = valueMDb / 100f
        setBand(band, dB)
    }

    fun applyPreset(preset: Preset) {
        applyJob?.cancel()
        app.applyEq(preset.bandsArray(), preset.name, true, preset.bassBoost, preset.virtualizer, preset.loudness, preset.preamp, instant = false, reverb = preset.reverb)
        viewModelScope.launch { app.presetRepository.markUsed(preset.id) }
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val s = c()
            app.presetRepository.save(
                Preset(name = name, bands = Preset.fromArray(s.bands), preamp = s.preamp,
                    bassBoost = s.bassBoost, virtualizer = s.virtualizer, loudness = s.loudness,
                    reverb = s.reverb)
            )
        }
    }
}
