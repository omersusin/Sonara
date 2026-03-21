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
    val trackTitle: String = "", val trackArtist: String = "",
    val genre: String = "Unknown", val mood: String = "Unknown",
    val energy: Float = 0.5f, val confidence: Float = 0f,
    val dataSource: String = "None",
    val headphoneConnected: Boolean = false, val headphoneName: String = "",
    val isAiEnabled: Boolean = true, val isPlaying: Boolean = false,
    val cacheSize: Int = 0, val eqActive: Boolean = false,
    val songsLearned: Int = 0, val songsViaLastFm: Int = 0, val songsViaLocal: Int = 0,
    val genreDistribution: Map<String, Int> = emptyMap(),
    val apiAccuracy: Int = 0, val eqStrategy: String = "none",
    val aiModelSamples: Int = 0, val route: String = "Unknown"
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val cache = TrackCache(app.database.trackCacheDao())

    private val _uiState = MutableStateFlow(InsightsUiState(
        eqActive = app.audioSessionManager.isInitialized,
        eqStrategy = app.audioSessionManager.activeStrategy.value
    ))
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    init {
        viewModelScope.launch { SonaraNotificationListener.nowPlaying.collect { np -> _uiState.update { it.copy(trackTitle = np.title, trackArtist = np.artist, isPlaying = np.isPlaying) } } }
        viewModelScope.launch { SonaraNotificationListener.currentGenre.collect { g -> if (g.isNotBlank()) _uiState.update { it.copy(genre = g) } } }
        viewModelScope.launch { SonaraNotificationListener.currentMood.collect { m -> if (m.isNotBlank()) _uiState.update { it.copy(mood = m) } } }
        viewModelScope.launch { SonaraNotificationListener.currentEnergy.collect { e -> _uiState.update { it.copy(energy = e) } } }
        viewModelScope.launch { SonaraNotificationListener.currentConfidence.collect { c -> _uiState.update { it.copy(confidence = c) } } }
        viewModelScope.launch { SonaraNotificationListener.currentSource.collect { s -> if (s.isNotBlank()) _uiState.update { it.copy(dataSource = s) } } }
        viewModelScope.launch { app.audioSessionManager.activeStrategy.collect { s -> _uiState.update { it.copy(eqStrategy = s, eqActive = s != "none") } } }
        viewModelScope.launch { app.currentRoute.collect { r -> _uiState.update { it.copy(route = r.displayName) } } }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        viewModelScope.launch { prefs.songsLearnedFlow.collect { n -> _uiState.update { it.copy(songsLearned = n) } } }
        viewModelScope.launch { prefs.songsViaLastFmFlow.collect { n -> _uiState.update { st -> st.copy(songsViaLastFm = n, apiAccuracy = if (st.songsLearned > 0) (n * 100 / st.songsLearned) else 0) } } }
        viewModelScope.launch { prefs.songsViaLocalFlow.collect { n -> _uiState.update { it.copy(songsViaLocal = n) } } }
        viewModelScope.launch { prefs.genreStatsFlow.collect { raw ->
            val map = if (raw.isBlank()) emptyMap()
            else raw.split(";").mapNotNull { entry -> val parts = entry.split(":"); if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null }.toMap()
            _uiState.update { it.copy(genreDistribution = map) }
        } }

        // ═══ REAL headphone + cache feeds ═══
        viewModelScope.launch {
            app.currentRoute.collect { route ->
                val isHP = route != com.sonara.app.intelligence.pipeline.AudioRoute.SPEAKER && route != com.sonara.app.intelligence.pipeline.AudioRoute.UNKNOWN
                _uiState.update { it.copy(headphoneConnected = isHP, headphoneName = if (isHP) route.displayName else "") }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cacheSize = cache.size(), aiModelSamples = app.adaptiveLearning.getTotalSamples()) }
        }
    }
}
