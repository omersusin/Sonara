package com.sonara.app.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.intelligence.lyrics.LrcParser
import com.sonara.app.intelligence.lyrics.LyricsCacheEntity
import com.sonara.app.intelligence.lyrics.LyricsHelper
import com.sonara.app.intelligence.lyrics.LyricsState
import com.sonara.app.intelligence.lyrics.ParsedLyrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LyricsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = SonaraDatabase.get(application).lyricsCacheDao()

    private val _state = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val state: StateFlow<LyricsState> = _state.asStateFlow()

    private var loadedKey = ""

    fun load(title: String, artist: String, album: String, durationMs: Long) {
        if (title.isBlank()) { _state.value = LyricsState.Idle; return }
        val key = "${title.trim().lowercase()}|${artist.trim().lowercase()}"
        if (key == loadedKey) return
        loadedKey = key
        _state.value = LyricsState.Loading

        viewModelScope.launch {
            // Check DB cache first — preserves raw LRC/TTML for full-fidelity re-parse
            val cached = dao.getByKey(key)
            if (cached != null) {
                _state.value = buildStateFromCache(cached.syncedLyrics, cached.plainLyrics)
                return@launch
            }

            // Fetch via priority chain: LrcLib → BetterLyrics → KuGou → YouLyPlus
            val result = LyricsHelper.getLyrics(title, artist, album, durationMs)
            if (result != null) {
                dao.insert(LyricsCacheEntity(
                    cacheKey = key,
                    syncedLyrics = result.rawSynced,
                    plainLyrics = result.plain,
                    source = result.provider
                ))
                _state.value = if (result.parsed.lines.isNotEmpty()) {
                    LyricsState.Ready(result.parsed, result.plain)
                } else if (result.plain != null) {
                    LyricsState.Ready(ParsedLyrics(emptyList(), false), result.plain)
                } else {
                    LyricsState.NotFound
                }
            } else {
                _state.value = LyricsState.NotFound
            }
        }
    }

    fun reset() {
        loadedKey = ""
        _state.value = LyricsState.Idle
    }

    /**
     * Re-parses a cached raw string — auto-detects TTML vs LRC so that
     * word-level Apple Music lyrics survive a cache round-trip.
     */
    private fun buildStateFromCache(rawSynced: String?, plain: String?): LyricsState {
        if (rawSynced != null) {
            val parsed = LyricsHelper.parseRaw(rawSynced)
            if (parsed.lines.isNotEmpty()) return LyricsState.Ready(parsed, plain)
        }
        return if (plain != null) LyricsState.Ready(ParsedLyrics(emptyList(), false), plain)
        else LyricsState.NotFound
    }
}
