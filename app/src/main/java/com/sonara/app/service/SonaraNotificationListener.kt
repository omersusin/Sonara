package com.sonara.app.service

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ListenerNowPlaying(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val packageName: String = "",
    val isPlaying: Boolean = false,
    val duration: Long = 0,
    val audioSessionId: Int = 0
)

class SonaraNotificationListener : NotificationListenerService() {

    private var activeController: MediaController? = null
    private var sessionManager: MediaSessionManager? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata?.let { extractMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            _nowPlaying.value = _nowPlaying.value.copy(
                isPlaying = state?.state == PlaybackState.STATE_PLAYING
            )
        }

        override fun onSessionDestroyed() {
            _nowPlaying.value = ListenerNowPlaying()
            _albumArt.value = null
            activeController = null
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        if (controllers != null) pickBestController(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshSessions()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (_: Exception) {}
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        instance = null
    }

    private fun refreshSessions() {
        try {
            val cn = ComponentName(this, SonaraNotificationListener::class.java)
            sessionManager?.addOnActiveSessionsChangedListener(sessionListener, cn)
            val sessions = sessionManager?.getActiveSessions(cn) ?: return
            pickBestController(sessions)
        } catch (_: SecurityException) {}
    }

    private fun pickBestController(controllers: List<MediaController>) {
        val playing = controllers.firstOrNull { c ->
            c.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        val target = playing ?: controllers.firstOrNull() ?: return
        attachTo(target)
    }

    private fun attachTo(controller: MediaController) {
        if (activeController?.sessionToken == controller.sessionToken) {
            controller.metadata?.let { extractMetadata(it) }
            return
        }

        activeController?.unregisterCallback(controllerCallback)
        activeController = controller
        controller.registerCallback(controllerCallback)

        controller.metadata?.let { extractMetadata(it) }

        _nowPlaying.value = _nowPlaying.value.copy(
            isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = controller.packageName ?: ""
        )
    }

    private fun extractMetadata(metadata: MediaMetadata) {
        val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        _albumArt.value = art

        _nowPlaying.value = _nowPlaying.value.copy(
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
            duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        )
    }

    companion object {
        var instance: SonaraNotificationListener? = null
            private set

        private val _nowPlaying = MutableStateFlow(ListenerNowPlaying())
        val nowPlaying: StateFlow<ListenerNowPlaying> = _nowPlaying.asStateFlow()

        private val _albumArt = MutableStateFlow<Bitmap?>(null)
        val albumArt: StateFlow<Bitmap?> = _albumArt.asStateFlow()

        fun isEnabled(context: Context): Boolean {
            val cn = ComponentName(context, SonaraNotificationListener::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat?.contains(cn.flattenToString()) == true
        }
    }
}
