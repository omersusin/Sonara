package com.sonara.app.intelligence.lyrics

sealed class LyricsState {
    object Idle : LyricsState()
    object Loading : LyricsState()
    data class Ready(val lyrics: ParsedLyrics, val plain: String?) : LyricsState()
    object NotFound : LyricsState()
    data class Error(val message: String) : LyricsState()
}
