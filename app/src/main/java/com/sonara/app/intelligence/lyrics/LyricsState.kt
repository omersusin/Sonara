package com.sonara.app.intelligence.lyrics

sealed class LyricsState {
    object Idle : LyricsState()
    object Loading : LyricsState()
    data class Ready(
        val lyrics: ParsedLyrics,
        val plain: String?,
        val translatedLines: List<String>? = null,
        val translationLanguage: String? = null
    ) : LyricsState()
    object NotFound : LyricsState()
    data class Error(val message: String) : LyricsState()
}
