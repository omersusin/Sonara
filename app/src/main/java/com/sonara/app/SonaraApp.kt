package com.sonara.app

import android.app.Application
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

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    companion object {
        lateinit var instance: SonaraApp private set
    }
}
