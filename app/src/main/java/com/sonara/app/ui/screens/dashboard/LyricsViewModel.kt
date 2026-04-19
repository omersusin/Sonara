package com.sonara.app.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.intelligence.lyrics.LrcLibClient
import com.sonara.app.intelligence.lyrics.LrcParser
import com.sonara.app.intelligence.lyrics.LyricsCacheEntity
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
            val cached = dao.getByKey(key)
            if (cached != null) {
                _state.value = buildState(cached.syncedLyrics, cached.plainLyrics)
                return@launch
            }

            val result = LrcLibClient.getLyrics(title, artist, album, (durationMs / 1000L).toInt())
            if (result != null) {
                dao.insert(LyricsCacheEntity(
                    cacheKey = key,
                    syncedLyrics = result.syncedLyrics,
                    plainLyrics = result.plainLyrics
                ))
                _state.value = buildState(result.syncedLyrics, result.plainLyrics)
            } else {
                _state.value = LyricsState.NotFound
            }
        }
    }

    fun reset() {
        loadedKey = ""
        _state.value = LyricsState.Idle
    }

    private fun buildState(syncedLrc: String?, plain: String?): LyricsState {
        if (syncedLrc != null) {
            val parsed = LrcParser.parse(syncedLrc)
            if (parsed.lines.isNotEmpty()) return LyricsState.Ready(parsed, plain)
        }
        return if (plain != null) LyricsState.Ready(ParsedLyrics(emptyList(), false), plain)
        else LyricsState.NotFound
    }
}
