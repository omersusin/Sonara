package com.sonara.app.ui.screens.insights

import com.sonara.app.data.preferences.SonaraPreferences
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.autoeq.HeadphoneDetector
import com.sonara.app.intelligence.ResolveSource
import com.sonara.app.intelligence.TrackResolver
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.intelligence.lastfm.LastFmResolver
import com.sonara.app.intelligence.local.LocalAudioAnalyzer
import com.sonara.app.service.SonaraNotificationListener
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
    // Learning stats
    val songsLearned: Int = 0, val songsViaLastFm: Int = 0, val songsViaLocal: Int = 0,
    val genreDistribution: Map<String, Int> = emptyMap(), val apiAccuracy: Int = 0
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val cache = TrackCache(app.database.trackCacheDao())
    private val trackResolver = TrackResolver(LastFmResolver(), LocalAudioAnalyzer(), cache)
    private val headphoneDetector = HeadphoneDetector(application)

    private val _uiState = MutableStateFlow(InsightsUiState(eqActive = app.audioEngine.isInitialized))
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    init {
        headphoneDetector.start()

        viewModelScope.launch {
            SonaraNotificationListener.nowPlaying.collect { np ->
                _uiState.update { it.copy(trackTitle = np.title, trackArtist = np.artist, isPlaying = np.isPlaying) }
                if (np.title.isNotBlank()) { trackResolver.resolve(np.title, np.artist, prefs.lastFmApiKeyFlow.first()) }
            }
        }

        viewModelScope.launch {
            trackResolver.result.collect { r ->
                _uiState.update { it.copy(
                    genre = r.trackInfo.genre.ifEmpty { "Unknown" }, mood = r.trackInfo.mood.ifEmpty { "Unknown" },
                    energy = r.trackInfo.energy, confidence = r.trackInfo.confidence,
                    dataSource = when (r.source) { ResolveSource.LASTFM -> "Last.fm"; ResolveSource.LASTFM_ARTIST -> "Last.fm (Artist)"; ResolveSource.LOCAL_AI -> "Local AI"; ResolveSource.CACHE -> "Cached"; ResolveSource.NONE -> "None" }
                ) }
            }
        }

        viewModelScope.launch { headphoneDetector.headphone.collect { hp -> _uiState.update { it.copy(headphoneName = hp.name, headphoneConnected = hp.isConnected) } } }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        viewModelScope.launch { prefs.autoEqEnabledFlow.collect { e -> _uiState.update { it.copy(isAutoEqEnabled = e) } } }

        // Learning stats
        viewModelScope.launch { prefs.songsLearnedFlow.collect { n -> _uiState.update { it.copy(songsLearned = n) } } }
        viewModelScope.launch { prefs.songsViaLastFmFlow.collect { n ->
            _uiState.update { st ->
                val total = st.songsLearned.coerceAtLeast(1)
                st.copy(songsViaLastFm = n, apiAccuracy = if (total > 0) (n * 100 / total) else 0)
            }
        } }
        viewModelScope.launch { prefs.songsViaLocalFlow.collect { n -> _uiState.update { it.copy(songsViaLocal = n) } } }
        viewModelScope.launch { prefs.genreStatsFlow.collect { raw ->
            _uiState.update { it.copy(genreDistribution = SonaraPreferences.SonaraPreferences.parseGenreStats(raw)) }
        } }

        refreshCache()
    }

    fun refreshCache() { viewModelScope.launch { _uiState.update { it.copy(cacheSize = cache.size()) } } }
    override fun onCleared() { super.onCleared(); headphoneDetector.stop() }
}
