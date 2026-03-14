package com.sonara.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.ui.theme.AccentColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val lastFmApiKey: String = "",
    val lastFmSharedSecret: String = "",
    val isApiKeySet: Boolean = false,
    val isSharedSecretSet: Boolean = false,
    val accentColor: AccentColor = AccentColor.Amber,
    val aiEnabled: Boolean = true,
    val autoEqEnabled: Boolean = true,
    val apiKeyInput: String = "",
    val sharedSecretInput: String = "",
    val cacheSize: Int = 0,
    val smoothTransitions: Boolean = true,
    val safetyLimiter: Boolean = true,
    val showClearDataDialog: Boolean = false,
    val showClearCacheDialog: Boolean = false
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
        refreshCacheSize()
    }

    fun updateApiKeyInput(v: String) { _uiState.update { it.copy(apiKeyInput = v) } }
    fun updateSharedSecretInput(v: String) { _uiState.update { it.copy(sharedSecretInput = v) } }

    fun saveApiKey() { viewModelScope.launch { val k = _uiState.value.apiKeyInput; if (k.isNotBlank()) { prefs.setLastFmApiKey(k); _uiState.update { it.copy(apiKeyInput = "") } } } }
    fun saveSharedSecret() { viewModelScope.launch { val s = _uiState.value.sharedSecretInput; if (s.isNotBlank()) { prefs.setLastFmSharedSecret(s); _uiState.update { it.copy(sharedSecretInput = "") } } } }

    fun setAccentColor(c: AccentColor) { viewModelScope.launch { prefs.setAccentColor(c) } }
    fun setAiEnabled(e: Boolean) { viewModelScope.launch { prefs.setAiEnabled(e) } }
    fun setAutoEqEnabled(e: Boolean) { viewModelScope.launch { prefs.setAutoEqEnabled(e) } }

    fun clearCache() {
        viewModelScope.launch {
            cache.clear()
            refreshCacheSize()
            _uiState.update { it.copy(showClearCacheDialog = false) }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            cache.clear()
            prefs.setLastFmApiKey("")
            prefs.setLastFmSharedSecret("")
            prefs.setAiEnabled(true)
            prefs.setAutoEqEnabled(true)
            prefs.setAccentColor(AccentColor.Amber)
            refreshCacheSize()
            _uiState.update { it.copy(showClearDataDialog = false) }
        }
    }

    fun showClearCacheDialog(show: Boolean) { _uiState.update { it.copy(showClearCacheDialog = show) } }
    fun showClearDataDialog(show: Boolean) { _uiState.update { it.copy(showClearDataDialog = show) } }

    private fun refreshCacheSize() {
        viewModelScope.launch { val size = cache.size(); _uiState.update { it.copy(cacheSize = size) } }
    }
}
