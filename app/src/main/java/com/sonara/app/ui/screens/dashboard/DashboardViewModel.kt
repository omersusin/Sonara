package com.sonara.app.ui.screens.dashboard

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.service.SonaraNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val title: String = "", val artist: String = "", val isPlaying: Boolean = false, val hasTrack: Boolean = false,
    val genre: String = "Unknown", val mood: String = "Unknown", val energy: Float = 0.5f, val confidence: Float = 0f,
    val sourceLabel: String = "None", val currentPresetName: String = "Flat", val isAiEnabled: Boolean = true,
    val bands: FloatArray = FloatArray(10), val bassBoost: Int = 0, val virtualizer: Int = 0,
    val notificationListenerEnabled: Boolean = false, val eqActive: Boolean = false,
    val isManualPreset: Boolean = false, val songsLearned: Int = 0,
    val eqStrategy: String = "none", val route: String = "Unknown"
) {
    override fun equals(other: Any?): Boolean { if (this === other) return true; if (other !is DashboardUiState) return false; return title == other.title && artist == other.artist && genre == other.genre && bands.contentEquals(other.bands) && currentPresetName == other.currentPresetName && eqStrategy == other.eqStrategy && confidence == other.confidence }
    override fun hashCode() = title.hashCode()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val _uiState = MutableStateFlow(DashboardUiState(eqActive = app.audioSessionManager.isInitialized, eqStrategy = app.audioSessionManager.activeStrategy.value))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    init {
        viewModelScope.launch { app.eqState.collect { eq -> _uiState.update { it.copy(bands = eq.bands, bassBoost = eq.bassBoost, virtualizer = eq.virtualizer, currentPresetName = eq.presetName, isManualPreset = eq.isManualPreset) } } }
        viewModelScope.launch { SonaraNotificationListener.nowPlaying.collect { np -> _uiState.update { it.copy(title = np.title, artist = np.artist, isPlaying = np.isPlaying, hasTrack = np.title.isNotBlank()) } } }
        viewModelScope.launch { SonaraNotificationListener.currentGenre.collect { g -> if (g.isNotBlank()) _uiState.update { it.copy(genre = g) } } }
        viewModelScope.launch { SonaraNotificationListener.currentMood.collect { m -> if (m.isNotBlank()) _uiState.update { it.copy(mood = m) } } }
        viewModelScope.launch { SonaraNotificationListener.currentEnergy.collect { e -> _uiState.update { it.copy(energy = e) } } }
        viewModelScope.launch { SonaraNotificationListener.currentConfidence.collect { c -> _uiState.update { it.copy(confidence = c) } } }
        viewModelScope.launch { SonaraNotificationListener.currentSource.collect { s -> if (s.isNotBlank()) _uiState.update { it.copy(sourceLabel = s) } } }
        viewModelScope.launch { app.audioSessionManager.activeStrategy.collect { s -> _uiState.update { it.copy(eqStrategy = s, eqActive = s != "none") } } }
        viewModelScope.launch { app.currentRoute.collect { r -> _uiState.update { it.copy(route = r.displayName) } } }
        viewModelScope.launch { app.preferences.songsLearnedFlow.collect { n -> _uiState.update { it.copy(songsLearned = n) } } }
        viewModelScope.launch { app.preferences.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        checkNotificationListener()
    }

    fun resetToAi() { app.resetToAi() }
    fun checkNotificationListener() { _uiState.update { it.copy(notificationListenerEnabled = SonaraNotificationListener.isEnabled(getApplication())) } }
}
