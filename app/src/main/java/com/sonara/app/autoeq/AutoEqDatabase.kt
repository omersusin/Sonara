/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sonara.app.autoeq

object AutoEqDatabase {
    // Correction bands: 10-band (31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz)
    // Values in dB — negative = cut, positive = boost to flatten frequency response
    private val profiles = mapOf(
        // ═══ Apple ═══
        "airpods" to floatArrayOf(1.5f, 0.5f, -0.5f, -1f, 0f, 1f, 0.5f, -1f, -2f, -1.5f),
        "airpods pro" to floatArrayOf(1f, 0f, -1f, -0.5f, 0.5f, 1.5f, 0f, -1.5f, -1f, -0.5f),
        "airpods pro 2" to floatArrayOf(0.8f, -0.2f, -0.8f, -0.3f, 0.4f, 1.2f, 0.2f, -1.2f, -0.8f, -0.3f),
        "airpods max" to floatArrayOf(0.5f, -0.5f, -1f, 0f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "airpods 3" to floatArrayOf(1.2f, 0.3f, -0.5f, -0.8f, 0.2f, 1f, 0.3f, -1.2f, -1.5f, -1f),
        // ═══ Samsung ═══
        "galaxy buds" to floatArrayOf(2f, 1f, 0f, -1f, -0.5f, 0.5f, 1f, 0f, -1.5f, -1f),
        "galaxy buds pro" to floatArrayOf(1.5f, 0.5f, -0.5f, -1f, 0f, 1f, 0.5f, -1f, -1.5f, -0.5f),
        "galaxy buds2" to floatArrayOf(1.5f, 0.5f, 0f, -1f, -0.5f, 0.5f, 1f, -0.5f, -1.5f, -1f),
        "galaxy buds2 pro" to floatArrayOf(1.2f, 0.3f, -0.3f, -0.8f, 0f, 0.8f, 0.5f, -0.8f, -1.2f, -0.5f),
        "galaxy buds fe" to floatArrayOf(1.8f, 0.8f, -0.2f, -1f, -0.3f, 0.5f, 0.8f, -0.5f, -1.5f, -0.8f),
        "galaxy buds3" to floatArrayOf(1f, 0.2f, -0.5f, -0.5f, 0.2f, 0.8f, 0.5f, -0.5f, -1f, -0.5f),
        "galaxy buds3 pro" to floatArrayOf(0.8f, 0f, -0.5f, -0.3f, 0.3f, 0.8f, 0.3f, -0.5f, -0.8f, -0.3f),
        // ═══ Sony ═══
        "sony wh-1000xm3" to floatArrayOf(0.8f, -0.2f, -1.5f, -0.3f, 0.8f, 1.5f, 0.5f, -1.2f, -2f, -1.2f),
        "sony wh-1000xm4" to floatArrayOf(0.5f, -0.5f, -1.5f, -0.5f, 1f, 1.5f, 0.5f, -1f, -1.5f, -1f),
        "sony wh-1000xm5" to floatArrayOf(0f, -0.5f, -1f, -0.5f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "sony wf-1000xm4" to floatArrayOf(1f, 0f, -1f, -0.5f, 0.5f, 1.5f, 0.5f, -1f, -1.5f, -0.5f),
        "sony wf-1000xm5" to floatArrayOf(0.5f, -0.5f, -1f, 0f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "sony wh-ch710n" to floatArrayOf(1f, 0f, -1f, -0.5f, 0.5f, 1.2f, 0.3f, -1f, -1.5f, -0.8f),
        "sony mdr-7506" to floatArrayOf(-0.5f, -0.5f, 0.5f, 0.5f, 0f, -0.5f, -1.5f, -2.5f, -2f, -1f),
        "sony mdr-xb950" to floatArrayOf(-3f, -2.5f, -1f, 0f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "sony linkbuds" to floatArrayOf(1.5f, 0.5f, -0.5f, -0.5f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "sony linkbuds s" to floatArrayOf(1f, 0f, -0.8f, -0.3f, 0.5f, 1f, 0.3f, -0.8f, -1f, -0.5f),
        // ═══ Sennheiser ═══
        "sennheiser hd 600" to floatArrayOf(1f, 0.5f, 0f, -0.5f, -1f, 0f, 1f, 1.5f, 0f, -0.5f),
        "sennheiser hd 650" to floatArrayOf(0.5f, 0f, -0.5f, -0.5f, -0.5f, 0.5f, 1f, 1.5f, 0.5f, 0f),
        "sennheiser hd 660s" to floatArrayOf(0.5f, 0f, -0.5f, -0.3f, -0.3f, 0.5f, 0.8f, 1.2f, 0.3f, -0.2f),
        "sennheiser hd 800" to floatArrayOf(0.5f, 0f, -0.5f, -0.3f, 0f, 0.5f, -0.5f, 0.5f, -1.5f, -0.5f),
        "sennheiser hd 560s" to floatArrayOf(0.3f, 0f, -0.3f, -0.3f, -0.3f, 0.3f, 0.5f, 1f, 0.2f, -0.2f),
        "sennheiser momentum" to floatArrayOf(0.5f, -0.5f, -1f, -0.3f, 0.5f, 1f, 0.3f, -1f, -1.5f, -0.5f),
        "sennheiser momentum 4" to floatArrayOf(0.3f, -0.3f, -0.8f, -0.2f, 0.3f, 0.8f, 0.3f, -0.5f, -1f, -0.3f),
        "sennheiser cx" to floatArrayOf(1.5f, 0.5f, -0.5f, -0.8f, 0.2f, 0.8f, 0.5f, -0.5f, -1.2f, -0.8f),
        // ═══ Beyerdynamic ═══
        "beyerdynamic dt 770" to floatArrayOf(-1f, -0.5f, 0.5f, 0.5f, 0f, -0.5f, -1.5f, -2f, -3f, -2.5f),
        "beyerdynamic dt 880" to floatArrayOf(-0.5f, -0.3f, 0.3f, 0.3f, 0f, -0.3f, -0.5f, -1.5f, -2f, -1.5f),
        "beyerdynamic dt 990" to floatArrayOf(0f, 0f, 0.5f, 0.5f, 0f, -1f, -2f, -3.5f, -4f, -3f),
        "beyerdynamic dt 900 pro x" to floatArrayOf(-0.3f, -0.2f, 0.2f, 0.3f, 0f, 0f, -0.3f, -0.8f, -1.5f, -1f),
        // ═══ Audio-Technica ═══
        "audio-technica m50x" to floatArrayOf(-1f, -1.5f, 0f, 0.5f, 0f, 0.5f, -0.5f, -2f, -1.5f, -1f),
        "audio-technica m40x" to floatArrayOf(-0.5f, -1f, 0f, 0.5f, 0f, 0f, -0.5f, -1.5f, -1f, -0.5f),
        "audio-technica m60x" to floatArrayOf(-0.3f, -0.5f, 0f, 0.3f, 0f, 0.3f, -0.3f, -1f, -0.8f, -0.5f),
        "audio-technica r70x" to floatArrayOf(0.5f, 0f, -0.3f, 0f, 0f, 0.3f, 0.5f, 0f, -0.5f, -0.3f),
        // ═══ AKG ═══
        "akg k371" to floatArrayOf(-0.3f, -0.3f, 0f, 0.2f, 0f, 0f, -0.2f, -0.3f, -0.3f, -0.2f),
        "akg k701" to floatArrayOf(1.5f, 0.5f, -0.3f, -0.5f, 0f, 0.5f, 0.5f, 0.5f, -0.5f, -0.3f),
        "akg k712" to floatArrayOf(1f, 0.3f, -0.3f, -0.3f, 0f, 0.3f, 0.3f, 0.3f, -0.5f, -0.3f),
        "akg n700nc" to floatArrayOf(0.5f, -0.3f, -0.8f, -0.3f, 0.3f, 0.8f, 0.3f, -0.5f, -1f, -0.5f),
        // ═══ Bose ═══
        "bose qc" to floatArrayOf(0.5f, 0f, -0.5f, -0.5f, 0f, 0.5f, 0f, -0.5f, -1f, -0.5f),
        "bose qc35" to floatArrayOf(0.5f, 0f, -0.5f, -0.5f, 0f, 0.5f, 0f, -0.5f, -1f, -0.5f),
        "bose qc45" to floatArrayOf(0.3f, -0.2f, -0.5f, -0.3f, 0.2f, 0.5f, 0f, -0.5f, -0.8f, -0.3f),
        "bose 700" to floatArrayOf(0f, -0.5f, -0.5f, 0f, 0.5f, 1f, 0f, -1f, -1.5f, -0.5f),
        "bose qc ultra" to floatArrayOf(0.2f, -0.2f, -0.5f, -0.2f, 0.3f, 0.6f, 0.1f, -0.4f, -0.7f, -0.3f),
        "bose sport" to floatArrayOf(1f, 0.3f, -0.3f, -0.5f, 0.2f, 0.5f, 0.3f, -0.5f, -1f, -0.5f),
        // ═══ JBL ═══
        "jbl tune" to floatArrayOf(1f, 0f, -1f, -0.5f, 0f, 1f, 0.5f, -1f, -2f, -1.5f),
        "jbl tune 760nc" to floatArrayOf(0.8f, -0.2f, -0.8f, -0.3f, 0.2f, 0.8f, 0.3f, -0.8f, -1.5f, -1f),
        "jbl live" to floatArrayOf(0.5f, -0.3f, -0.8f, -0.3f, 0.3f, 0.8f, 0.3f, -0.8f, -1.3f, -0.8f),
        "jbl club" to floatArrayOf(0.3f, -0.3f, -0.5f, -0.2f, 0.3f, 0.5f, 0.2f, -0.5f, -1f, -0.5f),
        "jbl endurance" to floatArrayOf(1.2f, 0.3f, -0.5f, -0.5f, 0.2f, 0.8f, 0.3f, -0.8f, -1.5f, -1f),
        "jbl reflect" to floatArrayOf(1f, 0.2f, -0.5f, -0.5f, 0.2f, 0.5f, 0.3f, -0.5f, -1.2f, -0.8f),
        // ═══ Beats ═══
        "beats solo" to floatArrayOf(-2.5f, -2f, -0.5f, 0.5f, 1f, 1.5f, 1f, 0f, -0.5f, -0.5f),
        "beats studio" to floatArrayOf(-2f, -1.5f, -0.5f, 0.5f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "beats studio buds" to floatArrayOf(-1.5f, -1f, -0.3f, 0.3f, 0.5f, 0.8f, 0.3f, -0.5f, -0.8f, -0.3f),
        "beats fit pro" to floatArrayOf(-1f, -0.5f, -0.2f, 0.2f, 0.3f, 0.5f, 0.2f, -0.5f, -0.5f, -0.3f),
        "beats studio pro" to floatArrayOf(-1.5f, -1f, -0.3f, 0.3f, 0.3f, 0.8f, 0.3f, -0.3f, -0.5f, -0.3f),
        // ═══ Google ═══
        "pixel buds" to floatArrayOf(1.5f, 0.5f, 0f, -0.5f, 0f, 0.5f, 0.5f, -0.5f, -1f, -0.5f),
        "pixel buds pro" to floatArrayOf(0.8f, 0f, -0.5f, -0.3f, 0.3f, 0.5f, 0.3f, -0.3f, -0.8f, -0.3f),
        "pixel buds a-series" to floatArrayOf(1.2f, 0.3f, -0.2f, -0.5f, 0.2f, 0.5f, 0.3f, -0.5f, -1f, -0.5f),
        // ═══ Nothing ═══
        "nothing ear" to floatArrayOf(1f, 0f, -0.5f, -0.5f, 0f, 1f, 0.5f, -1f, -1.5f, -1f),
        "nothing ear 2" to floatArrayOf(0.8f, -0.2f, -0.5f, -0.3f, 0.2f, 0.8f, 0.3f, -0.5f, -1f, -0.5f),
        "nothing ear (a)" to floatArrayOf(1f, 0.2f, -0.3f, -0.5f, 0.2f, 0.8f, 0.5f, -0.5f, -1.2f, -0.8f),
        // ═══ Xiaomi ═══
        "xiaomi buds" to floatArrayOf(1.5f, 0.5f, -0.5f, -1f, 0f, 0.5f, 0.5f, -1f, -1.5f, -1f),
        "xiaomi buds 4 pro" to floatArrayOf(0.8f, 0f, -0.5f, -0.5f, 0.2f, 0.5f, 0.3f, -0.5f, -1f, -0.5f),
        "redmi buds" to floatArrayOf(1.5f, 0.5f, -0.5f, -0.8f, 0f, 0.5f, 0.5f, -0.8f, -1.5f, -1f),
        // ═══ Huawei ═══
        "huawei freebuds" to floatArrayOf(1f, 0.5f, 0f, -0.5f, 0f, 0.5f, 0.5f, -0.5f, -1.5f, -1f),
        "huawei freebuds pro" to floatArrayOf(0.5f, 0f, -0.5f, -0.3f, 0.3f, 0.5f, 0.3f, -0.5f, -1f, -0.5f),
        "huawei freebuds pro 3" to floatArrayOf(0.3f, -0.2f, -0.5f, -0.2f, 0.3f, 0.5f, 0.2f, -0.3f, -0.8f, -0.3f),
        // ═══ OnePlus ═══
        "oneplus buds" to floatArrayOf(1.2f, 0.3f, -0.5f, -0.5f, 0.2f, 0.5f, 0.5f, -0.5f, -1.2f, -0.8f),
        "oneplus buds pro" to floatArrayOf(0.8f, 0f, -0.5f, -0.3f, 0.3f, 0.5f, 0.3f, -0.5f, -1f, -0.5f),
        // ═══ Hifiman ═══
        "hifiman sundara" to floatArrayOf(1f, 0.5f, 0f, -0.5f, -0.3f, 0.3f, 0.5f, 0f, -0.5f, -0.3f),
        "hifiman he400se" to floatArrayOf(1.5f, 0.8f, 0f, -0.5f, -0.3f, 0.5f, 0.5f, 0f, -0.8f, -0.5f),
        "hifiman ananda" to floatArrayOf(0.5f, 0.3f, 0f, -0.3f, -0.2f, 0.2f, 0.3f, 0.2f, -0.3f, -0.2f),
        // ═══ Moondrop ═══
        "moondrop" to floatArrayOf(0f, -0.5f, -0.5f, 0f, 0.5f, 0.5f, 0f, 0.5f, 0f, 0f),
        "moondrop aria" to floatArrayOf(0.2f, -0.3f, -0.3f, 0f, 0.3f, 0.5f, 0.2f, 0.3f, -0.2f, -0.2f),
        "moondrop starfield" to floatArrayOf(0f, -0.3f, -0.3f, 0.2f, 0.3f, 0.5f, 0.2f, 0.3f, -0.3f, -0.2f),
        "moondrop chu" to floatArrayOf(0.3f, -0.2f, -0.3f, 0f, 0.3f, 0.5f, 0.2f, 0.2f, -0.3f, -0.2f),
        // ═══ KZ ═══
        "kz" to floatArrayOf(-1f, -0.5f, 0f, 0.5f, 0.5f, 0f, -1f, -2f, -2.5f, -2f),
        "kz zsn pro" to floatArrayOf(-0.8f, -0.3f, 0f, 0.3f, 0.3f, 0f, -0.8f, -1.5f, -2f, -1.5f),
        "kz zex pro" to floatArrayOf(-0.5f, -0.2f, 0f, 0.3f, 0.3f, 0.2f, -0.5f, -1.2f, -1.8f, -1.2f),
        // ═══ Skullcandy ═══
        "skullcandy crusher" to floatArrayOf(-3f, -2.5f, -1f, 0f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "skullcandy indy" to floatArrayOf(1f, 0f, -0.5f, -0.5f, 0f, 0.8f, 0.3f, -0.8f, -1.5f, -1f),
        // ═══ Jabra ═══
        "jabra elite" to floatArrayOf(0.5f, -0.2f, -0.5f, -0.3f, 0.3f, 0.5f, 0.3f, -0.5f, -1f, -0.5f),
        "jabra elite 85t" to floatArrayOf(0.3f, -0.3f, -0.5f, -0.2f, 0.3f, 0.5f, 0.2f, -0.5f, -0.8f, -0.3f),
        "jabra elite 7" to floatArrayOf(0.5f, -0.2f, -0.5f, -0.2f, 0.3f, 0.5f, 0.3f, -0.3f, -0.8f, -0.3f),
        // ═══ Poco F5 speaker fallback ═══
        "poco" to floatArrayOf(3f, 2.5f, 1.5f, 0.5f, 0f, 0f, -0.5f, -1f, -0.5f, 0f)
    )

    fun findProfile(deviceName: String): HeadphoneProfile? {
        val name = deviceName.lowercase().trim()
        if (name.isBlank()) return null

        // Exact substring match
        profiles.entries.firstOrNull { name.contains(it.key) }?.let {
            return HeadphoneProfile(it.key, it.value, matchConfidence = 0.95f, source = "built-in-exact")
        }
        // Reverse match: profile key contains device name words
        profiles.entries.firstOrNull { entry ->
            val deviceWords = name.split(Regex("[\\s\\-_]+")).filter { it.length > 2 }
            deviceWords.count { word -> entry.key.contains(word) } >= 2
        }?.let {
            return HeadphoneProfile(it.key, it.value, matchConfidence = 0.75f, source = "built-in-multi")
        }
        // Single keyword fuzzy
        profiles.entries.firstOrNull { entry ->
            entry.key.split(" ").any { word -> word.length > 3 && name.contains(word) }
        }?.let {
            return HeadphoneProfile(it.key, it.value, matchConfidence = 0.45f, source = "built-in-fuzzy")
        }
        return null
    }

    /**
     * Extended search: built-in DB first, then Wavelet DB (5621 profiles).
     * Call this from AutoEqManager for comprehensive matching.
     */
    fun findProfileExtended(deviceName: String, context: android.content.Context): HeadphoneProfile? {
        // Try built-in first (curated, higher quality)
        findProfile(deviceName)?.let { return it }
        // Fallback to Wavelet DB (5621 profiles)
        return WaveletAutoEqLoader.findProfile(deviceName, context)
    }

    fun allProfileNames(): List<String> = profiles.keys.toList().sorted()
    fun getProfileByName(name: String): HeadphoneProfile? {
        val key = name.lowercase().trim()
        return profiles[key]?.let { HeadphoneProfile(key, it, 1f, "manual") }
    }
    fun profileCount(): Int = profiles.size
}
