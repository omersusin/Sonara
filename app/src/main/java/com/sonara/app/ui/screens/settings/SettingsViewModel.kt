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
    val communityDownloadEnabled: Boolean = false,
    val communityUploadEnabled: Boolean = false,
    val communityPending: Int = 0,
    val communityTotalSent: Int = 0
,
    val githubTokenInput: String = "",
    val isGithubTokenSet: Boolean = false,
    val syncInterval: Int = 50)

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
        viewModelScope.launch { prefs.lastFmApiKeyFlow.collect { k -> _uiState.update { it.copy(lastFmApiKey = k, isApiKeySet = k.isNotBlank() || secrets.getLastFmApiKey().isNotBlank()) } } }
        viewModelScope.launch { prefs.lastFmSharedSecretFlow.collect { s -> _uiState.update { it.copy(lastFmSharedSecret = s, isSharedSecretSet = s.isNotBlank() || secrets.getLastFmSharedSecret().isNotBlank()) } } }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(aiEnabled = e) } } }
        viewModelScope.launch { prefs.autoEqEnabledFlow.collect { e -> _uiState.update { it.copy(autoEqEnabled = e) } } }
        viewModelScope.launch { prefs.smoothTransitionsFlow.collect { e -> _uiState.update { it.copy(smoothTransitions = e) } } }
        viewModelScope.launch { prefs.safetyLimiterFlow.collect { e -> _uiState.update { it.copy(safetyLimiter = e) } } }
        viewModelScope.launch { prefs.scrobblingEnabledFlow.collect { e -> _uiState.update { it.copy(scrobblingEnabled = e) } } }
        viewModelScope.launch { prefs.autoPresetFlow.collect { e -> _uiState.update { it.copy(autoPreset = e) } } }
        // Gemini
        viewModelScope.launch { prefs.geminiEnabledFlow.collect { e -> _uiState.update { it.copy(geminiEnabled = e) } } }
        viewModelScope.launch { prefs.geminiModelFlow.collect { m -> _uiState.update { it.copy(geminiModel = m) } } }
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
                secrets.setLastFmApiKey(k); prefs.setLastFmApiKey(k); app.reloadPipeline()
                _uiState.update { it.copy(apiKeyInput = "", isApiKeySet = true) }
            }
        }
    }

    fun saveSharedSecret() {
        viewModelScope.launch {
            val s = _uiState.value.sharedSecretInput
            if (s.isNotBlank()) {
                secrets.setLastFmSharedSecret(s); prefs.setLastFmSharedSecret(s)
                _uiState.update { it.copy(sharedSecretInput = "", isSharedSecretSet = true) }
            }
        }
    }

    fun saveGeminiKey() {
        viewModelScope.launch {
            val k = _uiState.value.geminiKeyInput
            if (k.isNotBlank()) {
                prefs.setGeminiApiKey(k)
                _uiState.update { it.copy(geminiKeyInput = "", geminiApiKey = k) }
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
    fun setGeminiModel(m: String) { viewModelScope.launch { prefs.setGeminiModel(m) } }
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
        }
    }
    fun updateOpenRouterKeyInput(v: String) { _uiState.update { it.copy(openRouterKeyInput = v) } }
    fun setOpenRouterModel(v: String) { viewModelScope.launch { prefs.setOpenRouterModel(v) } }
    fun saveOpenRouterKey() {
        viewModelScope.launch {
            val k = _uiState.value.openRouterKeyInput
            if (k.isNotBlank()) {
                prefs.setOpenRouterApiKey(k)
                app.insightManager.configureOpenRouter(k, _uiState.value.openRouterModel)
                _uiState.update { it.copy(openRouterKeyInput = "", openRouterApiKey = k) }
            }
        }
    }
    fun updateGroqKeyInput(v: String) { _uiState.update { it.copy(groqKeyInput = v) } }
    fun setGroqModel(v: String) { viewModelScope.launch { prefs.setGroqModel(v) } }
    fun saveGroqKey() {
        viewModelScope.launch {
            val k = _uiState.value.groqKeyInput
            if (k.isNotBlank()) {
                prefs.setGroqApiKey(k)
                app.insightManager.configureGroq(k, _uiState.value.groqModel)
                _uiState.update { it.copy(groqKeyInput = "", groqApiKey = k) }
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

    fun saveGithubToken() {
        viewModelScope.launch {
            val token = _uiState.value.githubTokenInput
            if (token.isNotBlank()) {
                secrets.setGitHubToken(token)
                _uiState.update { it.copy(githubTokenInput = "", isGithubTokenSet = true) }
            }
        }
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
