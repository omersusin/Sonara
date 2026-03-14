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

    fun onHeadphoneChanged(info: HeadphoneInfo, autoEqEnabled: Boolean) {
        if (!info.isConnected || !autoEqEnabled) {
            _state.value = AutoEqState(headphone = info)
            return
        }
        val profile = AutoEqDatabase.findProfile(info.name)
        _state.value = AutoEqState(
            isActive = profile != null,
            profile = profile,
            headphone = info,
            correctionBands = profile?.correctionBands ?: FloatArray(10)
        )
    }

    fun setManualProfile(profileName: String) {
        val profile = AutoEqDatabase.findProfile(profileName)
        profile?.let {
            _state.value = _state.value.copy(isActive = true, profile = it, correctionBands = it.correctionBands)
        }
    }

    fun disable() {
        _state.value = _state.value.copy(isActive = false, correctionBands = FloatArray(10))
    }
}
