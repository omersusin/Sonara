package com.sonara.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sonara.app.ui.theme.AccentColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sonara_prefs")

class SonaraPreferences(private val context: Context) {

    companion object {
        private val ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val LASTFM_API_KEY = stringPreferencesKey("lastfm_api_key")
        private val LASTFM_SHARED_SECRET = stringPreferencesKey("lastfm_shared_secret")
        private val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        private val AUTOEQ_ENABLED = booleanPreferencesKey("autoeq_enabled")
    }

    val accentColorFlow: Flow<AccentColor> = context.dataStore.data.map { prefs ->
        val name = prefs[ACCENT_COLOR] ?: AccentColor.Amber.name
        AccentColor.entries.find { it.name == name } ?: AccentColor.Amber
    }

    suspend fun setAccentColor(color: AccentColor) {
        context.dataStore.edit { it[ACCENT_COLOR] = color.name }
    }

    val lastFmApiKeyFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_API_KEY] ?: "" }

    suspend fun setLastFmApiKey(key: String) {
        context.dataStore.edit { it[LASTFM_API_KEY] = key }
    }

    val lastFmSharedSecretFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_SHARED_SECRET] ?: "" }

    suspend fun setLastFmSharedSecret(secret: String) {
        context.dataStore.edit { it[LASTFM_SHARED_SECRET] = secret }
    }

    val aiEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[AI_ENABLED] ?: true }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[AI_ENABLED] = enabled }
    }

    val autoEqEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTOEQ_ENABLED] ?: true }

    suspend fun setAutoEqEnabled(enabled: Boolean) {
        context.dataStore.edit { it[AUTOEQ_ENABLED] = enabled }
    }
}
