package com.sonara.app.ui.screens.dashboard
import com.sonara.app.data.SonaraLogger

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.audio.engine.SmoothTransitionEngine
import com.sonara.app.audio.equalizer.TenBandEqualizer
import com.sonara.app.autoeq.AutoEqManager
import com.sonara.app.intelligence.ResolveSource
import com.sonara.app.intelligence.local.AiEqSuggestionEngine
import com.sonara.app.intelligence.local.SmartMediaType
import com.sonara.app.intelligence.local.MediaSourceDetector
import com.sonara.app.intelligence.local.MediaType
import com.sonara.app.intelligence.lastfm.ScrobblingManager
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
    val isComparing: Boolean = false, val isOriginalSound: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true; if (other !is DashboardUiState) return false
        return title == other.title && artist == other.artist && isPlaying == other.isPlaying &&
            genre == other.genre && bands.contentEquals(other.bands) && currentPresetName == other.currentPresetName &&
            notificationListenerEnabled == other.notificationListenerEnabled && smartMediaType == other.smartMediaType &&
            isComparing == other.isComparing && isOriginalSound == other.isOriginalSound
    }
    override fun hashCode() = title.hashCode()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val trackResolver = app.trackResolver
    private val headphoneDetector = app.headphoneDetector
    private val autoEqManager = AutoEqManager()
    private val scrobblingManager = ScrobblingManager()
    private val smoothEngine = SmoothTransitionEngine()
    private val compareManager = app.compareManager
    private val mediaClassifier = app.mediaClassifier
    private var lastProcessedTrack = ""
    private var scrobbleJob: Job? = null

    private val _uiState = MutableStateFlow(DashboardUiState(eqActive = app.audioEngine.isInitialized))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    init {
        viewModelScope.launch { app.eqState.collect { eq -> _uiState.update { it.copy(bands = eq.bands, bassBoost = eq.bassBoost, virtualizer = eq.virtualizer, currentPresetName = eq.presetName, isManualPreset = eq.isManualPreset) } } }
        viewModelScope.launch { compareManager.isComparing.collect { c -> _uiState.update { it.copy(isComparing = c) } } }
        viewModelScope.launch { compareManager.isOriginal.collect { o -> _uiState.update { it.copy(isOriginalSound = o) } } }

        viewModelScope.launch {
            SonaraNotificationListener.nowPlaying.collect { np ->
                _uiState.update { it.copy(title = np.title, artist = np.artist, isPlaying = np.isPlaying, hasTrack = np.title.isNotBlank()) }

                val key = "${np.title}::${np.artist}"
                if (np.title.isNotBlank() && key != lastProcessedTrack) {
                    lastProcessedTrack = key

                    // Smart Media Classification
                    val classification = mediaClassifier.classify(np.title, np.artist, np.album, np.packageName, np.duration)
                    _uiState.update { it.copy(smartMediaType = classification.type.label, smartMediaConfidence = classification.confidence) }

                    val eq = app.eqState.value
                    if (!eq.isManualPreset && prefs.aiEnabledFlow.first()) {
                        if (classification.type != SmartMediaType.MUSIC && classification.confidence >= 0.6f) {
                            val preset = MediaSourceDetector.suggestEqForMediaType(
                                when (classification.type) {
                                    SmartMediaType.FILM -> MediaType.VIDEO
                                    SmartMediaType.SERIES -> MediaType.VIDEO
                                    SmartMediaType.VIDEO -> MediaType.VIDEO
                                    SmartMediaType.PODCAST -> MediaType.PODCAST
                                    SmartMediaType.AUDIOBOOK -> MediaType.AUDIOBOOK
                                    SmartMediaType.GAME -> MediaType.GAME
                                    SmartMediaType.CALL -> MediaType.CALL
                                    else -> MediaType.UNKNOWN
                                }
                            )
                            applyWithTransition(preset.bands, "${classification.type.emoji} ${classification.type.label}", preset.bassBoost, preset.virtualizer, preset.loudness, preset.reasoning)
                            return@collect
                        }
                    }

                    val apiKey = prefs.lastFmApiKeyFlow.first()
                    trackResolver.resolve(np.title, np.artist, apiKey)

                    if (prefs.scrobblingEnabledFlow.first()) {
                        val secret = prefs.lastFmSharedSecretFlow.first(); val session = prefs.lastFmSessionKeyFlow.first()
                        if (apiKey.isNotBlank() && session.isNotBlank()) {
                            scrobblingManager.updateNowPlaying(np.title, np.artist, apiKey, secret, session)
                            scrobbleJob?.cancel(); scrobbleJob = viewModelScope.launch { delay(30_000); scrobblingManager.scrobble(np.title, np.artist, np.album, System.currentTimeMillis(), apiKey, secret, session) }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            trackResolver.result.collect { result ->
                if (result.isResolving) { _uiState.update { it.copy(isResolving = true) }; return@collect }
                if (result.source == ResolveSource.NONE) return@collect
                val ti = result.trackInfo
                val src = when (result.source) { ResolveSource.LASTFM -> "Last.fm"; ResolveSource.LASTFM_ARTIST -> "Last.fm (Artist)"; ResolveSource.LOCAL_AI -> "Local AI"; ResolveSource.CACHE -> "Cached"; ResolveSource.NONE -> "None" }
                _uiState.update { it.copy(genre = ti.genre.ifEmpty { "Unknown" }, mood = ti.mood.ifEmpty { "Unknown" }, energy = ti.energy, confidence = ti.confidence, isResolving = false, sourceLabel = src, pluginUsed = result.pluginUsed) }
                if (!result.source.name.contains("CACHE")) prefs.incrementSongLearned(result.source.name, ti.genre)

                val eq = app.eqState.value
                if (!eq.isManualPreset && prefs.aiEnabledFlow.first()) {
                    val suggestion = AiEqSuggestionEngine.suggest(ti)
                    val autoEqState = autoEqManager.state.value
                    val finalBands = FloatArray(10) { i -> TenBandEqualizer.clamp(suggestion.bands.getOrElse(i) { 0f } + if (autoEqState.isActive) autoEqState.correctionBands.getOrElse(i) { 0f } else 0f) }
                    applyWithTransition(finalBands, "AI: ${ti.genre.replaceFirstChar { it.uppercase() }}", suggestion.bassBoost, suggestion.virtualizer, app.eqState.value.loudness, suggestion.reasoning)
                }
            }
        }

        viewModelScope.launch { headphoneDetector.headphone.collect { hp -> _uiState.update { it.copy(headphoneName = hp.name, headphoneConnected = hp.isConnected, headphoneType = hp.type.name) }; autoEqManager.onHeadphoneChanged(hp, prefs.autoEqEnabledFlow.first()) } }
        viewModelScope.launch { autoEqManager.state.collect { a -> _uiState.update { it.copy(autoEqActive = a.isActive, autoEqProfile = a.profile?.name ?: "") } } }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        checkNotificationListener()
    }

    private fun applyWithTransition(bands: FloatArray, name: String, bass: Int, virt: Int, loud: Int, reasoning: String) {
        viewModelScope.launch {
            if (prefs.smoothTransitionsFlow.first()) { val cur = app.eqState.value.bands; smoothEngine.transition(cur, bands) { app.audioEngine.applyBands(it) } }
            app.applyEq(bands = bands, presetName = name, manual = false, bassBoost = bass, virtualizer = virt, loudness = loud)
            _uiState.update { it.copy(aiReasoning = reasoning) }
        }
    }

    fun quickCompare() { viewModelScope.launch { compareManager.quickCompare() } }
    fun resetToAi() { app.resetToAi(); lastProcessedTrack = "" }
    fun checkNotificationListener() { _uiState.update { it.copy(notificationListenerEnabled = SonaraNotificationListener.isEnabled(getApplication())) } }
}
