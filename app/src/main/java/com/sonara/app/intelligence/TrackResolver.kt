/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sonara.app.intelligence

import android.util.Log
import com.sonara.app.data.SonaraLogger
import com.sonara.app.data.models.TrackInfo
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.intelligence.lastfm.LastFmResolver
import com.sonara.app.intelligence.local.AudioContext
import com.sonara.app.intelligence.local.LocalInferencePlugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ResolveSource { NONE, CACHE, LASTFM, LASTFM_ARTIST, LOCAL_AI }

data class ResolveResult(
    val trackInfo: TrackInfo = TrackInfo(),
    val source: ResolveSource = ResolveSource.NONE,
    val isResolving: Boolean = false,
    val pluginUsed: String = ""
)

class TrackResolver(
    private val lastFmResolver: LastFmResolver,
    private val localPlugins: List<LocalInferencePlugin>,
    private val trackCache: TrackCache
) {
    private val TAG = "TrackResolver"
    private val _result = MutableStateFlow(ResolveResult())
    val result: StateFlow<ResolveResult> = _result.asStateFlow()
    private var lastKey = ""
    private var currentJob: Job? = null

    suspend fun resolve(title: String, artist: String, apiKey: String, audioContext: AudioContext? = null) {
        val key = "${title.lowercase().trim()}::${artist.lowercase().trim()}"
        if (key == lastKey && _result.value.source != ResolveSource.NONE) return
        if (title.isBlank()) { _result.value = ResolveResult(); lastKey = ""; return }

        lastKey = key
        _result.value = ResolveResult(isResolving = true)
        SonaraLogger.ai( "═══ Resolving: $title - $artist ═══")

        // ── 1. Cache (skip stale "other") ──
        val cached = trackCache.get(title, artist)
        if (cached != null && cached.genre != "other" && cached.genre.isNotBlank()) {
            SonaraLogger.ai( "✓ Cache hit: ${cached.genre} (conf=${cached.confidence})")
            val src = when {
                cached.source.contains("lastfm-artist") -> ResolveSource.LASTFM_ARTIST
                cached.source.contains("lastfm") -> ResolveSource.LASTFM
                else -> ResolveSource.CACHE
            }
            _result.value = ResolveResult(cached, src, pluginUsed = "cache")
            return
        }

        // ── 2. Last.fm ──
        var lastFmResult: TrackInfo? = null
        if (apiKey.isNotBlank()) {
            try {
                lastFmResult = lastFmResolver.resolve(title, artist, apiKey)
                if (lastFmResult != null && lastFmResult.genre != "other" && lastFmResult.genre.isNotBlank()) {
                    SonaraLogger.ai( "✓ Last.fm: ${lastFmResult.genre} (conf=${lastFmResult.confidence})")
                    trackCache.put(lastFmResult)
                    val src = if (lastFmResult.source.contains("artist")) ResolveSource.LASTFM_ARTIST else ResolveSource.LASTFM
                    _result.value = ResolveResult(lastFmResult, src, pluginUsed = "lastfm")
                    return
                } else {
                    SonaraLogger.ai( "✗ Last.fm returned 'other' or empty")
                }
            } catch (e: Exception) {
                SonaraLogger.w("Resolver", "✗ Last.fm error: ${e.message}")
            }
        } else {
            SonaraLogger.ai( "✗ No API key — skipping Last.fm")
        }

        // ── 3. Local AI plugins (sorted by priority) ──
        val sortedPlugins = localPlugins.sortedBy { it.priority }
        for (plugin in sortedPlugins) {
            try {
                SonaraLogger.ai( "→ Trying plugin: ${plugin.name}")
                val pluginResult = plugin.resolve(title, artist, audioContext)
                if (pluginResult != null && pluginResult.genre != "other" && pluginResult.confidence >= 0.25f) {
                    SonaraLogger.ai( "✓ ${plugin.name}: ${pluginResult.genre} (conf=${pluginResult.confidence})")
                    trackCache.put(pluginResult)
                    _result.value = ResolveResult(pluginResult, ResolveSource.LOCAL_AI, pluginUsed = plugin.name)
                    return
                }
            } catch (e: Exception) {
                SonaraLogger.w("Resolver", "✗ ${plugin.name} error: ${e.message}")
            }
        }

        // ── 4. Final fallback ──
        val fallbackResult = lastFmResult
            ?: localPlugins.firstOrNull()?.resolve(title, artist, audioContext)
            ?: TrackInfo(title = title, artist = artist, genre = "other", confidence = 0.1f, source = "fallback")

        SonaraLogger.ai( "⚠ Fallback: ${fallbackResult.genre}")

        // Don't cache low-confidence "other"
        if (fallbackResult.genre != "other" || fallbackResult.confidence >= 0.4f) {
            trackCache.put(fallbackResult)
        }

        _result.value = ResolveResult(fallbackResult, ResolveSource.LOCAL_AI, pluginUsed = "fallback")
    }

    fun forceReResolve() { lastKey = "" }
}
