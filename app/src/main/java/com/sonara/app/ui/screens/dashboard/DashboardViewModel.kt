package com.sonara.app.ui.screens.dashboard

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.ResolveSource
import com.sonara.app.service.SonaraNotificationListener
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
    val sourceLabel: String = "None", val isResolving: Boolean = false,
    val currentPresetName: String = "Flat", val isAiEnabled: Boolean = true,
    val bands: FloatArray = FloatArray(10), val bassBoost: Int = 0, val virtualizer: Int = 0,
    val notificationListenerEnabled: Boolean = false, val eqActive: Boolean = false,
    val isManualPreset: Boolean = false, val songsLearned: Int = 0,
    val eqStrategy: String = "none", val aiModelGenres: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true; if (other !is DashboardUiState) return false
        return title == other.title && artist == other.artist && isPlaying == other.isPlaying &&
            genre == other.genre && bands.contentEquals(other.bands) && currentPresetName == other.currentPresetName &&
            notificationListenerEnabled == other.notificationListenerEnabled && songsLearned == other.songsLearned &&
            eqStrategy == other.eqStrategy && aiModelGenres == other.aiModelGenres
    }
    override fun hashCode() = title.hashCode()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val trackResolver = app.trackResolver
    private var lastProcessedTrack = ""

    private val _uiState = MutableStateFlow(DashboardUiState(eqActive = app.audioSessionManager.isInitialized))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    init {
        viewModelScope.launch { app.eqState.collect { eq -> _uiState.update { it.copy(bands = eq.bands, bassBoost = eq.bassBoost, virtualizer = eq.virtualizer, currentPresetName = eq.presetName, isManualPreset = eq.isManualPreset) } } }

        // EQ strategy and AI stats
        viewModelScope.launch {
            while (true) {
                val strategy = app.audioSessionManager.activeStrategy.value
                val stats = app.adaptiveClassifier.getStats()
                _uiState.update { it.copy(eqStrategy = strategy, eqActive = app.audioSessionManager.isInitialized, aiModelGenres = stats["genres"] as? Int ?: 0) }
                delay(3000)
            }
        }

        // Now playing + self-training
        viewModelScope.launch {
            SonaraNotificationListener.nowPlaying.collect { np ->
                _uiState.update { it.copy(title = np.title, artist = np.artist, isPlaying = np.isPlaying, hasTrack = np.title.isNotBlank()) }
                val key = "${np.title}::${np.artist}"
                if (np.title.isNotBlank() && key != lastProcessedTrack) {
                    lastProcessedTrack = key
                    val apiKey = prefs.lastFmApiKeyFlow.first()

                    // Try adaptive classifier first
                    val (localGenre, localConf) = app.adaptiveClassifier.classify(np.title, np.artist, np.album)
                    if (localConf > 0.6f) {
                        _uiState.update { it.copy(genre = localGenre.replaceFirstChar { c -> c.uppercase() }, confidence = localConf, sourceLabel = "Local AI (trained)", isResolving = false) }
                        SonaraLogger.ai("Local AI: $localGenre (conf=${"%.2f".format(localConf)})")
                    }

                    // Resolve via Last.fm + plugins (background)
                    if (apiKey.isNotBlank()) {
                        viewModelScope.launch {
                            try {
                                trackResolver.resolve(np.title, np.artist, apiKey)
                                val result = trackResolver.result.value
                                val ti = result.trackInfo

                                if (result.source == ResolveSource.LASTFM || result.source == ResolveSource.LASTFM_ARTIST) {
                                    if (ti.genre.isNotBlank() && ti.genre != "other") {
                                        // SELF-TRAINING: Last.fm teaches local AI
                                        app.adaptiveClassifier.train(ti.genre, np.title, np.artist, np.album, weight = ti.confidence * 3f)
                                        prefs.incrementSongLearned(result.source.name, ti.genre)
                                        SonaraLogger.ai("Last.fm trained AI: '${ti.genre}' for ${np.artist}")

                                        _uiState.update { it.copy(
                                            genre = ti.genre.replaceFirstChar { c -> c.uppercase() },
                                            mood = ti.mood.replaceFirstChar { c -> c.uppercase() },
                                            energy = ti.energy, confidence = ti.confidence,
                                            sourceLabel = "Last.fm (AI trained)"
                                        ) }
                                    }
                                } else if (result.source == ResolveSource.LOCAL_AI || result.source == ResolveSource.CACHE) {
                                    _uiState.update { it.copy(
                                        genre = ti.genre.ifEmpty { localGenre }.replaceFirstChar { c -> c.uppercase() },
                                        mood = ti.mood.replaceFirstChar { c -> c.uppercase() },
                                        energy = ti.energy, confidence = maxOf(ti.confidence, localConf),
                                        sourceLabel = if (localConf > ti.confidence) "Local AI" else result.source.name
                                    ) }
                                }
                            } catch (e: Exception) { SonaraLogger.e("AI", "Resolve error: ${e.message}") }
                        }
                    } else if (localConf <= 0.6f) {
                        _uiState.update { it.copy(genre = localGenre.replaceFirstChar { c -> c.uppercase() }, confidence = localConf, sourceLabel = "Local AI") }
                    }
                }
            }
        }

        viewModelScope.launch { prefs.songsLearnedFlow.collect { n -> _uiState.update { it.copy(songsLearned = n) } } }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        checkNotificationListener()
    }

    fun resetToAi() { app.resetToAi(); lastProcessedTrack = "" }
    fun checkNotificationListener() { _uiState.update { it.copy(notificationListenerEnabled = SonaraNotificationListener.isEnabled(getApplication())) } }
}
