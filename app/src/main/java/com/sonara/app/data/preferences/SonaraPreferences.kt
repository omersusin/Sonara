package com.sonara.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        private val LASTFM_SESSION_KEY = stringPreferencesKey("lastfm_session_key")
        private val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        private val AUTOEQ_ENABLED = booleanPreferencesKey("autoeq_enabled")
        private val SMOOTH_TRANSITIONS = booleanPreferencesKey("smooth_transitions")
        private val SAFETY_LIMITER = booleanPreferencesKey("safety_limiter")
        private val SCROBBLING_ENABLED = booleanPreferencesKey("scrobbling_enabled")
        private val AUTO_PRESET = booleanPreferencesKey("auto_preset")
        private val NOTIFICATION_LISTENER_PROMPTED = booleanPreferencesKey("notification_listener_prompted")

        private val SONGS_LEARNED = intPreferencesKey("songs_learned")
        private val SONGS_VIA_LASTFM = intPreferencesKey("songs_via_lastfm")
        private val SONGS_VIA_LOCAL = intPreferencesKey("songs_via_local")
        private val GENRE_STATS = stringPreferencesKey("genre_stats")
    }

    val accentColorFlow: Flow<AccentColor> = context.dataStore.data.map { prefs ->
        val name = prefs[ACCENT_COLOR] ?: AccentColor.Amber.name
        AccentColor.entries.find { it.name == name } ?: AccentColor.Amber
    }
    suspend fun setAccentColor(color: AccentColor) { context.dataStore.edit { it[ACCENT_COLOR] = color.name } }

    val lastFmApiKeyFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_API_KEY] ?: "" }
    suspend fun setLastFmApiKey(key: String) { context.dataStore.edit { it[LASTFM_API_KEY] = key } }

    val lastFmSharedSecretFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_SHARED_SECRET] ?: "" }
    suspend fun setLastFmSharedSecret(secret: String) { context.dataStore.edit { it[LASTFM_SHARED_SECRET] = secret } }

    val lastFmSessionKeyFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_SESSION_KEY] ?: "" }
    suspend fun setLastFmSessionKey(key: String) { context.dataStore.edit { it[LASTFM_SESSION_KEY] = key } }

    val aiEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[AI_ENABLED] ?: true }
    suspend fun setAiEnabled(enabled: Boolean) { context.dataStore.edit { it[AI_ENABLED] = enabled } }

    val autoEqEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTOEQ_ENABLED] ?: true }
    suspend fun setAutoEqEnabled(enabled: Boolean) { context.dataStore.edit { it[AUTOEQ_ENABLED] = enabled } }

    val smoothTransitionsFlow: Flow<Boolean> = context.dataStore.data.map { it[SMOOTH_TRANSITIONS] ?: true }
    suspend fun setSmoothTransitions(enabled: Boolean) { context.dataStore.edit { it[SMOOTH_TRANSITIONS] = enabled } }

    val safetyLimiterFlow: Flow<Boolean> = context.dataStore.data.map { it[SAFETY_LIMITER] ?: true }
    suspend fun setSafetyLimiter(enabled: Boolean) { context.dataStore.edit { it[SAFETY_LIMITER] = enabled } }

    val scrobblingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[SCROBBLING_ENABLED] ?: false }
    suspend fun setScrobblingEnabled(enabled: Boolean) { context.dataStore.edit { it[SCROBBLING_ENABLED] = enabled } }

    val autoPresetFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_PRESET] ?: true }
    suspend fun setAutoPreset(enabled: Boolean) { context.dataStore.edit { it[AUTO_PRESET] = enabled } }

    val notificationListenerPromptedFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATION_LISTENER_PROMPTED] ?: false }
    suspend fun setNotificationListenerPrompted(prompted: Boolean) { context.dataStore.edit { it[NOTIFICATION_LISTENER_PROMPTED] = prompted } }

    // Learning stats
    val songsLearnedFlow: Flow<Int> = context.dataStore.data.map { it[SONGS_LEARNED] ?: 0 }
    val songsViaLastFmFlow: Flow<Int> = context.dataStore.data.map { it[SONGS_VIA_LASTFM] ?: 0 }
    val songsViaLocalFlow: Flow<Int> = context.dataStore.data.map { it[SONGS_VIA_LOCAL] ?: 0 }
    val genreStatsFlow: Flow<String> = context.dataStore.data.map { it[GENRE_STATS] ?: "" }

    suspend fun incrementSongLearned(source: String, genre: String) {
        context.dataStore.edit { prefs ->
            prefs[SONGS_LEARNED] = (prefs[SONGS_LEARNED] ?: 0) + 1
            if (source.contains("lastfm")) prefs[SONGS_VIA_LASTFM] = (prefs[SONGS_VIA_LASTFM] ?: 0) + 1
            else prefs[SONGS_VIA_LOCAL] = (prefs[SONGS_VIA_LOCAL] ?: 0) + 1

            val current = prefs[GENRE_STATS] ?: ""
            val map = parseGenreStats(current).toMutableMap()
            map[genre.lowercase()] = (map[genre.lowercase()] ?: 0) + 1
            prefs[GENRE_STATS] = serializeGenreStats(map)
        }
    }

    suspend fun resetAll() { context.dataStore.edit { it.clear() } }

    companion object {
        fun parseGenreStats(raw: String): Map<String, Int> {
            if (raw.isBlank()) return emptyMap()
            return raw.split(";").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
            }.toMap()
        }

        fun serializeGenreStats(map: Map<String, Int>): String {
            return map.entries.sortedByDescending { it.value }.joinToString(";") { "${it.key}:${it.value}" }
        }
    }
}
