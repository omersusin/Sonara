package com.sonara.app.ui.screens.dashboard

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.ai.SonaraAi
import com.sonara.app.ai.SonaraAiState
import com.sonara.app.intelligence.lastfm.LoveStateCache
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.components.DisplayLabelMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val currentPresetName: String = "Flat",
    val isAiEnabled: Boolean = true,
    val bands: FloatArray = FloatArray(10,
    val hasSeenHearTheDifference: Boolean = false),
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val notificationListenerEnabled: Boolean = false,
    val eqActive: Boolean = false,
    val isManualPreset: Boolean = false,
    val songsLearned: Int = 0,
    val eqStrategy: String = "none",
    val route: String = "Speaker",
    val headphoneName: String = "",
    val savedMessage: String = "",
    val isLoved: Boolean = false,
    val geminiSummary: String = ""
,
    val visualizerData: FloatArray? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DashboardUiState) return false
        return title == other.title && artist == other.artist &&
            isPlaying == other.isPlaying && hasTrack == other.hasTrack &&
            genre == other.genre && mood == other.mood &&
            energy == other.energy && confidence == other.confidence &&
            sourceLabel == other.sourceLabel &&
            currentPresetName == other.currentPresetName &&
            isAiEnabled == other.isAiEnabled &&
            bands.contentEquals(other.bands) &&
            bassBoost == other.bassBoost && virtualizer == other.virtualizer &&
            loudness == other.loudness &&
            notificationListenerEnabled == other.notificationListenerEnabled &&
            eqActive == other.eqActive && isManualPreset == other.isManualPreset &&
            songsLearned == other.songsLearned &&
            eqStrategy == other.eqStrategy && route == other.route &&
            headphoneName == other.headphoneName &&
            savedMessage == other.savedMessage && isLoved == other.isLoved &&
            geminiSummary == other.geminiSummary
    }
    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + genre.hashCode()
        result = 31 * result + mood.hashCode()
        result = 31 * result + route.hashCode()
        result = 31 * result + bands.contentHashCode()
        result = 31 * result + notificationListenerEnabled.hashCode()
        result = 31 * result + isLoved.hashCode()
        result = 31 * result + geminiSummary.hashCode()
        return result
    }
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val _uiState = MutableStateFlow(DashboardUiState(
        eqActive = app.audioSessionManager.isInitialized,
        eqStrategy = app.audioSessionManager.activeStrategy.value,
        route = app.currentRoute.value.displayName
    ))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    // AI state
    val aiState: StateFlow<SonaraAiState> =
        SonaraAi.getInstance()?.state ?: MutableStateFlow(SonaraAiState()).asStateFlow()

    init {
        viewModelScope.launch {
            SonaraAi.getInstance()?.visualizerData?.collect { data ->
                _uiState.update { it.copy(visualizerData = data) }
            }
        }

        viewModelScope.launch {
            app.preferences.hasSeenHearTheDifferenceFlow.collect { seen ->
                _uiState.update { it.copy(hasSeenHearTheDifference = seen) }
            }
        }

        viewModelScope.launch { app.eqState.collect { eq -> _uiState.update { it.copy(bands = eq.bands, bassBoost = eq.bassBoost, virtualizer = eq.virtualizer, loudness = eq.loudness, currentPresetName = eq.presetName, isManualPreset = eq.isManualPreset) } } }
        viewModelScope.launch { SonaraNotificationListener.nowPlaying.collect { np ->
            _uiState.update { it.copy(title = np.title, artist = np.artist, isPlaying = np.isPlaying, hasTrack = np.title.isNotBlank()) }
            if (np.title.isNotBlank()) {
                val cached = LoveStateCache.isLoved(np.title, np.artist)
                if (cached != null) _uiState.update { it.copy(isLoved = cached) }
                else _uiState.update { it.copy(isLoved = false) }
            }
        } }
        viewModelScope.launch { SonaraNotificationListener.currentGenre.collect { g -> if (g.isNotBlank()) _uiState.update { it.copy(genre = DisplayLabelMapper.formatGenre(g)) } } }
        viewModelScope.launch { SonaraNotificationListener.currentMood.collect { m -> if (m.isNotBlank()) _uiState.update { it.copy(mood = DisplayLabelMapper.formatMood(m)) } } }
        viewModelScope.launch { SonaraNotificationListener.currentEnergy.collect { e -> _uiState.update { it.copy(energy = e) } } }
        viewModelScope.launch { SonaraNotificationListener.currentConfidence.collect { c -> _uiState.update { it.copy(confidence = c) } } }
        viewModelScope.launch { SonaraNotificationListener.currentSource.collect { s -> if (s.isNotBlank()) _uiState.update { it.copy(sourceLabel = s) } } }
        viewModelScope.launch { app.audioSessionManager.activeStrategy.collect { s -> _uiState.update { it.copy(eqStrategy = s, eqActive = s != "none") } } }
        viewModelScope.launch { app.currentRoute.collect { r -> _uiState.update { it.copy(route = r.displayName) } } }
        viewModelScope.launch { app.preferences.songsLearnedFlow.collect { n -> _uiState.update { it.copy(songsLearned = n) } } }
        viewModelScope.launch { app.preferences.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        viewModelScope.launch { app.geminiInsight.collect { insight ->
            _uiState.update { it.copy(geminiSummary = insight?.summary ?: "") }
        } }
        // Sync AI learned count
        viewModelScope.launch { aiState.collect { ai ->
            if (ai.learnedCount > 0) _uiState.update { it.copy(songsLearned = maxOf(it.songsLearned, ai.learnedCount)) }
        } }
        checkNotificationListener()
    }

    fun resetToAi() { app.resetToAi() }

    fun saveCurrentAsPreset() {
        val s = _uiState.value
        val name = if (s.genre != "Unknown") "AI: ${s.genre} (${s.mood})" else "AI Preset"
        app.saveCurrentAsPreset(name)
        _uiState.update { it.copy(savedMessage = "Saved as \"$name\"") }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(savedMessage = "") }
        }
    }

    fun toggleLove() {
        val s = _uiState.value
        if (s.title.isBlank()) return
        val newState = !s.isLoved
        _uiState.update { it.copy(isLoved = newState) }
        LoveStateCache.setLoved(s.title, s.artist, newState)
        viewModelScope.launch {
            val ok = app.loveTrack(s.title, s.artist, newState)
            if (!ok) {
                _uiState.update { it.copy(isLoved = !newState) }
                LoveStateCache.setLoved(s.title, s.artist, !newState)
            }
        }
    }

    fun onAiFeedback(type: String) { SonaraAi.getInstance()?.onFeedback(type) }
    fun onGenreCorrection(genre: String) { SonaraAi.getInstance()?.onGenreCorrection(genre) }

    fun checkNotificationListener() {
        val instanceAlive = SonaraNotificationListener.instance != null
        val systemEnabled = SonaraNotificationListener.isEnabled(getApplication())
        _uiState.update { it.copy(notificationListenerEnabled = instanceAlive || systemEnabled) }
    }


    fun dismissHearTheDifference() {
        viewModelScope.launch {
            app.preferences.setHasSeenHearTheDifference(true)
        }
    }

    fun setEqTemporarilyDisabled(disabled: Boolean) {
        app.audioSessionManager.setEnabled(!disabled)
    }

}
