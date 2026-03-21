package com.sonara.app.intelligence.pipeline

/**
 * Madde 4: Track başlığı normalize eden utility.
 * (feat. ...), (live), (remaster), (sped up), (slowed), - Topic gibi
 * gereksiz suffix'leri temizler.
 */
object TitleNormalizer {

    private val PATTERNS = listOf(
        // feat / ft variations
        Regex("""\s*[\(\[]\s*(?:feat|ft|featuring)\.?\s+[^\)\]]+[\)\]]""", RegexOption.IGNORE_CASE),
        // "- Topic" (YouTube music auto-generated)
        Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE),
        // Live, remaster, remix markers (in parens/brackets)
        Regex("""\s*[\(\[]\s*(?:live|remaster(?:ed)?|deluxe|bonus track|explicit|clean|radio edit|acoustic version|instrumental|karaoke|demo|original mix)\s*(?:\d{4})?\s*[\)\]]""", RegexOption.IGNORE_CASE),
        // Speed modifications
        Regex("""\s*[\(\[]\s*(?:sped up|slowed|speed up|slowed \+ reverb|reverb|8d audio|nightcore|daycore)\s*[\)\]]""", RegexOption.IGNORE_CASE),
        // version markers
        Regex("""\s*[\(\[]\s*(?:version|ver\.|edit|mix|extended|short|radio|official|audio|video|lyric|official music video|official video|mv)\s*[\)\]]""", RegexOption.IGNORE_CASE),
        // trailing whitespace
        Regex("""\s+$""")
    )

    private val ARTIST_PATTERNS = listOf(
        Regex("""\s*[\(\[]\s*(?:feat|ft|featuring)\.?\s+[^\)\]]+[\)\]]""", RegexOption.IGNORE_CASE),
        Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE),
        Regex("""\s*VEVO$""", RegexOption.IGNORE_CASE),
        Regex("""\s*-\s*Official$""", RegexOption.IGNORE_CASE),
    )

    fun normalizeTitle(raw: String): String {
        var result = raw.trim()
        for (pattern in PATTERNS) {
            result = pattern.replace(result, "")
        }
        return result.trim()
    }

    fun normalizeArtist(raw: String): String {
        var result = raw.trim()
        for (pattern in ARTIST_PATTERNS) {
            result = pattern.replace(result, "")
        }
        return result.trim()
    }

    /**
     * Canonical key oluştur (cache + match için).
     * Küçük harf, trim, normalize edilmiş.
     */
    fun canonicalKey(title: String, artist: String): String {
        val nt = normalizeTitle(title).lowercase().trim()
        val na = normalizeArtist(artist).lowercase().trim()
        return "$na::$nt"
    }
}
