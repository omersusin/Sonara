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

package com.sonara.app.ai.enrichment

import android.util.Log
import kotlinx.coroutines.*

class EnrichmentManager(
    private val lastFm: LastFmEnricher?,
    private val lyrics: LyricsEnricher = LyricsEnricher(),
    private val apiAi: ApiAiEnricher?
) {
    companion object { private const val TAG = "SonaraEnrich"; private const val TIMEOUT_MS = 10000L }

    suspend fun enrichAll(title: String, artist: String, lyricsText: String? = null): EnrichmentBundle = coroutineScope {
        val startTime = System.currentTimeMillis()
        val lfm = async { withTimeoutOrNull(TIMEOUT_MS) { lastFm?.enrich(title, artist) } ?: EnrichmentSignal.empty("lastfm") }
        val lyr = async { try { lyrics.analyze(lyricsText) } catch (_: Exception) { EnrichmentSignal.empty("lyrics") } }
        val api = async { withTimeoutOrNull(TIMEOUT_MS) { apiAi?.enrich(title, artist) } ?: EnrichmentSignal.empty("api_ai") }
        val lr = try { lfm.await() } catch (_: Exception) { EnrichmentSignal.empty("lastfm") }
        val ly = try { lyr.await() } catch (_: Exception) { EnrichmentSignal.empty("lyrics") }
        val ar = try { api.await() } catch (_: Exception) { EnrichmentSignal.empty("api_ai") }
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Done in ${elapsed}ms (lastfm=${lr.isValid}, lyrics=${ly.isValid}, api=${ar.isValid})")
        EnrichmentBundle(lastFm = lr, lyrics = ly, apiAi = ar, fetchTimeMs = elapsed)
    }
}
