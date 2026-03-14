package com.sonara.app.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.autoeq.AutoEqManager
import com.sonara.app.autoeq.AutoEqState
import com.sonara.app.autoeq.HeadphoneDetector
import com.sonara.app.data.models.HeadphoneInfo
import com.sonara.app.intelligence.ResolveResult
import com.sonara.app.intelligence.ResolveSource
import com.sonara.app.intelligence.TrackResolver
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.intelligence.lastfm.LastFmResolver
import com.sonara.app.intelligence.local.LocalAudioAnalyzer
import com.sonara.app.media.MediaSessionMonitor
import com.sonara.app.media.NowPlayingInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val nowPlaying: NowPlayingInfo = NowPlayingInfo(),
    val resolveResult: ResolveResult = ResolveResult(),
    val headphone: HeadphoneInfo = HeadphoneInfo(),
    val autoEqState: AutoEqState = AutoEqState(),
    val currentPresetName: String = "Flat",
    val isAiEnabled: Boolean = true,
    val isAutoEqEnabled: Boolean = true,
    val bands: FloatArray = FloatArray(10)
) {
    val sourceLabel: String get() = when (resolveResult.source) {
        ResolveSource.LASTFM -> "Last.fm"
        ResolveSource.LASTFM_ARTIST -> "Last.fm (Artist)"
        ResolveSource.LOCAL_AI -> "Local AI"
        ResolveSource.CACHE -> "Cached"
        ResolveSource.NONE -> "None"
    }

    val isResolving: Boolean get() = resolveResult.isResolving
    val hasTrackInfo: Boolean get() = resolveResult.source != ResolveSource.NONE
    val genre: String get() = resolveResult.trackInfo.genre.ifEmpty { "Unknown" }
    val mood: String get() = resolveResult.trackInfo.mood.ifEmpty { "Unknown" }
    val energy: Float get() = resolveResult.trackInfo.energy
    val confidence: Float get() = resolveResult.trackInfo.confidence

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DashboardUiState) return false
        return nowPlaying == other.nowPlaying && resolveResult == other.resolveResult &&
            headphone == other.headphone && autoEqState == other.autoEqState &&
            currentPresetName == other.currentPresetName && bands.contentEquals(other.bands) &&
            isAiEnabled == other.isAiEnabled && isAutoEqEnabled == other.isAutoEqEnabled
    }
    override fun hashCode() = nowPlaying.hashCode()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences

    private val mediaMonitor = MediaSessionMonitor(application)
    private val headphoneDetector = HeadphoneDetector(application)
    private val autoEqManager = AutoEqManager()
    private val trackResolver: TrackResolver

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val cache = TrackCache(app.database.trackCacheDao())
        trackResolver = TrackResolver(LastFmResolver(), LocalAudioAnalyzer(), cache)

        viewModelScope.launch {
            mediaMonitor.nowPlaying.collect { np ->
                _uiState.update { it.copy(nowPlaying = np) }
                if (np.hasTrack) {
                    val apiKey = prefs.lastFmApiKeyFlow.first()
                    trackResolver.resolve(np.title, np.artist, apiKey)
                }
            }
        }

        viewModelScope.launch {
            trackResolver.result.collect { result ->
                _uiState.update { it.copy(resolveResult = result) }
            }
        }

        viewModelScope.launch {
            headphoneDetector.headphone.collect { hp ->
                _uiState.update { it.copy(headphone = hp) }
                val aeqEnabled = prefs.autoEqEnabledFlow.first()
                autoEqManager.onHeadphoneChanged(hp, aeqEnabled)
            }
        }

        viewModelScope.launch {
            autoEqManager.state.collect { aeq ->
                _uiState.update { it.copy(autoEqState = aeq) }
            }
        }

        viewModelScope.launch {
            prefs.aiEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isAiEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            prefs.autoEqEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isAutoEqEnabled = enabled) }
            }
        }

        headphoneDetector.start()
    }

    override fun onCleared() {
        super.onCleared()
        headphoneDetector.stop()
        mediaMonitor.stop()
    }
}
