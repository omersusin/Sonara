package com.sonara.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.preset.PresetExporter
import com.sonara.app.service.SonaraNotificationListener
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
    val cacheSize: Int = 0, val notificationListenerEnabled: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val cache = TrackCache(app.database.trackCacheDao())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { prefs.accentColorFlow.collect { c -> _uiState.update { it.copy(accentColor = c) } } }
        viewModelScope.launch { prefs.lastFmApiKeyFlow.collect { k -> _uiState.update { it.copy(lastFmApiKey = k, isApiKeySet = k.isNotBlank()) } } }
        viewModelScope.launch { prefs.lastFmSharedSecretFlow.collect { s -> _uiState.update { it.copy(lastFmSharedSecret = s, isSharedSecretSet = s.isNotBlank()) } } }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(aiEnabled = e) } } }
        viewModelScope.launch { prefs.autoEqEnabledFlow.collect { e -> _uiState.update { it.copy(autoEqEnabled = e) } } }
        viewModelScope.launch { prefs.smoothTransitionsFlow.collect { e -> _uiState.update { it.copy(smoothTransitions = e) } } }
        viewModelScope.launch { prefs.safetyLimiterFlow.collect { e -> _uiState.update { it.copy(safetyLimiter = e) } } }
        viewModelScope.launch { prefs.scrobblingEnabledFlow.collect { e -> _uiState.update { it.copy(scrobblingEnabled = e) } } }
        viewModelScope.launch { prefs.autoPresetFlow.collect { e -> _uiState.update { it.copy(autoPreset = e) } } }
        refreshCacheSize()
        checkNotificationListener()
    }

    fun updateApiKeyInput(v: String) { _uiState.update { it.copy(apiKeyInput = v) } }
    fun updateSharedSecretInput(v: String) { _uiState.update { it.copy(sharedSecretInput = v) } }

    fun saveApiKey() { viewModelScope.launch {
        val k = _uiState.value.apiKeyInput
        if (k.isNotBlank()) { prefs.setLastFmApiKey(k); _uiState.update { it.copy(apiKeyInput = "", isApiKeySet = true) } }
    } }

    fun saveSharedSecret() { viewModelScope.launch {
        val s = _uiState.value.sharedSecretInput
        if (s.isNotBlank()) { prefs.setLastFmSharedSecret(s); _uiState.update { it.copy(sharedSecretInput = "", isSharedSecretSet = true) } }
    } }

    fun setAccentColor(c: AccentColor) { viewModelScope.launch { prefs.setAccentColor(c) } }
    fun setAiEnabled(e: Boolean) { viewModelScope.launch { prefs.setAiEnabled(e) } }
    fun setAutoEqEnabled(e: Boolean) { viewModelScope.launch { prefs.setAutoEqEnabled(e) } }
    fun setSmoothTransitions(e: Boolean) { viewModelScope.launch { prefs.setSmoothTransitions(e) } }
    fun setSafetyLimiter(e: Boolean) { viewModelScope.launch { prefs.setSafetyLimiter(e) } }
    fun setScrobblingEnabled(e: Boolean) { viewModelScope.launch { prefs.setScrobblingEnabled(e) } }
    fun setAutoPreset(e: Boolean) { viewModelScope.launch { prefs.setAutoPreset(e) } }

    fun clearCache() { viewModelScope.launch { cache.clear(); refreshCacheSize() } }
    fun clearAllData() { viewModelScope.launch { cache.clear(); prefs.resetAll(); refreshCacheSize() } }

    fun checkNotificationListener() {
        _uiState.update { it.copy(notificationListenerEnabled = SonaraNotificationListener.isEnabled(getApplication())) }
    }

    private fun refreshCacheSize() { viewModelScope.launch { _uiState.update { it.copy(cacheSize = cache.size()) } } }

    fun exportPresets(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val presets = app.presetRepository.allPresets().first()
            val customPresets = presets.filter { !it.isBuiltIn }
            if (customPresets.isEmpty()) {
                val allJson = PresetExporter.exportToJson(presets)
                onResult(allJson)
            } else {
                val json = PresetExporter.exportToJson(customPresets)
                onResult(json)
            }
        }
    }

    fun importPresets(json: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (!PresetExporter.validateJson(json)) {
                onResult("✗ Invalid format. Must be a Sonara preset JSON.")
                return@launch
            }
            val imported = PresetExporter.importFromJson(json)
            if (imported == null || imported.isEmpty()) {
                onResult("✗ No valid presets found in the data.")
                return@launch
            }
            imported.forEach { preset ->
                app.presetRepository.save(preset)
            }
            onResult("✓ Imported ${imported.size} preset(s) successfully!")
        }
    }
}
