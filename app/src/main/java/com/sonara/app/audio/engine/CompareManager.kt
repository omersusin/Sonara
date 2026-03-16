package com.sonara.app.audio.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A/B Compare: Original vs Sonara
 * Toggles EQ on/off with timed intervals so user can HEAR the difference.
 */
class CompareManager(private val audioEngine: AudioEngine) {
    
    private val _isComparing = MutableStateFlow(false)
    val isComparing: StateFlow<Boolean> = _isComparing.asStateFlow()

    private val _isOriginal = MutableStateFlow(false)
    val isOriginal: StateFlow<Boolean> = _isOriginal.asStateFlow()

    /**
     * Quick taste: 2.5s original → 2.5s Sonara
     */
    suspend fun quickCompare() {
        if (_isComparing.value) return
        _isComparing.value = true

        // Original (EQ off)
        _isOriginal.value = true
        audioEngine.setEnabled(false)
        delay(2500)

        // Sonara (EQ on)
        _isOriginal.value = false
        audioEngine.setEnabled(true)
        delay(2500)

        _isComparing.value = false
    }

    /**
     * Manual toggle
     */
    fun toggleOriginal(original: Boolean) {
        _isOriginal.value = original
        audioEngine.setEnabled(!original)
    }

    fun stopCompare() {
        _isComparing.value = false
        _isOriginal.value = false
        audioEngine.setEnabled(true)
    }
}
