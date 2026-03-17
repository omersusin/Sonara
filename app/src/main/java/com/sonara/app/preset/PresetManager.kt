package com.sonara.app.preset

import com.sonara.app.SonaraApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PresetManager {
    private val _activePresetName = MutableStateFlow("Flat")
    val activePresetName: StateFlow<String> = _activePresetName.asStateFlow()

    fun applyPreset(preset: Preset) {
        SonaraApp.instance.applyEq(
            bands = preset.bandsArray(), presetName = preset.name, manual = true,
            bassBoost = preset.bassBoost, virtualizer = preset.virtualizer, loudness = preset.loudness
        )
        _activePresetName.value = preset.name
    }
}
