package com.sonara.app

import android.app.Application
import com.sonara.app.audio.engine.AudioEngine
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.preferences.SonaraPreferences
import com.sonara.app.preset.PresetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SonaraApp : Application() {
    lateinit var preferences: SonaraPreferences private set
    lateinit var database: SonaraDatabase private set
    lateinit var presetRepository: PresetRepository private set
    val audioEngine = AudioEngine()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeSessionId = MutableStateFlow(-1)
    val activeSessionId: StateFlow<Int> = _activeSessionId.asStateFlow()

    private val _pendingBands = MutableStateFlow<FloatArray?>(null)

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = SonaraPreferences(this)
        database = SonaraDatabase.get(this)
        presetRepository = PresetRepository(database.presetDao())

        appScope.launch {
            presetRepository.initBuiltIns()
            com.sonara.app.intelligence.cache.TrackCache(database.trackCacheDao()).cleanup()
        }
    }

    fun onAudioSessionOpen(sessionId: Int, pkg: String) {
        val success = audioEngine.init(sessionId)
        if (success) {
            _activeSessionId.value = sessionId
            _pendingBands.value?.let { bands ->
                audioEngine.applyBands(bands)
            }
        }
    }

    fun onAudioSessionClose(sessionId: Int) {
        if (audioEngine.sessionId == sessionId) {
            audioEngine.release()
            _activeSessionId.value = -1
        }
    }

    fun applyEqBands(bands: FloatArray) {
        _pendingBands.value = bands
        if (audioEngine.isInitialized) {
            audioEngine.applyBands(bands)
        }
    }

    companion object {
        lateinit var instance: SonaraApp private set
    }
}
