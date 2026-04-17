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

package com.sonara.app.intelligence.local

import android.util.Log
import com.sonara.app.data.SonaraLogger

/**
 * Sonara Smart Media Classifier
 * 
 * Uygulamaya özel akıllı medya sınıflandırma motoru.
 * Büyük AI modeli değil — hafif, hızlı, yerel çalışan karar algoritması.
 * 
 * Çoklu metadata sinyallerini ağırlıklı skorlama ile değerlendirir.
 * Confidence üretir. Geçmişten öğrenir.
 */

enum class SmartMediaType(val label: String, ) {
    MUSIC("Music"),
    FILM("Film"),
    SERIES("Series"),
    VIDEO("Video"),
    PODCAST("Podcast"),
    AUDIOBOOK("Audiobook"),
    GAME("Game"),
    CALL("Call"),
    UNKNOWN("Unknown")
}

data class ClassificationResult(
    val type: SmartMediaType,
    val confidence: Float,
    val scores: Map<SmartMediaType, Float>,
    val reasoning: List<String>
)

class SmartMediaClassifier {
    private val TAG = "MediaClassifier"

    // Kullanıcı davranış hafızası — local adaptation
    private val behaviorMemory = mutableMapOf<String, SmartMediaType>()

    fun classify(
        title: String,
        artist: String,
        album: String,
        packageName: String,
        duration: Long = 0
    ): ClassificationResult {
        val t = title.lowercase().trim()
        val a = artist.lowercase().trim()
        val alb = album.lowercase().trim()
        val pkg = packageName.lowercase()
        val durationMin = duration / 60000f

        val scores = mutableMapOf<SmartMediaType, Float>()
        SmartMediaType.entries.forEach { scores[it] = 0f }
        val reasons = mutableListOf<String>()

        // ══════════════════════════════════════════
        // STAGE 1: Title Pattern Analysis
        // ══════════════════════════════════════════

        // Series patterns (highest specificity)
        val seriesPatterns = listOf(
            Regex("""s\d{1,2}e\d{1,2}""", RegexOption.IGNORE_CASE),          // S01E03
            Regex("""season\s*\d+""", RegexOption.IGNORE_CASE),               // Season 2
            Regex("""episode\s*\d+""", RegexOption.IGNORE_CASE),              // Episode 5
            Regex("""sezon\s*\d+""", RegexOption.IGNORE_CASE),                // Sezon 1
            Regex("""bölüm\s*\d+""", RegexOption.IGNORE_CASE),               // Bölüm 7
            Regex("""ep\.?\s*\d+""", RegexOption.IGNORE_CASE),               // Ep 3, Ep. 12
            Regex("""chapter\s*\d+""", RegexOption.IGNORE_CASE),             // Chapter 5
            Regex("""part\s*\d+\s*(of|/)""", RegexOption.IGNORE_CASE),       // Part 3 of 10
            Regex("""\d+x\d+""", RegexOption.IGNORE_CASE)                     // 1x03
        )
        seriesPatterns.forEach { regex ->
            if (regex.containsMatchIn(t)) {
                scores[SmartMediaType.SERIES] = scores[SmartMediaType.SERIES]!! + 40f
                reasons.add("Title matches series pattern: ${regex.pattern}")
            }
        }

        // Film patterns
        val filmKeywords = mapOf(
            "1080p" to 30f, "2160p" to 30f, "4k" to 25f, "720p" to 20f,
            "bluray" to 35f, "blu-ray" to 35f, "bdrip" to 30f, "brrip" to 30f,
            "web-dl" to 30f, "webdl" to 30f, "webrip" to 25f, "web-rip" to 25f,
            "dvdrip" to 25f, "hdtv" to 20f, "hdrip" to 25f,
            "director's cut" to 35f, "directors cut" to 35f,
            "extended cut" to 30f, "extended edition" to 30f,
            "unrated" to 25f, "theatrical" to 25f,
            "x264" to 20f, "x265" to 20f, "hevc" to 20f, "aac" to 10f,
            "yify" to 30f, "rarbg" to 25f, "sparks" to 20f,
            "subtitle" to 15f, "altyazı" to 15f, "altyazılı" to 20f,
            "türkçe dublaj" to 20f, "dual audio" to 20f
        )
        filmKeywords.forEach { (kw, score) ->
            if (t.contains(kw)) {
                scores[SmartMediaType.FILM] = scores[SmartMediaType.FILM]!! + score
                reasons.add("Title contains film keyword: $kw (+$score)")
            }
        }

        // Video patterns
        val videoKeywords = mapOf(
            "official video" to 20f, "official music video" to 15f,
            "music video" to 15f, "lyric video" to 15f, "lyrics" to 10f,
            "trailer" to 25f, "teaser" to 20f,
            "gameplay" to 30f, "walkthrough" to 25f, "let's play" to 25f,
            "reaction" to 20f, "review" to 15f, "unboxing" to 20f,
            "tutorial" to 20f, "how to" to 15f, "nasıl" to 10f,
            "vlog" to 20f, "compilation" to 15f, "highlights" to 15f,
            "clip" to 10f, "shorts" to 15f, "#shorts" to 20f,
            "live stream" to 15f, "livestream" to 15f
        )
        videoKeywords.forEach { (kw, score) ->
            if (t.contains(kw)) {
                scores[SmartMediaType.VIDEO] = scores[SmartMediaType.VIDEO]!! + score
                reasons.add("Title contains video keyword: $kw (+$score)")
            }
        }

        // Music patterns
        val musicKeywords = mapOf(
            "feat." to 15f, "feat " to 15f, "ft." to 15f, "ft " to 12f,
            "remix" to 20f, "remaster" to 20f, "remastered" to 20f,
            "acoustic" to 15f, "unplugged" to 15f, "live" to 10f,
            "deluxe" to 10f, "bonus track" to 15f, "single" to 12f,
            "radio edit" to 20f, "extended mix" to 20f, "club mix" to 20f,
            "original mix" to 20f, "dj " to 12f, "prod." to 15f,
            "ost" to 10f, "soundtrack" to 10f
        )
        musicKeywords.forEach { (kw, score) ->
            if (t.contains(kw)) {
                scores[SmartMediaType.MUSIC] = scores[SmartMediaType.MUSIC]!! + score
                reasons.add("Title contains music keyword: $kw (+$score)")
            }
        }

        // Podcast patterns
        val podcastKeywords = mapOf(
            "podcast" to 40f, "episode" to 10f, "interview" to 15f,
            "konuk" to 15f, "sohbet" to 10f, "röportaj" to 15f,
            "#" to 5f // numbered episodes like "#47"
        )
        podcastKeywords.forEach { (kw, score) ->
            if (t.contains(kw)) {
                scores[SmartMediaType.PODCAST] = scores[SmartMediaType.PODCAST]!! + score
                reasons.add("Title contains podcast keyword: $kw (+$score)")
            }
        }

        // ══════════════════════════════════════════
        // STAGE 2: Artist/Album Analysis
        // ══════════════════════════════════════════

        if (a.isNotBlank() && a != "unknown" && a != "various" && a != "null") {
            scores[SmartMediaType.MUSIC] = scores[SmartMediaType.MUSIC]!! + 20f
            reasons.add("Artist field populated: '$a' (+20 music)")
        }

        if (alb.isNotBlank() && alb != "unknown" && alb != "null") {
            scores[SmartMediaType.MUSIC] = scores[SmartMediaType.MUSIC]!! + 10f
            reasons.add("Album field populated: '$alb' (+10 music)")
        }

        // ══════════════════════════════════════════
        // STAGE 3: Package Name Analysis
        // ══════════════════════════════════════════

        val packageScores = mapOf(
            // Music apps
            "spotify" to (SmartMediaType.MUSIC to 25f),
            "youtube.music" to (SmartMediaType.MUSIC to 25f),
            "apple.android.music" to (SmartMediaType.MUSIC to 25f),
            "deezer" to (SmartMediaType.MUSIC to 25f),
            "tidal" to (SmartMediaType.MUSIC to 25f),
            "soundcloud" to (SmartMediaType.MUSIC to 20f),
            "audioplayer" to (SmartMediaType.MUSIC to 20f),
            "musicplayer" to (SmartMediaType.MUSIC to 20f),
            "poweramp" to (SmartMediaType.MUSIC to 25f),
            "retro" to (SmartMediaType.MUSIC to 15f),
            "samsung.app.music" to (SmartMediaType.MUSIC to 20f),
            "miui.player" to (SmartMediaType.MUSIC to 20f),
            "anghami" to (SmartMediaType.MUSIC to 25f),

            // Video apps
            "youtube" to (SmartMediaType.VIDEO to 15f), // YouTube hem müzik hem video
            "netflix" to (SmartMediaType.FILM to 30f),
            "disney" to (SmartMediaType.FILM to 25f),
            "amazon.avod" to (SmartMediaType.FILM to 25f),
            "hbo" to (SmartMediaType.FILM to 25f),
            "paramount" to (SmartMediaType.FILM to 20f),
            "crunchyroll" to (SmartMediaType.SERIES to 20f),
            "twitch" to (SmartMediaType.VIDEO to 20f),
            "blutv" to (SmartMediaType.SERIES to 25f),
            "exxen" to (SmartMediaType.SERIES to 25f),
            "tabii" to (SmartMediaType.SERIES to 20f),
            "puhutv" to (SmartMediaType.SERIES to 20f),
            "gain" to (SmartMediaType.VIDEO to 15f),
            "mxtech" to (SmartMediaType.VIDEO to 15f),
            "vlc" to (SmartMediaType.VIDEO to 10f),

            // Podcast
            "podcast" to (SmartMediaType.PODCAST to 30f),
            "pocketcasts" to (SmartMediaType.PODCAST to 30f),
            "castbox" to (SmartMediaType.PODCAST to 25f),

            // Audiobook
            "audible" to (SmartMediaType.AUDIOBOOK to 35f),
            "books" to (SmartMediaType.AUDIOBOOK to 15f)
        )

        packageScores.forEach { (keyword, typeScore) ->
            if (pkg.contains(keyword)) {
                val (type, score) = typeScore
                scores[type] = scores[type]!! + score
                reasons.add("Package contains '$keyword' (+$score ${type.label})")
            }
        }

        // ══════════════════════════════════════════
        // STAGE 4: Duration Analysis
        // ══════════════════════════════════════════

        if (duration > 0) {
            when {
                durationMin < 1f -> {
                    scores[SmartMediaType.VIDEO] = scores[SmartMediaType.VIDEO]!! + 10f
                    reasons.add("Duration <1min → likely short video")
                }
                durationMin in 1f..6f -> {
                    scores[SmartMediaType.MUSIC] = scores[SmartMediaType.MUSIC]!! + 15f
                    reasons.add("Duration 1-6min → likely music")
                }
                durationMin in 6f..15f -> {
                    scores[SmartMediaType.VIDEO] = scores[SmartMediaType.VIDEO]!! + 10f
                    reasons.add("Duration 6-15min → could be video/long track")
                }
                durationMin in 15f..50f -> {
                    scores[SmartMediaType.SERIES] = scores[SmartMediaType.SERIES]!! + 15f
                    scores[SmartMediaType.PODCAST] = scores[SmartMediaType.PODCAST]!! + 10f
                    reasons.add("Duration 15-50min → episodic content")
                }
                durationMin in 50f..180f -> {
                    scores[SmartMediaType.FILM] = scores[SmartMediaType.FILM]!! + 20f
                    reasons.add("Duration 50-180min → likely film")
                }
                durationMin > 180f -> {
                    scores[SmartMediaType.AUDIOBOOK] = scores[SmartMediaType.AUDIOBOOK]!! + 15f
                    scores[SmartMediaType.PODCAST] = scores[SmartMediaType.PODCAST]!! + 10f
                    reasons.add("Duration >3hr → audiobook/long podcast")
                }
            }
        }

        // ══════════════════════════════════════════
        // STAGE 5: Behavior Memory (local learning)
        // ══════════════════════════════════════════

        val memoryKey = buildMemoryKey(t, pkg)
        behaviorMemory[memoryKey]?.let { remembered ->
            scores[remembered] = scores[remembered]!! + 30f
            reasons.add("Behavior memory: previously classified as ${remembered.label} (+30)")
        }

        // ══════════════════════════════════════════
        // STAGE 6: Default music bias
        // ══════════════════════════════════════════

        // If no strong signal, assume music (most common use case)
        if (scores.values.all { it < 10f }) {
            scores[SmartMediaType.MUSIC] = scores[SmartMediaType.MUSIC]!! + 10f
            reasons.add("No strong signals — default music bias (+10)")
        }

        // ══════════════════════════════════════════
        // FINAL: Calculate winner and confidence
        // ══════════════════════════════════════════

        val totalScore = scores.values.sum().coerceAtLeast(1f)
        val sorted = scores.entries.sortedByDescending { it.value }
        val winner = sorted.first()
        val runnerUp = sorted.getOrNull(1)

        val confidence = if (totalScore > 0f) {
            val winnerRatio = winner.value / totalScore
            val margin = winner.value - (runnerUp?.value ?: 0f)
            val marginBonus = (margin / totalScore).coerceAtMost(0.3f)
            (winnerRatio + marginBonus).coerceIn(0.1f, 0.99f)
        } else 0.1f

        val result = ClassificationResult(
            type = winner.key,
            confidence = confidence,
            scores = scores.toMap(),
            reasoning = reasons
        )

        SonaraLogger.ai( "═══ Classification: ${winner.key.label} (${(confidence * 100).toInt()}%) ═══")
        reasons.forEach { SonaraLogger.ai( "  → $it") }

        return result
    }

    /**
     * Kullanıcı manuel düzeltme yaptığında hafızaya al
     */
    fun recordUserCorrection(title: String, packageName: String, correctedType: SmartMediaType) {
        val key = buildMemoryKey(title.lowercase(), packageName.lowercase())
        behaviorMemory[key] = correctedType
        SonaraLogger.ai( "Behavior learned: $key → ${correctedType.label}")
    }

    private fun buildMemoryKey(title: String, pkg: String): String {
        // Normalize — aynı dizi/serinin farklı bölümleri aynı key'e düşsün
        val cleanTitle = title
            .replace(Regex("""s\d+e\d+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""episode\s*\d+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""bölüm\s*\d+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\d{3,4}p"""), "")
            .trim()
            .take(30) // Kısa tut
        return "$pkg::$cleanTitle"
    }

    fun getMemorySize(): Int = behaviorMemory.size
}
