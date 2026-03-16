package com.sonara.app.preset

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
        return presetName == other.presetName && finalBands.contentEquals(other.finalBands) &&
            preamp == other.preamp && bassBoost == other.bassBoost
    }
    override fun hashCode() = finalBands.contentHashCode()
}

class PresetManager(private val audioEngine: AudioEngine) {
    private val transitionEngine = SmoothTransitionEngine()

    private val _activeState = MutableStateFlow(ActiveSoundState())
    val activeState: StateFlow<ActiveSoundState> = _activeState.asStateFlow()

    fun applyPreset(preset: Preset, autoEqState: AutoEqState, aiBands: FloatArray = FloatArray(10)) {
        val presetBands = preset.bandsArray()
        val autoEqBands = if (autoEqState.isActive) autoEqState.correctionBands else FloatArray(10)

        val composed = ProfileComposer.compose(
            presetBands = presetBands,
            autoEqBands = autoEqBands,
            aiBands = aiBands,
            manualBands = FloatArray(10)
        )

        val (safeBands, safePreamp) = SafetyLimiter.limit(composed.bands, composed.preamp + preset.preamp)
        val isClipping = SafetyLimiter.wouldClip(composed.bands, composed.preamp + preset.preamp)

        audioEngine.applyBands(safeBands)
        audioEngine.applyBassBoost(preset.bassBoost)
        audioEngine.applyVirtualizer(preset.virtualizer)
        audioEngine.applyLoudness(preset.loudness)

        _activeState.value = ActiveSoundState(
            presetName = preset.name,
            presetBands = presetBands,
            autoEqBands = autoEqBands,
            aiBands = aiBands,
            finalBands = safeBands,
            preamp = safePreamp,
            bassBoost = preset.bassBoost,
            virtualizer = preset.virtualizer,
            loudness = preset.loudness,
            isClipping = isClipping
        )
    }

    fun applyBands(bands: FloatArray, preamp: Float = 0f) {
        val (safeBands, safePreamp) = SafetyLimiter.limit(bands, preamp)
        audioEngine.applyBands(safeBands)
        _activeState.value = _activeState.value.copy(finalBands = safeBands, preamp = safePreamp)
    }

    suspend fun smoothTransition(fromBands: FloatArray, toBands: FloatArray) {
        transitionEngine.transition(fromBands, toBands) { step ->
            audioEngine.applyBands(step)
        }
    }
}
