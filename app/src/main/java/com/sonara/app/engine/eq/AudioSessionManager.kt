package com.sonara.app.engine.eq

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioSessionManager(private val context: Context) {
    companion object { private const val TAG = "SessionMgr" }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var dpEq: DynamicsProcessingEq? = null
    private val sessionEqualizers = mutableMapOf<Int, SafeEqualizer>()
    private var fallbackEqualizer: SafeEqualizer? = null
    private var currentBandsDb = FloatArray(10) { 0f }
    private var eqEnabled = true

    private val _activeStrategy = MutableStateFlow("none")
    val activeStrategy: StateFlow<String> = _activeStrategy
    private val _activeSessions = MutableStateFlow<Set<Int>>(emptySet())
    val activeSessions: StateFlow<Set<Int>> = _activeSessions

    private val sessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val sid = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
            val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: ""
            when (intent.action) {
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> { SonaraLogger.eq("Session OPEN: $sid ($pkg)"); if (sid > 0) attachToSession(sid) }
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> { SonaraLogger.eq("Session CLOSE: $sid"); detachFromSession(sid) }
            }
        }
    }

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            handleActivePlayback(configs)
        }
    }

    fun start() {
        SonaraLogger.eq("Starting AudioSessionManager")
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.registerReceiver(sessionReceiver, filter, Context.RECEIVER_EXPORTED)
        else context.registerReceiver(sessionReceiver, filter)

        audioManager.registerAudioPlaybackCallback(playbackCallback, handler)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) tryDynamicsProcessing()
        handleActivePlayback(audioManager.activePlaybackConfigurations)
        if (dpEq == null) tryFallbackEqualizer()

        SonaraLogger.eq("Started. Strategy: ${_activeStrategy.value}")
    }

    fun stop() {
        try { context.unregisterReceiver(sessionReceiver) } catch (_: Exception) {}
        audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        dpEq?.release(); dpEq = null
        sessionEqualizers.values.forEach { it.release() }; sessionEqualizers.clear()
        fallbackEqualizer?.release(); fallbackEqualizer = null
        _activeStrategy.value = "none"
    }

    fun applyBands(bandsDb: FloatArray) {
        currentBandsDb = bandsDb.copyOf()
        dpEq?.setAllBands(bandsDb)
        val bandsMb = IntArray(bandsDb.size) { (bandsDb[it] * 100).toInt() }
        sessionEqualizers.values.forEach { it.setBands(bandsMb) }
        fallbackEqualizer?.setBands(bandsMb)
        SonaraLogger.eq("Bands applied (${_activeStrategy.value}): ${bandsDb.take(5).map { "%.1f".format(it) }}")
    }

    fun setEnabled(enabled: Boolean) {
        eqEnabled = enabled
        dpEq?.setEnabled(enabled)
        sessionEqualizers.values.forEach { it.setEnabled(enabled) }
        fallbackEqualizer?.setEnabled(enabled)
    }

    fun reinitialize() {
        val saved = currentBandsDb.copyOf(); val en = eqEnabled
        dpEq?.release(); dpEq = null; sessionEqualizers.values.forEach { it.release() }; sessionEqualizers.clear(); fallbackEqualizer?.release(); fallbackEqualizer = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) tryDynamicsProcessing()
        handleActivePlayback(audioManager.activePlaybackConfigurations)
        if (dpEq == null) tryFallbackEqualizer()
        currentBandsDb = saved; eqEnabled = en; applyBands(saved); setEnabled(en)
    }

    val isInitialized: Boolean get() = dpEq != null || sessionEqualizers.isNotEmpty() || fallbackEqualizer != null

    private fun tryDynamicsProcessing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        dpEq = DynamicsProcessingEq.create(sessionId = 0, priority = Int.MAX_VALUE)
        if (dpEq != null && dpEq!!.verify()) {
            _activeStrategy.value = "dynamics_processing"
            SonaraLogger.eq("DynamicsProcessing on session 0 VERIFIED WORKING")
        } else { dpEq?.release(); dpEq = null; SonaraLogger.w("EQ", "DynamicsProcessing failed") }
    }

    private fun tryFallbackEqualizer() {
        fallbackEqualizer = SafeEqualizer.create(Int.MAX_VALUE, 0)
        if (fallbackEqualizer != null) { _activeStrategy.value = "equalizer_session0"; applyBands(currentBandsDb) }
    }

    private fun attachToSession(sid: Int) {
        if (sid <= 0 || sid in sessionEqualizers) return
        val eq = SafeEqualizer.create(Int.MAX_VALUE, sid) ?: return
        sessionEqualizers[sid] = eq; _activeSessions.value = sessionEqualizers.keys.toSet()
        val mb = IntArray(currentBandsDb.size) { (currentBandsDb[it] * 100).toInt() }; eq.setBands(mb); eq.setEnabled(eqEnabled)
    }

    private fun detachFromSession(sid: Int) { sessionEqualizers.remove(sid)?.release(); _activeSessions.value = sessionEqualizers.keys.toSet() }

    private fun handleActivePlayback(configs: List<AudioPlaybackConfiguration>) {
        for (c in configs) { extractSessionId(c)?.let { if (it > 0) attachToSession(it) } }
        if (dpEq == null && sessionEqualizers.isEmpty() && fallbackEqualizer == null) tryFallbackEqualizer()
    }

    private fun extractSessionId(config: AudioPlaybackConfiguration): Int? {
        listOf("getClientSessionId").forEach { m -> try { val method = AudioPlaybackConfiguration::class.java.getDeclaredMethod(m); method.isAccessible = true; val id = method.invoke(config) as Int; if (id > 0) return id } catch (_: Exception) {} }
        listOf("mClientSessionId", "mSessionId").forEach { f -> try { val field = AudioPlaybackConfiguration::class.java.getDeclaredField(f); field.isAccessible = true; val id = field.getInt(config); if (id > 0) return id } catch (_: Exception) {} }
        return null
    }
}
