package com.sonara.app.ui.screens.insights

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.ai.SonaraAi
import com.sonara.app.ai.SonaraAiState
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.intelligence.lastfm.LastFmAuthManager
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.ui.components.DisplayLabelMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
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
    val aiModelSamples: Int = 0, val route: String = "Unknown",
    val personalSamples: Int = 0,
    // Last.fm stats
    val lastFmConnected: Boolean = false,
    val lastFmUsername: String = "",
    val totalScrobbles: String = "0",
    val totalArtists: String = "0",
    val topArtists: List<Triple<String, String, String>> = emptyList(),
    val topTracks: List<Triple<String, String, String>> = emptyList(),
    val weeklyTracks: List<Triple<String, String, String>> = emptyList()
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

    // AI state
    val aiState: StateFlow<SonaraAiState> =
        SonaraAi.getInstance()?.state ?: MutableStateFlow(SonaraAiState()).asStateFlow()

    init {
        viewModelScope.launch { SonaraNotificationListener.nowPlaying.collect { np -> _uiState.update { it.copy(trackTitle = np.title, trackArtist = np.artist, isPlaying = np.isPlaying) } } }
        viewModelScope.launch { SonaraNotificationListener.currentGenre.collect { g -> if (g.isNotBlank()) _uiState.update { it.copy(genre = DisplayLabelMapper.formatGenre(g)) } } }
        viewModelScope.launch { SonaraNotificationListener.currentMood.collect { m -> if (m.isNotBlank()) _uiState.update { it.copy(mood = DisplayLabelMapper.formatMood(m)) } } }
        viewModelScope.launch { SonaraNotificationListener.currentEnergy.collect { e -> _uiState.update { it.copy(energy = e) } } }
        viewModelScope.launch { SonaraNotificationListener.currentConfidence.collect { c -> _uiState.update { it.copy(confidence = c) } } }
        viewModelScope.launch { SonaraNotificationListener.currentSource.collect { s -> if (s.isNotBlank()) _uiState.update { it.copy(dataSource = DisplayLabelMapper.formatSource(s)) } } }
        viewModelScope.launch { app.audioSessionManager.activeStrategy.collect { s -> _uiState.update { it.copy(eqStrategy = s, eqActive = s != "none") } } }
        viewModelScope.launch { app.currentRoute.collect { r -> _uiState.update { it.copy(route = r.displayName) } } }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        viewModelScope.launch { prefs.songsLearnedFlow.collect { n -> _uiState.update { it.copy(songsLearned = n) } } }
        viewModelScope.launch { prefs.songsViaLastFmFlow.collect { n -> _uiState.update { st -> st.copy(songsViaLastFm = n, apiAccuracy = if (st.songsLearned > 0) minOf(100, n * 100 / st.songsLearned) else 0) } } }
        viewModelScope.launch { prefs.songsViaLocalFlow.collect { n -> _uiState.update { it.copy(songsViaLocal = n) } } }
        viewModelScope.launch { prefs.genreStatsFlow.collect { raw ->
            val map = if (raw.isBlank()) emptyMap()
            else raw.split(";").mapNotNull { entry -> val parts = entry.split(":"); if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null }.toMap()
            val formatted = map.mapKeys { (k, _) -> DisplayLabelMapper.formatGenre(k) }
            _uiState.update { it.copy(genreDistribution = formatted) }
        } }
        viewModelScope.launch {
            app.currentRoute.collect { route ->
                val isHP = route != com.sonara.app.intelligence.pipeline.AudioRoute.SPEAKER && route != com.sonara.app.intelligence.pipeline.AudioRoute.UNKNOWN
                _uiState.update { it.copy(headphoneConnected = isHP, headphoneName = if (isHP) route.displayName else "") }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(
                cacheSize = cache.size(),
                aiModelSamples = app.adaptiveLearning.getTotalSamples(),
                personalSamples = app.personalization.getTotalSamples()
            ) }
        }

        // Last.fm connection + stats
        viewModelScope.launch {
            app.lastFmAuth.authState.collect { state ->
                _uiState.update { it.copy(lastFmConnected = state == LastFmAuthManager.AuthState.CONNECTED) }
            }
        }
        viewModelScope.launch {
            app.lastFmAuth.username.collect { name ->
                _uiState.update { it.copy(lastFmUsername = name) }
                if (name.isNotBlank()) fetchLastFmStats(name)
            }
        }
        // Fallback: if auth says connected but username didn't flow yet, trigger fetch
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // Wait for flows to settle
            val st = _uiState.value
            if (st.lastFmConnected && st.lastFmUsername.isBlank()) {
                // Try to get username from connection info
                val info = app.lastFmAuth.getConnectionInfo()
                if (info.username.isNotBlank()) {
                    _uiState.update { it.copy(lastFmUsername = info.username) }
                    fetchLastFmStats(info.username)
                }
            }
        }
        // Fallback: if auth says connected but username didn't flow yet, trigger fetch
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // Wait for flows to settle
            val st = _uiState.value
            if (st.lastFmConnected && st.lastFmUsername.isBlank()) {
                // Try to get username from connection info
                val info = app.lastFmAuth.getConnectionInfo()
                if (info.username.isNotBlank()) {
                    _uiState.update { it.copy(lastFmUsername = info.username) }
                    fetchLastFmStats(info.username)
                }
            }
        }
    }

    private fun fetchLastFmStats(username: String) {
        val apiKey = app.lastFmAuth.getActiveApiKey()
        if (apiKey.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // User info
                val info = LastFmClient.api.getUserInfo(username, apiKey)
                info.user?.let { u ->
                    _uiState.update { it.copy(
                        totalScrobbles = u.playcount,
                        totalArtists = u.artist_count
                    ) }
                }
            } catch (_: Exception) {}
            try {
                // Top artists
                val artists = LastFmClient.api.getUserTopArtists(username, apiKey, "overall", 8)
                val list = artists.topartists?.artist?.map { a -> Triple(a.name, a.playcount, a.imageUrl ?: "") } ?: emptyList()
                _uiState.update { it.copy(topArtists = list) }
                // Fetch Deezer images (Last.fm deprecated artist images in 2020)
                val enriched = list.map { (name, plays, lfmImg) ->
                    val img = if (lfmImg.isNotBlank() && !lfmImg.contains("2a96cbd8b46e")) lfmImg
                              else DeezerImageResolver.getArtistImage(name) ?: ""
                    Triple(name, plays, img)
                }
                _uiState.update { it.copy(topArtists = enriched) }
            } catch (_: Exception) {}
            try {
                // Top tracks
                val tracks = LastFmClient.api.getUserTopTracks(username, apiKey, "overall", 8)
                val list = tracks.toptracks?.track?.map { t -> Triple(t.name, t.artist?.name ?: "", t.playcount) } ?: emptyList()
                _uiState.update { it.copy(topTracks = list) }
            } catch (_: Exception) {}
            try {
                // Weekly chart
                val weekly = LastFmClient.api.getWeeklyTrackChart(username, apiKey)
                val list = weekly.weeklytrackchart?.track?.take(8)?.map { t -> Triple(t.name, t.artist?.name ?: "", t.playcount) } ?: emptyList()
                _uiState.update { it.copy(weeklyTracks = list) }
            } catch (_: Exception) {}
        }
    }
}
