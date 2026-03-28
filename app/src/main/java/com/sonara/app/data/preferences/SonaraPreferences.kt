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
    private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    private val GEMINI_ENABLED = booleanPreferencesKey("gemini_enabled")
    private val GEMINI_MODEL = stringPreferencesKey("gemini_model")
    private val AI_PROVIDER = stringPreferencesKey("ai_provider")
    private val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
    private val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
    private val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
    private val GROQ_MODEL = stringPreferencesKey("groq_model")
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val DYNAMIC_COLORS_ENABLED = booleanPreferencesKey("dynamic_colors_enabled")
    private val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    private val KEEP_NOTIFICATION_PAUSED = booleanPreferencesKey("keep_notification_paused")
    private val SOURCE_LASTFM_ENABLED = booleanPreferencesKey("source_lastfm_enabled")
    private val SOURCE_LOCAL_AI_ENABLED = booleanPreferencesKey("source_local_ai_enabled")
    private val SOURCE_LYRICS_ENABLED = booleanPreferencesKey("source_lyrics_enabled")
    private val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
    private val KEY_HAS_SEEN_HEAR_DIFF = booleanPreferencesKey("has_seen_hear_the_difference")

    val accentColorFlow: Flow<AccentColor> = context.dataStore.data.map { p ->
        val name = p[ACCENT_COLOR] ?: AccentColor.Amber.name
        AccentColor.entries.find { it.name == name } ?: AccentColor.Amber
    }
    suspend fun setAccentColor(c: AccentColor) { context.dataStore.edit { it[ACCENT_COLOR] = c.name } }

    val lastFmApiKeyFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_API_KEY] ?: "" }
    suspend fun setLastFmApiKey(k: String) { context.dataStore.edit { it[LASTFM_API_KEY] = k } }

    val lastFmSharedSecretFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_SHARED_SECRET] ?: "" }
    suspend fun setLastFmSharedSecret(s: String) { context.dataStore.edit { it[LASTFM_SHARED_SECRET] = s } }

    val lastFmSessionKeyFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_SESSION_KEY] ?: "" }
    suspend fun setLastFmSessionKey(k: String) { context.dataStore.edit { it[LASTFM_SESSION_KEY] = k } }

    val aiEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[AI_ENABLED] ?: true }
    suspend fun setAiEnabled(e: Boolean) { context.dataStore.edit { it[AI_ENABLED] = e } }

    val autoEqEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTOEQ_ENABLED] ?: true }
    suspend fun setAutoEqEnabled(e: Boolean) { context.dataStore.edit { it[AUTOEQ_ENABLED] = e } }

    val smoothTransitionsFlow: Flow<Boolean> = context.dataStore.data.map { it[SMOOTH_TRANSITIONS] ?: true }
    suspend fun setSmoothTransitions(e: Boolean) { context.dataStore.edit { it[SMOOTH_TRANSITIONS] = e } }

    val safetyLimiterFlow: Flow<Boolean> = context.dataStore.data.map { it[SAFETY_LIMITER] ?: true }
    suspend fun setSafetyLimiter(e: Boolean) { context.dataStore.edit { it[SAFETY_LIMITER] = e } }

    val scrobblingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[SCROBBLING_ENABLED] ?: false }
    suspend fun setScrobblingEnabled(e: Boolean) { context.dataStore.edit { it[SCROBBLING_ENABLED] = e } }

    val autoPresetFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_PRESET] ?: true }
    suspend fun setAutoPreset(e: Boolean) { context.dataStore.edit { it[AUTO_PRESET] = e } }

    val notificationListenerPromptedFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATION_LISTENER_PROMPTED] ?: false }
    suspend fun setNotificationListenerPrompted(p: Boolean) { context.dataStore.edit { it[NOTIFICATION_LISTENER_PROMPTED] = p } }

    val songsLearnedFlow: Flow<Int> = context.dataStore.data.map { it[SONGS_LEARNED] ?: 0 }
    val songsViaLastFmFlow: Flow<Int> = context.dataStore.data.map { it[SONGS_VIA_LASTFM] ?: 0 }
    val songsViaLocalFlow: Flow<Int> = context.dataStore.data.map { it[SONGS_VIA_LOCAL] ?: 0 }
    val genreStatsFlow: Flow<String> = context.dataStore.data.map { it[GENRE_STATS] ?: "" }

    suspend fun incrementSongLearned(source: String, genre: String) {
        context.dataStore.edit { prefs ->
            prefs[SONGS_LEARNED] = (prefs[SONGS_LEARNED] ?: 0) + 1
            if (source.contains("lastfm", ignoreCase = true) || source.contains("merged", ignoreCase = true)) prefs[SONGS_VIA_LASTFM] = (prefs[SONGS_VIA_LASTFM] ?: 0) + 1
            else prefs[SONGS_VIA_LOCAL] = (prefs[SONGS_VIA_LOCAL] ?: 0) + 1
            val current = prefs[GENRE_STATS] ?: ""
            val map = parseGenreStats(current).toMutableMap()
            map[genre.lowercase()] = (map[genre.lowercase()] ?: 0) + 1
            prefs[GENRE_STATS] = serializeGenreStats(map)
        }
    }

    val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { it[GEMINI_API_KEY] ?: "" }
    suspend fun setGeminiApiKey(k: String) { context.dataStore.edit { it[GEMINI_API_KEY] = k } }

    val geminiEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[GEMINI_ENABLED] ?: false }
    suspend fun setGeminiEnabled(e: Boolean) { context.dataStore.edit { it[GEMINI_ENABLED] = e } }

    val geminiModelFlow: Flow<String> = context.dataStore.data.map { it[GEMINI_MODEL] ?: "fast" }
    suspend fun setGeminiModel(m: String) { context.dataStore.edit { it[GEMINI_MODEL] = m } }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "dark" }
    suspend fun setThemeMode(m: String) { context.dataStore.edit { it[THEME_MODE] = m } }

    val dynamicColorsFlow: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLORS_ENABLED] ?: false }
    suspend fun setDynamicColors(e: Boolean) { context.dataStore.edit { it[DYNAMIC_COLORS_ENABLED] = e } }

    val highContrastFlow: Flow<Boolean> = context.dataStore.data.map { it[HIGH_CONTRAST] ?: false }
    suspend fun setHighContrast(e: Boolean) { context.dataStore.edit { it[HIGH_CONTRAST] = e } }

    val keepNotificationPausedFlow: Flow<Boolean> = context.dataStore.data.map { it[KEEP_NOTIFICATION_PAUSED] ?: true }
    suspend fun setKeepNotificationPaused(e: Boolean) { context.dataStore.edit { it[KEEP_NOTIFICATION_PAUSED] = e } }

    val sourceLastFmEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[SOURCE_LASTFM_ENABLED] ?: true }
    suspend fun setSourceLastFmEnabled(e: Boolean) { context.dataStore.edit { it[SOURCE_LASTFM_ENABLED] = e } }

    val sourceLocalAiEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[SOURCE_LOCAL_AI_ENABLED] ?: true }
    suspend fun setSourceLocalAiEnabled(e: Boolean) { context.dataStore.edit { it[SOURCE_LOCAL_AI_ENABLED] = e } }

    val sourceLyricsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[SOURCE_LYRICS_ENABLED] ?: true }
    suspend fun setSourceLyricsEnabled(e: Boolean) { context.dataStore.edit { it[SOURCE_LYRICS_ENABLED] = e } }

    val amoledModeFlow: Flow<Boolean> = context.dataStore.data.map { it[AMOLED_MODE] ?: false }
    val hasSeenHearTheDifferenceFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_SEEN_HEAR_DIFF] ?: false }
    suspend fun setAmoledMode(e: Boolean) { context.dataStore.edit { it[AMOLED_MODE] = e } }

    val aiProviderFlow: Flow<String> = context.dataStore.data.map { it[AI_PROVIDER] ?: "gemini" }
    suspend fun setAiProvider(v: String) { context.dataStore.edit { it[AI_PROVIDER] = v } }

    val openRouterApiKeyFlow: Flow<String> = context.dataStore.data.map { it[OPENROUTER_API_KEY] ?: "" }
    suspend fun setOpenRouterApiKey(v: String) { context.dataStore.edit { it[OPENROUTER_API_KEY] = v } }
    val openRouterModelFlow: Flow<String> = context.dataStore.data.map { it[OPENROUTER_MODEL] ?: "google/gemini-2.5-flash" }
    suspend fun setOpenRouterModel(v: String) { context.dataStore.edit { it[OPENROUTER_MODEL] = v } }

    val groqApiKeyFlow: Flow<String> = context.dataStore.data.map { it[GROQ_API_KEY] ?: "" }
    suspend fun setGroqApiKey(v: String) { context.dataStore.edit { it[GROQ_API_KEY] = v } }
    val groqModelFlow: Flow<String> = context.dataStore.data.map { it[GROQ_MODEL] ?: "llama-3.3-70b-versatile" }
    suspend fun setGroqModel(v: String) { context.dataStore.edit { it[GROQ_MODEL] = v } }

    suspend fun setHasSeenHearTheDifference(value: Boolean) {
        context.dataStore.edit { it[KEY_HAS_SEEN_HEAR_DIFF] = value }
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
