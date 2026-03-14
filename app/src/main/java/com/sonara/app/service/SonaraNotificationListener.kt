package com.sonara.app.service

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
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

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata?.let { updateMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            val playing = state?.state == PlaybackState.STATE_PLAYING
            _nowPlaying.value = _nowPlaying.value.copy(isPlaying = playing)
        }

        override fun onSessionDestroyed() {
            _nowPlaying.value = ListenerNowPlaying()
            activeController = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Trigger session check
        checkActiveSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onListenerConnected() {
        super.onListenerConnected()
        checkActiveSessions()
    }

    private fun checkActiveSessions() {
        try {
            val sessions = getActiveSessions(null)
            val playing = sessions?.firstOrNull { controller ->
                controller.playbackState?.state == PlaybackState.STATE_PLAYING
            }
            val target = playing ?: sessions?.firstOrNull()
            target?.let { attachTo(it) }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun attachTo(controller: MediaController) {
        if (activeController?.sessionToken == controller.sessionToken) return

        activeController?.unregisterCallback(controllerCallback)
        activeController = controller
        controller.registerCallback(controllerCallback)

        controller.metadata?.let { updateMetadata(it) }

        val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        _nowPlaying.value = _nowPlaying.value.copy(
            isPlaying = playing,
            packageName = controller.packageName ?: "",
            audioSessionId = controller.sessionToken.hashCode()
        )
    }

    private fun updateMetadata(metadata: MediaMetadata) {
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

        fun isActive(): Boolean = instance != null
    }
}
