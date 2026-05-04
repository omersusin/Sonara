package com.sonara.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.graphics.Color
import com.sonara.app.ui.theme.AccentSeeds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sonara_prefs")

class SonaraPreferences(private val context: Context) {

    private val ACCENT_COLOR = stringPreferencesKey("accent_color")  // legacy key – kept for migration
    private val ACCENT_SEED  = stringPreferencesKey("accent_seed")
    private val LASTFM_API_KEY = stringPreferencesKey("lastfm_api_key")
    private val LASTFM_SHARED_SECRET = stringPreferencesKey("lastfm_shared_secret")
    private val LASTFM_SESSION_KEY = stringPreferencesKey("lastfm_session_key")
    private val LASTFM_USERNAME = stringPreferencesKey("lastfm_username")
    // Scrobble app filter
    private val ALLOWED_SCROBBLE_APPS = stringPreferencesKey("allowed_scrobble_apps")
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
    private val HF_API_KEY = stringPreferencesKey("hf_api_key")
    private val HF_MODEL = stringPreferencesKey("hf_model")
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val DYNAMIC_COLORS_ENABLED = booleanPreferencesKey("dynamic_colors_enabled")
    private val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    private val KEEP_NOTIFICATION_PAUSED = booleanPreferencesKey("keep_notification_paused")
    private val SOURCE_LASTFM_ENABLED = booleanPreferencesKey("source_lastfm_enabled")
    private val SOURCE_LOCAL_AI_ENABLED = booleanPreferencesKey("source_local_ai_enabled")
    private val SOURCE_LYRICS_ENABLED = booleanPreferencesKey("source_lyrics_enabled")
    private val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
    private val LEGACY_ANALYSIS = booleanPreferencesKey("legacy_analysis_layout")
    private val HEAR_THE_DIFF_ENABLED = booleanPreferencesKey("hear_the_difference_enabled")
    private val KEY_HAS_SEEN_HEAR_DIFF = booleanPreferencesKey("has_seen_hear_the_difference")
    private val COMMUNITY_SYNC_INTERVAL = intPreferencesKey("community_sync_interval")
    private val KEY_LYRICS_ANIMATION = stringPreferencesKey("lyrics_animation_style")
    private val KEY_LYRICS_TEXT_SIZE = floatPreferencesKey("lyrics_text_size")
    private val KEY_LYRICS_SYNC_OFFSET = intPreferencesKey("lyrics_sync_offset_ms")
    private val KEY_LYRICS_SHOW_TRANSLATED = booleanPreferencesKey("lyrics_show_translated")
    private val KEY_LYRICS_TARGET_LANGUAGE = stringPreferencesKey("lyrics_target_language")
    private val SELECTED_FONT          = stringPreferencesKey("selected_font")
    private val SELECTED_PALETTE_STYLE = stringPreferencesKey("selected_palette_style")

    // Seed-based accent (MD3E). On first read, migrates legacy enum name to hex seed.
    val accentSeedFlow: Flow<Color> = context.dataStore.data.map { p ->
        val hex = p[ACCENT_SEED]
        if (hex != null) {
            AccentSeeds.fromHex(hex)
        } else {
            // Migrate from old enum name, default to Amber
            AccentSeeds.fromLegacyName(p[ACCENT_COLOR] ?: "amber")
        }
    }

    suspend fun setAccentSeed(seed: Color) {
        context.dataStore.edit {
            it[ACCENT_SEED] = AccentSeeds.toHex(seed)
            it.remove(ACCENT_COLOR)  // clear legacy key after first write
        }
    }

