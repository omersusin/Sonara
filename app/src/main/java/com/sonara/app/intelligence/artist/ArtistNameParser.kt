package com.sonara.app.intelligence.artist

object ArtistNameParser {

    private val SEPARATORS = listOf(
        " featuring ", " feat. ", " feat ", " ft. ", " ft ",
        " & ", " x ", " X ", " vs. ", " vs ", " with ", " + ", ", ", ","
    )

    // " and " is intentionally excluded — too many false positives with band names
    // (e.g. "Florence and the Machine", "Of Monsters and Men")

    /**
     * Applies all separators in sequence so compound strings like
     * "Sarah de Warren, CIRCA96 & Craig Connelly" → ["Sarah de Warren", "CIRCA96", "Craig Connelly"]
     */
    fun resolve(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        var tokens = listOf(raw.trim())
        for (sep in SEPARATORS) {
            tokens = tokens.flatMap { it.split(sep) }
        }
        return tokens.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    /** "Zedd & Matthew Koma" → "Zedd · Matthew Koma" (for NowPlayingBar display) */
    fun formatForDisplay(raw: String): String =
        resolve(raw).joinToString(" · ")
}
