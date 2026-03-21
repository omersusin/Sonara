package com.sonara.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Madde 7: Tema ayarları.
 * ThemeMode: System / Light / Dark
 * Dynamic Colors: on/off
 * High Contrast: on/off
 */
class ThemePreferences(private val context: Context, private val prefs: SonaraPreferences) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        private val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        private val KEEP_NOTIFICATION_PAUSED = booleanPreferencesKey("keep_notification_paused")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val GEMINI_ENABLED = booleanPreferencesKey("gemini_enabled")
        private val GEMINI_MODEL = stringPreferencesKey("gemini_model")
    }

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    // Not: Bu alanlar mevcut DataStore'u kullanır.
    // Gerçek implementasyonda SonaraPreferences'a eklenmeli.
    // Burada ayrı tutuyoruz ki mevcut kod kırılmasın.
}
