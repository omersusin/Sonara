package com.sonara.app.engine.eq

import android.bluetooth.BluetoothA2dp
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicBoolean

class EqSessionController(private val context: Context) {
    companion object { private const val TAG = "EqSessionCtrl"; private const val PRIORITY = 1000; private const val SETTLE = 350L }
    enum class AudioRoute { SPEAKER, WIRED, BLUETOOTH }

    private var safeEq: SafeEqualizer? = null
    private var currentSessionId: Int = 0
    private var currentRoute: AudioRoute = AudioRoute.SPEAKER
    private var savedBandsMb: IntArray? = null
    private var eqEnabled = true
    private val initialized = AtomicBoolean(false)
    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val routeToken = Any()

    private val routeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            handler.removeCallbacksAndMessages(routeToken)
            handler.postAtTime(::onRouteChanged, routeToken, android.os.SystemClock.uptimeMillis() + SETTLE)
        }
    }

    fun attachSession(sessionId: Int) {
        synchronized(lock) {
            if (sessionId <= 0) return
            currentSessionId = sessionId; currentRoute = detectRoute(); rebuildEq()
            if (initialized.compareAndSet(false, true)) registerRouteReceiver()
        }
    }

    fun applyBands(bands: ShortArray) {
        synchronized(lock) {
            savedBandsMb = IntArray(bands.size) { bands[it].toInt() }
            safeEq?.setBands(savedBandsMb!!)
        }
    }

    fun applyBandsDb(bandsDb: FloatArray) {
        val mb = IntArray(bandsDb.size) { (bandsDb[it] * 100).toInt() }
        synchronized(lock) {
            savedBandsMb = mb
            safeEq?.setBands(mb)
        }
    }

    fun reapplyCurrentEq() { synchronized(lock) { if (currentSessionId > 0) rebuildEq() } }
    fun updateSessionId(newId: Int) { synchronized(lock) { if (newId > 0 && newId != currentSessionId) { currentSessionId = newId; rebuildEq() } } }
    fun setEnabled(on: Boolean) { synchronized(lock) { eqEnabled = on; safeEq?.setEnabled(on) } }

    fun detectRoute(): AudioRoute {
        return try {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            when {
                outputs.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } -> AudioRoute.BLUETOOTH
                outputs.any { it.type in listOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET) } -> AudioRoute.WIRED
                else -> AudioRoute.SPEAKER
            }
        } catch (_: Exception) { AudioRoute.SPEAKER }
    }

    fun release() { synchronized(lock) { initialized.set(false); handler.removeCallbacksAndMessages(null); try { context.unregisterReceiver(routeReceiver) } catch (_: Exception) {}; safeEq?.release(); scope.cancel() } }

    private fun rebuildEq() {
        safeEq?.release()
        safeEq = SafeEqualizer.create(PRIORITY, currentSessionId) ?: if (currentRoute == AudioRoute.SPEAKER) SafeEqualizer.create(PRIORITY, 0) else null
        safeEq?.setEnabled(eqEnabled)
        savedBandsMb?.let { safeEq?.setBands(it) }
        Log.d(TAG, "Rebuilt: session=$currentSessionId route=$currentRoute eq=${safeEq != null}")
    }

    private fun onRouteChanged() { synchronized(lock) { val nr = detectRoute(); if (nr != currentRoute) { currentRoute = nr; rebuildEq() } } }

    private fun registerRouteReceiver() {
        try {
            val f = IntentFilter().apply { addAction(AudioManager.ACTION_HEADSET_PLUG); addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY); addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) }
            context.registerReceiver(routeReceiver, f)
        } catch (e: Exception) { Log.e(TAG, "Receiver: ${e.message}") }
    }
}
