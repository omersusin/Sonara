package com.sonara.app.ui.screens.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.ResolveResult
import com.sonara.app.intelligence.ResolveSource
import com.sonara.app.intelligence.TrackResolver
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.intelligence.lastfm.LastFmResolver
import com.sonara.app.intelligence.local.LocalAudioAnalyzer
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
    val autoEqConfidence: Float = 0f,
    val headphoneName: String = "",
    val headphoneConnected: Boolean = false,
    val activePreset: String = "Flat",
    val aiAdjustment: String = "None",
    val isAiEnabled: Boolean = true,
    val isAutoEqEnabled: Boolean = true,
    val cacheSize: Int = 0,
    val isResolving: Boolean = false
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val cache = TrackCache(app.database.trackCacheDao())

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.aiEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(
                    isAiEnabled = enabled,
                    aiAdjustment = if (enabled) "Active" else "Disabled"
                ) }
            }
        }
        viewModelScope.launch {
            prefs.autoEqEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isAutoEqEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cacheSize = cache.size()) }
        }
    }

    fun updateFromResolveResult(result: ResolveResult) {
        _uiState.update {
            it.copy(
                trackTitle = result.trackInfo.title,
                trackArtist = result.trackInfo.artist,
                genre = result.trackInfo.genre.ifEmpty { "Unknown" },
                mood = result.trackInfo.mood.ifEmpty { "Unknown" },
                energy = result.trackInfo.energy,
                confidence = result.trackInfo.confidence,
                isResolving = result.isResolving,
                dataSource = when (result.source) {
                    ResolveSource.LASTFM -> "Last.fm"
                    ResolveSource.LASTFM_ARTIST -> "Last.fm (Artist)"
                    ResolveSource.LOCAL_AI -> "Local AI"
                    ResolveSource.CACHE -> "Cached"
                    ResolveSource.NONE -> "None"
                }
            )
        }
    }
}
