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
    val availablePresets: List<Preset> = emptyList(), val eqActive: Boolean = false, val isClipping: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true; if (other !is EqualizerUiState) return false
        return bands.contentEquals(other.bands) && preamp == other.preamp && bassBoost == other.bassBoost &&
            virtualizer == other.virtualizer && loudness == other.loudness && isEnabled == other.isEnabled &&
            currentPresetName == other.currentPresetName && eqActive == other.eqActive && isClipping == other.isClipping
    }
    override fun hashCode() = bands.contentHashCode()
}

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp

    private val _uiState = MutableStateFlow(EqualizerUiState(eqActive = app.audioEngine.isInitialized))
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            app.eqState.collect { eq ->
                _uiState.update { it.copy(bands = eq.bands, bassBoost = eq.bassBoost, virtualizer = eq.virtualizer,
                    loudness = eq.loudness, currentPresetName = eq.presetName, isEnabled = eq.isEnabled) }
            }
        }
        viewModelScope.launch { app.presetRepository.allPresets().collect { p -> _uiState.update { it.copy(availablePresets = p) } } }
    }

    private fun current() = _uiState.value

    fun setBand(i: Int, v: Float) {
        val bands = current().bands.copyOf()
        bands[i] = TenBandEqualizer.clamp(if (v in -0.5f..0.5f) 0f else v) // Snap to zero
        app.applyEq(bands = bands, presetName = "Custom", manual = true,
            bassBoost = current().bassBoost, virtualizer = current().virtualizer, loudness = current().loudness, preamp = current().preamp)
    }

    fun setPreamp(v: Float) {
        val clamped = TenBandEqualizer.clamp(v)
        _uiState.update { it.copy(preamp = clamped) }
        app.applyEq(bands = current().bands, presetName = current().currentPresetName, manual = true,
            bassBoost = current().bassBoost, virtualizer = current().virtualizer, loudness = current().loudness, preamp = clamped)
    }

    fun setBassBoost(v: Int) { app.applyEq(bands = current().bands, presetName = current().currentPresetName, manual = true, bassBoost = v.coerceIn(0, 1000), virtualizer = current().virtualizer, loudness = current().loudness) }
    fun setVirtualizer(v: Int) { app.applyEq(bands = current().bands, presetName = current().currentPresetName, manual = true, bassBoost = current().bassBoost, virtualizer = v.coerceIn(0, 1000), loudness = current().loudness) }
    fun setLoudness(v: Int) { app.applyEq(bands = current().bands, presetName = current().currentPresetName, manual = true, bassBoost = current().bassBoost, virtualizer = current().virtualizer, loudness = v.coerceIn(0, 1000)) }
    fun setEnabled(on: Boolean) { app.setEqEnabled(on) }

    fun resetBands() { app.applyEq(FloatArray(10), "Flat", manual = false, 0, 0, 0); _uiState.update { it.copy(preamp = 0f) } }

    fun applyPreset(preset: Preset) {
        app.applyEq(preset.bandsArray(), preset.name, manual = true, preset.bassBoost, preset.virtualizer, preset.loudness)
        viewModelScope.launch { app.presetRepository.markUsed(preset.id) }
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val s = current()
            app.presetRepository.save(Preset(name = name, bands = Preset.fromArray(s.bands), preamp = s.preamp, bassBoost = s.bassBoost, virtualizer = s.virtualizer, loudness = s.loudness))
        }
    }
}
