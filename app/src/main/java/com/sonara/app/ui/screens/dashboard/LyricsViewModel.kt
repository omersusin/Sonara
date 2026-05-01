package com.sonara.app.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.intelligence.lyrics.LrcParser
import com.sonara.app.intelligence.lyrics.LyricsCacheEntity
import com.sonara.app.intelligence.lyrics.LyricsHelper
import com.sonara.app.intelligence.lyrics.LyricsState
import com.sonara.app.intelligence.lyrics.LyricsTranslator
import com.sonara.app.intelligence.lyrics.ParsedLyrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LyricsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = SonaraDatabase.get(application).lyricsCacheDao()
    private val prefs = (application as SonaraApp).preferences

    private val _state = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val state: StateFlow<LyricsState> = _state.asStateFlow()

    private var loadedKey = ""

    fun load(title: String, artist: String, album: String, durationMs: Long, showTranslated: Boolean = false, targetLang: String = "en") {
        if (title.isBlank()) { _state.value = LyricsState.Idle; return }
        val key = "${title.trim().lowercase()}|${artist.trim().lowercase()}"
        if (key == loadedKey) return
        loadedKey = key
        _state.value = LyricsState.Idle
        _state.value = LyricsState.Loading()

        viewModelScope.launch {
            // Check DB cache first — preserves raw LRC/TTML for full-fidelity re-parse
            val cached = dao.getByKey(key)
            if (cached != null) {
                _state.value = buildStateFromCache(cached.syncedLyrics, cached.plainLyrics, showTranslated, targetLang)
                return@launch
            }

            // Fetch via priority chain (order depends on user's preferred provider)
            val preferred = prefs.preferredLyricsProviderFlow.first()
            val result = LyricsHelper.getLyrics(title, artist, album, durationMs,
                preferredProvider = preferred,
                onProviderTrying = { providerName ->
                    _state.value = LyricsState.Loading(providerName)
                }
            )
            if (result != null) {
                dao.insert(LyricsCacheEntity(
                    cacheKey = key,
                    syncedLyrics = result.rawSynced,
                    plainLyrics = result.plain,
                    source = result.provider
                ))
                val ready = if (result.parsed.lines.isNotEmpty()) {
                    LyricsState.Ready(result.parsed, result.plain)
                } else if (result.plain != null) {
                    LyricsState.Ready(ParsedLyrics(emptyList(), false), result.plain)
                } else {
                    null
                }
                if (ready != null) {
                    _state.value = ready
                    if (showTranslated && targetLang.isNotBlank() && result.parsed.lines.isNotEmpty()) {
                        val translated = LyricsTranslator.translate(result.parsed.lines.map { it.text }, targetLang)
                        if (translated != null) {
                            _state.value = ready.copy(translatedLines = translated, translationLanguage = targetLang)
                        }
                    }
                } else {
                    _state.value = LyricsState.NotFound
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
     * Re-fetches lyrics with a corrected title/artist, bypassing the cache.
     * Used by the lyrics correction dialog (CROSS-02).
     */
    fun searchLyricsWithCorrection(title: String, artist: String) {
        if (title.isBlank()) return
        val key = "${title.trim().lowercase()}|${artist.trim().lowercase()}"
        loadedKey = key
        _state.value = LyricsState.Loading()

        viewModelScope.launch {
            // Invalidate both old and new cache keys so we force a fresh fetch
            LyricsHelper.invalidate(title, artist)

            val result = LyricsHelper.getLyrics(title, artist,
                onProviderTrying = { providerName ->
                    _state.value = LyricsState.Loading(providerName)
                }
            )
            if (result != null) {
                dao.insert(LyricsCacheEntity(
                    cacheKey     = key,
                    syncedLyrics = result.rawSynced,
                    plainLyrics  = result.plain,
                    source       = result.provider
                ))
                val ready = if (result.parsed.lines.isNotEmpty()) {
                    LyricsState.Ready(result.parsed, result.plain)
                } else if (result.plain != null) {
                    LyricsState.Ready(ParsedLyrics(emptyList(), false), result.plain)
                } else null
                _state.value = ready ?: LyricsState.NotFound
            } else {
                _state.value = LyricsState.NotFound
            }
        }
    }

    /**
     * Re-parses a cached raw string — auto-detects TTML vs LRC so that
     * word-level Apple Music lyrics survive a cache round-trip.
     */
    private suspend fun buildStateFromCache(rawSynced: String?, plain: String?, showTranslated: Boolean = false, targetLang: String = "en"): LyricsState {
        if (rawSynced != null) {
            val parsed = LyricsHelper.parseRaw(rawSynced)
            if (parsed.lines.isNotEmpty()) {
                val ready = LyricsState.Ready(parsed, plain)
                if (showTranslated && targetLang.isNotBlank()) {
                    val translated = LyricsTranslator.translate(parsed.lines.map { it.text }, targetLang)
                    if (translated != null) {
                        return ready.copy(translatedLines = translated, translationLanguage = targetLang)
                    }
                }
                return ready
            }
        }
        return if (plain != null) LyricsState.Ready(ParsedLyrics(emptyList(), false), plain)
        else LyricsState.NotFound
    }
}
