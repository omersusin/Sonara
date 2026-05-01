package com.sonara.app.intelligence.lyrics

import javax.xml.parsers.DocumentBuilderFactory

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
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val doc = factory.newDocumentBuilder().parse(ttml.byteInputStream())
            val lines = mutableListOf<LyricLine>()
            val allElements = doc.getElementsByTagName("*")
            for (i in 0 until allElements.length) {
                val el = allElements.item(i) as? org.w3c.dom.Element ?: continue
                if (!el.tagName.endsWith("p", ignoreCase = true)) continue
                val begin = el.getAttribute("begin").takeIf { it.isNotEmpty() } ?: continue
                val startMs = parseTtmlTs(begin) ?: continue
                val agent = (0 until el.attributes.length)
                    .map { el.attributes.item(it) }
                    .firstOrNull { it.nodeName.endsWith("agent", ignoreCase = true) }
                    ?.nodeValue?.takeIf { it.isNotEmpty() }
                val role = el.getAttribute("role")
                val isBg = role == "x-bg" || role == "x-bgf"
                parseParagraphDom(el, startMs, agent, isBg)?.let { lines.add(it) }
            }
            val hasWordTs = lines.any { it.words.isNotEmpty() }
            ParsedLyrics(lines.sortedBy { it.startMs }, hasWordTs)
        } catch (e: Exception) {
            com.sonara.app.data.SonaraLogger.e("TTMLParser", "Parse failed: ${e.message}")
            ParsedLyrics(emptyList(), false)
        }
    }

    private fun parseParagraphDom(el: org.w3c.dom.Element, startMs: Long, agent: String?, isBg: Boolean): LyricLine? {
        val words = mutableListOf<LyricWord>()
        val textBuf = StringBuilder()
        val spans = el.getElementsByTagName("*")
        for (i in 0 until spans.length) {
            val span = spans.item(i) as? org.w3c.dom.Element ?: continue
            if (!span.tagName.endsWith("span", ignoreCase = true)) continue
            val spanBegin = span.getAttribute("begin").takeIf { it.isNotEmpty() } ?: continue
            val spanEnd = span.getAttribute("end").takeIf { it.isNotEmpty() }
            val spanText = span.textContent?.trim() ?: continue
            if (spanText.isBlank()) continue
            val wStart = parseTtmlTs(spanBegin) ?: 0L
            val wEnd = spanEnd?.let { parseTtmlTs(it) } ?: -1L
            words.add(LyricWord(spanText, wStart, wEnd))
            textBuf.append(spanText)
        }
        if (words.isEmpty()) {
            val text = el.textContent?.trim() ?: return null
            if (text.isBlank()) return null
            return LyricLine(startMs, text, emptyList(), agent, isBg)
        }
        val text = textBuf.toString().trim()
        if (text.isEmpty()) return null
        return LyricLine(startMs, text, words, agent, isBg)
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
