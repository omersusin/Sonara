package com.sonara.app.intelligence.pipeline

/**
 * Madde 3: Source string üretimi tek bir mapper'da olmalı.
 * ViewModel'de değil, burada üretilir.
 */
object PredictionSourceMapper {

    data class SourceDisplay(
        val primary: String,
        val detail: String,
        val contributors: List<String>
    )

    fun map(prediction: SonaraPrediction, hasLyrics: Boolean = false): SourceDisplay {
        val contributors = mutableListOf<String>()

        when (prediction.source) {
            PredictionSource.LASTFM -> {
                contributors.add("Last.fm")
                if (hasLyrics) contributors.add("Lyrics")
            }
            PredictionSource.LOCAL_CLASSIFIER -> {
                contributors.add("Local AI")
                if (hasLyrics) contributors.add("Lyrics")
            }
            PredictionSource.MERGED -> {
                contributors.add("Last.fm")
                contributors.add("Local AI")
                if (hasLyrics) contributors.add("Lyrics")
            }
            PredictionSource.ADAPTIVE_OVERRIDE -> {
                contributors.add("Learned")
            }
            PredictionSource.USER_PRESET -> {
                contributors.add("User Preset")
            }
            PredictionSource.CACHE -> {
                contributors.add("Cache")
            }
            PredictionSource.FALLBACK -> {
                contributors.add("Fallback")
            }
        }

        val primary = when {
            contributors.size >= 2 -> "Merged"
            contributors.size == 1 -> contributors.first()
            else -> "Unknown"
        }

        val detail = if (contributors.size > 1) {
            contributors.joinToString(" + ")
        } else {
            ""
        }

        return SourceDisplay(primary, detail, contributors)
    }
}
