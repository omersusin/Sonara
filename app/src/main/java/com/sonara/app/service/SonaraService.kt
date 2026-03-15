package com.sonara.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import com.sonara.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SonaraService : Service() {
    companion object {
        const val CHANNEL_ID = "sonara_engine"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.sonara.app.STOP"
        fun start(ctx: Context) { ctx.startForegroundService(Intent(ctx, SonaraService::class.java)) }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate(); createChannel()
        scope.launch {
            combine(SonaraNotificationListener.nowPlaying, SonaraNotificationListener.albumArt) { np, art -> np to art }
                .collect { (np, art) ->
                    val n = buildNotification(np.title.ifBlank { "Sonara" }, np.artist, np.isPlaying, art)
                    getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, n)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        startForeground(NOTIFICATION_ID, buildNotification("Sonara", "Sound engine active", false, null))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private fun buildNotification(title: String, artist: String, isPlaying: Boolean, art: Bitmap?): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }, PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, SonaraService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE)
        val sub = when { artist.isNotBlank() && isPlaying -> "$artist · Playing"; artist.isNotBlank() -> artist; isPlaying -> "Playing"; else -> "Sound engine active" }

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title).setContentText(sub)
            .setContentIntent(open).addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .setOngoing(true).setShowWhen(false)

        // SAFE: Only use bitmap if not recycled
        if (art != null && !art.isRecycled) { try { builder.setLargeIcon(art) } catch (_: Exception) {} }

        return builder.build()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Sonara Engine", NotificationManager.IMPORTANCE_LOW)
        ch.description = "Sound processing"; ch.setShowBadge(false); ch.setSound(null, null)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
}
