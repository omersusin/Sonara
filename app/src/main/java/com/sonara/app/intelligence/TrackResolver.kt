package com.sonara.app.intelligence

import android.util.Log
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
    val isResolving: Boolean = false
)

class TrackResolver(
    private val lastFmResolver: LastFmResolver,
    private val localAnalyzer: LocalAudioAnalyzer,
    private val trackCache: TrackCache
) {
    private val _result = MutableStateFlow(ResolveResult())
    val result: StateFlow<ResolveResult> = _result.asStateFlow()

    private var lastKey = ""

    suspend fun resolve(title: String, artist: String, apiKey: String) {
        val key = "${title.lowercase().trim()}::${artist.lowercase().trim()}"
        if (key == lastKey && _result.value.source != ResolveSource.NONE) return
        if (title.isBlank()) { _result.value = ResolveResult(); lastKey = ""; return }

        lastKey = key
        _result.value = ResolveResult(isResolving = true)
        Log.d("TrackResolver", "Resolving: $title - $artist")

        // 1. Cache
        val cached = trackCache.get(title, artist)
        if (cached != null) {
            Log.d("TrackResolver", "Cache hit: ${cached.genre}")
            val src = when {
                cached.source.contains("lastfm-artist") -> ResolveSource.LASTFM_ARTIST
                cached.source.contains("lastfm") -> ResolveSource.LASTFM
                cached.source.contains("local") -> ResolveSource.LOCAL_AI
                else -> ResolveSource.CACHE
            }
            _result.value = ResolveResult(cached, src)
            return
        }

        // 2. Last.fm
        if (apiKey.isNotBlank()) {
            try {
                val lfm = lastFmResolver.resolve(title, artist, apiKey)
                if (lfm != null && lfm.genre != "other" && lfm.genre.isNotBlank()) {
                    Log.d("TrackResolver", "Last.fm: ${lfm.genre} conf=${lfm.confidence}")
                    trackCache.put(lfm)
                    val src = if (lfm.source.contains("artist")) ResolveSource.LASTFM_ARTIST else ResolveSource.LASTFM
                    _result.value = ResolveResult(lfm, src)
                    return
                }
            } catch (e: Exception) {
                Log.w("TrackResolver", "Last.fm failed: ${e.message}")
            }
        }

        // 3. Local AI
        val local = localAnalyzer.analyze(title, artist)
        Log.d("TrackResolver", "Local AI: ${local.genre}")
        trackCache.put(local)
        _result.value = ResolveResult(local, ResolveSource.LOCAL_AI)
    }

    fun forceReResolve() { lastKey = "" }
}
