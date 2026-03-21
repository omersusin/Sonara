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
import com.sonara.app.intelligence.pipeline.SonaraTrackInfo
import com.sonara.app.media.ArtworkResolver
import com.sonara.app.receiver.AudioEffectSessionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    private var lastTrackKey = ""
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Scrobbling state
    private var scrobbleJob: Job? = null
    private var playStartTime: Long = 0
    private var hasScrobbled = false
    private var nowPlayingSent = false

    private val controllerCb = object : MediaController.Callback() {
        override fun onMetadataChanged(m: MediaMetadata?) { m?.let { processMetadata(it) } }
        override fun onPlaybackStateChanged(s: PlaybackState?) {
            val playing = s?.state == PlaybackState.STATE_PLAYING
            val wasPlaying = _nowPlaying.value.isPlaying
            _nowPlaying.value = _nowPlaying.value.copy(isPlaying = playing)

            // Scrobble timing
            if (playing && !wasPlaying) {
                playStartTime = System.currentTimeMillis()
                hasScrobbled = false
                scheduleScrobble()
            } else if (!playing && wasPlaying) {
                scrobbleJob?.cancel()
            }
        }
        override fun onSessionDestroyed() {
            _nowPlaying.value = ListenerNowPlaying()
            _albumArt.value = null
            activeController = null
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        if (controllers != null) pickBest(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val cn = ComponentName(this, SonaraNotificationListener::class.java)
            if (!sessionListenerRegistered) {
                sessionManager?.addOnActiveSessionsChangedListener(sessionListener, cn)
                sessionListenerRegistered = true
            }
            pickBest(sessionManager?.getActiveSessions(cn) ?: emptyList())
        } catch (e: Exception) { SonaraLogger.e("NLS", "Setup: ${e.message}") }
        AudioEffectSessionReceiver.bridgeCallback = { action, sid, _ ->
            if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION && sid > 0) {
                try { (application as SonaraApp).audioSessionManager.onSessionOpened(sid) } catch (_: Exception) {}
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            pickBest(sessionManager?.getActiveSessions(
                ComponentName(this, SonaraNotificationListener::class.java)) ?: emptyList())
        } catch (_: Exception) {}
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onDestroy() {
        super.onDestroy()
        if (sessionListenerRegistered) {
            try { sessionManager?.removeOnActiveSessionsChangedListener(sessionListener) } catch (_: Exception) {}
            sessionListenerRegistered = false
        }
        activeController?.let { try { it.unregisterCallback(controllerCb) } catch (_: Exception) {} }
        AudioEffectSessionReceiver.bridgeCallback = null
        _albumArt.value = null
        instance = null
        scope.cancel()
    }

    private fun pickBest(controllers: List<MediaController>) {
        val target = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull() ?: return

        if (activeController?.sessionToken == target.sessionToken) {
            target.metadata?.let { processMetadata(it) }
            return
        }
        activeController?.let { try { it.unregisterCallback(controllerCb) } catch (_: Exception) {} }
        activeController = target
        target.registerCallback(controllerCb)
        target.metadata?.let { processMetadata(it) }
        _nowPlaying.value = _nowPlaying.value.copy(
            isPlaying = target.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = target.packageName ?: ""
        )
    }

    private fun processMetadata(metadata: MediaMetadata) {
        _albumArt.value = try { ArtworkResolver.extract(metadata, contentResolver) } catch (_: Exception) { null }

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        _nowPlaying.value = _nowPlaying.value.copy(title = title, artist = artist, album = album, duration = duration)

        val key = "$title::$artist"
        if (title.isBlank() || key == lastTrackKey) return
        lastTrackKey = key

        // Reset scrobble state on new track
        playStartTime = System.currentTimeMillis()
        hasScrobbled = false
        nowPlayingSent = false
        scrobbleJob?.cancel()

        scope.launch {
            try {
                val app = application as SonaraApp
                val isManualPreset = app.eqState.value.isManualPreset
                val aiOn = app.preferences.aiEnabledFlow.first()

                // ═══ Try preloaded prediction first ═══
                val preloaded = app.nextTrackPreloader.consumeIfMatch(title, artist)
                val prediction = if (preloaded != null) {
                    SonaraLogger.ai("Using PRELOADED prediction for $title")
                    preloaded
                } else {
                    val track = SonaraTrackInfo(title, artist, album, duration, _nowPlaying.value.packageName)
                    app.inferencePipeline.analyze(track)
                }

                // Update genre info for Dashboard
                _currentGenre.value = prediction.genre.displayName
                _currentMood.value = prediction.mood.displayName
                _currentEnergy.value = prediction.energy
                _currentConfidence.value = prediction.confidence
                _currentSource.value = prediction.source.displayName

                // Apply EQ with smooth transition
                if (prediction.genre != com.sonara.app.intelligence.pipeline.Genre.UNKNOWN && prediction.confidence > 0.05f) {
                    if (!isManualPreset && aiOn) app.applyFromPrediction(prediction)
                }

                // Train adaptive classifier
                if (prediction.source == com.sonara.app.intelligence.pipeline.PredictionSource.LASTFM ||
                    prediction.source == com.sonara.app.intelligence.pipeline.PredictionSource.MERGED) {
                    app.preferences.incrementSongLearned(prediction.source.name, prediction.genre.name)
                }

                // ═══ Preload NEXT track ═══
                app.nextTrackPreloader.tryPreload(activeController)

                // ═══ Scrobbling: updateNowPlaying ═══
                sendNowPlaying(app, title, artist)

            } catch (e: Exception) { SonaraLogger.e("NLS", "Process: ${e.message}") }
        }

        // Schedule scrobble if playing
        if (_nowPlaying.value.isPlaying) scheduleScrobble()
    }

    private fun sendNowPlaying(app: SonaraApp, title: String, artist: String) {
        if (nowPlayingSent) return
        scope.launch {
            try {
                val scrobblingEnabled = app.preferences.scrobblingEnabledFlow.first()
                if (!scrobblingEnabled) return@launch
                val apiKey = app.secureSecrets.getLastFmApiKey().takeIf { it.isNotBlank() }
                    ?: app.preferences.lastFmApiKeyFlow.first()
                val secret = app.secureSecrets.getLastFmSharedSecret().takeIf { it.isNotBlank() }
                    ?: app.preferences.lastFmSharedSecretFlow.first()
                val sessionKey = app.secureSecrets.getLastFmSessionKey().takeIf { it.isNotBlank() }
                    ?: app.preferences.lastFmSessionKeyFlow.first()
                if (apiKey.isBlank() || sessionKey.isBlank()) return@launch

                val scrobbler = com.sonara.app.intelligence.lastfm.ScrobblingManager()
                scrobbler.updateNowPlaying(title, artist, apiKey, secret, sessionKey)
                nowPlayingSent = true
                SonaraLogger.i("Scrobble", "NowPlaying sent: $title")
            } catch (e: Exception) {
                SonaraLogger.w("Scrobble", "NowPlaying failed: ${e.message}")
            }
        }
    }

    private fun scheduleScrobble() {
        scrobbleJob?.cancel()
        scrobbleJob = scope.launch {
            // Last.fm rule: scrobble after 50% of track or 4 minutes, whichever is first
            val np = _nowPlaying.value
            val waitMs = if (np.duration > 0) {
                minOf(np.duration / 2, 4 * 60 * 1000L).coerceAtLeast(30_000L)
            } else {
                4 * 60 * 1000L
            }
            delay(waitMs)
            if (!hasScrobbled && _nowPlaying.value.isPlaying) {
                doScrobble()
            }
        }
    }

    private suspend fun doScrobble() {
        if (hasScrobbled) return
        val np = _nowPlaying.value
        if (np.title.isBlank()) return
        try {
            val app = application as SonaraApp
            val scrobblingEnabled = app.preferences.scrobblingEnabledFlow.first()
            if (!scrobblingEnabled) return
            val apiKey = app.secureSecrets.getLastFmApiKey().takeIf { it.isNotBlank() }
                ?: app.preferences.lastFmApiKeyFlow.first()
            val secret = app.secureSecrets.getLastFmSharedSecret().takeIf { it.isNotBlank() }
                ?: app.preferences.lastFmSharedSecretFlow.first()
            val sessionKey = app.secureSecrets.getLastFmSessionKey().takeIf { it.isNotBlank() }
                ?: app.preferences.lastFmSessionKeyFlow.first()
            if (apiKey.isBlank() || sessionKey.isBlank()) return

            val scrobbler = com.sonara.app.intelligence.lastfm.ScrobblingManager()
            val ok = scrobbler.scrobble(np.title, np.artist, np.album, playStartTime, apiKey, secret, sessionKey)
            if (ok) {
                hasScrobbled = true
                SonaraLogger.i("Scrobble", "Scrobbled: ${np.title}")
            }
        } catch (e: Exception) { SonaraLogger.w("Scrobble", "Scrobble failed: ${e.message}") }
    }

    companion object {
        var instance: SonaraNotificationListener? = null; private set
        private val _nowPlaying = MutableStateFlow(ListenerNowPlaying())
        val nowPlaying: StateFlow<ListenerNowPlaying> = _nowPlaying.asStateFlow()
        private val _albumArt = MutableStateFlow<Bitmap?>(null)
        val albumArt: StateFlow<Bitmap?> = _albumArt.asStateFlow()
        val _currentGenre = MutableStateFlow("")
        val currentGenre: StateFlow<String> = _currentGenre.asStateFlow()
        val _currentMood = MutableStateFlow("")
        val currentMood: StateFlow<String> = _currentMood.asStateFlow()
        val _currentEnergy = MutableStateFlow(0.5f)
        val currentEnergy: StateFlow<Float> = _currentEnergy.asStateFlow()
        val _currentConfidence = MutableStateFlow(0f)
        val currentConfidence: StateFlow<Float> = _currentConfidence.asStateFlow()
        val _currentSource = MutableStateFlow("None")
        val currentSource: StateFlow<String> = _currentSource.asStateFlow()
        fun isEnabled(ctx: Context): Boolean {
            val cn = ComponentName(ctx, SonaraNotificationListener::class.java)
            val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            return flat?.contains(cn.flattenToString()) == true
        }
    }
}
