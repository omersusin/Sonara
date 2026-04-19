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
 *   3. KuGou      — Mandarin / Cantonese specialization
 *   4. YouLyPlus  — multi-server fallback
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
        durationMs: Long = 0L
    ): LyricsResult? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null

        val key = cacheKey(title, artist)
        memCache.get(key)?.let { return@withContext it }

        // 1. LrcLib — best coverage worldwide, returns LRC + optional plain text
        tryLrcLib(title, artist, album, durationMs)?.let {
            memCache.put(key, it); return@withContext it
        }

        // 2. BetterLyrics — Apple Music TTML with word-level sync
        tryProvider("BetterLyrics") {
            BetterLyricsClient.getRawLyrics(title, artist)?.let { raw ->
                val p = parseRaw(raw)
                if (p.lines.isNotEmpty()) LyricsResult(p, raw, null, "betterlyrics") else null
            }
        }?.let { memCache.put(key, it); return@withContext it }

        // 3. KuGou — Chinese lyrics specialization
        tryProvider("KuGou") {
            KuGouClient.getRawLyrics(title, artist)?.let { raw ->
                val p = LrcParser.parse(raw)
                if (p.lines.isNotEmpty()) LyricsResult(p, raw, null, "kugou") else null
            }
        }?.let { memCache.put(key, it); return@withContext it }

        // 4. YouLyPlus — multi-server fallback
        tryProvider("YouLyPlus") {
            YouLyPlusClient.getRawLyrics(title, artist)?.let { raw ->
                val p = LrcParser.parse(raw)
                if (p.lines.isNotEmpty()) LyricsResult(p, raw, null, "youlyplus") else null
            }
        }?.let { memCache.put(key, it); return@withContext it }

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

    fun invalidate(title: String, artist: String) {
        memCache.remove(cacheKey(title, artist))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
            if (res.plainLyrics != null) {
                return LyricsResult(ParsedLyrics(emptyList(), false), null, res.plainLyrics, "lrclib")
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

    private fun cacheKey(title: String, artist: String) =
        "${title.trim().lowercase()}|${artist.trim().lowercase()}"
}
