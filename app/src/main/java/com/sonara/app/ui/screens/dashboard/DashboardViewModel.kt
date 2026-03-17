package com.sonara.app.ui.screens.dashboard

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.ResolveSource
import com.sonara.app.intelligence.local.AiEqSuggestionEngine
import com.sonara.app.intelligence.local.SmartMediaType
import com.sonara.app.intelligence.local.MediaSourceDetector
import com.sonara.app.intelligence.local.MediaType
import com.sonara.app.service.SonaraNotificationListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val title: String = "", val artist: String = "", val isPlaying: Boolean = false, val hasTrack: Boolean = false,
    val genre: String = "Unknown", val mood: String = "Unknown", val energy: Float = 0.5f, val confidence: Float = 0f,
    val sourceLabel: String = "None", val isResolving: Boolean = false, val pluginUsed: String = "",
    val headphoneName: String = "", val headphoneConnected: Boolean = false, val headphoneType: String = "",
    val autoEqActive: Boolean = false, val autoEqProfile: String = "",
    val currentPresetName: String = "Flat", val isAiEnabled: Boolean = true,
    val bands: FloatArray = FloatArray(10), val bassBoost: Int = 0, val virtualizer: Int = 0,
    val aiReasoning: String = "", val notificationListenerEnabled: Boolean = false,
    val eqActive: Boolean = false, val isManualPreset: Boolean = false,
    val smartMediaType: String = "Music", val smartMediaConfidence: Float = 0f,
    val isComparing: Boolean = false, val isOriginalSound: Boolean = false,
    val songsLearned: Int = 0, val engineRoute: String = "SPEAKER"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true; if (other !is DashboardUiState) return false
        return title == other.title && artist == other.artist && isPlaying == other.isPlaying &&
            genre == other.genre && bands.contentEquals(other.bands) && currentPresetName == other.currentPresetName &&
            notificationListenerEnabled == other.notificationListenerEnabled && smartMediaType == other.smartMediaType &&
            songsLearned == other.songsLearned && engineRoute == other.engineRoute
    }
    override fun hashCode() = title.hashCode()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val trackResolver = app.trackResolver
    private var lastProcessedTrack = ""

    private val _uiState = MutableStateFlow(DashboardUiState(eqActive = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    init {
        // Observe shared EQ state
        viewModelScope.launch { app.eqState.collect { eq -> _uiState.update { it.copy(bands = eq.bands, bassBoost = eq.bassBoost, virtualizer = eq.virtualizer, currentPresetName = eq.presetName, isManualPreset = eq.isManualPreset) } } }

        // Observe bridge state for genre/mood/route info
        viewModelScope.launch {
            // Poll bridge state every 2 seconds
            while (true) {
                val state = app.sessionBridge.currentState
                if (state.title?.isNotBlank() == true) {
                    _uiState.update {
                        it.copy(
                            genre = state.genre.replaceFirstChar { c -> c.uppercase() },
                            mood = state.mood.replaceFirstChar { c -> c.uppercase() },
                            energy = state.energy,
                            confidence = state.confidence,
                            engineRoute = state.route.name,
                            sourceLabel = if (state.confidence > 0.5f) "AI Engine" else "Local AI"
                        )
                    }
                }
                delay(2000)
            }
        }

        // Now playing
        viewModelScope.launch {
            SonaraNotificationListener.nowPlaying.collect { np ->
                _uiState.update { it.copy(title = np.title, artist = np.artist, isPlaying = np.isPlaying, hasTrack = np.title.isNotBlank()) }

                val key = "${np.title}::${np.artist}"
                if (np.title.isNotBlank() && key != lastProcessedTrack) {
                    lastProcessedTrack = key

                    // SELF-TRAINING: Feed Last.fm results to local AI
                    val apiKey = prefs.lastFmApiKeyFlow.first()
                    if (apiKey.isNotBlank()) {
                        viewModelScope.launch {
                            try {
                                trackResolver.resolve(np.title, np.artist, apiKey)
                                val result = trackResolver.result.value
                                if (result.source == ResolveSource.LASTFM || result.source == ResolveSource.LASTFM_ARTIST) {
                                    val ti = result.trackInfo
                                    if (ti.genre.isNotBlank() && ti.genre != "other") {
                                        // Train local classifier with Last.fm data
                                        val tokens = buildSet {
                                            np.title.lowercase().split(Regex("[^a-z0-9&]+")).filter { it.length > 1 }.let { addAll(it) }
                                            np.artist.lowercase().split(Regex("[^a-z0-9&]+")).filter { it.length > 1 }.let { addAll(it) }
                                        }
                                        app.sessionBridge.classifier.adaptWeights("other", ti.genre.lowercase(), tokens)
                                        SonaraLogger.ai("Self-train: Last.fm taught '${ti.genre}' for ${np.artist} - ${np.title}")
                                        prefs.incrementSongLearned(result.source.name, ti.genre)

                                        _uiState.update { it.copy(sourceLabel = "Last.fm -> AI trained") }
                                    }
                                }
                            } catch (e: Exception) {
                                SonaraLogger.e("AI", "Self-train error: ${e.message}")
                            }
                        }
                    }
                }
            }
        }

        // Learning stats
        viewModelScope.launch { prefs.songsLearnedFlow.collect { n -> _uiState.update { it.copy(songsLearned = n) } } }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        checkNotificationListener()
    }

    fun resetToAi() { app.resetToAi(); lastProcessedTrack = "" }
    fun checkNotificationListener() { _uiState.update { it.copy(notificationListenerEnabled = SonaraNotificationListener.isEnabled(getApplication())) } }
}
