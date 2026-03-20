package com.sonara.app.engine.eq

import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.P)
class DynamicsProcessingEq private constructor(
    private val dp: DynamicsProcessing,
    private val bandCount: Int,
    private val bandFrequencies: FloatArray
) {
    companion object {
        private const val TAG = "DPEq"
        val FREQUENCIES_10 = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

        fun create(sessionId: Int = 0, priority: Int = Int.MAX_VALUE, frequencies: FloatArray = FREQUENCIES_10): DynamicsProcessingEq? {
            return try {
                val bc = frequencies.size

                // Build config with post-EQ enabled
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1, false, 0, false, 0, true, bc, false
                ).build()

                // Create DynamicsProcessing instance
                val dp = DynamicsProcessing(priority, sessionId, config)

                // Configure bands AFTER creation (not in Builder)
                for (ch in 0 until 1) { // mono/stereo handled by allChannels methods
                    for (i in 0 until bc) {
                        dp.setPostEqBandAllChannelsTo(i, DynamicsProcessing.EqBand(true, frequencies[i], 0f))
                    }
                }

                dp.enabled = true
                Log.i(TAG, "Created session=$sessionId bands=$bc")
                DynamicsProcessingEq(dp, bc, frequencies)
            } catch (e: Exception) {
                Log.e(TAG, "Create failed: ${e.message}")
                null
            }
        }
    }

    fun setBandGain(band: Int, gainDb: Float) {
        if (band !in 0 until bandCount) return
        try { dp.setPostEqBandAllChannelsTo(band, DynamicsProcessing.EqBand(true, bandFrequencies[band], gainDb.coerceIn(-24f, 24f))) }
        catch (e: Exception) { Log.e(TAG, "setBand($band): ${e.message}") }
    }

    fun setAllBands(gainsDb: FloatArray) {
        val src = if (gainsDb.size != bandCount) interpolate(gainsDb, bandCount) else gainsDb
        src.forEachIndexed { i, g -> setBandGain(i, g) }
    }

    fun getBandGain(band: Int): Float = try { dp.getPostEqBandByChannelIndex(0, band).gain } catch (_: Exception) { 0f }
    fun getBandCount(): Int = bandCount
    fun setEnabled(enabled: Boolean) { dp.enabled = enabled }

    fun verify(): Boolean {
        return try {
            val orig = getBandGain(0); setBandGain(0, 3.0f); val rb = getBandGain(0); setBandGain(0, orig)
            val works = kotlin.math.abs(rb - 3.0f) < 0.5f
            Log.i(TAG, "Verify: wrote=3.0 read=$rb works=$works"); works
        } catch (e: Exception) { Log.e(TAG, "Verify: ${e.message}"); false }
    }

    fun release() { try { dp.enabled = false; dp.release() } catch (_: Exception) {} }

    private fun interpolate(src: FloatArray, tgt: Int): FloatArray {
        if (src.isEmpty()) return FloatArray(tgt); if (src.size == tgt) return src
        return FloatArray(tgt) { i -> val p = i.toFloat() * (src.size - 1) / (tgt - 1).coerceAtLeast(1); val lo = p.toInt().coerceIn(0, src.size - 1); val hi = (lo + 1).coerceIn(0, src.size - 1); val f = p - lo; src[lo] * (1 - f) + src[hi] * f }
    }
}
