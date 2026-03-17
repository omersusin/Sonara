package com.sonara.app.preset

import com.sonara.app.SonaraApp
import com.sonara.app.audio.engine.SafetyLimiter
import com.sonara.app.audio.engine.SmoothTransitionEngine
import com.sonara.app.audio.equalizer.ProfileComposer
import com.sonara.app.autoeq.AutoEqState
import com.sonara.app.data.models.EqProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveSoundState(
    val presetName: String = "Flat",
    val presetBands: FloatArray = FloatArray(10),
    val finalBands: FloatArray = FloatArray(10),
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val isClipping: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true; if (other !is ActiveSoundState) return false
        return presetName == other.presetName && finalBands.contentEquals(other.finalBands)
    }
    override fun hashCode() = finalBands.contentHashCode()
}

class PresetManager {
    private val _activeState = MutableStateFlow(ActiveSoundState())
    val activeState: StateFlow<ActiveSoundState> = _activeState.asStateFlow()

    fun applyPreset(preset: Preset, autoEqState: AutoEqState = AutoEqState(), aiBands: FloatArray = FloatArray(10)) {
        val presetBands = preset.bandsArray()
        val composed = ProfileComposer.compose(presetBands, if (autoEqState.isActive) autoEqState.correctionBands else FloatArray(10), aiBands, FloatArray(10))
        val (safeBands, safePreamp) = SafetyLimiter.limit(composed.bands, composed.preamp + preset.preamp)

        // Apply through SonaraApp central method
        SonaraApp.instance.applyEq(
            bands = safeBands,
            presetName = preset.name,
            manual = true,
            bassBoost = preset.bassBoost,
            virtualizer = preset.virtualizer,
            loudness = preset.loudness,
            preamp = safePreamp
        )

        _activeState.value = ActiveSoundState(preset.name, presetBands, safeBands, safePreamp, preset.bassBoost, preset.virtualizer, preset.loudness, SafetyLimiter.wouldClip(composed.bands, composed.preamp))
    }
}
