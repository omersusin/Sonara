package com.sonara.app.intelligence.artist

object ArtistNameParser {

    private val SPLIT_PATTERNS = listOf(
        " featuring ", " feat. ", " feat ", " ft. ", " ft ",
        " & ", " and ", " ve ", " x ", " vs. ", " vs ",
        " with ", " + ", ", "
    )

    fun resolve(rawArtist: String): List<String> {
        if (rawArtist.isBlank()) return emptyList()

        for (pattern in SPLIT_PATTERNS) {
            val parts = rawArtist.split(Regex(Regex.escape(pattern), RegexOption.IGNORE_CASE))
                .map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size > 1) return parts
        }

        // "(feat. Artist)" parantez içi
        val featInParen = Regex("""\((?:feat\.|ft\.|featuring)\s+(.+?)\)""", RegexOption.IGNORE_CASE)
        featInParen.find(rawArtist)?.let { match ->
            val main = rawArtist.substring(0, match.range.first).trim()
            val featured = match.groupValues[1].trim()
            if (main.isNotBlank() && featured.isNotBlank()) return listOf(main, featured)
        }

        return listOf(rawArtist.trim())
    }

    /** "Zedd & Matthew Koma" → "Zedd · Matthew Koma" (NowPlayingBar için) */
    fun formatForDisplay(rawArtist: String): String =
        resolve(rawArtist).joinToString(" · ")
}
