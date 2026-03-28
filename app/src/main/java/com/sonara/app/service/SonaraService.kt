package com.sonara.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.IBinder
import com.sonara.app.MainActivity
import com.sonara.app.R
import com.sonara.app.SonaraApp
import com.sonara.app.ai.SonaraAi
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SonaraService : Service() {
    companion object {
        const val CHANNEL_ID = "sonara_engine"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.sonara.app.STOP"
        const val ACTION_LOVE = "com.sonara.app.LOVE"
        fun start(ctx: Context) { ctx.startForegroundService(Intent(ctx, SonaraService::class.java)) }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val aiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sonaraAi: SonaraAi? = null
    private var isLoved = false
    private var hasSessionKey = false
    private var lastTrackKey = ""
    private var dismissJob: Job? = null

    override fun onCreate() {
        super.onCreate(); createChannel()

        // AI initialization
        val db = SonaraDatabase.get(applicationContext)
        sonaraAi = SonaraAi.create(applicationContext, db.trainingExampleDao())
        aiScope.launch(Dispatchers.IO) {
            sonaraAi?.initialize()
        }

        scope.launch {
            combine(SonaraNotificationListener.nowPlaying, SonaraNotificationListener.albumArt) { np, art -> np to art }
                .collect { (np, art) ->
                    val key = "${np.title}::${np.artist}"
                    if (key != lastTrackKey) {
                        isLoved = false; lastTrackKey = key
                        // Notify AI of track change
                        if (np.title.isNotBlank()) {
                            sonaraAi?.onTrackChanged(
                                title = np.title,
                                artist = np.artist,
                                albumArt = null,
                                audioSessionId = null
                            )
                        }
                    }
                    val n = buildNotification(np.title.ifBlank { "Sonara" }, np.artist, np.isPlaying, art)
                    getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, n)

                    if (!np.isPlaying && np.title.isNotBlank()) {
                        handlePausedState()
                    } else {
                        dismissJob?.cancel()
                        dismissJob = null
                    }
                }
        }
    }

    private fun handlePausedState() {
        dismissJob?.cancel()
        dismissJob = scope.launch {
            try {
                val app = application as SonaraApp
                val keepNotification = app.preferences.keepNotificationPausedFlow.first()
                if (!keepNotification) {
                    delay(5000)
                    val stillPaused = !SonaraNotificationListener.nowPlaying.value.isPlaying
                    if (stillPaused) {
                        SonaraLogger.i("Service", "Removing notification (keepNotification=false, paused)")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                return START_NOT_STICKY
            }
            ACTION_LOVE -> {
                val np = SonaraNotificationListener.nowPlaying.value
                if (!hasSessionKey) {
                    SonaraLogger.w("Love", "No session key, ignoring")
                    return START_STICKY
                }
                if (np.title.isNotBlank()) {
                    isLoved = !isLoved
                    updateNotification()
                    SonaraLogger.i("Love", "Toggle love=${isLoved} for: ${np.title} - ${np.artist}")
                    scope.launch(Dispatchers.IO) {
                        try {
                            val app = application as SonaraApp
                            val ok = app.loveTrack(np.title, np.artist, isLoved)
                            SonaraLogger.i("Love", "API result=$ok loved=$isLoved")
                            if (!ok) {
                                SonaraLogger.w("Love", "API failed, reverting")
                                isLoved = !isLoved
                                scope.launch(Dispatchers.Main) { updateNotification() }
                            }
                        } catch (e: Exception) {
                            SonaraLogger.e("Love", "Exception: ${e.message}")
                            isLoved = !isLoved
                            scope.launch(Dispatchers.Main) { updateNotification() }
                        }
                    }
                }
                return START_STICKY
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification("Sonara", "Sound engine active", false, null))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        dismissJob?.cancel()
        sonaraAi?.release()
        aiScope.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun updateNotification() {
        val np = SonaraNotificationListener.nowPlaying.value
        val art = SonaraNotificationListener.albumArt.value
        val n = buildNotification(np.title.ifBlank { "Sonara" }, np.artist, np.isPlaying, art)
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, n)
    }

    private fun buildNotification(title: String, artist: String, isPlaying: Boolean, art: Bitmap?): Notification {
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, SonaraService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE)
        val love = PendingIntent.getService(this, 2,
            Intent(this, SonaraService::class.java).apply { action = ACTION_LOVE },
            PendingIntent.FLAG_IMMUTABLE)

        val sub = when {
            artist.isNotBlank() && isPlaying -> "$artist · Playing"
            artist.isNotBlank() -> artist
            isPlaying -> "Playing"
            else -> "Sound engine active"
        }

        val heartIcon = Icon.createWithResource(this,
            if (isLoved) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        val heartAction = if (hasSessionKey) Notification.Action.Builder(heartIcon, if (isLoved) "Loved" else "Love", love).build() else null
        val stopAction = Notification.Action.Builder(null, "Stop", stop).build()

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(sub)
            .setContentIntent(open)
            .also { if (heartAction != null) it.addAction(heartAction) }
            .addAction(stopAction)
            .setOngoing(isPlaying)
            .setShowWhen(false)

        if (art != null && !art.isRecycled) {
            try { builder.setLargeIcon(art) } catch (_: Exception) {}
        }

        return builder.build()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Sonara Engine", NotificationManager.IMPORTANCE_LOW)
        ch.description = "Sound processing"; ch.setShowBadge(false); ch.setSound(null, null)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
}
