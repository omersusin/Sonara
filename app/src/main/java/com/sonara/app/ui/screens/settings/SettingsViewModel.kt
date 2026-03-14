package com.sonara.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
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
    val sharedSecretInput: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = (application as SonaraApp).preferences
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.accentColorFlow.collect { color ->
                _uiState.update { it.copy(accentColor = color) }
            }
        }
        viewModelScope.launch {
            prefs.lastFmApiKeyFlow.collect { key ->
                _uiState.update { it.copy(lastFmApiKey = key, isApiKeySet = key.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            prefs.lastFmSharedSecretFlow.collect { secret ->
                _uiState.update { it.copy(lastFmSharedSecret = secret, isSharedSecretSet = secret.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            prefs.aiEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(aiEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            prefs.autoEqEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(autoEqEnabled = enabled) }
            }
        }
    }

    fun updateApiKeyInput(value: String) { _uiState.update { it.copy(apiKeyInput = value) } }
    fun updateSharedSecretInput(value: String) { _uiState.update { it.copy(sharedSecretInput = value) } }

    fun saveApiKey() {
        viewModelScope.launch {
            val key = _uiState.value.apiKeyInput
            if (key.isNotBlank()) { prefs.setLastFmApiKey(key); _uiState.update { it.copy(apiKeyInput = "") } }
        }
    }

    fun saveSharedSecret() {
        viewModelScope.launch {
            val secret = _uiState.value.sharedSecretInput
            if (secret.isNotBlank()) { prefs.setLastFmSharedSecret(secret); _uiState.update { it.copy(sharedSecretInput = "") } }
        }
    }

    fun setAccentColor(color: AccentColor) { viewModelScope.launch { prefs.setAccentColor(color) } }
    fun setAiEnabled(enabled: Boolean) { viewModelScope.launch { prefs.setAiEnabled(enabled) } }
    fun setAutoEqEnabled(enabled: Boolean) { viewModelScope.launch { prefs.setAutoEqEnabled(enabled) } }
}
