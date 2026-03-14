package com.sonara.app.intelligence

import com.sonara.app.data.models.TrackInfo
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.intelligence.lastfm.LastFmResolver
import com.sonara.app.intelligence.local.LocalAudioAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ResolveSource { NONE, CACHE, LASTFM, LASTFM_ARTIST, LOCAL_AI }

data class ResolveResult(
    val trackInfo: TrackInfo = TrackInfo(),
    val source: ResolveSource = ResolveSource.NONE,
    val isResolving: Boolean = false,
    val error: String? = null
)

class TrackResolver(
    private val lastFmResolver: LastFmResolver,
    private val localAnalyzer: LocalAudioAnalyzer,
    private val trackCache: TrackCache
) {
    private val _result = MutableStateFlow(ResolveResult())
    val result: StateFlow<ResolveResult> = _result.asStateFlow()

    private var lastResolvedKey = ""

    suspend fun resolve(title: String, artist: String, apiKey: String) {
        val key = "$title::$artist".lowercase()
        if (key == lastResolvedKey) return
        if (title.isBlank()) {
            _result.value = ResolveResult()
            lastResolvedKey = ""
            return
        }

        lastResolvedKey = key
        _result.value = ResolveResult(isResolving = true)

        // 1. Cache
        val cached = trackCache.get(title, artist)
        if (cached != null) {
            _result.value = ResolveResult(
                trackInfo = cached,
                source = when {
                    cached.source.contains("lastfm-artist") -> ResolveSource.LASTFM_ARTIST
                    cached.source.contains("lastfm") -> ResolveSource.LASTFM
                    cached.source.contains("local") -> ResolveSource.LOCAL_AI
                    else -> ResolveSource.CACHE
                }
            )
            return
        }

        // 2. Last.fm
        if (apiKey.isNotBlank()) {
            try {
                val lastFmResult = lastFmResolver.resolve(title, artist, apiKey)
                if (lastFmResult != null && lastFmResult.genre != "other") {
                    trackCache.put(lastFmResult)
                    _result.value = ResolveResult(
                        trackInfo = lastFmResult,
                        source = if (lastFmResult.source.contains("artist")) ResolveSource.LASTFM_ARTIST else ResolveSource.LASTFM
                    )
                    return
                }
            } catch (e: Exception) {
                // Fall through to local
            }
        }

        // 3. Local AI
        val localResult = localAnalyzer.analyze(title, artist)
        trackCache.put(localResult)
        _result.value = ResolveResult(trackInfo = localResult, source = ResolveSource.LOCAL_AI)
    }

    fun clear() {
        _result.value = ResolveResult()
        lastResolvedKey = ""
    }
}
