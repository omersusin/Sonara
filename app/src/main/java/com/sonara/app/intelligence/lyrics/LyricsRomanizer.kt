package com.sonara.app.intelligence.lyrics

import com.sonara.app.data.SonaraLogger

/**
 * LyricsRomanizer — lightweight Romanization helper for Japanese and Korean text.
 *
 * Uses ICU4J transliteration when available on the device (API 24+).
 * Falls back to returning the original text unchanged so lyrics still display
 * even without transliteration support.
 *
 * Usage:
 *   val romanized = LyricsRomanizer.romanize(lines)
 */
object LyricsRomanizer {

    private const val TAG = "LyricsRomanizer"

    /**
     * Returns a list of romanized strings parallel to [lines].
     * Lines that are already in Latin script are returned unchanged.
     * Returns null if romanization is unavailable or all conversions fail.
     */
    fun romanize(lines: List<String>): List<String>? {
        if (lines.isEmpty()) return null
        return try {
            lines.map { line -> romanizeLine(line) }
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Romanization failed: ${e.message}")
            null
        }
    }

    /**
     * Romanizes a single line of text.
     * Detects Japanese/Korean characters and attempts ICU4J transliteration.
     */
    fun romanizeLine(text: String): String {
        if (text.isBlank()) return text
        if (!needsRomanization(text)) return text
        return try {
            // Use Android's built-in ICU transliteration via reflection
            val transClass = Class.forName("android.icu.text.Transliterator")
            val getInstance = transClass.getMethod("getInstance", String::class.java)

            // Try Japanese first (Hiragana/Katakana → Latin), then Korean (Hangul → Latin)
            val rule = when {
                containsJapanese(text) -> "Hiragana-Latin; Katakana-Latin; Han-Latin"
                containsKorean(text) -> "Hangul-Latin"
                else -> "Any-Latin"
            }

            val trans = getInstance.invoke(null, rule)
            val transMethod = transClass.getMethod("transliterate", String::class.java)
            transMethod.invoke(trans, text) as? String ?: text
        } catch (_: Exception) {
            // ICU4J not available or transliteration failed — return original
            text
        }
    }

    private fun needsRomanization(text: String): Boolean {
        return text.any { c ->
            Character.UnicodeBlock.of(c) in setOf(
                Character.UnicodeBlock.HIRAGANA,
                Character.UnicodeBlock.KATAKANA,
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
                Character.UnicodeBlock.HANGUL_SYLLABLES,
                Character.UnicodeBlock.HANGUL_JAMO,
                Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
            )
        }
    }

    private fun containsJapanese(text: String): Boolean {
        return text.any { c ->
            Character.UnicodeBlock.of(c) in setOf(
                Character.UnicodeBlock.HIRAGANA,
                Character.UnicodeBlock.KATAKANA
            )
        }
    }

    private fun containsKorean(text: String): Boolean {
        return text.any { c ->
            Character.UnicodeBlock.of(c) in setOf(
                Character.UnicodeBlock.HANGUL_SYLLABLES,
                Character.UnicodeBlock.HANGUL_JAMO,
                Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
            )
        }
    }
}
