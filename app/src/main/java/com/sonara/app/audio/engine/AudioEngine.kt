package com.sonara.app.audio.engine

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.sonara.app.data.SonaraLogger

data class HardwareReport(val eqWorks: Boolean, val bassWorks: Boolean, val virtWorks: Boolean, val loudWorks: Boolean, val bandCount: Int, val levelRange: Pair<Short, Short>)

class AudioEngine(private val context: Context) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null

    var isInitialized: Boolean = false; private set
    var hardwareReport: HardwareReport? = null; private set

    private var lastBands: FloatArray = FloatArray(10)
    private var lastBass: Int = 0
    private var lastVirt: Int = 0
    private var lastLoud: Int = 0
    private var isEnabled: Boolean = true

    // Bass simulation layer — used when BassBoost hardware fails
    private var bassSimulationActive = false
    private var bassSimulationDb: Float = 0f

    fun init(): Boolean {
        if (isInitialized) return true
        return try {
            equalizer = Equalizer(Int.MAX_VALUE, 0).apply { enabled = true }
            val bands = equalizer?.numberOfBands ?: 0
            val range = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
            SonaraLogger.eq("╔══ EQ CREATED ══╗")
            SonaraLogger.eq("║ Session: 0 (global), Priority: MAX")
            SonaraLogger.eq("║ Bands: $bands, Range: ${range[0]}..${range[1]}")

            try { bassBoost = BassBoost(Int.MAX_VALUE, 0).apply { enabled = true }; SonaraLogger.eq("║ BassBoost: ✓") } catch (e: Exception) { SonaraLogger.w("EQ", "║ BassBoost: ✗ ${e.message}") }
            try { virtualizer = Virtualizer(Int.MAX_VALUE, 0).apply { enabled = true }; SonaraLogger.eq("║ Virtualizer: ✓") } catch (e: Exception) { SonaraLogger.w("EQ", "║ Virtualizer: ✗ ${e.message}") }
            try { loudness = LoudnessEnhancer(0).apply { enabled = true }; SonaraLogger.eq("║ Loudness: ✓") } catch (e: Exception) { SonaraLogger.w("EQ", "║ Loudness: ✗ ${e.message}") }
            SonaraLogger.eq("╚════════════════╝")

            isInitialized = true
            probeHardware()
            true
        } catch (e: Exception) {
            SonaraLogger.e("EQ", "Init FAILED: ${e.message}")
            isInitialized = false
            false
        }
    }

    /**
     * Probe hardware capabilities — check what actually works on this device
     */
    private fun probeHardware() {
        val eq = equalizer ?: return
        val range = eq.bandLevelRange

        // Test EQ
        val origLevel = eq.getBandLevel(0)
        eq.setBandLevel(0, 600)
        val readBack = eq.getBandLevel(0)
        val eqWorks = readBack == 600.toShort()
        eq.setBandLevel(0, origLevel) // Restore

        // Test BassBoost
        val bb = bassBoost
        var bassWorks = false
        if (bb != null) {
            bb.setStrength(500)
            bassWorks = bb.roundedStrength > 0
            bb.setStrength(0) // Reset
        }

        // Test Virtualizer
        val vr = virtualizer
        var virtWorks = false
        if (vr != null) {
            vr.setStrength(500)
            virtWorks = vr.roundedStrength > 0
            vr.setStrength(0) // Reset
        }

        val loudWorks = loudness != null

        hardwareReport = HardwareReport(eqWorks, bassWorks, virtWorks, loudWorks, eq.numberOfBands.toInt(), range[0] to range[1])

        SonaraLogger.eq("╔══ HW PROBE ══╗")
        SonaraLogger.eq("║ EQ: ${if (eqWorks) "✓ WORKS" else "✗ BROKEN"}")
        SonaraLogger.eq("║ BassBoost: ${if (bassWorks) "✓ WORKS" else "✗ WILL SIMULATE VIA EQ"}")
        SonaraLogger.eq("║ Virtualizer: ${if (virtWorks) "✓ WORKS" else "✗ NOT AVAILABLE"}")
        SonaraLogger.eq("║ Loudness: ${if (loudWorks) "✓ WORKS" else "✗ NOT AVAILABLE"}")
        SonaraLogger.eq("╚═══════════════╝")

        bassSimulationActive = !bassWorks
        if (bassSimulationActive) {
            SonaraLogger.eq("Bass simulation mode ACTIVE — using EQ low bands instead")
        }
    }

    fun applyBands(tenBands: FloatArray) {
        lastBands = tenBands.copyOf()
        val eq = equalizer ?: return
        try {
            val count = eq.numberOfBands.toInt()
            if (count == 0) return
            val range = eq.bandLevelRange
            val freqs = IntArray(count) { eq.getCenterFreq(it.toShort()) / 1000 }

            // Add bass simulation offset to low bands
            val adjustedBands = if (bassSimulationActive && bassSimulationDb > 0f) {
                FloatArray(tenBands.size) { i ->
                    val bassOffset = when (i) {
                        0 -> bassSimulationDb        // 31Hz — full boost
                        1 -> bassSimulationDb * 0.8f // 62Hz — 80%
                        2 -> bassSimulationDb * 0.5f // 125Hz — 50%
                        3 -> bassSimulationDb * 0.2f // 250Hz — 20%
                        else -> 0f
                    }
                    (tenBands[i] + bassOffset).coerceIn(-12f, 12f)
                }
            } else tenBands

            val mapped = BandMapper.mapToDevice(adjustedBands, count, freqs)
            for (i in 0 until count) {
                val level = mapped[i].coerceIn(range[0], range[1])
                eq.setBandLevel(i.toShort(), level)
            }

            // Verify
            val readBack = ShortArray(count) { eq.getBandLevel(it.toShort()) }
            SonaraLogger.eq("Bands: ${readBack.toList()} ${if (bassSimulationActive) "(+bass sim ${bassSimulationDb}dB)" else ""}")
        } catch (e: Exception) { SonaraLogger.e("EQ", "applyBands: ${e.message}") }
    }

    fun applyBassBoost(s: Int) {
        lastBass = s
        if (bassSimulationActive) {
            // Hardware BassBoost doesn't work — simulate via EQ low bands
            bassSimulationDb = s / 100f // 0-1000 → 0-10dB
            applyBands(lastBands) // Re-apply with simulation offset
            SonaraLogger.eq("Bass SIMULATED via EQ: ${bassSimulationDb}dB (hw not available)")
            return
        }
        try {
            val bb = bassBoost
            if (bb != null) {
                bb.setStrength(s.coerceIn(0, 1000).toShort())
                SonaraLogger.eq("Bass: req=$s actual=${bb.roundedStrength}")
            }
        } catch (e: Exception) { SonaraLogger.e("EQ", "Bass: ${e.message}") }
    }

    fun applyVirtualizer(s: Int) {
        lastVirt = s
        try {
            val vr = virtualizer
            if (vr != null) {
                vr.setStrength(s.coerceIn(0, 1000).toShort())
                SonaraLogger.eq("Virt: req=$s actual=${vr.roundedStrength}")
            }
        } catch (e: Exception) { SonaraLogger.e("EQ", "Virt: ${e.message}") }
    }

    fun applyLoudness(g: Int) {
        lastLoud = g
        try {
            val le = loudness
            if (le != null) { le.setTargetGain(g); le.enabled = isEnabled && g > 0; SonaraLogger.eq("Loud: ${g}mB (${g / 100f}dB)") }
        } catch (e: Exception) { SonaraLogger.e("EQ", "Loud: ${e.message}") }
    }

    fun setEnabled(on: Boolean) {
        isEnabled = on
        try { equalizer?.enabled = on } catch (_: Exception) {}
        try { bassBoost?.enabled = on } catch (_: Exception) {}
        try { virtualizer?.enabled = on } catch (_: Exception) {}
        try { loudness?.enabled = on && lastLoud > 0 } catch (_: Exception) {}
        SonaraLogger.eq("All effects enabled=$on")
    }

    fun release() {
        SonaraLogger.eq("Releasing effects")
        try { equalizer?.release() } catch (_: Exception) {}; try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}; try { loudness?.release() } catch (_: Exception) {}
        equalizer = null; bassBoost = null; virtualizer = null; loudness = null; isInitialized = false
    }
}
