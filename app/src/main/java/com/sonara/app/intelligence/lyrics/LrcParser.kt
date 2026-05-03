package com.sonara.app.intelligence.lyrics

/**
 * LRC parser — Standard, Enhanced (word-level), and vivi-music agent/background formats.
 *
 * Standard:   [mm:ss.xx] Lyric line
 * Enhanced:   [mm:ss.xx]<mm:ss.xx>Word <mm:ss.xx>Word2
 * Agent:      [mm:ss.xx]{agent:v1}<mm:ss.xx>Word ...
 * Background: [mm:ss.xx]{bg}Background vocal
 */
object LrcParser {

    private val LINE_TS    = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)$""")
    private val WORD_TS    = Regex("""<(\d{2}):(\d{2})\.(\d{2,3})>([^<]*)""")
    private val AGENT_RE   = Regex("""^\{agent:([^}]+)\}""")
    private val HTML_ENTS  = mapOf(
        "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
        "&quot;" to "\"", "&apos;" to "'"
    )

    fun parse(raw: String): ParsedLyrics {
        if (raw.isBlank()) return ParsedLyrics(emptyList(), false)

        val lines = mutableListOf<LyricLine>()
        var hasWordTimestamps = false

        for (rawLine in raw.lines()) {
            val lineMatch = LINE_TS.matchEntire(rawLine.trim()) ?: continue
            val lineMs = timestampToMs(
                lineMatch.groupValues[1].toInt(),
                lineMatch.groupValues[2].toInt(),
                lineMatch.groupValues[3]
            )
            var content = lineMatch.groupValues[4]

            // Agent marker: {agent:v1}
            var agent: String? = null
            val agentMatch = AGENT_RE.find(content)
            if (agentMatch != null) {
                agent = agentMatch.groupValues[1]
                content = content.removePrefix(agentMatch.value)
            }

            // Background vocal marker: {bg}
            val isBackground = content.startsWith("{bg}")
            if (isBackground) content = content.removePrefix("{bg}")

            val wordMatches = WORD_TS.findAll(content).toList()
            if (wordMatches.isNotEmpty()) {
                hasWordTimestamps = true
                val words = wordMatches.mapIndexed { idx, m ->
                    val wordMs = timestampToMs(
                        m.groupValues[1].toInt(),
                        m.groupValues[2].toInt(),
                        m.groupValues[3]
                    )
                    // endMs = next word's startMs (or -1 for last word)
                    val endMs = if (idx + 1 < wordMatches.size) {
                        timestampToMs(
                            wordMatches[idx + 1].groupValues[1].toInt(),
                            wordMatches[idx + 1].groupValues[2].toInt(),
                            wordMatches[idx + 1].groupValues[3]
                        )
                    } else -1L
                    LyricWord(text = decodeHtml(m.groupValues[4]), startMs = wordMs, endMs = endMs)
                }
                lines.add(LyricLine(
                    startMs = lineMs,
                    text = decodeHtml(words.joinToString("") { it.text }),
                    words = words,
                    agent = agent,
                    isBackground = isBackground
                ))
            } else {
                val text = decodeHtml(content.trim())
                // Keep blank lines — they represent instrumental gaps in LRC files.
                // SyncedLyricLine checks line.text.isBlank() to show dots.
                lines.add(LyricLine(
                    startMs = lineMs, text = text, words = emptyList(),
                    agent = agent, isBackground = isBackground
                ))
            }
        }

        return ParsedLyrics(lines.sortedBy { it.startMs }, hasWordTimestamps)
    }

    private fun decodeHtml(s: String): String {
        var result = s
        HTML_ENTS.forEach { (entity, char) -> result = result.replace(entity, char) }
        return result
    }

    private fun timestampToMs(min: Int, sec: Int, centStr: String): Long {
        val cents = centStr.padEnd(3, '0').take(3).toLong()
        return min * 60_000L + sec * 1_000L + cents
    }

    /**
     * Returns the index of the currently active lyric line for [positionMs].
     * Uses a 300 ms forward buffer (same as vivi-music) so the highlighted line
     * appears just before the audio reaches it, compensating for render latency.
     * Returns -1 when playback is before the first lyric; lastIndex when past all.
     */
    fun activeLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        val target = positionMs + 300L
        var low = 0; var high = lines.lastIndex
        while (low <= high) {
            val mid = (low + high).ushr(1)
            if (lines[mid].startMs < target) low = mid + 1 else high = mid - 1
        }
        return high.coerceIn(0, lines.lastIndex)
    }

    /** Active word index within a line for given position */
    fun activeWordIndex(line: LyricLine, positionMs: Long): Int {
        if (line.words.isEmpty()) return -1
        var result = 0
        for (i in line.words.indices) {
            if (line.words[i].startMs <= positionMs) result = i else break
        }
        return result
    }
}

data class ParsedLyrics(
    val lines: List<LyricLine>,
    val hasWordTimestamps: Boolean
)

data class LyricLine(
    val startMs: Long,
    val text: String,
    val words: List<LyricWord>,
    val agent: String? = null,
    val isBackground: Boolean = false
)

data class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long = -1L
)
