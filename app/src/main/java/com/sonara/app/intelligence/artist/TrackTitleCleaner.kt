package com.sonara.app.intelligence.artist

data class CleanTrackResult(
    val cleanTitle: String,
    val featuredArtists: List<String>
)

object TrackTitleCleaner {

    // "(feat. X)" or "[feat. X]" — captures only what's inside the brackets
    private val FEAT_IN_PARENS = Regex(
        """\s*[\(\[]\s*(?:feat\.|ft\.|featuring|feat)\s+(.+?)[\)\]]""",
        RegexOption.IGNORE_CASE
    )

    // "Title feat. X" at the end — stops before any trailing "(" or "["
    // so "Title feat. X (Radio Edit)" captures only "X"
    private val FEAT_BARE = Regex(
        """\s+(?:feat\.|ft\.|featuring|feat)\s+([^(\[]+?)(?:\s*[\(\[].*)?$""",
        RegexOption.IGNORE_CASE
    )

    fun clean(rawTitle: String): CleanTrackResult {
        if (rawTitle.isBlank()) return CleanTrackResult(rawTitle, emptyList())

        val parenMatch = FEAT_IN_PARENS.find(rawTitle)
        if (parenMatch != null) {
            val cleanTitle = rawTitle.removeRange(parenMatch.range)
                .replace(Regex("\\s{2,}"), " ")
                .trim()
            val featured = ArtistNameParser.resolve(parenMatch.groupValues[1].trim())
            return CleanTrackResult(cleanTitle, featured)
        }

        val bareMatch = FEAT_BARE.find(rawTitle)
        if (bareMatch != null) {
            val cleanTitle = rawTitle.removeRange(bareMatch.range)
                .replace(Regex("\\s{2,}"), " ")
                .trim()
            val featured = ArtistNameParser.resolve(bareMatch.groupValues[1].trim())
            return CleanTrackResult(cleanTitle, featured)
        }

        return CleanTrackResult(rawTitle.trim(), emptyList())
    }
}
