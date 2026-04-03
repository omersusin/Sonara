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
    val isEnabled: Boolean = true,
    val currentPresetName: String = "Flat",
    val availablePresets: List<Preset> = emptyList(),
    val eqActive: Boolean = false,
    val eqStrategy: String = "none",
    val isClipping: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqualizerUiState) return false
        return bands.contentEquals(other.bands) && preamp == other.preamp &&
            bassBoost == other.bassBoost && virtualizer == other.virtualizer &&
            loudness == other.loudness && isEnabled == other.isEnabled &&
            currentPresetName == other.currentPresetName &&
            availablePresets.size == other.availablePresets.size &&
            eqStrategy == other.eqStrategy && isClipping == other.isClipping
    }
    override fun hashCode() = bands.contentHashCode()
}

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private var applyJob: Job? = null

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
                        loudness = eq.loudness, currentPresetName = eq.presetName, isEnabled = eq.isEnabled)
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
    }

    private fun c() = _uiState.value

    private fun debouncedApply(bands: FloatArray, name: String, bass: Int, virt: Int, loud: Int, preamp: Float = 0f) {
        applyJob?.cancel()
        applyJob = viewModelScope.launch {
            delay(80)
            app.applyEq(bands, name, manual = true, bass, virt, loud, preamp)
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
        _uiState.update { it.copy(preamp = cl) }
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

    fun setEnabled(on: Boolean) { app.setEqEnabled(on) }

    fun resetBands() {
        applyJob?.cancel()
        app.applyEq(FloatArray(10), "Flat", false, 0, 0, 0)
        _uiState.update { it.copy(preamp = 0f) }
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

    fun applyPreset(preset: Preset) {
        applyJob?.cancel()
        app.applyEq(preset.bandsArray(), preset.name, true, preset.bassBoost, preset.virtualizer, preset.loudness, preset.preamp)
        viewModelScope.launch { app.presetRepository.markUsed(preset.id) }
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val s = c()
            app.presetRepository.save(
                Preset(name = name, bands = Preset.fromArray(s.bands), preamp = s.preamp,
                    bassBoost = s.bassBoost, virtualizer = s.virtualizer, loudness = s.loudness)
            )
        }
    }
}
