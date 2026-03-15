package com.sonara.app.ui.screens.insights

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.service.SonaraNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsUiState(
    val trackTitle: String = "",
    val trackArtist: String = "",
    val dataSource: String = "None",
    val genre: String = "Unknown",
    val mood: String = "Unknown",
    val energy: Float = 0.5f,
    val confidence: Float = 0f,
    val autoEqActive: Boolean = false,
    val autoEqProfile: String = "",
    val headphoneName: String = "",
    val headphoneConnected: Boolean = false,
    val activePreset: String = "Flat",
    val aiAdjustment: String = "None",
    val isAiEnabled: Boolean = true,
    val isAutoEqEnabled: Boolean = true,
    val cacheSize: Int = 0,
    val isResolving: Boolean = false,
    val isPlaying: Boolean = false,
    val eqSessionActive: Boolean = false
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val cache = TrackCache(app.database.trackCacheDao())

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    init {
        viewModelScope.launch {
            SonaraNotificationListener.nowPlaying.collect { np ->
                _uiState.update { it.copy(trackTitle = np.title, trackArtist = np.artist, isPlaying = np.isPlaying) }
            }
        }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e, aiAdjustment = if (e) "Active" else "Disabled") } } }
        viewModelScope.launch { prefs.autoEqEnabledFlow.collect { e -> _uiState.update { it.copy(isAutoEqEnabled = e) } } }
        viewModelScope.launch { app.activeSessionId.collect { sid -> _uiState.update { it.copy(eqSessionActive = sid > 0) } } }
        viewModelScope.launch { _uiState.update { it.copy(cacheSize = cache.size()) } }
    }

    fun refreshCache() {
        viewModelScope.launch { _uiState.update { it.copy(cacheSize = cache.size()) } }
    }
}
