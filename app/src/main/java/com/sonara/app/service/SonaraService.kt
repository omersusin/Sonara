package com.sonara.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.IBinder
import com.sonara.app.MainActivity
import com.sonara.app.R
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

        fun start(context: Context) {
            val intent = Intent(context, SonaraService::class.java)
            context.startForegroundService(intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createChannel()

        scope.launch {
            combine(
                SonaraNotificationListener.nowPlaying,
                SonaraNotificationListener.albumArt
            ) { np, art -> np to art }.collect { (np, art) ->
                val n = buildNotification(np.title.ifBlank { "Sonara" }, np.artist, np.isPlaying, art)
                getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, n)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification("Sonara", "Sound engine active", false, null))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private fun buildNotification(title: String, artist: String, isPlaying: Boolean, art: android.graphics.Bitmap?): Notification {
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, SonaraService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE)

        val sub = when {
            artist.isNotBlank() && isPlaying -> "$artist · Playing"
            artist.isNotBlank() -> artist
            isPlaying -> "Playing"
            else -> "Sound engine active"
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(sub)
            .setSubText("Sonara EQ")
            .setContentIntent(open)
            .setColor(getColor(R.color.sonara_primary))
            .addAction(Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_stop), "Stop", stop
            ).build())
            .setStyle(Notification.MediaStyle().setShowActionsInCompactView(0))
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setShowWhen(false)
            .apply { if (art != null) setLargeIcon(art) }
            .build()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Sonara Sound Engine", NotificationManager.IMPORTANCE_LOW)
        ch.description = "Shows current track and EQ status"
        ch.setShowBadge(false)
        ch.setSound(null, null)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
}
