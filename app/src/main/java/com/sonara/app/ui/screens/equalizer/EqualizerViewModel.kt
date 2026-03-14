package com.sonara.app.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import com.sonara.app.audio.equalizer.TenBandEqualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EqualizerUiState(
    val bands: FloatArray = TenBandEqualizer.defaultBands(),
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val isEnabled: Boolean = true,
    val currentPresetName: String = "Custom"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqualizerUiState) return false
        return bands.contentEquals(other.bands) &&
            preamp == other.preamp &&
            bassBoost == other.bassBoost &&
            virtualizer == other.virtualizer &&
            loudness == other.loudness &&
            isEnabled == other.isEnabled &&
            currentPresetName == other.currentPresetName
    }
    override fun hashCode(): Int = bands.contentHashCode()
}

class EqualizerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    fun setBand(index: Int, value: Float) {
        _uiState.update { state ->
            val newBands = state.bands.copyOf()
            newBands[index] = TenBandEqualizer.clamp(value)
            state.copy(bands = newBands, currentPresetName = "Custom")
        }
    }

    fun setPreamp(value: Float) {
        _uiState.update { it.copy(preamp = TenBandEqualizer.clamp(value)) }
    }

    fun setBassBoost(value: Int) {
        _uiState.update { it.copy(bassBoost = value.coerceIn(0, 1000)) }
    }

    fun setVirtualizer(value: Int) {
        _uiState.update { it.copy(virtualizer = value.coerceIn(0, 1000)) }
    }

    fun setLoudness(value: Int) {
        _uiState.update { it.copy(loudness = value.coerceIn(0, 1000)) }
    }

    fun setEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
    }

    fun resetBands() {
        _uiState.update { it.copy(
            bands = TenBandEqualizer.defaultBands(),
            preamp = 0f,
            currentPresetName = "Flat"
        ) }
    }

    fun applyPreset(name: String, bands: FloatArray) {
        _uiState.update { it.copy(
            bands = bands.copyOf(),
            currentPresetName = name
        ) }
    }
}
