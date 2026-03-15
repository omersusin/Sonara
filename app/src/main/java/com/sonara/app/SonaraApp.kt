package com.sonara.app

import android.app.Application
import com.sonara.app.audio.engine.AudioEngine
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.preferences.SonaraPreferences
import com.sonara.app.preset.PresetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SonaraApp : Application() {
    lateinit var preferences: SonaraPreferences private set
    lateinit var database: SonaraDatabase private set
    lateinit var presetRepository: PresetRepository private set
    lateinit var audioEngine: AudioEngine private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Shared EQ state — all screens observe this
    var currentBands: FloatArray = FloatArray(10); private set
    var currentPresetName: String = "Flat"; private set
    var isManualPreset: Boolean = false; private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = SonaraPreferences(this)
        database = SonaraDatabase.get(this)
        presetRepository = PresetRepository(database.presetDao())
        audioEngine = AudioEngine()
        audioEngine.init()

        appScope.launch {
            presetRepository.initBuiltIns()
            com.sonara.app.intelligence.cache.TrackCache(database.trackCacheDao()).cleanup()
        }
    }

    fun applyEqBands(bands: FloatArray, presetName: String = currentPresetName, manual: Boolean = false) {
        currentBands = bands.copyOf()
        currentPresetName = presetName
        isManualPreset = manual
        if (!audioEngine.isInitialized) audioEngine.init()
        audioEngine.applyBands(bands)
    }

    fun applyEffects(bass: Int = 0, virt: Int = 0, loud: Int = 0) {
        audioEngine.applyBassBoost(bass)
        audioEngine.applyVirtualizer(virt)
        audioEngine.applyLoudness(loud)
    }

    companion object {
        lateinit var instance: SonaraApp private set
    }
}
