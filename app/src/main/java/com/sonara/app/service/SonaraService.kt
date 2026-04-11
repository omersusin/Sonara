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
import android.util.Log
import com.sonara.app.MainActivity
import com.sonara.app.R
import com.sonara.app.SonaraApp
import com.sonara.app.ai.AiStatus
import com.sonara.app.ai.SonaraAi
import com.sonara.app.ai.bridge.AudioSessionTracker
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
        const val ACTION_REQUEST = "com.sonara.app.REQUEST"
        const val EXTRA_REQUEST_TEXT = "request_text"
        fun start(ctx: Context) { ctx.startForegroundService(Intent(ctx, SonaraService::class.java)) }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val aiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sonaraAi: SonaraAi? = null
    private var aiBridgeJob: Job? = null
    private var isLoved = false
    private var hasSessionKey = false
    private var lastTrackKey = ""
    private var dismissJob: Job? = null

    override fun onCreate() {
        super.onCreate(); createChannel()

        // AI init
        val db = SonaraDatabase.get(applicationContext)
        sonaraAi = SonaraAi.create(applicationContext, db.trainingExampleDao())
        aiScope.launch(Dispatchers.IO) { sonaraAi?.initialize() }

        // AI → EQ bridge: AI sonuç üretince gerçek EQ engine'e uygula
        // AI EQ Bridge disabled — verifying AI logs before enabling
        Log.d("SonaraService", "AI EQ Bridge DISABLED to prevent pipeline conflict")

        // Session tracker listener — yeni session gelince AI'a bildir
        AudioSessionTracker.addListener { sessionId ->
            Log.d("SonaraService", "Session from tracker: $sessionId")
            sonaraAi?.onSessionChanged(sessionId)
        }

        scope.launch {
            combine(SonaraNotificationListener.nowPlaying, SonaraNotificationListener.albumArt) { np, art -> np to art }
                .collect { (np, art) ->
                    val key = "${np.title}::${np.artist}"
                    if (key != lastTrackKey) {
                        isLoved = false; lastTrackKey = key
                        if (np.title.isNotBlank()) {
                            // Track change → AI'a bildir (session ID tracker'dan)
                            sonaraAi?.onTrackChanged(
                                title = np.title,
                                artist = np.artist,
                                albumArt = null,
                                audioSessionId = AudioSessionTracker.get().takeIf { it > 0 }
                            )
                        }
                    }
                    val n = buildNotification(np.title.ifBlank { "Sonara" }, np.artist, np.isPlaying, art)
                    getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, n)

                    if (!np.isPlaying) {
                        if (np.title.isNotBlank()) handlePausedState()
                        else {
                            // Nothing playing at all — check if we should dismiss
                            val keepNotif = try { (application as SonaraApp).preferences.keepNotificationPausedFlow.first() } catch (_: Exception) { true }
                            if (!keepNotif) {
                                scope.launch { delay(2000); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
                            }
                        }
                    } else { dismissJob?.cancel(); dismissJob = null }
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
                    delay(3000)
                    val np = SonaraNotificationListener.nowPlaying.value
                    if (!np.isPlaying) {
                        SonaraLogger.i("Service", "Removing notification (keepNotification=false, paused)")
                        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
            ACTION_REQUEST -> {
                val remoteResults = android.app.RemoteInput.getResultsFromIntent(intent)
                val text = remoteResults?.getCharSequence(EXTRA_REQUEST_TEXT)?.toString()
                    ?: intent?.getStringExtra(EXTRA_REQUEST_TEXT)
                if (!text.isNullOrBlank()) {
                    SonaraLogger.ai("Notification request: $text")
                    scope.launch(Dispatchers.IO) {
                        try {
                            val app = application as SonaraApp
                            val np = SonaraNotificationListener.nowPlaying.value
                            val eqBands = app.eqState.value.bands
                            val result = app.insightManager.getInsight(
                                com.sonara.app.intelligence.provider.InsightRequest(
                                    title = np.title,
                                    artist = np.artist,
                                    genre = SonaraNotificationListener._currentGenre.value,
                                    subGenre = null,
                                    tags = listOf(text),
                                    lyricalTone = null,
                                    energy = SonaraNotificationListener._currentEnergy.value,
                                    confidence = SonaraNotificationListener._currentConfidence.value,
                                    currentEqBands = eqBands
                                )
                            )
                            if (result.success && result.eqAdjustment != null) {
                                app.applyEq(
                                    bands = result.eqAdjustment!!,
                                    presetName = "AI Request",
                                    manual = false,
                                    bassBoost = result.bassBoost,
                                    virtualizer = result.virtualizer,
                                    loudness = result.loudness,
                                    preamp = result.preamp
                                )
                                SonaraLogger.ai("Request applied: $text")
                            }
                        } catch (e: Exception) {
                            SonaraLogger.e("Service", "Request failed: ${e.message}")
                        }
                    }
                }
                return START_STICKY
            }
            ACTION_LOVE -> {
                val np = SonaraNotificationListener.nowPlaying.value
                if (!hasSessionKey) { SonaraLogger.w("Love", "No session key"); return START_STICKY }
                if (np.title.isNotBlank()) {
                    isLoved = !isLoved; updateNotification()
                    scope.launch(Dispatchers.IO) {
                        try {
                            val ok = (application as SonaraApp).loveTrack(np.title, np.artist, isLoved)
                            if (!ok) { isLoved = !isLoved; scope.launch(Dispatchers.Main) { updateNotification() } }
                        } catch (_: Exception) { isLoved = !isLoved; scope.launch(Dispatchers.Main) { updateNotification() } }
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
        dismissJob?.cancel(); aiBridgeJob?.cancel()
        sonaraAi?.release(); aiScope.cancel(); scope.cancel()
        super.onDestroy()
    }

    private fun updateNotification() {
        val np = SonaraNotificationListener.nowPlaying.value
        val art = SonaraNotificationListener.albumArt.value
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID,
            buildNotification(np.title.ifBlank { "Sonara" }, np.artist, np.isPlaying, art))
    }

    private fun buildNotification(title: String, artist: String, isPlaying: Boolean, art: Bitmap?): Notification {
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }, PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, SonaraService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE)
        val love = PendingIntent.getService(this, 2,
            Intent(this, SonaraService::class.java).apply { action = ACTION_LOVE }, PendingIntent.FLAG_IMMUTABLE)
        val sub = when {
            artist.isNotBlank() && isPlaying -> "$artist · Playing"
            artist.isNotBlank() -> artist; isPlaying -> "Playing"; else -> "Sound engine active"
        }
        // Request button with RemoteInput for inline text
        val requestIntent = PendingIntent.getService(this, 3,
            Intent(this, SonaraService::class.java).apply { action = ACTION_REQUEST },
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val remoteInput = android.app.RemoteInput.Builder(EXTRA_REQUEST_TEXT)
            .setLabel("Ask AI (e.g. more bass)")
            .build()
        val requestAction = Notification.Action.Builder(null, "🧠 Ask AI", requestIntent)
            .addRemoteInput(remoteInput)
            .build()

        val heartIcon = Icon.createWithResource(this, if (isLoved) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        val heartAction = if (hasSessionKey) Notification.Action.Builder(heartIcon, if (isLoved) "Loved" else "Love", love).build() else null
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title).setContentText(sub)
            .setContentIntent(open).also { if (heartAction != null) it.addAction(heartAction) }
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .setOngoing(isPlaying).setShowWhen(false)
        if (art != null && !art.isRecycled) { try { builder.setLargeIcon(art) } catch (_: Exception) {} }
        return builder.build()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Sonara Engine", NotificationManager.IMPORTANCE_LOW)
        ch.description = "Sound processing"; ch.setShowBadge(false); ch.setSound(null, null)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
}
