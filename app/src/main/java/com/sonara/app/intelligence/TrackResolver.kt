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

data class ResolveResult(val trackInfo: TrackInfo = TrackInfo(), val source: ResolveSource = ResolveSource.NONE, val isResolving: Boolean = false)

class TrackResolver(private val lastFmResolver: LastFmResolver, private val localAnalyzer: LocalAudioAnalyzer, private val trackCache: TrackCache) {
    private val _result = MutableStateFlow(ResolveResult())
    val result: StateFlow<ResolveResult> = _result.asStateFlow()
    private var lastKey = ""

    suspend fun resolve(title: String, artist: String, apiKey: String) {
        val key = "${title.lowercase().trim()}::${artist.lowercase().trim()}"
        if (key == lastKey && _result.value.source != ResolveSource.NONE) return
        if (title.isBlank()) { _result.value = ResolveResult(); lastKey = ""; return }
        lastKey = key; _result.value = ResolveResult(isResolving = true)
        Log.d("TrackResolver", "Resolving: $title - $artist")

        // 1. Cache — skip "other" with low confidence
        val cached = trackCache.get(title, artist)
        if (cached != null && cached.genre != "other" && cached.genre.isNotBlank()) {
            Log.d("TrackResolver", "Cache hit: ${cached.genre}")
            val src = when { cached.source.contains("lastfm-artist") -> ResolveSource.LASTFM_ARTIST; cached.source.contains("lastfm") -> ResolveSource.LASTFM; cached.source.contains("local") -> ResolveSource.LOCAL_AI; else -> ResolveSource.CACHE }
            _result.value = ResolveResult(cached, src); return
        }

        // 2. Last.fm
        var lfmResult: TrackInfo? = null
        if (apiKey.isNotBlank()) {
            try {
                lfmResult = lastFmResolver.resolve(title, artist, apiKey)
                if (lfmResult != null && lfmResult.genre != "other" && lfmResult.genre.isNotBlank()) {
                    Log.d("TrackResolver", "Last.fm: ${lfmResult.genre} conf=${lfmResult.confidence}")
                    trackCache.put(lfmResult)
                    _result.value = ResolveResult(lfmResult, if (lfmResult.source.contains("artist")) ResolveSource.LASTFM_ARTIST else ResolveSource.LASTFM); return
                }
            } catch (e: Exception) { Log.w("TrackResolver", "Last.fm failed: ${e.message}") }
        }

        // 3. Local AI
        val local = localAnalyzer.analyze(title, artist)
        Log.d("TrackResolver", "Local AI: ${local.genre} conf=${local.confidence}")

        val finalResult = if (local.genre != "other" && local.confidence >= 0.3f) local
            else if (lfmResult != null) lfmResult.copy(source = "lastfm-local-fallback")
            else local

        // Don't cache low-confidence "other"
        if (finalResult.genre != "other" || finalResult.confidence >= 0.4f) trackCache.put(finalResult)
        else Log.d("TrackResolver", "Skip cache for low-conf other")

        _result.value = ResolveResult(finalResult, if (finalResult.source.contains("lastfm")) ResolveSource.LASTFM else ResolveSource.LOCAL_AI)
    }

    fun forceReResolve() { lastKey = "" }
}
