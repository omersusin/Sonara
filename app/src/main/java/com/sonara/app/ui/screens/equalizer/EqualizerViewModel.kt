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
    val eqSessionActive: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqualizerUiState) return false
        return bands.contentEquals(other.bands) && preamp == other.preamp &&
            bassBoost == other.bassBoost && virtualizer == other.virtualizer &&
            loudness == other.loudness && isEnabled == other.isEnabled &&
            currentPresetName == other.currentPresetName && eqSessionActive == other.eqSessionActive
    }
    override fun hashCode() = bands.contentHashCode()
}

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val _uiState = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            app.presetRepository.allPresets().collect { presets ->
                _uiState.update { it.copy(availablePresets = presets) }
            }
        }
        viewModelScope.launch {
            app.activeSessionId.collect { sid ->
                _uiState.update { it.copy(eqSessionActive = sid > 0) }
            }
        }
    }

    private fun applyToEngine() {
        val state = _uiState.value
        if (!state.isEnabled) return
        app.applyEqBands(state.bands)
        app.audioEngine.applyBassBoost(state.bassBoost)
        app.audioEngine.applyVirtualizer(state.virtualizer)
        app.audioEngine.applyLoudness(state.loudness)
    }

    fun setBand(index: Int, value: Float) {
        _uiState.update { s ->
            val newBands = s.bands.copyOf()
            newBands[index] = TenBandEqualizer.clamp(value)
            s.copy(bands = newBands, currentPresetName = "Custom")
        }
        applyToEngine()
    }

    fun setPreamp(value: Float) { _uiState.update { it.copy(preamp = TenBandEqualizer.clamp(value)) }; applyToEngine() }
    fun setBassBoost(value: Int) { _uiState.update { it.copy(bassBoost = value.coerceIn(0, 1000)) }; applyToEngine() }
    fun setVirtualizer(value: Int) { _uiState.update { it.copy(virtualizer = value.coerceIn(0, 1000)) }; applyToEngine() }
    fun setLoudness(value: Int) { _uiState.update { it.copy(loudness = value.coerceIn(0, 1000)) }; applyToEngine() }

    fun setEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
        app.audioEngine.setEnabled(enabled)
        if (enabled) applyToEngine()
    }

    fun resetBands() {
        _uiState.update { it.copy(bands = TenBandEqualizer.defaultBands(), preamp = 0f, bassBoost = 0, virtualizer = 0, loudness = 0, currentPresetName = "Flat") }
        applyToEngine()
    }

    fun applyPreset(preset: Preset) {
        _uiState.update { it.copy(bands = preset.bandsArray(), preamp = preset.preamp, bassBoost = preset.bassBoost, virtualizer = preset.virtualizer, loudness = preset.loudness, currentPresetName = preset.name, showPresetPicker = false) }
        viewModelScope.launch { app.presetRepository.markUsed(preset.id) }
        applyToEngine()
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val state = _uiState.value
            app.presetRepository.save(Preset(name = name, bands = Preset.fromArray(state.bands), preamp = state.preamp, bassBoost = state.bassBoost, virtualizer = state.virtualizer, loudness = state.loudness))
            _uiState.update { it.copy(currentPresetName = name) }
        }
    }

    fun togglePresetPicker() { _uiState.update { it.copy(showPresetPicker = !it.showPresetPicker) } }
}
