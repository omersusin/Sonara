package com.sonara.app.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.intelligence.lyrics.LrcParser
import com.sonara.app.intelligence.lyrics.LyricsCacheEntity
import com.sonara.app.intelligence.lyrics.LyricsHelper
import com.sonara.app.intelligence.lyrics.LyricsRomanizer
import com.sonara.app.intelligence.lyrics.LyricsState
import com.sonara.app.intelligence.lyrics.LyricsTranslator
import com.sonara.app.intelligence.lyrics.ParsedLyrics
import kotlinx.coroutines.cancelChildren
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

    fun load(
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        showTranslated: Boolean = false,
        targetLang: String = "en"
    ) {
        if (title.isBlank()) { _state.value = LyricsState.Idle; return }
        val key = "${title.trim().lowercase()}|${artist.trim().lowercase()}|${album.trim().lowercase()}"
        // BUG-FIX 11e: don't skip if we're in Idle or Error state (covers retry after reset)
        if (key == loadedKey && _state.value !is LyricsState.Idle && _state.value !is LyricsState.Error) return
        loadedKey = key
        _state.value = LyricsState.Loading()

        viewModelScope.launch {
            // Check DB cache first
            val cached = dao.getByKey(key)
            if (cached != null) {
                _state.value = buildStateFromCache(cached.syncedLyrics, cached.plainLyrics, showTranslated, targetLang)
                return@launch
            }

            val preferred = prefs.preferredLyricsProviderFlow.first()
            val result = LyricsHelper.getLyrics(
                title, artist, album, durationMs,
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

                val ready = when {
                    result.parsed.lines.isNotEmpty() -> LyricsState.Ready(result.parsed, result.plain)
                    result.plain != null             -> LyricsState.Ready(ParsedLyrics(emptyList(), false), result.plain)
                    else                             -> null
                }

                if (ready != null) {
                    // BUG-FIX 11b: fetch translation + romanization concurrently, emit ONCE
                    var translatedLines: List<String>? = null
                    var romanizedLines: List<String>? = null

                    if (showTranslated && targetLang.isNotBlank() && result.parsed.lines.isNotEmpty()) {
                        val geminiKey = prefs.geminiApiKeyFlow.first()
                        translatedLines = LyricsTranslator.translate(
                            lines       = result.parsed.lines.map { it.text },
                            targetLang  = targetLang,
                            geminiApiKey = geminiKey,
                            songTitle   = title,
                            artistName  = artist
                        )
                    }
                    if (result.parsed.lines.isNotEmpty()) {
                        romanizedLines = LyricsRomanizer.romanize(result.parsed.lines.map { it.text })
                    }

                    _state.value = ready.copy(
                        translatedLines     = translatedLines,
                        translationLanguage = if (translatedLines != null) targetLang else null,
                        romanizedLines      = romanizedLines
                    )
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
        viewModelScope.coroutineContext.cancelChildren() // BUG-FIX 11a: cancel in-flight coroutines
    }

    /**
     * Re-fetches lyrics with a corrected title/artist, bypassing the cache.
     */
    fun searchLyricsWithCorrection(title: String, artist: String, album: String = "") {
        if (title.isBlank()) return
        val key = "${title.trim().lowercase()}|${artist.trim().lowercase()}|${album.trim().lowercase()}"
        loadedKey = key
        _state.value = LyricsState.Loading()

        viewModelScope.launch {
            LyricsHelper.invalidate(title, artist, album)

            val result = LyricsHelper.getLyrics(title, artist, album,
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
                val ready = when {
                    result.parsed.lines.isNotEmpty() -> LyricsState.Ready(result.parsed, result.plain)
                    result.plain != null             -> LyricsState.Ready(ParsedLyrics(emptyList(), false), result.plain)
                    else                             -> null
                }
                _state.value = ready ?: LyricsState.NotFound
            } else {
                _state.value = LyricsState.NotFound
            }
        }
    }

    /**
     * Re-applies translation and romanization to already-loaded lyrics.
     * Call when the user toggles showTranslated or changes targetLang mid-session.
     */
    fun applyDisplaySettings(showTranslated: Boolean, targetLang: String, romanize: Boolean) {
        val current = _state.value as? LyricsState.Ready ?: return
        val lines = current.lyrics.lines
        if (lines.isEmpty()) return

        viewModelScope.launch {
            var updated = current

            if (showTranslated && targetLang.isNotBlank()) {
                val geminiKey = prefs.geminiApiKeyFlow.first()
                val translated = LyricsTranslator.translate(
                    lines        = lines.map { it.text },
                    targetLang   = targetLang,
                    geminiApiKey = geminiKey
                )
                updated = if (translated != null) {
                    updated.copy(translatedLines = translated, translationLanguage = targetLang)
                } else {
                    updated.copy(translatedLines = null, translationLanguage = null)
                }
            } else {
                updated = updated.copy(translatedLines = null, translationLanguage = null)
            }

            if (romanize) {
                val romanized = LyricsRomanizer.romanize(lines.map { it.text })
                if (romanized != null) updated = updated.copy(romanizedLines = romanized)
            } else {
                updated = updated.copy(romanizedLines = null)
            }

            _state.value = updated
        }
    }

    private suspend fun buildStateFromCache(
        rawSynced: String?,
        plain: String?,
        showTranslated: Boolean = false,
        targetLang: String = "en"
    ): LyricsState {
        if (rawSynced != null) {
            val parsed = LyricsHelper.parseRaw(rawSynced)
            if (parsed.lines.isNotEmpty()) {
                var ready = LyricsState.Ready(parsed, plain)
                if (showTranslated && targetLang.isNotBlank()) {
                    val geminiKey = prefs.geminiApiKeyFlow.first()
                    val translated = LyricsTranslator.translate(
                        lines        = parsed.lines.map { it.text },
                        targetLang   = targetLang,
                        geminiApiKey = geminiKey
                    )
                    if (translated != null) ready = ready.copy(translatedLines = translated, translationLanguage = targetLang)
                }
                val romanized = LyricsRomanizer.romanize(parsed.lines.map { it.text })
                return if (romanized != null) ready.copy(romanizedLines = romanized) else ready
            }
        }
        return if (plain != null) LyricsState.Ready(ParsedLyrics(emptyList(), false), plain)
        else LyricsState.NotFound
    }
}
