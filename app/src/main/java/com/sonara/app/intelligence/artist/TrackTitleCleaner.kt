package com.sonara.app.intelligence.artist

data class CleanTrackResult(
    val cleanTitle: String,
    val featuredArtists: List<String>
)

object TrackTitleCleaner {

    // "(feat. X)" veya "[feat. X]" parantez içi
    private val FEAT_IN_PARENS = Regex(
        """\s*[\(\[]\s*(?:feat\.|ft\.|featuring|feat)\s+(.+?)[\)\]]""",
        RegexOption.IGNORE_CASE
    )

    // "Title feat. X" — parantez dışı, sonda
    private val FEAT_BARE = Regex(
        """\s+(?:feat\.|ft\.|featuring|feat)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    // Remix/Edit/Mix açıklamaları — bunları DOKUNMA (feat içermiyorsa)
    private val REMIX_TAG = Regex(
        """\((?!.*(?:feat\.|ft\.|featuring)).*(?:remix|edit|mix|version|vip|extended|radio|original)\s*\)""",
        RegexOption.IGNORE_CASE
    )

    fun clean(rawTitle: String): CleanTrackResult {
        if (rawTitle.isBlank()) return CleanTrackResult(rawTitle, emptyList())

        // Parantez içinde feat varsa
        val parenMatch = FEAT_IN_PARENS.find(rawTitle)
        if (parenMatch != null) {
            val cleanTitle = rawTitle.removeRange(parenMatch.range).trim()
            val featured = ArtistNameParser.resolve(parenMatch.groupValues[1])
            return CleanTrackResult(cleanTitle, featured)
        }

        // Parantez dışında feat varsa
        val bareMatch = FEAT_BARE.find(rawTitle)
        if (bareMatch != null) {
            val cleanTitle = rawTitle.removeRange(bareMatch.range).trim()
            val featured = ArtistNameParser.resolve(bareMatch.groupValues[1])
            return CleanTrackResult(cleanTitle, featured)
        }

        return CleanTrackResult(rawTitle.trim(), emptyList())
    }
}
