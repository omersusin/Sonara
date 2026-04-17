/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
