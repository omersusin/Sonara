package com.sonara.app.intelligence.lyrics

/**
 * LRC parser — Standard ve Enhanced (word-level) formatları destekler.
 *
 * Standard:  [mm:ss.xx] Lyric line
 * Enhanced:  [mm:ss.xx]<mm:ss.xx>Word <mm:ss.xx>Word2
 */
object LrcParser {

    private val LINE_TS = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)$""")
    private val WORD_TS = Regex("""<(\d{2}):(\d{2})\.(\d{2,3})>([^<]*)""")

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
            val content = lineMatch.groupValues[4]

            val wordMatches = WORD_TS.findAll(content).toList()
            if (wordMatches.isNotEmpty()) {
                hasWordTimestamps = true
                val words = wordMatches.map { m ->
                    val wordMs = timestampToMs(
                        m.groupValues[1].toInt(),
                        m.groupValues[2].toInt(),
                        m.groupValues[3]
                    )
                    LyricWord(text = m.groupValues[4], startMs = wordMs)
                }
                lines.add(LyricLine(startMs = lineMs, text = words.joinToString("") { it.text }, words = words))
            } else {
                val text = content.trim()
                if (text.isNotEmpty()) lines.add(LyricLine(startMs = lineMs, text = text, words = emptyList()))
            }
        }

        return ParsedLyrics(lines.sortedBy { it.startMs }, hasWordTimestamps)
    }

    private fun timestampToMs(min: Int, sec: Int, centStr: String): Long {
        val cents = centStr.padEnd(3, '0').take(3).toLong()
        return min * 60_000L + sec * 1_000L + cents
    }

    /** Active line index for given playback position */
    fun activeLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var result = 0
        for (i in lines.indices) {
            if (lines[i].startMs <= positionMs) result = i else break
        }
        return result
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
    val words: List<LyricWord>
)

data class LyricWord(
    val text: String,
    val startMs: Long
)
