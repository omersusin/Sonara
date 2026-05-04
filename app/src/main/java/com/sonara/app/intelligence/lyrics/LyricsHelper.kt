package com.sonara.app.intelligence.lyrics

import android.util.LruCache
import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Priority-based lyrics orchestrator with in-memory LRU cache.
 *
 * Provider order (first non-empty result wins):
 *   1. LrcLib     — broadest coverage, synced + plain
 *   2. BetterLyrics — Apple Music TTML (word-level when available)
 *   3. SimpMusic  — YouTube Music TTML/LRC
 *   4. KuGou      — Mandarin / Cantonese specialization
 *   5. YouLyPlus  — multi-server fallback
 *
 * Raw format strings (LRC or TTML) are preserved inside [LyricsResult.rawSynced]
 * so the calling layer can persist them to Room and re-parse with full fidelity.
 */
object LyricsHelper {

    private const val TAG = "LyricsHelper"

    data class LyricsResult(
        val parsed: ParsedLyrics,
        val rawSynced: String? = null,
        val plain: String? = null,
        val provider: String
    )

    private val memCache = LruCache<String, LyricsResult>(6)

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun getLyrics(
        title: String,
        artist: String,
        album: String = "",
        durationMs: Long = 0L,
        preferredProvider: String = "lrclib",
        onProviderTrying: ((String) -> Unit)? = null
    ): LyricsResult? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null

        val key = cacheKey(title, artist, album)
        memCache.get(key)?.let { return@withContext it }

        data class ProviderDef(val name: String, val id: String, val fetch: suspend () -> LyricsResult?)

        val allProviders = listOf(
            ProviderDef("LrcLib", "lrclib") { tryLrcLib(title, artist, album, durationMs) },
            ProviderDef("BetterLyrics", "betterlyrics") {
                BetterLyricsClient.getRawLyrics(title, artist)?.let { raw ->
                    val p = parseRaw(raw)
                    if (p.lines.isNotEmpty() && isMeaningfulLyrics(raw)) LyricsResult(p, raw, null, "betterlyrics") else null
                }
            },
            ProviderDef("SimpMusic", "simpmusic") {
                SimpMusicClient.getRawLyrics(title, artist)?.let { raw ->
                    val p = parseRaw(raw)
                    if (p.lines.isNotEmpty() && isMeaningfulLyrics(raw)) LyricsResult(p, raw, null, "simpmusic") else null
                }
            },
            ProviderDef("KuGou", "kugou") {
                KuGouClient.getRawLyrics(title, artist)?.let { raw ->
                    val p = LrcParser.parse(raw)
                    if (p.lines.isNotEmpty() && isMeaningfulLyrics(raw)) LyricsResult(p, raw, null, "kugou") else null
                }
            },
            ProviderDef("YouLyPlus", "youlyplus") {
                YouLyPlusClient.getRawLyrics(title, artist)?.let { raw ->
                    val p = LrcParser.parse(raw)
                    if (p.lines.isNotEmpty() && isMeaningfulLyrics(raw)) LyricsResult(p, raw, null, "youlyplus") else null
                }
            }
        )

        val preferred = allProviders.firstOrNull { it.id == preferredProvider }
        val ordered = if (preferred != null)
            listOf(preferred) + allProviders.filter { it.id != preferredProvider }
        else allProviders

        for (provider in ordered) {
            onProviderTrying?.invoke(provider.name)
            tryProvider(provider.name) { provider.fetch() }
                ?.let { memCache.put(key, it); return@withContext it }
        }

        // No provider found synced lyrics — fall back to LrcLib plain text if available
        try {
            val res = LrcLibClient.getLyrics(title, artist, album, (durationMs / 1000L).toInt())
            if (res?.plainLyrics != null) {
                val fallback = LyricsResult(ParsedLyrics(emptyList(), false), null, res.plainLyrics, "lrclib-plain")
                memCache.put(key, fallback)
                return@withContext fallback
            }
        } catch (_: Exception) {}

        null
    }

    /** Parses a stored raw string (auto-detects TTML vs LRC). */
    fun parseRaw(raw: String): ParsedLyrics {
        val trimmed = raw.trimStart()
        return if (trimmed.startsWith("<?xml") || trimmed.startsWith("<tt")) {
            TTMLParser.parse(raw)
        } else {
            LrcParser.parse(raw)
        }
    }

    fun invalidate(title: String, artist: String, album: String = "") {
        memCache.remove(cacheKey(title, artist, album))
        // Also remove legacy key (without album) for backward-compat cache hits
        memCache.remove(cacheKey(title, artist, ""))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the raw lyrics string contains meaningful visible text.
     * Handles both LRC timestamps and TTML/XML tags so that empty TTML shells
     * (provider returns a valid document with no actual words) are correctly
     * rejected rather than accepted as "lyrics found".
     */
    private fun isMeaningfulLyrics(raw: String): Boolean {
        val invisibleRe = Regex("""[​‌‍⁠­]""")
        val timestampRe = Regex("""\[\d{1,2}:\d{2}(?:\.\d{1,3})?]""")
        val ttmlTagRe   = Regex("""<[^>]+>""")
        val normalized  = raw.replace("﻿", "").replace(invisibleRe, "").trim()
        if (normalized.isEmpty()) return false
        val noTs   = timestampRe.replace(normalized, "")
        val noTags = ttmlTagRe.replace(noTs, "").replace(invisibleRe, "").trim()
        return noTags.any { !it.isWhitespace() && it != ' ' }
    }

    private suspend fun tryLrcLib(
        title: String,
        artist: String,
        album: String,
        durationMs: Long
    ): LyricsResult? {
        return try {
            val res = LrcLibClient.getLyrics(title, artist, album, (durationMs / 1000L).toInt())
                ?: return null
            if (res.syncedLyrics != null) {
                val p = LrcParser.parse(res.syncedLyrics)
                if (p.lines.isNotEmpty()) {
                    return LyricsResult(p, res.syncedLyrics, res.plainLyrics, "lrclib")
                }
            }
            null
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "LrcLib failed: ${e.message}")
            null
        }
    }

    private suspend fun tryProvider(
        name: String,
        block: suspend () -> LyricsResult?
    ): LyricsResult? {
        return try {
            block()
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "$name failed: ${e.message}")
            null
        }
    }

    private fun cacheKey(title: String, artist: String, album: String = "") =
        "${title.trim().lowercase()}|${artist.trim().lowercase()}|${album.trim().lowercase()}"
}
