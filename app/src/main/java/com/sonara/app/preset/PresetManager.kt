package com.sonara.app.preset

import com.sonara.app.audio.engine.AudioSessionManager
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
    val autoEqBands: FloatArray = FloatArray(10),
    val aiBands: FloatArray = FloatArray(10),
    val finalBands: FloatArray = FloatArray(10),
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val isClipping: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActiveSoundState) return false
        return presetName == other.presetName && finalBands.contentEquals(other.finalBands)
    }
    override fun hashCode() = finalBands.contentHashCode()
}

class PresetManager(private val sessionManager: AudioSessionManager) {
    private val transitionEngine = SmoothTransitionEngine()
    private val _activeState = MutableStateFlow(ActiveSoundState())
    val activeState: StateFlow<ActiveSoundState> = _activeState.asStateFlow()

    fun applyPreset(preset: Preset, autoEqState: AutoEqState, aiBands: FloatArray = FloatArray(10)) {
        val presetBands = preset.bandsArray()
        val autoEqBands = if (autoEqState.isActive) autoEqState.correctionBands else FloatArray(10)
        val composed = ProfileComposer.compose(presetBands, autoEqBands, aiBands, FloatArray(10))
        val (safeBands, safePreamp) = SafetyLimiter.limit(composed.bands, composed.preamp + preset.preamp)
        val isClipping = SafetyLimiter.wouldClip(composed.bands, composed.preamp + preset.preamp)

        sessionManager.applyBands(safeBands)
        sessionManager.applyBass(preset.bassBoost)
        sessionManager.applyVirt(preset.virtualizer)
        sessionManager.applyLoudness(preset.loudness)

        _activeState.value = ActiveSoundState(preset.name, presetBands, autoEqBands, aiBands, safeBands, safePreamp, preset.bassBoost, preset.virtualizer, preset.loudness, isClipping)
    }

    fun applyBands(bands: FloatArray, preamp: Float = 0f) {
        val (safeBands, safePreamp) = SafetyLimiter.limit(bands, preamp)
        sessionManager.applyBands(safeBands)
        _activeState.value = _activeState.value.copy(finalBands = safeBands, preamp = safePreamp)
    }

    suspend fun smoothTransition(fromBands: FloatArray, toBands: FloatArray) {
        transitionEngine.transition(fromBands, toBands) { step -> sessionManager.applyBands(step) }
    }
}