    // VULN-25 note: prefer SecureSecrets for sensitive keys. These DataStore flows
    // are kept for backward compatibility in NLS/Onboarding/scrobbling code paths.
    val lastFmApiKeyFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_API_KEY] ?: "" }
    suspend fun setLastFmApiKey(k: String) { context.dataStore.edit { it[LASTFM_API_KEY] = k } }
    val lastFmSharedSecretFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_SHARED_SECRET] ?: "" }
    suspend fun setLastFmSharedSecret(s: String) { context.dataStore.edit { it[LASTFM_SHARED_SECRET] = s } }
    val lastFmSessionKeyFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_SESSION_KEY] ?: "" }
    suspend fun setLastFmSessionKey(k: String) { context.dataStore.edit { it[LASTFM_SESSION_KEY] = k } }

    val lastFmUsernameFlow: Flow<String> = context.dataStore.data.map { it[LASTFM_USERNAME] ?: "" }
    suspend fun setLastFmUsername(u: String) { context.dataStore.edit { it[LASTFM_USERNAME] = u } }

    // Scrobble app filter
    val allowedScrobbleAppsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[ALLOWED_SCROBBLE_APPS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }
    suspend fun setAllowedScrobbleApps(apps: Set<String>) {
        context.dataStore.edit { it[ALLOWED_SCROBBLE_APPS] = apps.joinToString(",") }
    }

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
    val legacyAnalysisFlow: Flow<Boolean> = context.dataStore.data.map { it[LEGACY_ANALYSIS] ?: false }
    val hearTheDiffEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[HEAR_THE_DIFF_ENABLED] ?: true }
    suspend fun setLegacyAnalysis(v: Boolean) { context.dataStore.edit { it[LEGACY_ANALYSIS] = v } }
    suspend fun setHearTheDiffEnabled(v: Boolean) { context.dataStore.edit { it[HEAR_THE_DIFF_ENABLED] = v } }

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

    val huggingFaceApiKeyFlow: Flow<String> = context.dataStore.data.map { it[HF_API_KEY] ?: "" }
    suspend fun setHuggingFaceApiKey(v: String) { context.dataStore.edit { it[HF_API_KEY] = v } }
    val huggingFaceModelFlow: Flow<String> = context.dataStore.data.map { it[HF_MODEL] ?: "meta-llama/Meta-Llama-3.1-8B-Instruct" }
    suspend fun setHuggingFaceModel(v: String) { context.dataStore.edit { it[HF_MODEL] = v } }

    val communitySyncIntervalFlow: Flow<Int> = context.dataStore.data.map { it[COMMUNITY_SYNC_INTERVAL] ?: 50 }
    suspend fun setCommunitySyncInterval(value: Int) { context.dataStore.edit { it[COMMUNITY_SYNC_INTERVAL] = value } }

    val lyricsAnimationFlow: Flow<String> = context.dataStore.data.map { it[KEY_LYRICS_ANIMATION] ?: "karaoke" }
    suspend fun setLyricsAnimation(style: String) { context.dataStore.edit { it[KEY_LYRICS_ANIMATION] = style } }

    val lyricsTextSizeFlow: Flow<Float> = context.dataStore.data.map { it[KEY_LYRICS_TEXT_SIZE] ?: 14f }
    suspend fun setLyricsTextSize(size: Float) { context.dataStore.edit { it[KEY_LYRICS_TEXT_SIZE] = size } }

    val lyricsSyncOffsetFlow: Flow<Int> = context.dataStore.data.map { it[KEY_LYRICS_SYNC_OFFSET] ?: 0 }
    suspend fun setLyricsSyncOffset(ms: Int) { context.dataStore.edit { it[KEY_LYRICS_SYNC_OFFSET] = ms } }

    val lyricsShowTranslatedFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_LYRICS_SHOW_TRANSLATED] ?: false }
    suspend fun setLyricsShowTranslated(v: Boolean) { context.dataStore.edit { it[KEY_LYRICS_SHOW_TRANSLATED] = v } }
    val lyricsTargetLanguageFlow: Flow<String> = context.dataStore.data.map { it[KEY_LYRICS_TARGET_LANGUAGE] ?: "en" }
    suspend fun setLyricsTargetLanguage(code: String) { context.dataStore.edit { it[KEY_LYRICS_TARGET_LANGUAGE] = code } }

    suspend fun setHasSeenHearTheDifference(value: Boolean) {
        context.dataStore.edit { it[KEY_HAS_SEEN_HEAR_DIFF] = value }
    }

    private val PREF_LYRICS_PROVIDER = stringPreferencesKey("preferred_lyrics_provider")
    val preferredLyricsProviderFlow: Flow<String> = context.dataStore.data.map { it[PREF_LYRICS_PROVIDER] ?: "lrclib" }
    suspend fun setPreferredLyricsProvider(p: String) { context.dataStore.edit { it[PREF_LYRICS_PROVIDER] = p } }

    private val PREF_LYRICS_LINE_SPACING = floatPreferencesKey("lyrics_line_spacing")
    val lyricsLineSpacingFlow: Flow<Float> = context.dataStore.data.map { it[PREF_LYRICS_LINE_SPACING] ?: 1.3f }
    suspend fun setLyricsLineSpacing(v: Float) { context.dataStore.edit { it[PREF_LYRICS_LINE_SPACING] = v } }

    private val PREF_LYRICS_BLUR_INACTIVE = booleanPreferencesKey("lyrics_blur_inactive")
    val lyricsBlurInactiveFlow: Flow<Boolean> = context.dataStore.data.map { it[PREF_LYRICS_BLUR_INACTIVE] ?: true }
    suspend fun setLyricsBlurInactive(v: Boolean) { context.dataStore.edit { it[PREF_LYRICS_BLUR_INACTIVE] = v } }

    private val PREF_LYRICS_ROMANIZE = booleanPreferencesKey("lyrics_romanize")
    val lyricsRomanizeFlow: Flow<Boolean> = context.dataStore.data.map { it[PREF_LYRICS_ROMANIZE] ?: false }
    suspend fun setLyricsRomanize(v: Boolean) { context.dataStore.edit { it[PREF_LYRICS_ROMANIZE] = v } }

    private val PREF_LYRICS_POSITION = stringPreferencesKey("lyrics_position")
    val lyricsPositionFlow: Flow<String> = context.dataStore.data.map { it[PREF_LYRICS_POSITION] ?: "center" }
    suspend fun setLyricsPosition(v: String) { context.dataStore.edit { it[PREF_LYRICS_POSITION] = v } }

    private val PREF_LYRICS_AUTO_SCROLL = booleanPreferencesKey("lyrics_auto_scroll")
    val lyricsAutoScrollFlow: Flow<Boolean> = context.dataStore.data.map { it[PREF_LYRICS_AUTO_SCROLL] ?: true }
    suspend fun setLyricsAutoScroll(v: Boolean) { context.dataStore.edit { it[PREF_LYRICS_AUTO_SCROLL] = v } }

    private val PREF_PER_APP_EQ = stringPreferencesKey("per_app_eq_map")
    val perAppEqMapFlow: Flow<Map<String, String>> = context.dataStore.data.map {
        it[PREF_PER_APP_EQ]?.let { j ->
            try {
                val obj = org.json.JSONObject(j)
                obj.keys().asSequence().associate { k -> k to obj.getString(k) }
            } catch (_: Exception) { emptyMap() }
        } ?: emptyMap()
    }
    suspend fun setPerAppEq(pkg: String, preset: String) = context.dataStore.edit {
        val j = it[PREF_PER_APP_EQ]?.let { s ->
            try { org.json.JSONObject(s) } catch (_: Exception) { org.json.JSONObject() }
        } ?: org.json.JSONObject()
        j.put(pkg, preset); it[PREF_PER_APP_EQ] = j.toString()
    }

    private val PREF_LYRICS_GLOW = booleanPreferencesKey("lyrics_glow_enabled")
    val lyricsGlowEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[PREF_LYRICS_GLOW] ?: false }
    suspend fun setLyricsGlowEnabled(v: Boolean) { context.dataStore.edit { it[PREF_LYRICS_GLOW] = v } }

    private val PREF_LYRICS_BG = stringPreferencesKey("lyrics_background")
    val lyricsBackgroundFlow: Flow<String> = context.dataStore.data.map { it[PREF_LYRICS_BG] ?: "solid" }
    suspend fun setLyricsBackground(v: String) { context.dataStore.edit { it[PREF_LYRICS_BG] = v } }

    private val DIGEST_ENABLED = booleanPreferencesKey("digest_enabled")
    val digestEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[DIGEST_ENABLED] ?: true }
    suspend fun setDigestEnabled(e: Boolean) { context.dataStore.edit { it[DIGEST_ENABLED] = e } }

    private val HIDDEN_TAGS_KEY = stringPreferencesKey("hidden_tags")

    val hiddenTagsFlow: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[HIDDEN_TAGS_KEY]?.split("|||")?.filter { it.isNotBlank() }?.toSet() ?: emptySet() }

    suspend fun addHiddenTag(tag: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[HIDDEN_TAGS_KEY]?.split("|||")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.add(tag.trim().lowercase())
            prefs[HIDDEN_TAGS_KEY] = current.joinToString("|||")
        }
    }

    suspend fun removeHiddenTag(tag: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[HIDDEN_TAGS_KEY]?.split("|||")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.remove(tag.trim().lowercase())
            prefs[HIDDEN_TAGS_KEY] = current.joinToString("|||")
        }
    }

    val selectedFontFlow: Flow<String> = context.dataStore.data.map { it[SELECTED_FONT] ?: "INTER" }
    suspend fun setSelectedFont(font: String) { context.dataStore.edit { it[SELECTED_FONT] = font } }

    val selectedPaletteStyleFlow: Flow<String> = context.dataStore.data.map { it[SELECTED_PALETTE_STYLE] ?: "EXPRESSIVE" }
    suspend fun setSelectedPaletteStyle(style: String) { context.dataStore.edit { it[SELECTED_PALETTE_STYLE] = style } }

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
