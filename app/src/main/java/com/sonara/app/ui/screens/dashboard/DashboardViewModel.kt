package com.sonara.app.ui.screens.dashboard

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.autoeq.AutoEqManager
import com.sonara.app.autoeq.HeadphoneDetector
import com.sonara.app.intelligence.ResolveResult
import com.sonara.app.intelligence.ResolveSource
import com.sonara.app.intelligence.TrackResolver
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.intelligence.lastfm.LastFmResolver
import com.sonara.app.intelligence.local.AiEqSuggestionEngine
import com.sonara.app.intelligence.local.LocalAudioAnalyzer
import com.sonara.app.service.SonaraNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val hasTrack: Boolean = false,
    val genre: String = "Unknown",
    val mood: String = "Unknown",
    val energy: Float = 0.5f,
    val confidence: Float = 0f,
    val sourceLabel: String = "None",
    val isResolving: Boolean = false,
    val headphoneName: String = "",
    val headphoneConnected: Boolean = false,
    val headphoneType: String = "",
    val autoEqActive: Boolean = false,
    val autoEqProfile: String = "",
    val autoEqConfidence: Float = 0f,
    val currentPresetName: String = "Flat",
    val isAiEnabled: Boolean = true,
    val isAutoEqEnabled: Boolean = true,
    val bands: FloatArray = FloatArray(10),
    val aiReasoning: String = "",
    val notificationListenerEnabled: Boolean = false,
    val eqActive: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DashboardUiState) return false
        return title == other.title && artist == other.artist && isPlaying == other.isPlaying &&
            genre == other.genre && sourceLabel == other.sourceLabel &&
            headphoneConnected == other.headphoneConnected && autoEqActive == other.autoEqActive &&
            bands.contentEquals(other.bands) && notificationListenerEnabled == other.notificationListenerEnabled &&
            eqActive == other.eqActive
    }
    override fun hashCode() = title.hashCode()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val headphoneDetector = HeadphoneDetector(application)
    private val autoEqManager = AutoEqManager()
    private val trackResolver: TrackResolver

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    init {
        val cache = TrackCache(app.database.trackCacheDao())
        trackResolver = TrackResolver(LastFmResolver(), LocalAudioAnalyzer(), cache)

        _uiState.update { it.copy(eqActive = app.audioEngine.isInitialized) }

        viewModelScope.launch {
            SonaraNotificationListener.nowPlaying.collect { np ->
                _uiState.update { it.copy(title = np.title, artist = np.artist, isPlaying = np.isPlaying, hasTrack = np.title.isNotBlank()) }
                if (np.title.isNotBlank()) {
                    val apiKey = prefs.lastFmApiKeyFlow.first()
                    trackResolver.resolve(np.title, np.artist, apiKey)
                }
            }
        }

        viewModelScope.launch {
            trackResolver.result.collect { result ->
                val suggestion = if (result.source != ResolveSource.NONE && _uiState.value.isAiEnabled)
                    AiEqSuggestionEngine.suggest(result.trackInfo) else null

                _uiState.update { it.copy(
                    genre = result.trackInfo.genre.ifEmpty { "Unknown" },
                    mood = result.trackInfo.mood.ifEmpty { "Unknown" },
                    energy = result.trackInfo.energy,
                    confidence = result.trackInfo.confidence,
                    isResolving = result.isResolving,
                    sourceLabel = when (result.source) {
                        ResolveSource.LASTFM -> "Last.fm"; ResolveSource.LASTFM_ARTIST -> "Last.fm (Artist)"
                        ResolveSource.LOCAL_AI -> "Local AI"; ResolveSource.CACHE -> "Cached"; ResolveSource.NONE -> "None"
                    },
                    aiReasoning = suggestion?.reasoning ?: ""
                ) }

                if (suggestion != null) {
                    _uiState.update { it.copy(bands = suggestion.bands) }
                    app.applyEqBands(suggestion.bands)
                    app.audioEngine.applyBassBoost(suggestion.bassBoost)
                    app.audioEngine.applyVirtualizer(suggestion.virtualizer)
                }
            }
        }

        viewModelScope.launch {
            headphoneDetector.headphone.collect { hp ->
                _uiState.update { it.copy(headphoneName = hp.name, headphoneConnected = hp.isConnected, headphoneType = hp.type.name) }
                autoEqManager.onHeadphoneChanged(hp, prefs.autoEqEnabledFlow.first())
            }
        }

        viewModelScope.launch {
            autoEqManager.state.collect { aeq ->
                _uiState.update { it.copy(autoEqActive = aeq.isActive, autoEqProfile = aeq.profile?.name ?: "", autoEqConfidence = aeq.profile?.matchConfidence ?: 0f) }
                if (aeq.isActive) app.audioEngine.applyBands(aeq.correctionBands)
            }
        }

        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        viewModelScope.launch { prefs.autoEqEnabledFlow.collect { e -> _uiState.update { it.copy(isAutoEqEnabled = e) } } }

        checkNotificationListener()
        headphoneDetector.start()
    }

    fun checkNotificationListener() {
        _uiState.update { it.copy(notificationListenerEnabled = SonaraNotificationListener.isEnabled(getApplication())) }
    }

    override fun onCleared() { super.onCleared(); headphoneDetector.stop() }
}
