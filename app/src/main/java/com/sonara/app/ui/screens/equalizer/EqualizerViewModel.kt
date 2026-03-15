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
    val showPresetPicker: Boolean = false,
    val eqActive: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqualizerUiState) return false
        return bands.contentEquals(other.bands) && preamp == other.preamp &&
            bassBoost == other.bassBoost && virtualizer == other.virtualizer &&
            loudness == other.loudness && isEnabled == other.isEnabled &&
            currentPresetName == other.currentPresetName && eqActive == other.eqActive
    }
    override fun hashCode() = bands.contentHashCode()
}

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val engine = app.audioEngine

    private val _uiState = MutableStateFlow(EqualizerUiState(eqActive = engine.isInitialized))
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        if (!engine.isInitialized) engine.init()
        _uiState.update { it.copy(eqActive = engine.isInitialized) }

        viewModelScope.launch {
            app.presetRepository.allPresets().collect { presets ->
                _uiState.update { it.copy(availablePresets = presets) }
            }
        }
    }

    private fun apply() {
        val s = _uiState.value
        if (!s.isEnabled) return
        if (!engine.isInitialized) { engine.init(); _uiState.update { it.copy(eqActive = engine.isInitialized) } }
        engine.applyBands(s.bands)
        engine.applyBassBoost(s.bassBoost)
        engine.applyVirtualizer(s.virtualizer)
        engine.applyLoudness(s.loudness)
    }

    fun setBand(i: Int, v: Float) {
        _uiState.update { s -> val b = s.bands.copyOf(); b[i] = TenBandEqualizer.clamp(v); s.copy(bands = b, currentPresetName = "Custom") }
        apply()
    }

    fun setPreamp(v: Float) { _uiState.update { it.copy(preamp = TenBandEqualizer.clamp(v)) }; apply() }
    fun setBassBoost(v: Int) { _uiState.update { it.copy(bassBoost = v.coerceIn(0, 1000)) }; apply() }
    fun setVirtualizer(v: Int) { _uiState.update { it.copy(virtualizer = v.coerceIn(0, 1000)) }; apply() }
    fun setLoudness(v: Int) { _uiState.update { it.copy(loudness = v.coerceIn(0, 1000)) }; apply() }

    fun setEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
        engine.setEnabled(enabled)
        if (enabled) apply()
    }

    fun resetBands() {
        _uiState.update { it.copy(bands = TenBandEqualizer.defaultBands(), preamp = 0f, bassBoost = 0, virtualizer = 0, loudness = 0, currentPresetName = "Flat") }
        apply()
    }

    fun applyPreset(preset: Preset) {
        _uiState.update { it.copy(bands = preset.bandsArray(), preamp = preset.preamp, bassBoost = preset.bassBoost, virtualizer = preset.virtualizer, loudness = preset.loudness, currentPresetName = preset.name) }
        viewModelScope.launch { app.presetRepository.markUsed(preset.id) }
        apply()
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val s = _uiState.value
            app.presetRepository.save(Preset(name = name, bands = Preset.fromArray(s.bands), preamp = s.preamp, bassBoost = s.bassBoost, virtualizer = s.virtualizer, loudness = s.loudness))
            _uiState.update { it.copy(currentPresetName = name) }
        }
    }

    fun togglePresetPicker() { _uiState.update { it.copy(showPresetPicker = !it.showPresetPicker) } }
}
