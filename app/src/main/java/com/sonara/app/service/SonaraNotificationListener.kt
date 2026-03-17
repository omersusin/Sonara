package com.sonara.app.service

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.audiofx.AudioEffect
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sonara.app.SonaraApp
import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.ResolveSource
import com.sonara.app.intelligence.local.AiEqSuggestionEngine
import com.sonara.app.media.ArtworkResolver
import com.sonara.app.receiver.AudioEffectSessionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ListenerNowPlaying(
    val title: String = "", val artist: String = "", val album: String = "",
    val packageName: String = "", val isPlaying: Boolean = false, val duration: Long = 0
)

class SonaraNotificationListener : NotificationListenerService() {
    private var sessionManager: MediaSessionManager? = null
    private var sessionListenerRegistered = false
    private var activeController: MediaController? = null
    private var metaCb: MediaController.Callback? = null
    private var lastProcessedTrack = ""
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Shared genre info — Dashboard observes this
    private val _currentGenre = MutableStateFlow("")
    private val _currentMood = MutableStateFlow("")
    private val _currentEnergy = MutableStateFlow(0.5f)
    private val _currentConfidence = MutableStateFlow(0f)
    private val _currentSource = MutableStateFlow("None")

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) { metadata?.let { processMetadata(it) } }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            _nowPlaying.value = _nowPlaying.value.copy(isPlaying = state?.state == PlaybackState.STATE_PLAYING)
        }
        override fun onSessionDestroyed() { _nowPlaying.value = ListenerNowPlaying(); _albumArt.value = null; activeController = null }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        if (controllers != null) pickBest(controllers)
    }

    override fun onCreate() { super.onCreate(); instance = this; sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager }

    override fun onListenerConnected() {
        super.onListenerConnected()
        SonaraLogger.i("NLS", "Connected")
        try {
            val cn = ComponentName(this, SonaraNotificationListener::class.java)
            if (!sessionListenerRegistered) { sessionManager?.addOnActiveSessionsChangedListener(sessionListener, cn); sessionListenerRegistered = true }
            pickBest(sessionManager?.getActiveSessions(cn) ?: emptyList())
        } catch (e: Exception) { SonaraLogger.e("NLS", "Setup: ${e.message}") }

        AudioEffectSessionReceiver.bridgeCallback = { action, sessionId, _ ->
            if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION && sessionId > 0) {
                try { (application as SonaraApp).audioSessionManager.onSessionOpened(sessionId) } catch (_: Exception) {}
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            val cn = ComponentName(this, SonaraNotificationListener::class.java)
            pickBest(sessionManager?.getActiveSessions(cn) ?: emptyList())
        } catch (_: Exception) {}
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onDestroy() {
        super.onDestroy()
        if (sessionListenerRegistered) { try { sessionManager?.removeOnActiveSessionsChangedListener(sessionListener) } catch (_: Exception) {}; sessionListenerRegistered = false }
        activeController?.let { try { it.unregisterCallback(controllerCallback) } catch (_: Exception) {} }
        AudioEffectSessionReceiver.bridgeCallback = null
        _albumArt.value = null; instance = null; scope.cancel()
    }

    private fun pickBest(controllers: List<MediaController>) {
        val playing = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val target = playing ?: controllers.firstOrNull() ?: return
        attachTo(target)
    }

    private fun attachTo(controller: MediaController) {
        if (activeController?.sessionToken == controller.sessionToken) { controller.metadata?.let { processMetadata(it) }; return }
        activeController?.let { try { it.unregisterCallback(controllerCallback) } catch (_: Exception) {} }
        activeController = controller
        controller.registerCallback(controllerCallback)
        controller.metadata?.let { processMetadata(it) }
        _nowPlaying.value = _nowPlaying.value.copy(isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING, packageName = controller.packageName ?: "")
    }

    private fun processMetadata(metadata: MediaMetadata) {
        val artwork = try { ArtworkResolver.extract(metadata, contentResolver) } catch (_: Exception) { null }
        _albumArt.value = artwork

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        _nowPlaying.value = _nowPlaying.value.copy(title = title, artist = artist, album = album, duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION))

        val trackKey = "$title::$artist"
        if (title.isBlank() || trackKey == lastProcessedTrack) return
        lastProcessedTrack = trackKey

        scope.launch {
            try {
                val app = application as SonaraApp
                if (app.eqState.value.isManualPreset) return@launch

                // 1. Resolve — cache/lastfm/plugins
                val apiKey = app.preferences.lastFmApiKeyFlow.first()
                app.trackResolver.resolve(title, artist, apiKey)
                val result = app.trackResolver.result.value
                val ti = result.trackInfo

                // 2. Update genre info for Dashboard
                if (ti.genre.isNotBlank() && ti.genre != "other") {
                    _currentGenre.value = ti.genre
                    _currentMood.value = ti.mood
                    _currentEnergy.value = ti.energy
                    _currentConfidence.value = ti.confidence
                    _currentSource.value = when (result.source) {
                        ResolveSource.LASTFM -> "Last.fm"
                        ResolveSource.LASTFM_ARTIST -> "Last.fm"
                        ResolveSource.CACHE -> "Cached"
                        ResolveSource.LOCAL_AI -> "Local AI"
                        else -> "AI"
                    }
                }

                // 3. ALWAYS apply EQ if genre is known — NO confidence gate, NO source gate
                if (ti.genre.isNotBlank() && ti.genre != "other") {
                    val suggestion = AiEqSuggestionEngine.suggest(ti)
                    app.applyEq(
                        bands = suggestion.bands,
                        presetName = "AI: ${ti.genre.replaceFirstChar { it.uppercase() }}",
                        manual = false,
                        bassBoost = suggestion.bassBoost,
                        virtualizer = suggestion.virtualizer
                    )
                    SonaraLogger.ai("EQ applied for ${ti.genre}: ${suggestion.bands.take(5).map { "%.1f".format(it) }}")
                }

                // 4. Train adaptive classifier with ANY valid result
                if (ti.genre.isNotBlank() && ti.genre != "other") {
                    val weight = when (result.source) {
                        ResolveSource.LASTFM, ResolveSource.LASTFM_ARTIST -> ti.confidence * 3f
                        ResolveSource.LOCAL_AI -> ti.confidence * 1.5f
                        else -> ti.confidence
                    }
                    app.adaptiveClassifier.train(ti.genre, title, artist, album, weight)
                    if (result.source == ResolveSource.LASTFM || result.source == ResolveSource.LASTFM_ARTIST) {
                        app.preferences.incrementSongLearned(result.source.name, ti.genre)
                    }
                }
            } catch (e: Exception) {
                SonaraLogger.e("NLS", "Process error: ${e.message}")
            }
        }
    }

    companion object {
        var instance: SonaraNotificationListener? = null; private set
        private val _nowPlaying = MutableStateFlow(ListenerNowPlaying())
        val nowPlaying: StateFlow<ListenerNowPlaying> = _nowPlaying.asStateFlow()
        private val _albumArt = MutableStateFlow<Bitmap?>(null)
        val albumArt: StateFlow<Bitmap?> = _albumArt.asStateFlow()

        // Genre info — Dashboard reads these
        val currentGenre: StateFlow<String> get() = instance?._currentGenre ?: MutableStateFlow("")
        val currentMood: StateFlow<String> get() = instance?._currentMood ?: MutableStateFlow("")
        val currentEnergy: StateFlow<Float> get() = instance?._currentEnergy ?: MutableStateFlow(0.5f)
        val currentConfidence: StateFlow<Float> get() = instance?._currentConfidence ?: MutableStateFlow(0f)
        val currentSource: StateFlow<String> get() = instance?._currentSource ?: MutableStateFlow("None")

        fun isEnabled(ctx: Context): Boolean {
            val cn = ComponentName(ctx, SonaraNotificationListener::class.java)
            val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            return flat?.contains(cn.flattenToString()) == true
        }
    }
}
