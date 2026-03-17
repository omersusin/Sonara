package com.sonara.app.ui.screens.insights

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.ResolveSource
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.service.SonaraNotificationListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsUiState(
    val trackTitle: String = "", val trackArtist: String = "",
    val dataSource: String = "None", val genre: String = "Unknown", val mood: String = "Unknown",
    val energy: Float = 0.5f, val confidence: Float = 0f,
    val headphoneName: String = "", val headphoneConnected: Boolean = false,
    val isAiEnabled: Boolean = true, val isAutoEqEnabled: Boolean = true,
    val cacheSize: Int = 0, val isPlaying: Boolean = false, val eqActive: Boolean = false,
    val songsLearned: Int = 0, val songsViaLastFm: Int = 0, val songsViaLocal: Int = 0,
    val genreDistribution: Map<String, Int> = emptyMap(), val apiAccuracy: Int = 0,
    val engineRoute: String = "SPEAKER"
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val cache = TrackCache(app.database.trackCacheDao())
    private val trackResolver = app.trackResolver

    private val _uiState = MutableStateFlow(InsightsUiState(eqActive = true))
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    init {
        viewModelScope.launch {
            SonaraNotificationListener.nowPlaying.collect { np ->
                _uiState.update { it.copy(trackTitle = np.title, trackArtist = np.artist, isPlaying = np.isPlaying) }
                if (np.title.isNotBlank()) trackResolver.resolve(np.title, np.artist, prefs.lastFmApiKeyFlow.first())
            }
        }

        viewModelScope.launch {
            trackResolver.result.collect { r ->
                _uiState.update { it.copy(
                    genre = r.trackInfo.genre.ifEmpty { "Unknown" },
                    mood = r.trackInfo.mood.ifEmpty { "Unknown" },
                    energy = r.trackInfo.energy, confidence = r.trackInfo.confidence,
                    dataSource = when (r.source) { ResolveSource.LASTFM -> "Last.fm"; ResolveSource.LASTFM_ARTIST -> "Last.fm (Artist)"; ResolveSource.LOCAL_AI -> "Local AI"; ResolveSource.CACHE -> "Cached"; ResolveSource.NONE -> "None" }
                ) }
            }
        }

        // Headphone from bridge
        viewModelScope.launch {
            while (true) {
                val route = app.sessionBridge.eqController.detectRoute()
                val routeName = route.name
                _uiState.update { it.copy(
                    engineRoute = routeName,
                    headphoneConnected = route != com.sonara.app.engine.eq.EqSessionController.AudioRoute.SPEAKER,
                    headphoneName = when (route) {
                        com.sonara.app.engine.eq.EqSessionController.AudioRoute.BLUETOOTH -> "Bluetooth"
                        com.sonara.app.engine.eq.EqSessionController.AudioRoute.WIRED -> "Wired"
                        else -> ""
                    }
                ) }
                delay(3000)
            }
        }

        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        viewModelScope.launch { prefs.autoEqEnabledFlow.collect { e -> _uiState.update { it.copy(isAutoEqEnabled = e) } } }
        viewModelScope.launch { prefs.songsLearnedFlow.collect { n -> _uiState.update { it.copy(songsLearned = n) } } }
        viewModelScope.launch { prefs.songsViaLastFmFlow.collect { n -> _uiState.update { st -> st.copy(songsViaLastFm = n, apiAccuracy = if (st.songsLearned > 0) (n * 100 / st.songsLearned) else 0) } } }
        viewModelScope.launch { prefs.songsViaLocalFlow.collect { n -> _uiState.update { it.copy(songsViaLocal = n) } } }
        viewModelScope.launch { prefs.genreStatsFlow.collect { raw ->
            val map = if (raw.isBlank()) emptyMap() else raw.split(";").mapNotNull { val p = it.split(":"); if (p.size == 2) p[0] to (p[1].toIntOrNull() ?: 0) else null }.toMap()
            _uiState.update { it.copy(genreDistribution = map) }
        } }
        refreshCache()
    }

    fun refreshCache() { viewModelScope.launch { _uiState.update { it.copy(cacheSize = cache.size()) } } }
}
