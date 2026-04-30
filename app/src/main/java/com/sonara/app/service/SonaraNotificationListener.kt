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
import com.sonara.app.intelligence.provider.InsightRequest
import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.lyrics.LyricsInsightEngine
import com.sonara.app.intelligence.lyrics.LyricsResolver
import com.sonara.app.intelligence.pipeline.PredictionSourceMapper
import com.sonara.app.intelligence.pipeline.SonaraTrackInfo
import com.sonara.app.intelligence.pipeline.TitleNormalizer
import com.sonara.app.intelligence.lastfm.PendingScrobble
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListenerNowPlaying(
    val title: String = "", val artist: String = "", val album: String = "",
    val packageName: String = "", val isPlaying: Boolean = false, val duration: Long = 0,
    val position: Long = 0, val positionTimestamp: Long = 0
)

class SonaraNotificationListener : NotificationListenerService() {
    private var sessionManager: MediaSessionManager? = null
    private var sessionListenerRegistered = false
    internal var activeController: MediaController? = null
    private var lastTrackKey = ""
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cached preference: avoids runBlocking in the hot pickBest() path
    @Volatile private var cachedAllowedApps: Set<String> = emptySet()

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
            _nowPlaying.update { cur ->
                cur.copy(
                    isPlaying = playing,
                    position = s?.position ?: cur.position,
                    positionTimestamp = System.currentTimeMillis()
                )
            }

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
        // Keep cachedAllowedApps in sync without blocking
        scope.launch {
            try {
                (application as SonaraApp).preferences.allowedScrobbleAppsFlow.collect {
                    cachedAllowedApps = it
                }
            } catch (_: Exception) {}
        }
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
        // Scrobble app filter: skip controllers from non-allowed apps
        val allowedApps = cachedAllowedApps

        val filtered = if (allowedApps.isEmpty()) controllers
        else controllers.filter { c -> c.packageName in allowedApps }

        // When specific apps are selected and none are playing, clear now playing state
        if (allowedApps.isNotEmpty() && filtered.isEmpty()) {
            if (_nowPlaying.value.title.isNotBlank()) {
                _nowPlaying.value = ListenerNowPlaying()
                _albumArt.value = null
                activeController?.let { try { it.unregisterCallback(controllerCb) } catch (_: Exception) {} }
                activeController = null
            }
            return
        }

        // Prefer playing controller, but keep current if just paused
        val playing = filtered.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val current = if (activeController != null) filtered.firstOrNull { it.sessionToken == activeController?.sessionToken } else null
        val target = playing ?: current ?: filtered.firstOrNull() ?: return

