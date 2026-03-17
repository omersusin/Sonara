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
    private var metaCb: MediaController.Callback? = null
    private var lastProcessedTrack = ""
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) { metadata?.let { processMetadata(it) } }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            _nowPlaying.value = _nowPlaying.value.copy(isPlaying = state?.state == PlaybackState.STATE_PLAYING)
        }
        override fun onSessionDestroyed() {
            _nowPlaying.value = ListenerNowPlaying(); _albumArt.value = null; activeController = null
        }
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
            if (!sessionListenerRegistered) {
                sessionManager?.addOnActiveSessionsChangedListener(sessionListener, cn)
                sessionListenerRegistered = true
            }
            pickBest(sessionManager?.getActiveSessions(cn) ?: emptyList())
        } catch (e: Exception) { SonaraLogger.e("NLS", "Setup: ${e.message}") }

        // Wire broadcast receiver — catches session IDs from players
        AudioEffectSessionReceiver.bridgeCallback = { action, sessionId, pkg ->
            if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION && sessionId > 0) {
                SonaraLogger.eq("Broadcast session: $sessionId ($pkg)")
                // Feed session to AudioSessionManager for additional coverage
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
        if (sessionListenerRegistered) {
            try { sessionManager?.removeOnActiveSessionsChangedListener(sessionListener) } catch (_: Exception) {}
            sessionListenerRegistered = false
        }
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
        if (activeController?.sessionToken == controller.sessionToken) {
            controller.metadata?.let { processMetadata(it) }
            return
        }
        activeController?.let { try { it.unregisterCallback(controllerCallback) } catch (_: Exception) {} }
        activeController = controller
        controller.registerCallback(controllerCallback)
        controller.metadata?.let { processMetadata(it) }
        _nowPlaying.value = _nowPlaying.value.copy(
            isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = controller.packageName ?: ""
        )
    }

    private fun processMetadata(metadata: MediaMetadata) {
        // Album art
        val artwork = try { ArtworkResolver.extract(metadata, contentResolver) } catch (_: Exception) { null }
        _albumArt.value = artwork

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        _nowPlaying.value = _nowPlaying.value.copy(title = title, artist = artist, album = album,
            duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION))

        // Classify and apply EQ — ONLY if not manual preset
        val trackKey = "$title::$artist"
        if (title.isNotBlank() && trackKey != lastProcessedTrack) {
            lastProcessedTrack = trackKey
            scope.launch {
                try {
                    val app = application as SonaraApp
                    val eqState = app.eqState.value
                    if (eqState.isManualPreset) return@launch // User chose preset, don't override

                    // 1. Try adaptive classifier first (instant, no API call)
                    val (localGenre, localConf) = app.adaptiveClassifier.classify(title, artist, album)
                    SonaraLogger.ai("Local classify: $localGenre (${(localConf * 100).toInt()}%)")

                    // 2. Apply EQ from best available classification
                    if (localConf > 0.5f) {
                        val suggestion = AiEqSuggestionEngine.suggest(
                            com.sonara.app.data.models.TrackInfo(title = title, artist = artist, genre = localGenre, confidence = localConf, source = "local-ai")
                        )
                        app.applyEq(bands = suggestion.bands, presetName = "AI: ${localGenre.replaceFirstChar { it.uppercase() }}", manual = false,
                            bassBoost = suggestion.bassBoost, virtualizer = suggestion.virtualizer)
                    }

                    // 3. Background: Last.fm for better accuracy + self-training
                    val apiKey = app.preferences.lastFmApiKeyFlow.first()
                    if (apiKey.isNotBlank()) {
                        app.trackResolver.resolve(title, artist, apiKey)
                        val result = app.trackResolver.result.value
                        if ((result.source == ResolveSource.LASTFM || result.source == ResolveSource.LASTFM_ARTIST) &&
                            result.trackInfo.genre.isNotBlank() && result.trackInfo.genre != "other") {

                            // Self-train local classifier with Last.fm data
                            app.adaptiveClassifier.train(result.trackInfo.genre, title, artist, album, weight = result.trackInfo.confidence * 3f)
                            app.preferences.incrementSongLearned(result.source.name, result.trackInfo.genre)
                            SonaraLogger.ai("Last.fm trained AI: '${result.trackInfo.genre}' for $artist")

                            // Apply better EQ if Last.fm has higher confidence
                            if (result.trackInfo.confidence > localConf && !app.eqState.value.isManualPreset) {
                                val suggestion = AiEqSuggestionEngine.suggest(result.trackInfo)
                                app.applyEq(bands = suggestion.bands, presetName = "AI: ${result.trackInfo.genre.replaceFirstChar { it.uppercase() }}",
                                    manual = false, bassBoost = suggestion.bassBoost, virtualizer = suggestion.virtualizer)
                            }
                        }
                    }
                } catch (e: Exception) { SonaraLogger.e("NLS", "Process error: ${e.message}") }
            }
        }
    }

    companion object {
        var instance: SonaraNotificationListener? = null; private set
        private val _nowPlaying = MutableStateFlow(ListenerNowPlaying())
        val nowPlaying: StateFlow<ListenerNowPlaying> = _nowPlaying.asStateFlow()
        private val _albumArt = MutableStateFlow<Bitmap?>(null)
        val albumArt: StateFlow<Bitmap?> = _albumArt.asStateFlow()
        fun isEnabled(ctx: Context): Boolean {
            val cn = ComponentName(ctx, SonaraNotificationListener::class.java)
            val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            return flat?.contains(cn.flattenToString()) == true
        }
    }
}
