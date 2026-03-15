package com.sonara.app.autoeq

import com.sonara.app.audio.equalizer.TenBandEqualizer

object AutoEqImporter {

    data class ImportResult(
        val success: Boolean,
        val bands: FloatArray = FloatArray(10),
        val name: String = "",
        val error: String = ""
    )

    fun parseGraphicEq(input: String): ImportResult {
        try {
            val clean = input.trim()

            // Format: GraphicEQ: 20 -0.2; 25 -0.3; 31 -0.5; ...
            if (clean.startsWith("GraphicEQ:", ignoreCase = true)) {
                return parseGraphicEqLine(clean)
            }

            // Format: 10 comma-separated dB values
            if (clean.contains(",")) {
                return parseCommaSeparated(clean)
            }

            // Format: 10 lines of "freq gain" or just 10 gain values
            val lines = clean.lines().filter { it.isNotBlank() }
            if (lines.size >= 10) {
                return parseLines(lines)
            }

            // Format: 10 space-separated values
            val parts = clean.split("\\s+".toRegex())
            if (parts.size >= 10) {
                val bands = parts.take(10).map { it.toFloatOrNull() ?: 0f }.toFloatArray()
                return ImportResult(true, bands)
            }

            return ImportResult(false, error = "Unrecognized format. Use GraphicEQ format, 10 comma-separated values, or 10 lines of gain values.")
        } catch (e: Exception) {
            return ImportResult(false, error = "Parse error: ${e.message}")
        }
    }

    private fun parseGraphicEqLine(line: String): ImportResult {
        val data = line.substringAfter(":").trim()
        val pairs = data.split(";").mapNotNull { segment ->
            val parts = segment.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                val freq = parts[0].toFloatOrNull() ?: return@mapNotNull null
                val gain = parts[1].toFloatOrNull() ?: return@mapNotNull null
                freq to gain
            } else null
        }

        if (pairs.isEmpty()) return ImportResult(false, error = "No valid frequency-gain pairs found")

        val targetFreqs = TenBandEqualizer.FREQUENCIES
        val bands = FloatArray(10) { i ->
            interpolateFromPairs(pairs, targetFreqs[i].toFloat())
        }

        return ImportResult(true, bands)
    }

    private fun parseCommaSeparated(input: String): ImportResult {
        val values = input.split(",").mapNotNull { it.trim().toFloatOrNull() }
        if (values.size < 10) return ImportResult(false, error = "Need at least 10 values, got ${values.size}")
        val bands = values.take(10).toFloatArray()
        return ImportResult(true, bands)
    }

    private fun parseLines(lines: List<String>): ImportResult {
        val bands = FloatArray(10)
        var index = 0
        for (line in lines) {
            if (index >= 10) break
            val parts = line.trim().split("\\s+".toRegex())
            val gain = when {
                parts.size >= 2 -> parts.last().toFloatOrNull() ?: parts.first().toFloatOrNull()
                parts.size == 1 -> parts[0].toFloatOrNull()
                else -> null
            }
            if (gain != null) {
                bands[index] = gain.coerceIn(TenBandEqualizer.MIN_LEVEL, TenBandEqualizer.MAX_LEVEL)
                index++
            }
        }
        if (index < 10) return ImportResult(false, error = "Could only parse $index values, need 10")
        return ImportResult(true, bands)
    }

    private fun interpolateFromPairs(pairs: List<Pair<Float, Float>>, targetFreq: Float): Float {
        if (pairs.isEmpty()) return 0f
        val sorted = pairs.sortedBy { it.first }

        if (targetFreq <= sorted.first().first) return sorted.first().second
        if (targetFreq >= sorted.last().first) return sorted.last().second

        for (i in 0 until sorted.size - 1) {
            val (f0, g0) = sorted[i]
            val (f1, g1) = sorted[i + 1]
            if (targetFreq in f0..f1) {
                val ratio = (targetFreq - f0) / (f1 - f0)
                return (g0 + (g1 - g0) * ratio).coerceIn(TenBandEqualizer.MIN_LEVEL, TenBandEqualizer.MAX_LEVEL)
            }
        }
        return 0f
    }

    val EXAMPLE_FORMAT = """Supported formats:
• GraphicEQ: 20 -0.2; 31 -0.5; 62 -1.0; ...
• 10 comma-separated values: -2.5, -1.0, 0.5, 1.0, 0.0, -0.5, 1.0, 2.0, 1.5, 0.5
• 10 lines with gain values (one per band, 31Hz to 16kHz)"""
}
