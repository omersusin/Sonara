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
    val showPresetPicker: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqualizerUiState) return false
        return bands.contentEquals(other.bands) && preamp == other.preamp &&
            bassBoost == other.bassBoost && virtualizer == other.virtualizer &&
            loudness == other.loudness && isEnabled == other.isEnabled &&
            currentPresetName == other.currentPresetName &&
            showPresetPicker == other.showPresetPicker
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
    }

    fun setBand(index: Int, value: Float) {
        _uiState.update { state ->
            val newBands = state.bands.copyOf()
            newBands[index] = TenBandEqualizer.clamp(value)
            state.copy(bands = newBands, currentPresetName = "Custom")
        }
    }

    fun setPreamp(value: Float) { _uiState.update { it.copy(preamp = TenBandEqualizer.clamp(value)) } }
    fun setBassBoost(value: Int) { _uiState.update { it.copy(bassBoost = value.coerceIn(0, 1000)) } }
    fun setVirtualizer(value: Int) { _uiState.update { it.copy(virtualizer = value.coerceIn(0, 1000)) } }
    fun setLoudness(value: Int) { _uiState.update { it.copy(loudness = value.coerceIn(0, 1000)) } }
    fun setEnabled(enabled: Boolean) { _uiState.update { it.copy(isEnabled = enabled) } }

    fun resetBands() {
        _uiState.update { it.copy(bands = TenBandEqualizer.defaultBands(), preamp = 0f, currentPresetName = "Flat") }
    }

    fun applyPreset(preset: Preset) {
        _uiState.update { it.copy(
            bands = preset.bandsArray(),
            preamp = preset.preamp,
            bassBoost = preset.bassBoost,
            virtualizer = preset.virtualizer,
            loudness = preset.loudness,
            currentPresetName = preset.name,
            showPresetPicker = false
        ) }
        viewModelScope.launch { app.presetRepository.markUsed(preset.id) }
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val state = _uiState.value
            app.presetRepository.save(Preset(
                name = name,
                bands = Preset.fromArray(state.bands),
                preamp = state.preamp,
                bassBoost = state.bassBoost,
                virtualizer = state.virtualizer,
                loudness = state.loudness
            ))
            _uiState.update { it.copy(currentPresetName = name) }
        }
    }

    fun togglePresetPicker() { _uiState.update { it.copy(showPresetPicker = !it.showPresetPicker) } }
}
