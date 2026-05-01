package com.sonara.app.intelligence.lyrics

sealed class LyricsState {
    object Idle : LyricsState()
    data class Loading(val providerName: String = "") : LyricsState()
    data class Ready(
        val lyrics: ParsedLyrics,
        val plain: String?,
        val translatedLines: List<String>? = null,
        val translationLanguage: String? = null,
        val romanizedLines: List<String>? = null
    ) : LyricsState()
    object NotFound : LyricsState()
    data class Error(val message: String) : LyricsState()
}
