package com.sonara.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        notificationManager = getSystemService(NotificationManager::class.java)

        scope.launch {
            combine(
                SonaraNotificationListener.nowPlaying,
                SonaraNotificationListener.albumArt
            ) { np, art -> np to art }.collect { (np, art) ->
                val notification = buildNotification(np.title, np.artist, np.isPlaying, art)
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification("Sonara", "Sound engine starting...", false, null)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(title: String, artist: String, isPlaying: Boolean, art: Bitmap?): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, SonaraService::class.java).apply { action = ACTION_STOP }
        val pendingStop = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val displayTitle = title.ifBlank { "Sonara" }
        val displayText = if (artist.isNotBlank()) "$artist${if (isPlaying) " · Playing" else ""}"
                          else if (isPlaying) "Playing" else "Sound engine active"

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setContentIntent(pendingOpen)
            .addAction(Notification.Action.Builder(null, "Stop", pendingStop).build())
            .setOngoing(true)
            .setShowWhen(false)

        if (art != null) {
            builder.setLargeIcon(art)
        }

        return builder.build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Sonara Engine", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Sound processing service"
            setShowBadge(false)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
