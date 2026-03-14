package com.sonara.app.autoeq

object AutoEqDatabase {
    private val profiles = mapOf(
        "airpods" to floatArrayOf(1.5f, 0.5f, -0.5f, -1f, 0f, 1f, 0.5f, -1f, -2f, -1.5f),
        "airpods pro" to floatArrayOf(1f, 0f, -1f, -0.5f, 0.5f, 1.5f, 0f, -1.5f, -1f, -0.5f),
        "airpods max" to floatArrayOf(0.5f, -0.5f, -1f, 0f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "galaxy buds" to floatArrayOf(2f, 1f, 0f, -1f, -0.5f, 0.5f, 1f, 0f, -1.5f, -1f),
        "galaxy buds pro" to floatArrayOf(1.5f, 0.5f, -0.5f, -1f, 0f, 1f, 0.5f, -1f, -1.5f, -0.5f),
        "galaxy buds2" to floatArrayOf(1.5f, 0.5f, 0f, -1f, -0.5f, 0.5f, 1f, -0.5f, -1.5f, -1f),
        "sony wh-1000xm4" to floatArrayOf(0.5f, -0.5f, -1.5f, -0.5f, 1f, 1.5f, 0.5f, -1f, -1.5f, -1f),
        "sony wh-1000xm5" to floatArrayOf(0f, -0.5f, -1f, -0.5f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "sony wf-1000xm4" to floatArrayOf(1f, 0f, -1f, -0.5f, 0.5f, 1.5f, 0.5f, -1f, -1.5f, -0.5f),
        "sony wf-1000xm5" to floatArrayOf(0.5f, -0.5f, -1f, 0f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "sennheiser hd 600" to floatArrayOf(1f, 0.5f, 0f, -0.5f, -1f, 0f, 1f, 1.5f, 0f, -0.5f),
        "sennheiser hd 650" to floatArrayOf(0.5f, 0f, -0.5f, -0.5f, -0.5f, 0.5f, 1f, 1.5f, 0.5f, 0f),
        "beyerdynamic dt 770" to floatArrayOf(-1f, -0.5f, 0.5f, 0.5f, 0f, -0.5f, -1.5f, -2f, -3f, -2.5f),
        "beyerdynamic dt 990" to floatArrayOf(0f, 0f, 0.5f, 0.5f, 0f, -1f, -2f, -3.5f, -4f, -3f),
        "audio-technica m50x" to floatArrayOf(-1f, -1.5f, 0f, 0.5f, 0f, 0.5f, -0.5f, -2f, -1.5f, -1f),
        "jbl tune" to floatArrayOf(1f, 0f, -1f, -0.5f, 0f, 1f, 0.5f, -1f, -2f, -1.5f),
        "bose qc" to floatArrayOf(0.5f, 0f, -0.5f, -0.5f, 0f, 0.5f, 0f, -0.5f, -1f, -0.5f),
        "bose 700" to floatArrayOf(0f, -0.5f, -0.5f, 0f, 0.5f, 1f, 0f, -1f, -1.5f, -0.5f),
        "beats solo" to floatArrayOf(-2.5f, -2f, -0.5f, 0.5f, 1f, 1.5f, 1f, 0f, -0.5f, -0.5f),
        "beats studio" to floatArrayOf(-2f, -1.5f, -0.5f, 0.5f, 0.5f, 1f, 0.5f, -0.5f, -1f, -0.5f),
        "pixel buds" to floatArrayOf(1.5f, 0.5f, 0f, -0.5f, 0f, 0.5f, 0.5f, -0.5f, -1f, -0.5f),
        "nothing ear" to floatArrayOf(1f, 0f, -0.5f, -0.5f, 0f, 1f, 0.5f, -1f, -1.5f, -1f),
        "moondrop" to floatArrayOf(0f, -0.5f, -0.5f, 0f, 0.5f, 0.5f, 0f, 0.5f, 0f, 0f),
        "kz" to floatArrayOf(-1f, -0.5f, 0f, 0.5f, 0.5f, 0f, -1f, -2f, -2.5f, -2f),
        "xiaomi buds" to floatArrayOf(1.5f, 0.5f, -0.5f, -1f, 0f, 0.5f, 0.5f, -1f, -1.5f, -1f),
        "huawei freebuds" to floatArrayOf(1f, 0.5f, 0f, -0.5f, 0f, 0.5f, 0.5f, -0.5f, -1.5f, -1f)
    )

    fun findProfile(deviceName: String): HeadphoneProfile? {
        val name = deviceName.lowercase().trim()
        profiles.entries.firstOrNull { name.contains(it.key) }?.let {
            return HeadphoneProfile(it.key, it.value, matchConfidence = 0.9f, source = "built-in")
        }
        val fuzzy = profiles.entries.firstOrNull { entry ->
            entry.key.split(" ").any { word -> word.length > 2 && name.contains(word) }
        }
        fuzzy?.let {
            return HeadphoneProfile(it.key, it.value, matchConfidence = 0.5f, source = "fuzzy-match")
        }
        return null
    }

    fun allProfileNames(): List<String> = profiles.keys.toList().sorted()
}
