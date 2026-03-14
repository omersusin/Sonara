package com.sonara.app.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaSessionMonitor(private val context: Context) {
    private val _nowPlaying = MutableStateFlow(NowPlayingInfo())
    val nowPlaying: StateFlow<NowPlayingInfo> = _nowPlaying.asStateFlow()

    private var activeController: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata?.let { updateFromMetadata(it) }
        }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            _nowPlaying.value = _nowPlaying.value.copy(
                isPlaying = state?.state == PlaybackState.STATE_PLAYING
            )
        }
    }

    fun start() {
        try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val component = ComponentName(context, NotificationListenerService::class.java)
            manager.addOnActiveSessionsChangedListener({ controllers ->
                controllers?.firstOrNull()?.let { attachTo(it) }
            }, component)
            manager.getActiveSessions(component).firstOrNull()?.let { attachTo(it) }
        } catch (e: SecurityException) {
            // Notification listener permission not granted
        }
    }

    fun stop() {
        activeController?.unregisterCallback(callback)
        activeController = null
    }

    private fun attachTo(controller: MediaController) {
        activeController?.unregisterCallback(callback)
        activeController = controller
        controller.registerCallback(callback)
        controller.metadata?.let { updateFromMetadata(it) }
        _nowPlaying.value = _nowPlaying.value.copy(
            isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = controller.packageName ?: ""
        )
    }

    private fun updateFromMetadata(metadata: MediaMetadata) {
        _nowPlaying.value = _nowPlaying.value.copy(
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
            duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        )
    }
}
