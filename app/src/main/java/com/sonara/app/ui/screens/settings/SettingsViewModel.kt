package com.sonara.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.intelligence.lastfm.LastFmAuthManager
import com.sonara.app.data.BackupManager
import com.sonara.app.preset.PresetExporter
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ai.SonaraAi
import com.sonara.app.ui.theme.AccentColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class LyricsAnimationStyle(val id: String, val displayName: String) {
    NONE("NONE", "None"),
    FADE("FADE", "Fade"),
    GLOW("GLOW", "Glow"),
    SLIDE("SLIDE", "Slide"),
    KARAOKE("KARAOKE", "Karaoke"),
    APPLE("APPLE", "Apple Music"),
    APPLE_V2("APPLE_V2", "Apple Music V2"),
    VIVIMUSIC("VIVIMUSIC", "Vivimusic (Fluid)"),
    LYRICS_V2("LYRICS_V2", "Lyrics V2 (Flowing)"),
    METRO("METRO", "MetroLyrics");

    val label: String get() = displayName

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: KARAOKE
    }
}

data class SettingsUiState(
    val lastFmApiKey: String = "", val lastFmSharedSecret: String = "",
    val isApiKeySet: Boolean = false, val isSharedSecretSet: Boolean = false,
    val accentColor: AccentColor = AccentColor.Amber,
    val aiEnabled: Boolean = true, val autoEqEnabled: Boolean = true,
    val smoothTransitions: Boolean = true, val safetyLimiter: Boolean = true,
    val scrobblingEnabled: Boolean = false, val autoPreset: Boolean = true,
    val apiKeyInput: String = "", val sharedSecretInput: String = "",
    val cacheSize: Int = 0, val notificationListenerEnabled: Boolean = false,
    // New: Last.fm connection
    val lastFmConnected: Boolean = false,
    val lastFmUsername: String = "",
    val pendingScrobbles: Int = 0,
    // New: Gemini
    val geminiApiKey: String = "",
    val geminiEnabled: Boolean = false,
    val geminiModel: String = "fast",
    val geminiKeyInput: String = "",
    // New: Theme
    val themeMode: String = "system",
    val dynamicColors: Boolean = true,
    val highContrast: Boolean = false,
    // New: Notification
    val keepNotificationPaused: Boolean = true,
    // New: Personalization
    val personalSamples: Int = 0,
    val sourceLastFm: Boolean = true,
    val sourceLocalAi: Boolean = true,
    val sourceLyrics: Boolean = true,
    val amoledMode: Boolean = false,
    val aiProvider: String = "gemini",
    val openRouterApiKey: String = "", val openRouterKeyInput: String = "",
    val openRouterModel: String = "google/gemini-2.5-flash",
    val groqApiKey: String = "", val groqKeyInput: String = "",
    val groqModel: String = "llama-3.3-70b-versatile",
    val huggingFaceApiKey: String = "", val huggingFaceKeyInput: String = "",
    val huggingFaceModel: String = "meta-llama/Meta-Llama-3.1-8B-Instruct",
    val communityDownloadEnabled: Boolean = false,
    val communityUploadEnabled: Boolean = false,
    val communityPending: Int = 0,
    val communityTotalSent: Int = 0
,
    val githubTokenInput: String = "",
    val isGithubTokenSet: Boolean = false,
    val syncInterval: Int = 50,
    val legacyAnalysis: Boolean = false,
    val hearTheDiffEnabled: Boolean = true,
    // Model dropdown
    val availableModels: List<Pair<String, String>> = emptyList(),
    val isLoadingModels: Boolean = false,
    // Scrobble app filter
    val allowedScrobbleApps: Set<String> = emptySet(),
    // Last.fm direct login
    val lastFmUsernameInput: String = "",
    val lastFmPasswordInput: String = "",
    val lyricsAnimationStyle: LyricsAnimationStyle = LyricsAnimationStyle.KARAOKE,
    val lyricsTextSize: Float = 14f,
    val lyricsSyncOffsetMs: Int = 0,
    val lyricsShowTranslated: Boolean = false)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val secrets = app.secureSecrets
    private val cache = TrackCache(app.database.trackCacheDao())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isGithubTokenSet = secrets.getGitHubTokenInstance().isNotBlank()) }

        viewModelScope.launch { prefs.accentColorFlow.collect { c -> _uiState.update { it.copy(accentColor = c) } } }
        // Read key status from SecureSecrets directly
        _uiState.update { it.copy(
            isApiKeySet = secrets.getLastFmApiKey().isNotBlank(),
            isSharedSecretSet = secrets.getLastFmSharedSecret().isNotBlank(),
            isGithubTokenSet = secrets.getGitHubTokenInstance().isNotBlank()
        ) }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(aiEnabled = e) } } }
        viewModelScope.launch { prefs.autoEqEnabledFlow.collect { e -> _uiState.update { it.copy(autoEqEnabled = e) } } }
        viewModelScope.launch { prefs.smoothTransitionsFlow.collect { e -> _uiState.update { it.copy(smoothTransitions = e) } } }
        viewModelScope.launch { prefs.safetyLimiterFlow.collect { e -> _uiState.update { it.copy(safetyLimiter = e) } } }
        viewModelScope.launch { prefs.scrobblingEnabledFlow.collect { e -> _uiState.update { it.copy(scrobblingEnabled = e) } } }
        viewModelScope.launch { prefs.autoPresetFlow.collect { e -> _uiState.update { it.copy(autoPreset = e) } } }
        // Gemini
        viewModelScope.launch { prefs.geminiEnabledFlow.collect { e -> _uiState.update { it.copy(geminiEnabled = e) } } }
        viewModelScope.launch {
            prefs.geminiModelFlow.collect { m ->
                _uiState.update { it.copy(geminiModel = m) }
                app.geminiEngine.customModelId = m.takeIf { it.contains("gemini", ignoreCase = true) }
            }
        }
        viewModelScope.launch { prefs.geminiApiKeyFlow.collect { k -> _uiState.update { it.copy(geminiApiKey = k) } } }
        // Theme
        viewModelScope.launch { prefs.themeModeFlow.collect { m -> _uiState.update { it.copy(themeMode = m) } } }
        viewModelScope.launch { prefs.dynamicColorsFlow.collect { e -> _uiState.update { it.copy(dynamicColors = e) } } }
        viewModelScope.launch { prefs.highContrastFlow.collect { e -> _uiState.update { it.copy(highContrast = e) } } }
        // AI Sources
        viewModelScope.launch { prefs.sourceLastFmEnabledFlow.collect { e -> _uiState.update { it.copy(sourceLastFm = e) } } }
        viewModelScope.launch { prefs.sourceLocalAiEnabledFlow.collect { e -> _uiState.update { it.copy(sourceLocalAi = e) } } }
        viewModelScope.launch { prefs.sourceLyricsEnabledFlow.collect { e -> _uiState.update { it.copy(sourceLyrics = e) } } }
        // AMOLED
        viewModelScope.launch { prefs.amoledModeFlow.collect { e -> _uiState.update { it.copy(amoledMode = e) } } }
        // Provider
        viewModelScope.launch { prefs.aiProviderFlow.collect { v -> _uiState.update { it.copy(aiProvider = v) } } }
        viewModelScope.launch { prefs.openRouterApiKeyFlow.collect { v -> _uiState.update { it.copy(openRouterApiKey = v) } } }
        viewModelScope.launch { prefs.openRouterModelFlow.collect { v -> _uiState.update { it.copy(openRouterModel = v) } } }
        viewModelScope.launch { prefs.groqApiKeyFlow.collect { v -> _uiState.update { it.copy(groqApiKey = v) } } }
        viewModelScope.launch { prefs.groqModelFlow.collect { v -> _uiState.update { it.copy(groqModel = v) } } }
        viewModelScope.launch { prefs.huggingFaceApiKeyFlow.collect { v -> _uiState.update { it.copy(huggingFaceApiKey = v) } } }
        viewModelScope.launch { prefs.huggingFaceModelFlow.collect { v -> _uiState.update { it.copy(huggingFaceModel = v) } } }
        // Notification
        viewModelScope.launch { prefs.keepNotificationPausedFlow.collect { e -> _uiState.update { it.copy(keepNotificationPaused = e) } } }
        // Last.fm connection status
        viewModelScope.launch { app.lastFmAuth.authState.collect { state ->
            _uiState.update { it.copy(lastFmConnected = state == LastFmAuthManager.AuthState.CONNECTED) }
        } }
        viewModelScope.launch { app.lastFmAuth.username.collect { name ->
            _uiState.update { it.copy(lastFmUsername = name) }
        } }

        // Community
        viewModelScope.launch {
            val cloud = SonaraAi.getInstance()?.cloudManager
            if (cloud != null) {
                _uiState.update { it.copy(
                    communityUploadEnabled = cloud.isContributionEnabled(),
                    communityPending = cloud.getPendingCount(),
                    communityTotalSent = cloud.getTotalSent()
                ) }
            }
        }
        viewModelScope.launch { prefs.communitySyncIntervalFlow.collect { v -> _uiState.update { it.copy(syncInterval = v) } } }
        // Lyrics settings
        viewModelScope.launch { prefs.lyricsAnimationFlow.collect { v ->
            _uiState.update { it.copy(lyricsAnimationStyle = LyricsAnimationStyle.fromId(v)) }
        } }
        viewModelScope.launch { prefs.lyricsTextSizeFlow.collect { v -> _uiState.update { it.copy(lyricsTextSize = v) } } }
        viewModelScope.launch { prefs.lyricsSyncOffsetFlow.collect { v -> _uiState.update { it.copy(lyricsSyncOffsetMs = v) } } }
        viewModelScope.launch { prefs.lyricsShowTranslatedFlow.collect { v -> _uiState.update { it.copy(lyricsShowTranslated = v) } } }

        refreshCacheSize(); checkNotificationListener(); refreshPendingScrobbles()
        _uiState.update { it.copy(personalSamples = app.personalization.getTotalSamples()) }
    }

    fun updateApiKeyInput(v: String) { _uiState.update { it.copy(apiKeyInput = v) } }
    fun updateSharedSecretInput(v: String) { _uiState.update { it.copy(sharedSecretInput = v) } }
    fun updateGeminiKeyInput(v: String) { _uiState.update { it.copy(geminiKeyInput = v) } }

    fun saveApiKey() {
        viewModelScope.launch {
            val k = _uiState.value.apiKeyInput
            if (k.isNotBlank()) {
                secrets.setLastFmApiKey(k); app.reloadPipeline()
                _uiState.update { it.copy(apiKeyInput = "", isApiKeySet = true) }
            }
        }
    }

    fun saveSharedSecret() {
        viewModelScope.launch {
            val s = _uiState.value.sharedSecretInput
            if (s.isNotBlank()) {
                secrets.setLastFmSharedSecret(s)
                _uiState.update { it.copy(sharedSecretInput = "", isSharedSecretSet = true) }
            }
        }
    }

    fun saveGeminiKey() {
        viewModelScope.launch {
            val k = _uiState.value.geminiKeyInput
            if (k.isNotBlank()) {
                secrets.setGeminiApiKey(k)
                prefs.setGeminiApiKey(k)
                app.geminiEngine.updateApiKey(k)
                _uiState.update { it.copy(geminiKeyInput = "", geminiApiKey = k) }
                fetchModels("gemini")
            }
        }
    }

    fun setAccentColor(c: AccentColor) { viewModelScope.launch { prefs.setAccentColor(c) } }
    fun setAiEnabled(e: Boolean) { viewModelScope.launch { prefs.setAiEnabled(e) } }
    fun setAutoEqEnabled(e: Boolean) { viewModelScope.launch { prefs.setAutoEqEnabled(e) } }
    fun setSmoothTransitions(e: Boolean) { viewModelScope.launch { prefs.setSmoothTransitions(e) } }
    fun setSafetyLimiter(e: Boolean) { viewModelScope.launch { prefs.setSafetyLimiter(e) } }
    fun setScrobblingEnabled(e: Boolean) { viewModelScope.launch { prefs.setScrobblingEnabled(e) } }
    fun setAutoPreset(e: Boolean) { viewModelScope.launch { prefs.setAutoPreset(e) } }
    fun setGeminiEnabled(e: Boolean) { viewModelScope.launch { prefs.setGeminiEnabled(e) } }
    fun setGeminiModel(m: String) {
        viewModelScope.launch {
            prefs.setGeminiModel(m)
            app.geminiEngine.customModelId = m.takeIf { it.contains("gemini", ignoreCase = true) }
        }
    }
    fun setThemeMode(m: String) { viewModelScope.launch { prefs.setThemeMode(m) } }
    fun setDynamicColors(e: Boolean) { viewModelScope.launch { prefs.setDynamicColors(e) } }
    fun setHighContrast(e: Boolean) { viewModelScope.launch { prefs.setHighContrast(e) } }
    fun setKeepNotificationPaused(e: Boolean) { viewModelScope.launch { prefs.setKeepNotificationPaused(e) } }

    fun connectLastFm(onIntent: (android.content.Intent) -> Unit) {
        viewModelScope.launch {
            val intent = app.lastFmAuth.startAuth()
            if (intent != null) onIntent(intent)
        }
    }

    fun disconnectLastFm() {
        app.lastFmAuth.disconnect()
        viewModelScope.launch { _uiState.update { it.copy(lastFmConnected = false, lastFmUsername = "") } }
    }
    fun setSourceLastFm(e: Boolean) { viewModelScope.launch { prefs.setSourceLastFmEnabled(e) } }
    fun setSourceLocalAi(e: Boolean) { viewModelScope.launch { prefs.setSourceLocalAiEnabled(e) } }
    fun setSourceLyrics(e: Boolean) { viewModelScope.launch { prefs.setSourceLyricsEnabled(e) } }
    fun setAmoledMode(e: Boolean) { viewModelScope.launch { prefs.setAmoledMode(e) } }

    fun setAiProvider(v: String) {
        viewModelScope.launch {
            prefs.setAiProvider(v)
            app.insightManager.setPrimary(v)
            fetchModels(v)
        }
    }
    fun updateOpenRouterKeyInput(v: String) { _uiState.update { it.copy(openRouterKeyInput = v) } }
    fun setOpenRouterModel(v: String) { viewModelScope.launch { prefs.setOpenRouterModel(v) } }
    fun saveOpenRouterKey() {
        viewModelScope.launch {
            val k = _uiState.value.openRouterKeyInput
            if (k.isNotBlank()) {
                secrets.setOpenRouterApiKey(k)
                prefs.setOpenRouterApiKey(k)
                app.insightManager.configureOpenRouter(k, _uiState.value.openRouterModel)
                _uiState.update { it.copy(openRouterKeyInput = "", openRouterApiKey = k) }
                fetchModels("openrouter")
            }
        }
    }
    fun updateGroqKeyInput(v: String) { _uiState.update { it.copy(groqKeyInput = v) } }
    fun setGroqModel(v: String) { viewModelScope.launch { prefs.setGroqModel(v) } }
    fun saveGroqKey() {
        viewModelScope.launch {
            val k = _uiState.value.groqKeyInput
            if (k.isNotBlank()) {
                secrets.setGroqApiKey(k)
                prefs.setGroqApiKey(k)
                app.insightManager.configureGroq(k, _uiState.value.groqModel)
                _uiState.update { it.copy(groqKeyInput = "", groqApiKey = k) }
                fetchModels("groq")
            }
        }
    }

    fun updateHuggingFaceKeyInput(v: String) { _uiState.update { it.copy(huggingFaceKeyInput = v) } }
    fun setHuggingFaceModel(v: String) { viewModelScope.launch { prefs.setHuggingFaceModel(v) } }
    fun saveHuggingFaceKey() {
        viewModelScope.launch {
            val k = _uiState.value.huggingFaceKeyInput
            if (k.isNotBlank()) {
                secrets.setHuggingFaceApiKey(k)
                prefs.setHuggingFaceApiKey(k)
                app.insightManager.configureHuggingFace(k, _uiState.value.huggingFaceModel)
                _uiState.update { it.copy(huggingFaceKeyInput = "", huggingFaceApiKey = k) }
                fetchModels("huggingface")
            }
        }
    }

    fun setCommunityDownload(enabled: Boolean) {
        _uiState.update { it.copy(communityDownloadEnabled = enabled) }
        if (enabled) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                SonaraAi.getInstance()?.cloudManager?.syncNow()
            }
        }
    }
    fun setCommunityUpload(enabled: Boolean) {
        SonaraAi.getInstance()?.cloudManager?.setContributionEnabled(enabled)
        _uiState.update { it.copy(communityUploadEnabled = enabled) }
        refreshCommunityStats()
    }

    fun refreshCommunityStats() {
        val cloud = SonaraAi.getInstance()?.cloudManager
        if (cloud != null) {
            _uiState.update { it.copy(
                communityUploadEnabled = cloud.isContributionEnabled(),
                communityPending = cloud.getPendingCount(),
                communityTotalSent = cloud.getTotalSent()
            ) }
        }
    }
    fun syncCommunityNow() {
        SonaraAi.getInstance()?.cloudManager?.syncNow()
    }

    fun clearCache() { viewModelScope.launch { cache.clear(); refreshCacheSize() } }

    fun clearAllData() {
        viewModelScope.launch {
            app.clearAllData(); refreshCacheSize()
            _uiState.update { it.copy(isApiKeySet = false, isSharedSecretSet = false, cacheSize = 0) }
        }
    }

    fun checkNotificationListener() {
        viewModelScope.launch { app.preferences.legacyAnalysisFlow.collect { v -> _uiState.update { it.copy(legacyAnalysis = v) } } }
        viewModelScope.launch { app.preferences.hearTheDiffEnabledFlow.collect { v -> _uiState.update { it.copy(hearTheDiffEnabled = v) } } }
        val alive = SonaraNotificationListener.instance != null
        val sys = SonaraNotificationListener.isEnabled(getApplication())
        _uiState.update { it.copy(notificationListenerEnabled = alive || sys) }
    }

    fun refreshPendingScrobbles() {
        viewModelScope.launch {
            try {
                val count = app.database.pendingScrobbleDao().count()
                _uiState.update { it.copy(pendingScrobbles = count) }
            } catch (_: Exception) {}
        }
    }

    private fun refreshCacheSize() { viewModelScope.launch { _uiState.update { it.copy(cacheSize = cache.size()) } } }

    fun exportPresets(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val all = app.presetRepository.allPresets().first()
            val custom = all.filter { !it.isBuiltIn }
            val toExport = custom.ifEmpty { all }
            if (toExport.isEmpty()) { onResult(""); return@launch }
            onResult(PresetExporter.exportToJson(toExport))
        }
    }

    fun importPresets(json: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (!PresetExporter.validateJson(json)) { onResult("Invalid format."); return@launch }
            val imported = PresetExporter.importFromJson(json)
            if (imported.isNullOrEmpty()) { onResult("No valid presets found."); return@launch }
            imported.forEach { app.presetRepository.save(it) }
            onResult("Imported ${imported.size} presets")
        }
    }

    fun updateGithubTokenInput(v: String) {
        _uiState.update { it.copy(githubTokenInput = v) }
    }

    fun setLegacyAnalysis(v: Boolean) { viewModelScope.launch { app.preferences.setLegacyAnalysis(v); _uiState.update { it.copy(legacyAnalysis = v) } } }
    fun setHearTheDiffEnabled(v: Boolean) {
        viewModelScope.launch {
            app.preferences.setHearTheDiffEnabled(v)
            _uiState.update { it.copy(hearTheDiffEnabled = v) }
            // Reset "seen" flag when re-enabled so banner shows again
            if (v) {
                app.preferences.setHasSeenHearTheDifference(false)
            }
        }
    }

    fun saveGithubToken() {
        viewModelScope.launch {
            val token = _uiState.value.githubTokenInput
            if (token.isNotBlank()) {
                secrets.setGitHubToken(token)
                com.sonara.app.ai.SonaraAi.getInstance()?.cloudManager?.setContributionEnabled(true)
                _uiState.update { it.copy(githubTokenInput = "", isGithubTokenSet = true, communityUploadEnabled = true) }
            }
        }
    }

    // ═══ Direct Last.fm login ═══
    fun updateLastFmUsernameInput(v: String) { _uiState.update { it.copy(lastFmUsernameInput = v) } }
    fun updateLastFmPasswordInput(v: String) { _uiState.update { it.copy(lastFmPasswordInput = v) } }

    fun directLoginLastFm() {
        viewModelScope.launch {
            val user = _uiState.value.lastFmUsernameInput
            val pass = _uiState.value.lastFmPasswordInput
            if (user.isNotBlank() && pass.isNotBlank()) {
                val ok = app.lastFmAuth.directLogin(user, pass)
                if (ok) {
                    _uiState.update { it.copy(lastFmUsernameInput = "", lastFmPasswordInput = "", lastFmConnected = true) }
                }
            }
        }
    }

    // ═══ Model Dropdown ═══
    fun fetchModels(provider: String? = null) {
        val target = provider ?: _uiState.value.aiProvider
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true) }
            val models = withContext(Dispatchers.IO) { fetchModelsFromApi(target) }
            _uiState.update { it.copy(availableModels = models, isLoadingModels = false) }
        }
    }

    private fun fetchModelsFromApi(provider: String): List<Pair<String, String>> {
        if (provider == "gemini") {
            val key = secrets.getGeminiApiKey()
            val fallbackGemini = listOf(
                "gemini-2.5-flash" to "Gemini 2.5 Flash",
                "gemini-2.5-pro" to "Gemini 2.5 Pro",
                "gemini-2.5-flash-lite" to "Gemini 2.5 Flash Lite",
                "gemini-2.0-flash" to "Gemini 2.0 Flash",
                "gemini-2.0-flash-lite" to "Gemini 2.0 Flash Lite",
                "gemini-1.5-flash" to "Gemini 1.5 Flash",
                "gemini-1.5-pro" to "Gemini 1.5 Pro"
            )
            if (key.isBlank()) return fallbackGemini
            try {
                val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
                val req = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models?key=$key")
                    .get().build()
                val resp = client.newCall(req).execute()
                val json = resp.body?.string() ?: return fallbackGemini
                val obj = JSONObject(json)
                val data = obj.optJSONArray("models") ?: return fallbackGemini
                val list = mutableListOf<Pair<String, String>>()
                for (i in 0 until data.length()) {
                    val m = data.getJSONObject(i)
                    val name = m.optString("name", "")
                    val displayName = m.optString("displayName", "")
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    val supportsGenerate = (0 until (methods?.length() ?: 0)).any { methods!!.getString(it) == "generateContent" }
                    if (!supportsGenerate || !name.contains("gemini")) continue
                    val modelId = name.substringAfter("models/")
                    list.add(modelId to displayName.ifBlank { modelId })
                }
                return if (list.isEmpty()) fallbackGemini else list
            } catch (_: Exception) { return fallbackGemini }
        }
        val fallbackOpenRouter = listOf(
            "google/gemini-2.5-flash" to "Gemini 2.5 Flash",
            "google/gemini-2.5-pro" to "Gemini 2.5 Pro",
            "openai/gpt-4o-mini" to "GPT-4o Mini",
            "openai/gpt-4o" to "GPT-4o",
            "anthropic/claude-sonnet-4" to "Claude Sonnet 4",
            "anthropic/claude-3.5-haiku" to "Claude 3.5 Haiku",
            "meta-llama/llama-3.3-70b-instruct" to "Llama 3.3 70B",
            "deepseek/deepseek-chat" to "DeepSeek V3",
            "deepseek/deepseek-r1" to "DeepSeek R1"
        )
        val fallbackGroq = listOf(
            "llama-3.3-70b-versatile" to "Llama 3.3 70B Versatile",
            "llama-3.1-70b-versatile" to "Llama 3.1 70B Versatile",
            "llama-3.1-8b-instant" to "Llama 3.1 8B Instant",
            "mixtral-8x7b-32768" to "Mixtral 8x7B",
            "gemma2-9b-it" to "Gemma 2 9B"
        )
        if (provider == "groq") {
            val key = secrets.getGroqApiKey()
            if (key.isBlank()) return fallbackGroq
            try {
                val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
                val req = Request.Builder().url("https://api.groq.com/openai/v1/models")
                    .addHeader("Authorization", "Bearer $key").get().build()
                val resp = client.newCall(req).execute()
                val json = resp.body?.string() ?: return fallbackGroq
                val obj = JSONObject(json)
                val data = obj.optJSONArray("data") ?: return fallbackGroq
                val list = mutableListOf<Pair<String, String>>()
                for (i in 0 until data.length()) {
                    val m = data.getJSONObject(i)
                    val id = m.getString("id")
                    if (id.contains("whisper") || id.contains("tts") || id.contains("guard")) continue
                    list.add(id to id.replace("-", " ").replaceFirstChar { it.uppercase() })
                }
                return if (list.isEmpty()) fallbackGroq else list.take(20)
            } catch (_: Exception) { return fallbackGroq }
        }
        if (provider == "openrouter") {
            val key = secrets.getOpenRouterApiKey()
            if (key.isBlank()) return fallbackOpenRouter
            try {
                val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
                val req = Request.Builder().url("https://openrouter.ai/api/v1/models")
                    .addHeader("Authorization", "Bearer $key").get().build()
                val resp = client.newCall(req).execute()
                val json = resp.body?.string() ?: return fallbackOpenRouter
                val obj = JSONObject(json)
                val data = obj.optJSONArray("data") ?: return fallbackOpenRouter
                val list = mutableListOf<Pair<String, String>>()
                val keywords = listOf("gpt", "claude", "gemini", "llama", "mistral", "deepseek", "qwen", "gemma", "command")
                for (i in 0 until data.length()) {
                    val m = data.getJSONObject(i)
                    val id = m.getString("id")
                    val name = m.optString("name", id)
                    if (keywords.any { id.contains(it, ignoreCase = true) }) {
                        list.add(id to name)
                    }
                }
                return if (list.isEmpty()) fallbackOpenRouter else list.take(30)
            } catch (_: Exception) { return fallbackOpenRouter }
        }
        val fallbackHuggingFace = listOf(
            "meta-llama/Meta-Llama-3.1-8B-Instruct" to "Llama 3.1 8B Instruct",
            "meta-llama/Llama-3.3-70B-Instruct" to "Llama 3.3 70B Instruct",
            "mistralai/Mistral-7B-Instruct-v0.3" to "Mistral 7B Instruct v0.3",
            "mistralai/Mixtral-8x7B-Instruct-v0.1" to "Mixtral 8x7B Instruct",
            "Qwen/Qwen2.5-7B-Instruct" to "Qwen 2.5 7B Instruct",
            "Qwen/Qwen2.5-72B-Instruct" to "Qwen 2.5 72B Instruct",
            "google/gemma-2-9b-it" to "Gemma 2 9B IT",
            "HuggingFaceH4/zephyr-7b-beta" to "Zephyr 7B Beta"
        )
        if (provider == "huggingface") {
            val key = secrets.getHuggingFaceApiKey()
            if (key.isBlank()) return fallbackHuggingFace
            try {
                val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
                val req = Request.Builder().url("https://router.huggingface.co/v1/models")
                    .addHeader("Authorization", "Bearer $key").get().build()
                val resp = client.newCall(req).execute()
                val json = resp.body?.string() ?: return fallbackHuggingFace
                val obj = JSONObject(json)
                val data = obj.optJSONArray("data") ?: return fallbackHuggingFace
                val list = mutableListOf<Pair<String, String>>()
                for (i in 0 until data.length()) {
                    val m = data.getJSONObject(i)
                    val id = m.getString("id")
                    val name = m.optString("name", id)
                    list.add(id to name)
                }
                return if (list.isEmpty()) fallbackHuggingFace else list.take(40)
            } catch (_: Exception) { return fallbackHuggingFace }
        }
        return emptyList()
    }

    // ═══ Scrobble App Filter ═══
    fun loadAllowedApps() {
        viewModelScope.launch {
            prefs.allowedScrobbleAppsFlow.collect { apps ->
                _uiState.update { it.copy(allowedScrobbleApps = apps) }
            }
        }
    }

    fun setAllowedScrobbleApps(apps: Set<String>) {
        viewModelScope.launch {
            prefs.setAllowedScrobbleApps(apps)
            _uiState.update { it.copy(allowedScrobbleApps = apps) }
        }
    }

    fun setLyricsAnimationStyle(style: LyricsAnimationStyle) {
        viewModelScope.launch { prefs.setLyricsAnimation(style.id) }
    }
    fun setLyricsTextSize(size: Float) {
        viewModelScope.launch { prefs.setLyricsTextSize(size.coerceIn(10f, 24f)) }
    }
    fun setLyricsSyncOffset(ms: Int) {
        viewModelScope.launch { prefs.setLyricsSyncOffset(ms.coerceIn(-2000, 2000)) }
    }
    fun setLyricsShowTranslated(v: Boolean) {
        viewModelScope.launch { prefs.setLyricsShowTranslated(v) }
    }

    fun setSyncInterval(value: Int) {
        viewModelScope.launch {
            prefs.setCommunitySyncInterval(value.coerceIn(1, 9999))
            _uiState.update { it.copy(syncInterval = value.coerceIn(1, 9999)) }
        }
    }

    fun exportFullBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = BackupManager.exportFull(app)
            onResult(json)
        }
    }

    fun importFullBackup(json: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = BackupManager.importFull(app, json)
            onResult(result)
        }
    }
}
