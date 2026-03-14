package com.sonara.app

import android.app.Application
import com.sonara.app.data.preferences.SonaraPreferences

class SonaraApp : Application() {
    lateinit var preferences: SonaraPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = SonaraPreferences(this)
    }

    companion object {
        lateinit var instance: SonaraApp
            private set
    }
}
