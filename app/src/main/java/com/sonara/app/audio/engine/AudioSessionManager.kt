package com.sonara.app.audio.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.audiofx.*
import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

data class SessionEffects(
    val sessionId: Int,
    val equalizer: Equalizer?,
    val bassBoost: BassBoost?,
    val virtualizer: Virtualizer?,
    val loudness: LoudnessEnhancer?,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudness?.release() }
    }
}

data class EffectCapabilities(
    val eqWorksOnSession0: Boolean,
    val bassWorksOnSession0: Boolean,
    val virtWorksOnSession0: Boolean,
    val loudWorksOnSession0: Boolean,
    val competing: List<String>
)

class AudioSessionManager(private val context: Context) {
    private val PRIORITY = Int.MAX_VALUE
    private val activeSessions = ConcurrentHashMap<Int, SessionEffects>()
    private var globalSession: SessionEffects? = null

    private var currentBands = ShortArray(0)
    private var currentBass: Short = 0
    private var currentVirt: Short = 0
    private var currentLoudness: Int = 0
    private var effectsEnabled = true
    private var bassSimulation = false

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _capabilities = MutableStateFlow<EffectCapabilities?>(null)
    val capabilities: StateFlow<EffectCapabilities?> = _capabilities

    private val _activeSessionId = MutableStateFlow(0)
    val activeSessionId: StateFlow<Int> = _activeSessionId

    val isInitialized: Boolean get() = globalSession != null

