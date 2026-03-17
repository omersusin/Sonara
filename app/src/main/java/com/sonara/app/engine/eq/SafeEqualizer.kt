package com.sonara.app.engine.eq

import android.media.audiofx.Equalizer
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class SafeEqualizer private constructor(private var eq: Equalizer, val sessionId: Int) {
    companion object {
        private const val TAG = "SafeEQ"
        fun create(priority: Int, sessionId: Int): SafeEqualizer? {
            return try { SafeEqualizer(Equalizer(priority, sessionId), sessionId) } catch (e: Exception) { Log.e(TAG, "Create@$sessionId: ${e.message}"); null }
        }
    }
    private val released = AtomicBoolean(false)
    private val errors = AtomicInteger(0)
    private val lock = Any()
    private var cachedBands: ShortArray? = null
    private var cachedEnabled = true

    val isReleased get() = released.get()
    val hasExcessiveErrors get() = errors.get() >= 6

    fun getNumberOfBands(): Int = safe("nBands") { eq.numberOfBands.toInt() } ?: 5
    fun getBandLevelRange(): ShortArray = safe("range") { eq.bandLevelRange } ?: shortArrayOf(-1500, 1500)
    fun getCenterFreq(band: Int): Int = safe("freq") { eq.getCenterFreq(band.toShort()) } ?: 1000000

    fun applyAllBands(bands: ShortArray): Int {
        synchronized(lock) {
            if (released.get()) return 0
            val n = getNumberOfBands(); val range = getBandLevelRange(); var ok = 0
            for (i in 0 until minOf(bands.size, n)) {
                try { eq.setBandLevel(i.toShort(), bands[i].coerceIn(range[0], range[1])); ok++ }
                catch (e: Exception) { errors.incrementAndGet(); break }
            }
            if (ok > 0) { cachedBands = bands.copyOf(); try { eq.enabled = cachedEnabled } catch (_: Exception) {} }
            return ok
        }
    }

    fun setEnabled(on: Boolean): Boolean { cachedEnabled = on; return safe("enable") { eq.enabled = on; true } ?: false }
    fun restoreCachedBands(): Boolean = cachedBands?.let { applyAllBands(it) > 0 } ?: false
    fun exportBands(): ShortArray? = cachedBands?.copyOf()

    fun release() {
        if (released.getAndSet(true)) return
        synchronized(lock) { try { eq.enabled = false } catch (_: Exception) {}; try { eq.release() } catch (_: Exception) {} }
    }

    private fun <T> safe(op: String, block: () -> T): T? {
        if (released.get()) return null
        return try { block() } catch (e: Exception) { Log.w(TAG, "$op: ${e.message}"); errors.incrementAndGet(); null }
    }
}
