package com.sonara.app.audio.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CompareManager(private val sessionManager: AudioSessionManager) {
    private val _isComparing = MutableStateFlow(false)
    val isComparing: StateFlow<Boolean> = _isComparing.asStateFlow()
    private val _isOriginal = MutableStateFlow(false)
    val isOriginal: StateFlow<Boolean> = _isOriginal.asStateFlow()

    suspend fun quickCompare() {
        if (_isComparing.value) return
        _isComparing.value = true
        _isOriginal.value = true; sessionManager.setEnabled(false); delay(2500)
        _isOriginal.value = false; sessionManager.setEnabled(true); delay(2500)
        _isComparing.value = false
    }

    fun stopCompare() { _isComparing.value = false; _isOriginal.value = false; sessionManager.setEnabled(true) }
}
