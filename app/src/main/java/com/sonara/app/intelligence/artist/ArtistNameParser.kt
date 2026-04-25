package com.sonara.app.intelligence.artist

object ArtistNameParser {

    // Ordered by specificity — longer/more specific patterns first
    private val SPLIT_PATTERNS = listOf(
        " featuring ", " feat. ", " feat ", " ft. ", " ft ",
        " & ", " x ", " vs. ", " vs ", " with ", " + ", ", "
    )

    // " and " is intentionally excluded — too many false positives with band names
    // (e.g. "Florence and the Machine", "Of Monsters and Men")

    fun resolve(rawArtist: String): List<String> {
        if (rawArtist.isBlank()) return emptyList()

        // Check parenthetical feat first (most specific)
        val featInParen = Regex("""\((?:feat\.|ft\.|featuring)\s+(.+?)\)""", RegexOption.IGNORE_CASE)
        featInParen.find(rawArtist)?.let { match ->
            val main = rawArtist.substring(0, match.range.first).trim()
            val featured = match.groupValues[1].trim()
            if (main.isNotBlank() && featured.isNotBlank()) {
                return listOf(main) + resolveInner(featured)
            }
        }

        for (pattern in SPLIT_PATTERNS) {
            val parts = rawArtist.split(Regex(Regex.escape(pattern), RegexOption.IGNORE_CASE))
                .map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size > 1) return parts
        }

        return listOf(rawArtist.trim())
    }

    // Recursively resolve the featured-artist substring (may itself contain " & " etc.)
    private fun resolveInner(raw: String): List<String> {
        for (pattern in SPLIT_PATTERNS) {
            val parts = raw.split(Regex(Regex.escape(pattern), RegexOption.IGNORE_CASE))
                .map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size > 1) return parts
        }
        return listOf(raw.trim())
    }

    /** "Zedd & Matthew Koma" → "Zedd · Matthew Koma" (NowPlayingBar için) */
    fun formatForDisplay(rawArtist: String): String =
        resolve(rawArtist).joinToString(" · ")
}
