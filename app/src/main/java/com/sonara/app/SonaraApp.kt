package com.sonara.app

import android.app.Application
import android.util.Log
import com.sonara.app.audio.engine.AudioEngine
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.models.SharedEqState
import com.sonara.app.data.preferences.SonaraPreferences
import com.sonara.app.preset.PresetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SonaraApp : Application() {
    lateinit var preferences: SonaraPreferences private set
    lateinit var database: SonaraDatabase private set
    lateinit var presetRepository: PresetRepository private set
    val audioEngine = AudioEngine()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _eqState = MutableStateFlow(SharedEqState())
    val eqState: StateFlow<SharedEqState> = _eqState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = SonaraPreferences(this)
        database = SonaraDatabase.get(this)
        presetRepository = PresetRepository(database.presetDao())

        val ok = audioEngine.init()
        Log.d("SonaraApp", "AudioEngine init: $ok")

        appScope.launch {
            presetRepository.initBuiltIns()
            com.sonara.app.intelligence.cache.TrackCache(database.trackCacheDao()).cleanup()
        }
    }

    fun applyEq(
        bands: FloatArray,
        presetName: String = _eqState.value.presetName,
        manual: Boolean = _eqState.value.isManualPreset,
        bassBoost: Int = _eqState.value.bassBoost,
        virtualizer: Int = _eqState.value.virtualizer,
        loudness: Int = _eqState.value.loudness
    ) {
        if (!audioEngine.isInitialized) audioEngine.init()
        audioEngine.applyBands(bands)
        audioEngine.applyBassBoost(bassBoost)
        audioEngine.applyVirtualizer(virtualizer)
        audioEngine.applyLoudness(loudness)

        _eqState.update {
            it.copy(
                bands = bands.copyOf(),
                bassBoost = bassBoost,
                virtualizer = virtualizer,
                loudness = loudness,
                presetName = presetName,
                isManualPreset = manual
            )
        }
        Log.d("SonaraApp", "Applied EQ: $presetName manual=$manual bass=$bassBoost virt=$virtualizer")
    }

    fun setEqEnabled(enabled: Boolean) {
        audioEngine.setEnabled(enabled)
        _eqState.update { it.copy(isEnabled = enabled) }
    }

    fun resetToAi() {
        _eqState.update { it.copy(isManualPreset = false, presetName = "AI Auto") }
    }

    companion object {
        lateinit var instance: SonaraApp private set
    }
}
