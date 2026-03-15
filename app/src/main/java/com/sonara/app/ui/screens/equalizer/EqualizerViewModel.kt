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
    val preamp: Float = 0f, val bassBoost: Int = 0, val virtualizer: Int = 0, val loudness: Int = 0,
    val isEnabled: Boolean = true, val currentPresetName: String = "Flat",
    val availablePresets: List<Preset> = emptyList(), val eqActive: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true; if (other !is EqualizerUiState) return false
        return bands.contentEquals(other.bands) && bassBoost == other.bassBoost &&
            virtualizer == other.virtualizer && loudness == other.loudness &&
            isEnabled == other.isEnabled && currentPresetName == other.currentPresetName && eqActive == other.eqActive
    }
    override fun hashCode() = bands.contentHashCode()
}

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp

    private val _uiState = MutableStateFlow(EqualizerUiState(eqActive = app.audioEngine.isInitialized))
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        // Observe shared EQ state — syncs with Dashboard and Presets
        viewModelScope.launch {
            app.eqState.collect { eq ->
                _uiState.update { it.copy(
                    bands = eq.bands, bassBoost = eq.bassBoost, virtualizer = eq.virtualizer,
                    loudness = eq.loudness, currentPresetName = eq.presetName, isEnabled = eq.isEnabled
                ) }
            }
        }

        viewModelScope.launch {
            app.presetRepository.allPresets().collect { p ->
                _uiState.update { it.copy(availablePresets = p) }
            }
        }
    }

    fun setBand(i: Int, v: Float) {
        val bands = _uiState.value.bands.copyOf()
        bands[i] = TenBandEqualizer.clamp(v)
        app.applyEq(bands = bands, presetName = "Custom", manual = true,
            bassBoost = _uiState.value.bassBoost, virtualizer = _uiState.value.virtualizer, loudness = _uiState.value.loudness)
    }

    fun setPreamp(v: Float) { _uiState.update { it.copy(preamp = TenBandEqualizer.clamp(v)) } }

    fun setBassBoost(v: Int) {
        app.applyEq(bands = _uiState.value.bands, presetName = _uiState.value.currentPresetName,
            manual = true, bassBoost = v.coerceIn(0, 1000), virtualizer = _uiState.value.virtualizer, loudness = _uiState.value.loudness)
    }

    fun setVirtualizer(v: Int) {
        app.applyEq(bands = _uiState.value.bands, presetName = _uiState.value.currentPresetName,
            manual = true, bassBoost = _uiState.value.bassBoost, virtualizer = v.coerceIn(0, 1000), loudness = _uiState.value.loudness)
    }

    fun setLoudness(v: Int) {
        app.applyEq(bands = _uiState.value.bands, presetName = _uiState.value.currentPresetName,
            manual = true, bassBoost = _uiState.value.bassBoost, virtualizer = _uiState.value.virtualizer, loudness = v.coerceIn(0, 1000))
    }

    fun setEnabled(on: Boolean) { app.setEqEnabled(on) }

    fun resetBands() {
        app.applyEq(bands = FloatArray(10), presetName = "Flat", manual = false, bassBoost = 0, virtualizer = 0, loudness = 0)
    }

    fun applyPreset(preset: Preset) {
        app.applyEq(
            bands = preset.bandsArray(), presetName = preset.name, manual = true,
            bassBoost = preset.bassBoost, virtualizer = preset.virtualizer, loudness = preset.loudness
        )
        viewModelScope.launch { app.presetRepository.markUsed(preset.id) }
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val s = _uiState.value
            app.presetRepository.save(Preset(
                name = name, bands = Preset.fromArray(s.bands), preamp = s.preamp,
                bassBoost = s.bassBoost, virtualizer = s.virtualizer, loudness = s.loudness
            ))
        }
    }
}
