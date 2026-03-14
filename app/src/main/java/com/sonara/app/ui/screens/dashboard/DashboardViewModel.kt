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
import kotlinx.coroutines.flow.combine
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DashboardUiState) return false
        return nowPlaying == other.nowPlaying && resolveResult == other.resolveResult &&
            headphone == other.headphone && autoEqState == other.autoEqState &&
            currentPresetName == other.currentPresetName && bands.contentEquals(other.bands)
    }
    override fun hashCode() = nowPlaying.hashCode()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences

    val mediaMonitor = MediaSessionMonitor(application)
    val headphoneDetector = HeadphoneDetector(application)
    val autoEqManager = AutoEqManager()
    val trackResolver: TrackResolver

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val cache = TrackCache(app.database.trackCacheDao())
        trackResolver = TrackResolver(LastFmResolver(), LocalAudioAnalyzer(), cache)

        viewModelScope.launch {
            combine(
                mediaMonitor.nowPlaying,
                trackResolver.result,
                headphoneDetector.headphone,
                autoEqManager.state,
                prefs.aiEnabledFlow,
                prefs.autoEqEnabledFlow
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val np = values[0] as NowPlayingInfo
                val rr = values[1] as ResolveResult
                val hp = values[2] as HeadphoneInfo
                val aeq = values[3] as AutoEqState
                val aiOn = values[4] as Boolean
                val aeqOn = values[5] as Boolean
                DashboardUiState(
                    nowPlaying = np,
                    resolveResult = rr,
                    headphone = hp,
                    autoEqState = aeq,
                    isAiEnabled = aiOn,
                    isAutoEqEnabled = aeqOn
                )
            }.collect { _uiState.value = it }
        }

        viewModelScope.launch {
            mediaMonitor.nowPlaying.collect { np ->
                if (np.hasTrack) {
                    val apiKey = prefs.lastFmApiKeyFlow.let { flow ->
                        var key = ""
                        flow.collect { key = it; return@collect }
                        key
                    }
                    trackResolver.resolve(np.title, np.artist, apiKey)
                }
            }
        }

        viewModelScope.launch {
            headphoneDetector.headphone.collect { hp ->
                var aeqEnabled = true
                prefs.autoEqEnabledFlow.collect { aeqEnabled = it; return@collect }
                autoEqManager.onHeadphoneChanged(hp, aeqEnabled)
            }
        }
    }
}