    private val sessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val sid = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
            val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: ""
            if (sid <= 0) return
            when (intent.action) {
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                    SonaraLogger.eq("🎵 Session OPEN: $sid ($pkg)")
                    attachToSession(sid)
                }
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                    SonaraLogger.eq("🎵 Session CLOSE: $sid")
                    detachFromSession(sid)
                }
            }
        }
    }

    fun initialize() {
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        context.registerReceiver(sessionReceiver, filter)

        globalSession = createEffects(0)
        SonaraLogger.eq("╔══ SESSION MANAGER INIT ══╗")
        SonaraLogger.eq("║ Global session 0: ${if (globalSession != null) "✓" else "✗"}")

        scope.launch {
            val caps = probeCapabilities()
            _capabilities.value = caps
            bassSimulation = !caps.bassWorksOnSession0
            SonaraLogger.eq("║ EQ@0: ${if (caps.eqWorksOnSession0) "✓" else "✗"}")
            SonaraLogger.eq("║ Bass@0: ${if (caps.bassWorksOnSession0) "✓ HW" else "✗ SIMULATED"}")
            SonaraLogger.eq("║ Virt@0: ${if (caps.virtWorksOnSession0) "✓" else "✗"}")
            SonaraLogger.eq("║ Loud@0: ${if (caps.loudWorksOnSession0) "✓" else "✗"}")
            if (caps.competing.isNotEmpty()) SonaraLogger.w("EQ", "║ ⚠️ Competing: ${caps.competing}")
            SonaraLogger.eq("╚═════════════════════════╝")

            // Cleanup loop
            while (isActive) { delay(60_000); cleanupStaleSessions() }
        }
    }

    private fun createEffects(sid: Int): SessionEffects? {
        return try {
            val eq = try { Equalizer(PRIORITY, sid).apply { enabled = effectsEnabled } } catch (e: Exception) { SonaraLogger.e("EQ", "EQ@$sid: ${e.message}"); null }
            val bass = try { BassBoost(PRIORITY, sid).apply { enabled = effectsEnabled } } catch (e: Exception) { null }
            val virt = try { Virtualizer(PRIORITY, sid).apply { enabled = effectsEnabled } } catch (e: Exception) { null }
            val loud = try { LoudnessEnhancer(sid).apply { enabled = effectsEnabled } } catch (e: Exception) { null }
            SessionEffects(sid, eq, bass, virt, loud)
        } catch (e: Exception) { SonaraLogger.e("EQ", "Create@$sid FAIL: ${e.message}"); null }
    }

    private fun attachToSession(sid: Int) {
        if (activeSessions.containsKey(sid)) return
        val effects = createEffects(sid) ?: return
        activeSessions[sid] = effects
        applyCurrentTo(effects)
        _activeSessionId.value = sid
        SonaraLogger.eq("✅ Attached session $sid (total=${activeSessions.size})")
    }

    private fun detachFromSession(sid: Int) {
        activeSessions.remove(sid)?.release()
        if (_activeSessionId.value == sid) _activeSessionId.value = activeSessions.keys.firstOrNull() ?: 0
    }

    // ─── Apply to ALL sessions ───

    fun applyBands(tenBands: FloatArray) {
        val mapped = mapBands(tenBands)
        currentBands = mapped
        forAllSessions { effects ->
            val eq = effects.equalizer ?: return@forAllSessions
            val count = eq.numberOfBands.toInt()
            val freqs = IntArray(count) { eq.getCenterFreq(it.toShort()) / 1000 }
            val deviceMapped = BandMapper.mapToDevice(tenBands, count, freqs)
            val range = eq.bandLevelRange ?: shortArrayOf(-1500, 1500)
            for (i in 0 until count) eq.setBandLevel(i.toShort(), deviceMapped[i].coerceIn(range[0], range[1]))
        }
        val readBack = globalSession?.equalizer?.let { eq -> ShortArray(eq.numberOfBands.toInt()) { eq.getBandLevel(it.toShort()) } }
        SonaraLogger.eq("Bands: ${readBack?.toList() ?: "null"} sessions=${1 + activeSessions.size}")
    }

    fun applyBass(strength: Int) {
        val s = strength.coerceIn(0, 1000).toShort()
        currentBass = s
        if (bassSimulation) {
            SonaraLogger.eq("Bass SIM: ${strength / 100f}dB via EQ")
            forAllSessions { simulateBass(it.equalizer, s) }
            return
        }
        forAllSessions { it.bassBoost?.setStrength(s) }
        val actual = globalSession?.bassBoost?.roundedStrength
        SonaraLogger.eq("Bass: req=$strength actual=$actual")
    }

    fun applyVirt(strength: Int) {
        val s = strength.coerceIn(0, 1000).toShort()
        currentVirt = s
        forAllSessions { it.virtualizer?.setStrength(s) }
    }

    fun applyLoudness(gainMb: Int) {
        currentLoudness = gainMb
        forAllSessions { it.loudness?.let { l -> l.setTargetGain(gainMb); l.enabled = effectsEnabled && gainMb > 0 } }
    }

    fun setEnabled(on: Boolean) {
        effectsEnabled = on
        forAllSessions { e ->
            e.equalizer?.enabled = on
            e.bassBoost?.enabled = on
            e.virtualizer?.enabled = on
            e.loudness?.enabled = on && currentLoudness > 0
        }
        SonaraLogger.eq("Enabled=$on")
    }

    fun refreshAllEffects() {
        SonaraLogger.eq("🔄 Refreshing all effects")
        forAllSessions { e -> e.equalizer?.let { it.enabled = false; it.enabled = effectsEnabled } }
        if (currentBands.isNotEmpty()) applyBands(shortArrayToFloat(currentBands))
        if (currentBass > 0) applyBass(currentBass.toInt())
        if (currentVirt > 0) applyVirt(currentVirt.toInt())
        if (currentLoudness > 0) applyLoudness(currentLoudness)
    }

    // ─── Helpers ───

    private fun mapBands(tenBands: FloatArray): ShortArray {
        return ShortArray(tenBands.size) { (tenBands[it] * 100).toInt().toShort() } // dB→millibel
    }

    private fun shortArrayToFloat(arr: ShortArray): FloatArray = FloatArray(arr.size) { arr[it] / 100f }

    private fun simulateBass(eq: Equalizer?, strength: Short) {
        eq ?: return
        val boost = (strength.toFloat() / 1000f * 600f).toInt().toShort()
        val count = eq.numberOfBands.toInt()
        if (count >= 1) { val c = eq.getBandLevel(0); eq.setBandLevel(0, (c + boost).toShort()) }
        if (count >= 2) { val c = eq.getBandLevel(1); eq.setBandLevel(1, (c + (boost * 0.6f).toInt().toShort()).toShort()) }
        if (count >= 3) { val c = eq.getBandLevel(2); eq.setBandLevel(2, (c + (boost * 0.3f).toInt().toShort()).toShort()) }
    }

    private fun forAllSessions(action: (SessionEffects) -> Unit) {
        globalSession?.let { runCatching { action(it) }.onFailure { e -> SonaraLogger.e("EQ", "S0: ${e.message}") } }
        activeSessions.values.forEach { runCatching { action(it) }.onFailure { e -> SonaraLogger.e("EQ", "S${it.sessionId}: ${e.message}") } }
    }

    private fun applyCurrentTo(effects: SessionEffects) {
        effects.equalizer?.let { eq ->
            val count = eq.numberOfBands.toInt()
            if (currentBands.isNotEmpty()) {
                val freqs = IntArray(count) { eq.getCenterFreq(it.toShort()) / 1000 }
                val deviceMapped = BandMapper.mapToDevice(shortArrayToFloat(currentBands), count, freqs)
                val range = eq.bandLevelRange ?: shortArrayOf(-1500, 1500)
                for (i in 0 until count) eq.setBandLevel(i.toShort(), deviceMapped[i].coerceIn(range[0], range[1]))
            }
            eq.enabled = effectsEnabled
        }
        effects.bassBoost?.let { it.setStrength(currentBass); it.enabled = effectsEnabled }
        effects.virtualizer?.let { it.setStrength(currentVirt); it.enabled = effectsEnabled }
        effects.loudness?.let { it.setTargetGain(currentLoudness); it.enabled = effectsEnabled && currentLoudness > 0 }
    }

    private fun probeCapabilities(): EffectCapabilities {
        val s0 = globalSession
        val eqOk = s0?.equalizer?.let { val o = it.getBandLevel(0); it.setBandLevel(0, 600); val r = it.getBandLevel(0) == 600.toShort(); it.setBandLevel(0, o); r } ?: false
        val bassOk = s0?.bassBoost?.let { it.setStrength(500); val r = it.roundedStrength > 0; it.setStrength(0); r } ?: false
        val virtOk = s0?.virtualizer?.let { it.setStrength(500); val r = it.roundedStrength > 0; it.setStrength(0); r } ?: false
        val loudOk = s0?.loudness != null
        val competing = detectCompeting()
        return EffectCapabilities(eqOk, bassOk, virtOk, loudOk, competing)
    }

    private fun detectCompeting(): List<String> {
        val known = mapOf(
            "com.dolby.dax" to "Dolby Atmos", "com.sec.android.app.soundalive" to "Samsung SoundAlive",
            "com.miui.misound" to "Xiaomi MIUI Audio", "com.huawei.histen" to "Huawei Histen",
            "com.asus.maxxaudio" to "ASUS MaxxAudio", "com.oneplus.sound.tuner" to "OnePlus Audio"
        )
        val pm = context.packageManager
        return known.mapNotNull { (pkg, name) -> try { pm.getPackageInfo(pkg, 0); name } catch (_: Exception) { null } }
    }

    private fun cleanupStaleSessions() {
        val now = System.currentTimeMillis()
        activeSessions.filter { now - it.value.createdAt > 30 * 60 * 1000L }.forEach { (id, e) -> e.release(); activeSessions.remove(id) }
    }

    fun release() {
        runCatching { context.unregisterReceiver(sessionReceiver) }
        globalSession?.release(); activeSessions.values.forEach { it.release() }; activeSessions.clear(); scope.cancel()
    }

    fun getDebugInfo(): String = buildString {
        appendLine("Session0: ${globalSession != null}, Active: ${activeSessions.keys}, Verified: ${_activeSessionId.value}")
        appendLine("Enabled: $effectsEnabled, BassSimulation: $bassSimulation")
        _capabilities.value?.let { appendLine("EQ=${it.eqWorksOnSession0} Bass=${it.bassWorksOnSession0} Virt=${it.virtWorksOnSession0} Competing=${it.competing}") }
    }
}
