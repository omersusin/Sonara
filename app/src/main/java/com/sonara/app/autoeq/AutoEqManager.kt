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

package com.sonara.app.autoeq

import com.sonara.app.data.models.HeadphoneInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AutoEqState(
    val isActive: Boolean = false,
    val profile: HeadphoneProfile? = null,
    val headphone: HeadphoneInfo = HeadphoneInfo(),
    val correctionBands: FloatArray = FloatArray(10)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AutoEqState) return false
        return isActive == other.isActive && profile == other.profile &&
            headphone == other.headphone && correctionBands.contentEquals(other.correctionBands)
    }
    override fun hashCode() = profile.hashCode()
}

class AutoEqManager {
    private val _state = MutableStateFlow(AutoEqState())
    val state: StateFlow<AutoEqState> = _state.asStateFlow()

    fun onHeadphoneChanged(info: HeadphoneInfo, autoEqEnabled: Boolean, context: android.content.Context? = null) {
        if (!info.isConnected || !autoEqEnabled) {
            _state.value = AutoEqState(headphone = info)
            return
        }
        val profile = if (context != null) AutoEqDatabase.findProfileExtended(info.name, context) else AutoEqDatabase.findProfile(info.name)
        _state.value = AutoEqState(
            isActive = profile != null,
            profile = profile,
            headphone = info,
            correctionBands = profile?.correctionBands ?: FloatArray(10)
        )
    }

    fun setManualProfile(profileName: String, context: android.content.Context? = null) {
        val profile = AutoEqDatabase.findProfile(profileName)
            ?: (if (context != null) WaveletAutoEqLoader.findProfile(profileName, context) else null)
        if (profile != null) {
            _state.value = _state.value.copy(isActive = true, profile = profile, correctionBands = profile.correctionBands)
        }
    }

    fun disable() {
        _state.value = _state.value.copy(isActive = false, correctionBands = FloatArray(10))
    }
}
