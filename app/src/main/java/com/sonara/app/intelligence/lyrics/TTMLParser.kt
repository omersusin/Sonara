package com.sonara.app.intelligence.lyrics

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * TTML parser for Apple Music word-level lyrics.
 *
 * Supports:
 *   - <p begin="HH:MM:SS.mmm" ttm:agent="v1"> line containers
 *   - <span begin="..." end="..."> word-level timestamps
 *   - role="x-bg" / role="x-bgf" for background vocals
 *   - ttm:agent for multi-singer alignment (v1=left, v2=right, v1000=center)
 */
object TTMLParser {

    fun parse(ttml: String): ParsedLyrics {
        if (ttml.isBlank()) return ParsedLyrics(emptyList(), false)
        return try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(StringReader(ttml))
            }
            val lines = mutableListOf<LyricLine>()
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val localName = parser.name.substringAfterLast(':')
                    if (localName == "p") {
                        parseParagraph(parser)?.let { lines.add(it) }
                    }
                }
                eventType = parser.next()
            }
            val hasWordTs = lines.any { it.words.isNotEmpty() }
            ParsedLyrics(lines.sortedBy { it.startMs }, hasWordTs)
        } catch (_: Exception) {
            ParsedLyrics(emptyList(), false)
        }
    }

    private fun parseParagraph(parser: XmlPullParser): LyricLine? {
        val begin = parser.getAttributeValue(null, "begin") ?: return null
        val startMs = parseTtmlTs(begin) ?: return null

        // agent: prefer "ttm:agent", fall back to bare "agent"
        val agent = (0 until parser.attributeCount)
            .firstOrNull { parser.getAttributeName(it).endsWith("agent") }
            ?.let { parser.getAttributeValue(it) }

        val role = parser.getAttributeValue(null, "role") ?: ""
        val isBackground = role == "x-bg" || role == "x-bgf"

        val words = mutableListOf<LyricWord>()
        val textBuf = StringBuilder()

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name.substringAfterLast(':') == "p")) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.substringAfterLast(':') == "span") {
                        val spanBegin = parser.getAttributeValue(null, "begin")
                        val spanEnd   = parser.getAttributeValue(null, "end")
                        val spanText  = collectSpanText(parser)
                        if (spanBegin != null && spanText.isNotBlank()) {
                            val wStart = parseTtmlTs(spanBegin) ?: 0L
                            val wEnd   = spanEnd?.let { parseTtmlTs(it) } ?: -1L
                            words.add(LyricWord(spanText, wStart, wEnd))
                            textBuf.append(spanText)
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val t = parser.text?.trim() ?: ""
                    if (t.isNotEmpty() && words.isEmpty()) textBuf.append(t)
                }
            }
            event = parser.next()
        }

        val text = textBuf.toString().trim()
        if (text.isEmpty()) return null
        return LyricLine(startMs, text, words, agent, isBackground)
    }

    /** Collects all text inside a <span> element (handles nested spans). */
    private fun collectSpanText(parser: XmlPullParser): String {
        val buf = StringBuilder()
        var depth = 1
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name.substringAfterLast(':') == "span" && depth == 1)) {
            when (event) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG   -> depth--
                XmlPullParser.TEXT      -> buf.append(parser.text ?: "")
            }
            if (depth <= 0) break
            event = parser.next()
        }
        return buf.toString()
    }

    /**
     * Parses TTML timestamps in multiple formats:
     *   HH:MM:SS.mmm  →  3 parts
     *   MM:SS.mmm     →  2 parts
     *   SS.mmms       →  1 part (trailing 's' allowed)
     */
    private fun parseTtmlTs(ts: String): Long? {
        return try {
            val clean = ts.trimEnd('s')
            val parts = clean.split(":")
            when (parts.size) {
                3 -> {
                    val h = parts[0].toLong()
                    val m = parts[1].toLong()
                    val s = parts[2].toDouble()
                    h * 3_600_000L + m * 60_000L + (s * 1000).toLong()
                }
                2 -> {
                    val m = parts[0].toLong()
                    val s = parts[1].toDouble()
                    m * 60_000L + (s * 1000).toLong()
                }
                1 -> (clean.toDouble() * 1000).toLong()
                else -> null
            }
        } catch (_: Exception) { null }
    }
}
