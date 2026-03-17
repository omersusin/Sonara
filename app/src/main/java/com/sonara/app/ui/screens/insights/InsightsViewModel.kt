package com.sonara.app.ui.screens.insights

import android.app.Application
import android.graphics.Bitmap
import android.media.AudioDeviceInfo
import android.media.AudioManager
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
    val trackTitle: String = "",
    val trackArtist: String = "",
    val genre: String = "Unknown",
    val mood: String = "Unknown",
    val energy: Float = 0.5f,
    val confidence: Float = 0f,
    val dataSource: String = "None",
    val headphoneConnected: Boolean = false,
    val headphoneName: String = "",
    val isAiEnabled: Boolean = true,
    val isPlaying: Boolean = false,
    val cacheSize: Int = 0,
    val eqActive: Boolean = false,
    val songsLearned: Int = 0,
    val songsViaLastFm: Int = 0,
    val songsViaLocal: Int = 0,
    val genreDistribution: Map<String, Int> = emptyMap(),
    val apiAccuracy: Int = 0,
    val eqStrategy: String = "none",
    val aiModelGenres: Int = 0
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val cache = TrackCache(app.database.trackCacheDao())
    private val audioManager = application.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager

    private val _uiState = MutableStateFlow(InsightsUiState(
        eqActive = app.audioSessionManager.isInitialized,
        eqStrategy = app.audioSessionManager.activeStrategy.value
    ))
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    init {
        // Now playing
        viewModelScope.launch {
            SonaraNotificationListener.nowPlaying.collect { np ->
                _uiState.update { it.copy(trackTitle = np.title, trackArtist = np.artist, isPlaying = np.isPlaying) }
                if (np.title.isNotBlank()) {
                    val apiKey = prefs.lastFmApiKeyFlow.first()
                    app.trackResolver.resolve(np.title, np.artist, apiKey)
                }
            }
        }

        // Track resolver results
        viewModelScope.launch {
            app.trackResolver.result.collect { r ->
                _uiState.update { it.copy(
                    genre = r.trackInfo.genre.ifEmpty { "Unknown" },
                    mood = r.trackInfo.mood.ifEmpty { "Unknown" },
                    energy = r.trackInfo.energy,
                    confidence = r.trackInfo.confidence,
                    dataSource = when (r.source) {
                        ResolveSource.LASTFM -> "Last.fm"
                        ResolveSource.LASTFM_ARTIST -> "Last.fm (Artist)"
                        ResolveSource.LOCAL_AI -> "Local AI"
                        ResolveSource.CACHE -> "Cached"
                        ResolveSource.NONE -> "None"
                    }
                ) }
            }
        }

        // Headphone detection via AudioManager
        viewModelScope.launch {
            while (true) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val headphone = devices.firstOrNull { d ->
                    d.type in listOf(
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        AudioDeviceInfo.TYPE_USB_HEADSET
                    )
                }
                _uiState.update { it.copy(
                    headphoneConnected = headphone != null,
                    headphoneName = headphone?.productName?.toString()?.takeIf { n -> n.isNotBlank() && n != "null" }
                        ?: when (headphone?.type) {
                            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
                            AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired"
                            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB"
                            else -> ""
                        }
                ) }
                delay(5000)
            }
        }

        // EQ strategy
        viewModelScope.launch { app.audioSessionManager.activeStrategy.collect { s -> _uiState.update { it.copy(eqStrategy = s, eqActive = s != "none") } } }

        // Prefs
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        viewModelScope.launch { prefs.songsLearnedFlow.collect { n -> _uiState.update { it.copy(songsLearned = n) } } }
        viewModelScope.launch { prefs.songsViaLastFmFlow.collect { n -> _uiState.update { st -> st.copy(songsViaLastFm = n, apiAccuracy = if (st.songsLearned > 0) (n * 100 / st.songsLearned) else 0) } } }
        viewModelScope.launch { prefs.songsViaLocalFlow.collect { n -> _uiState.update { it.copy(songsViaLocal = n) } } }
        viewModelScope.launch { prefs.genreStatsFlow.collect { raw ->
            val map = if (raw.isBlank()) emptyMap() else raw.split(";").mapNotNull { val p = it.split(":"); if (p.size == 2) p[0] to (p[1].toIntOrNull() ?: 0) else null }.toMap()
            _uiState.update { it.copy(genreDistribution = map) }
        } }

        // AI model stats
        viewModelScope.launch {
            val stats = app.adaptiveClassifier.getStats()
            _uiState.update { it.copy(aiModelGenres = stats["genres"] as? Int ?: 0) }
        }

        refreshCache()
    }

    fun refreshCache() { viewModelScope.launch { _uiState.update { it.copy(cacheSize = cache.size()) } } }
}