        if (activeController?.sessionToken == target.sessionToken) {
            target.metadata?.let { processMetadata(it) }
            return
        }
        activeController?.let { try { it.unregisterCallback(controllerCb) } catch (_: Exception) {} }
        activeController = target
        target.registerCallback(controllerCb)
        target.metadata?.let { processMetadata(it) }
        _nowPlaying.update { it.copy(
            isPlaying = target.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = target.packageName ?: ""
        ) }
    }

    private fun processMetadata(metadata: MediaMetadata) {
        _albumArt.value = try { ArtworkResolver.extract(metadata, contentResolver) } catch (_: Exception) { null }

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        _nowPlaying.update { it.copy(title = title, artist = artist, album = album, duration = duration) }

        val key = "$title::$artist"
        if (title.isBlank() || key == lastTrackKey) return
        lastTrackKey = key

        playStartTime = System.currentTimeMillis()
        hasScrobbled = false
        nowPlayingSent = false
        scrobbleJob?.cancel()

        scope.launch {
            try {
                val app = application as SonaraApp
                val isManualPreset = app.eqState.value.isManualPreset
                val aiOn = app.preferences.aiEnabledFlow.first()
                val srcLastFm = app.preferences.sourceLastFmEnabledFlow.first()
                val srcLocalAi = app.preferences.sourceLocalAiEnabledFlow.first()
                val srcLyrics = app.preferences.sourceLyricsEnabledFlow.first()

                // ═══ Try preloaded prediction first ═══
                val preloaded = app.nextTrackPreloader.consumeIfMatch(title, artist)
                val normTitle = TitleNormalizer.normalizeTitle(title)
                val normArtist = TitleNormalizer.normalizeArtist(artist)

                val prediction = if (preloaded != null) {
                    SonaraLogger.ai("Using PRELOADED prediction for $title")
                    preloaded
                } else {
                    val track = SonaraTrackInfo(normTitle, normArtist, album, duration, _nowPlaying.value.packageName)
                    app.inferencePipeline.analyze(track, useLastFm = srcLastFm, useLocalAi = srcLocalAi)
                }

                // ═══ Lyrics fetch (only if lyrics source enabled) ═══
                var lyricsModifier: FloatArray? = null
                var hasLyrics = false
                if (srcLyrics) try {
                    val lyricsResult = LyricsResolver.resolve(normTitle, normArtist, duration)
                    if (lyricsResult != null) {
                        val insight = LyricsInsightEngine.analyze(lyricsResult.plainLyrics)
                        if (insight.confidence > 0.15f) {
                            lyricsModifier = insight.eqModifier
                            hasLyrics = true
                        }
                        _lyricsInsight.value = insight
                        SonaraLogger.ai("Lyrics: tone=${insight.tone} conf=${insight.confidence}")
                    }
                } catch (e: Exception) {
                    SonaraLogger.w("NLS", "Lyrics: ${e.message}")
                }

                // ═══ Source label — HONEST with nuance ═══
                val isUnknown = prediction.genre == com.sonara.app.intelligence.pipeline.Genre.UNKNOWN || prediction.confidence < 0.1f
                val sourceLabel = when {
                    isUnknown -> "No Match"
                    prediction.confidence < 0.5f -> {
                        val sd = PredictionSourceMapper.map(prediction, hasLyrics)
                        "Weak (${sd.primary})"
                    }
                    else -> {
                        val sd = PredictionSourceMapper.map(prediction, hasLyrics)
                        if (sd.detail.isNotBlank()) "${sd.primary} (${sd.detail})" else sd.primary
                    }
                }

                _currentGenre.value = prediction.genre.displayName
                _currentMood.value = prediction.mood.displayName
                _currentEnergy.value = prediction.energy
                _currentConfidence.value = prediction.confidence
                _currentSource.value = sourceLabel

                // ═══ Madde 15 FIX: Auto Preset — genre'ye göre preset seç ═══
                val autoPresetOn = app.preferences.autoPresetFlow.first()
                if (autoPresetOn && !isManualPreset && aiOn && prediction.genre != com.sonara.app.intelligence.pipeline.Genre.UNKNOWN) {
                    val presets = app.presetRepository.allPresets().first()
                    val matchingPreset = presets.firstOrNull {
                        it.genre.equals(prediction.genre.name, ignoreCase = true) && !it.isBuiltIn
                    }
                    if (matchingPreset != null) {
                        SonaraLogger.ai("Auto Preset: Using '${matchingPreset.name}' for ${prediction.genre}")
                        app.applyEq(
                            bands = matchingPreset.bandsArray(),
                            presetName = matchingPreset.name,
                            manual = false,
                            bassBoost = matchingPreset.bassBoost,
                            virtualizer = matchingPreset.virtualizer,
                            loudness = matchingPreset.loudness,
                            reverb = matchingPreset.reverb,
                            preamp = matchingPreset.preamp
                        )
                        // Auto Preset uygulandı, AI EQ uygulanmasın
                    } else {
                        // Auto Preset eşleşmedi — normal AI akışı
                        applyAiEq(app, prediction, isManualPreset, aiOn, lyricsModifier)
                    }
                } else {
                    applyAiEq(app, prediction, isManualPreset, aiOn, lyricsModifier)
                }

                // Train adaptive classifier + personalization
                // Count ALL analyzed tracks
                app.preferences.incrementSongLearned(prediction.source.name, prediction.genre.name)
                app.personalization.recordListen(prediction.genre.name, app.currentRoute.value.name)

                // ═══ Preload NEXT track ═══
                app.nextTrackPreloader.tryPreload(activeController)

                // ═══ Scrobbling: updateNowPlaying ═══
                sendNowPlaying(app, title, artist)

                // ═══ AI Insight via provider manager (Gemini/OpenRouter/Groq with fallback) ═══
                scope.launch {
                    try {
                        val geminiEnabled = app.preferences.geminiEnabledFlow.first()
                        if (geminiEnabled) {
                            val request = InsightRequest(
                                title = normTitle, artist = normArtist,
                                genre = prediction.genre.displayName,
                                subGenre = prediction.subGenre,
                                tags = prediction.tags,
                                lyricalTone = _lyricsInsight.value?.tone,
                                energy = prediction.energy,
                                confidence = prediction.confidence,
                                currentEqBands = app.eqState.value.bands
                            )
                            val result = app.insightManager.getInsight(request)
                            if (result.success) {
                                app.updateGeminiInsight(com.sonara.app.intelligence.gemini.GeminiInsightEngine.GeminiInsight(
                                    summary = result.summary, whyThisEq = result.whyThisEq,
                                    listeningFocus = result.listeningFocus, lyricalTone = result.lyricalTone,
                                    confidenceNote = result.confidenceNote, success = true
                                ))
                                SonaraLogger.ai("${result.provider} insight: ${result.summary.take(60)}...")
                            }
                        }
                    } catch (e: Exception) {
                        SonaraLogger.w("NLS", "Insight: ${e.message}")
                    }
                }

            } catch (e: Exception) { SonaraLogger.e("NLS", "Process: ${e.message}") }
        }

        if (_nowPlaying.value.isPlaying) scheduleScrobble()
    }

    /** AI EQ uygulaması — ortak fonksiyon */
    private fun applyAiEq(app: SonaraApp, prediction: com.sonara.app.intelligence.pipeline.SonaraPrediction,
                          isManualPreset: Boolean, aiOn: Boolean, lyricsModifier: FloatArray?) {
        if (prediction.genre != com.sonara.app.intelligence.pipeline.Genre.UNKNOWN && prediction.confidence > 0.05f) {
            if (!isManualPreset && aiOn) app.applyFromPrediction(prediction, lyricsModifier)
        } else if (!isManualPreset && aiOn) {
            app.applyEq(
                bands = FloatArray(10),
                presetName = "Flat (Unknown)",
                manual = false,
                bassBoost = 0, virtualizer = 0, loudness = 0
            )
            SonaraLogger.ai("UNKNOWN track → EQ reset to flat")
        }
    }

    private fun sendNowPlaying(app: SonaraApp, title: String, artist: String) {
        if (nowPlayingSent) return
        scope.launch {
            try {
                val scrobblingEnabled = app.preferences.scrobblingEnabledFlow.first()
                if (!scrobblingEnabled) return@launch
                val apiKey = app.secureSecrets.getLastFmApiKey()
                val secret = app.secureSecrets.getLastFmSharedSecret()
                val sessionKey = app.secureSecrets.getLastFmSessionKey()
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
            val apiKey = app.secureSecrets.getLastFmApiKey()
            val secret = app.secureSecrets.getLastFmSharedSecret()
            val sessionKey = app.secureSecrets.getLastFmSessionKey()
            if (apiKey.isBlank() || sessionKey.isBlank()) return

            val scrobbler = com.sonara.app.intelligence.lastfm.ScrobblingManager()
            val ok = scrobbler.scrobble(np.title, np.artist, np.album, playStartTime, apiKey, secret, sessionKey)
            if (ok) {
                hasScrobbled = true
                SonaraLogger.i("Scrobble", "Scrobbled: ${np.title}")
            } else {
                try {
                    app.database.pendingScrobbleDao().insert(
                        PendingScrobble(track = np.title, artist = np.artist, album = np.album, timestamp = playStartTime)
                    )
                    SonaraLogger.i("Scrobble", "Queued for retry: ${np.title}")
                } catch (_: Exception) {}
            }
        } catch (e: Exception) { SonaraLogger.w("Scrobble", "Scrobble failed: ${e.message}") }
    }

    companion object {
        var instance: SonaraNotificationListener? = null; private set
        private val _nowPlaying = MutableStateFlow(ListenerNowPlaying())
        val nowPlaying: StateFlow<ListenerNowPlaying> = _nowPlaying.asStateFlow()
        private val _albumArt = MutableStateFlow<Bitmap?>(null)
        val albumArt: StateFlow<Bitmap?> = _albumArt.asStateFlow()
        private val _currentGenre = MutableStateFlow("")
        val currentGenre: StateFlow<String> = _currentGenre.asStateFlow()
        private val _currentMood = MutableStateFlow("")
        val currentMood: StateFlow<String> = _currentMood.asStateFlow()
        private val _currentEnergy = MutableStateFlow(0.5f)
        val currentEnergy: StateFlow<Float> = _currentEnergy.asStateFlow()
        private val _currentConfidence = MutableStateFlow(0f)
        val currentConfidence: StateFlow<Float> = _currentConfidence.asStateFlow()
        private val _currentSource = MutableStateFlow("None")
        val currentSource: StateFlow<String> = _currentSource.asStateFlow()

        // Lyrics insight state
        private val _lyricsInsight = MutableStateFlow<LyricsInsightEngine.LyricsInsight?>(null)
        val lyricsInsight: StateFlow<LyricsInsightEngine.LyricsInsight?> = _lyricsInsight.asStateFlow()

        fun isEnabled(ctx: Context): Boolean {
            val cn = ComponentName(ctx, SonaraNotificationListener::class.java)
            val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            return flat?.contains(cn.flattenToString()) == true
        }

        /** Mini player transport controls */
        fun sendPlayPause() {
            try {
                instance?.activeController?.transportControls?.let { tc ->
                    if (_nowPlaying.value.isPlaying) tc.pause() else tc.play()
                }
            } catch (_: Exception) {}
        }

        fun sendNext() {
            try { instance?.activeController?.transportControls?.skipToNext() } catch (_: Exception) {}
        }

        fun sendPrevious() {
            try { instance?.activeController?.transportControls?.skipToPrevious() } catch (_: Exception) {}
        }

        fun seekTo(positionMs: Long) {
            try { instance?.activeController?.transportControls?.seekTo(positionMs) } catch (_: Exception) {}
        }
    }
}
