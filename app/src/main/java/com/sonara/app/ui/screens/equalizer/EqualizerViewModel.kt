package com.sonara.app.ui.screens.equalizer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.audio.equalizer.TenBandEqualizer
import com.sonara.app.preset.Preset
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
    val deviceBandCount: Int = 0,
    val isManualPreset: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqualizerUiState) return false
        return bands.contentEquals(other.bands) && preamp == other.preamp &&
            bassBoost == other.bassBoost && virtualizer == other.virtualizer &&
            loudness == other.loudness && isEnabled == other.isEnabled &&
            currentPresetName == other.currentPresetName && eqActive == other.eqActive &&
            isManualPreset == other.isManualPreset
    }
    override fun hashCode() = bands.contentHashCode()
}

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val engine = app.audioEngine

    private val _uiState = MutableStateFlow(EqualizerUiState(
        eqActive = engine.isInitialized,
        deviceBandCount = engine.getDeviceBandCount()
    ))
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        if (!engine.isInitialized) {
            engine.init()
            _uiState.update { it.copy(eqActive = engine.isInitialized, deviceBandCount = engine.getDeviceBandCount()) }
        }
        viewModelScope.launch {
            app.presetRepository.allPresets().collect { presets ->
                _uiState.update { it.copy(availablePresets = presets) }
            }
        }
    }

    private fun applyToEngine() {
        val s = _uiState.value
        if (!s.isEnabled) return
        if (!engine.isInitialized) { engine.init(); _uiState.update { it.copy(eqActive = engine.isInitialized) } }
        engine.applyBands(s.bands)
        engine.applyBassBoost(s.bassBoost)
        engine.applyVirtualizer(s.virtualizer)
        engine.applyLoudness(s.loudness)
    }

    fun setBand(i: Int, v: Float) {
        _uiState.update { s ->
            val b = s.bands.copyOf(); b[i] = TenBandEqualizer.clamp(v)
            s.copy(bands = b, currentPresetName = "Custom", isManualPreset = true)
        }
        applyToEngine()
    }

    fun setPreamp(v: Float) { _uiState.update { it.copy(preamp = TenBandEqualizer.clamp(v)) }; applyToEngine() }
    fun setBassBoost(v: Int) { _uiState.update { it.copy(bassBoost = v.coerceIn(0, 1000)) }; applyToEngine() }
    fun setVirtualizer(v: Int) { _uiState.update { it.copy(virtualizer = v.coerceIn(0, 1000)) }; applyToEngine() }
    fun setLoudness(v: Int) { _uiState.update { it.copy(loudness = v.coerceIn(0, 1000)) }; applyToEngine() }

    fun setEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
        engine.setEnabled(enabled)
        if (enabled) applyToEngine()
    }

    fun resetBands() {
        _uiState.update { it.copy(
            bands = TenBandEqualizer.defaultBands(), preamp = 0f,
            bassBoost = 0, virtualizer = 0, loudness = 0,
            currentPresetName = "Flat", isManualPreset = false
        ) }
        applyToEngine()
    }

    // PRESET = DIRECTLY APPLIED. AI does NOT override manual preset.
    fun applyPreset(preset: Preset) {
        _uiState.update { it.copy(
            bands = preset.bandsArray(), preamp = preset.preamp,
            bassBoost = preset.bassBoost, virtualizer = preset.virtualizer,
            loudness = preset.loudness, currentPresetName = preset.name,
            isManualPreset = true
        ) }
        viewModelScope.launch { app.presetRepository.markUsed(preset.id) }
        applyToEngine()
    }

    fun applyAutoEqBands(bands: FloatArray, name: String) {
        _uiState.update { it.copy(
            bands = bands, currentPresetName = "AutoEQ: $name", isManualPreset = true
        ) }
        applyToEngine()
    }

    fun enableAiMode() {
        _uiState.update { it.copy(isManualPreset = false, currentPresetName = "AI Auto") }
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val s = _uiState.value
            app.presetRepository.save(Preset(
                name = name, bands = Preset.fromArray(s.bands), preamp = s.preamp,
                bassBoost = s.bassBoost, virtualizer = s.virtualizer, loudness = s.loudness
            ))
            _uiState.update { it.copy(currentPresetName = name) }
        }
    }
}
