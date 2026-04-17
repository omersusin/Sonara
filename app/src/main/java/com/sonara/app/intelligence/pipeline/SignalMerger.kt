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

package com.sonara.app.intelligence.pipeline

object SignalMerger {
    data class LastFmSignal(
        val genre: Genre, val subGenre: String = "",
        val mood: Mood?, val energy: Float?,
        val tags: List<String>
    )

    fun merge(lastFm: LastFmSignal?, local: com.sonara.app.intelligence.classifier.ClassificationResult?): SonaraPrediction {
        if (lastFm == null && local == null) return fallback()
        if (lastFm == null && local != null) return fromLocal(local)
        if (lastFm != null && local == null) return fromLastFm(lastFm)
        return mergeSignals(lastFm!!, local!!)
    }

    private fun fromLocal(l: com.sonara.app.intelligence.classifier.ClassificationResult) =
        SonaraPrediction(l.genre, null, l.mood, l.energy, l.confidence * 0.9f, PredictionSource.LOCAL_CLASSIFIER, l.mediaType, l.reasoning)

    private fun fromLastFm(l: LastFmSignal) =
        SonaraPrediction(l.genre, l.subGenre.takeIf { it.isNotBlank() }, l.mood ?: Mood.NEUTRAL,
            l.energy ?: 0.5f, 0.90f, PredictionSource.LASTFM, MediaType.MUSIC,
            listOf("Last.fm: ${l.tags.take(5).joinToString()}"),
            tags = l.tags)

    private fun mergeSignals(lfm: LastFmSignal, local: com.sonara.app.intelligence.classifier.ClassificationResult): SonaraPrediction {
        val genre = if (lfm.genre == local.genre) lfm.genre else lfm.genre
        val conf = if (lfm.genre == local.genre) (0.85f + local.confidence).coerceAtMost(0.98f) else 0.85f * 0.85f
        return SonaraPrediction(genre, lfm.subGenre.takeIf { it.isNotBlank() },
            lfm.mood ?: local.mood, lfm.energy ?: local.energy, conf,
            PredictionSource.MERGED, local.mediaType,
            local.reasoning + "Last.fm: ${lfm.tags.take(5).joinToString()}",
            tags = lfm.tags)
    }

    private fun fallback() = SonaraPrediction(Genre.UNKNOWN, null, Mood.NEUTRAL, 0.5f, 0.05f, PredictionSource.FALLBACK, MediaType.UNKNOWN, listOf("No signal"))
}
