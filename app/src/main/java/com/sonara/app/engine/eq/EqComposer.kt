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

package com.sonara.app.engine.eq

import com.sonara.app.intelligence.pipeline.*

class EqComposer {
    companion object { const val BAND_COUNT = 10 }

    // ═══ Base genre curves ═══
    private val genreEq: Map<Genre, FloatArray> = mapOf(
        Genre.POP to floatArrayOf(1.0f,1.5f,1.0f,0.0f,-0.5f,0.0f,1.0f,2.0f,2.5f,1.5f),
        Genre.ROCK to floatArrayOf(3.0f,2.5f,1.5f,0.5f,-1.0f,-0.5f,0.5f,2.0f,3.0f,2.5f),
        Genre.METAL to floatArrayOf(4.0f,3.5f,2.0f,0.0f,-2.0f,-1.5f,0.0f,2.5f,4.0f,3.5f),
        Genre.HIP_HOP to floatArrayOf(5.0f,4.5f,3.0f,1.0f,0.0f,-0.5f,0.0f,1.0f,2.0f,1.0f),
        Genre.RNB to floatArrayOf(3.5f,3.0f,2.0f,1.0f,0.0f,0.0f,0.5f,1.5f,1.5f,1.0f),
        Genre.ELECTRONIC to floatArrayOf(4.5f,4.0f,2.5f,0.0f,-1.0f,0.0f,1.0f,2.0f,3.0f,3.5f),
        Genre.DANCE to floatArrayOf(5.0f,4.5f,3.0f,0.0f,-1.5f,0.0f,1.5f,2.5f,3.0f,2.5f),
        Genre.JAZZ to floatArrayOf(1.5f,1.0f,0.5f,0.5f,0.0f,0.5f,1.0f,1.5f,1.0f,0.5f),
        Genre.BLUES to floatArrayOf(2.0f,1.5f,1.0f,0.5f,0.0f,0.0f,0.5f,1.0f,1.5f,1.0f),
        Genre.CLASSICAL to floatArrayOf(0.5f,0.5f,0.0f,0.0f,0.0f,0.0f,0.0f,0.5f,0.5f,0.5f),
        Genre.COUNTRY to floatArrayOf(2.0f,1.5f,1.0f,0.5f,0.0f,0.5f,1.0f,1.5f,2.0f,1.5f),
        Genre.FOLK to floatArrayOf(1.0f,0.5f,0.5f,0.5f,0.0f,0.5f,1.0f,1.5f,1.0f,0.5f),
        Genre.REGGAE to floatArrayOf(4.0f,3.5f,2.0f,0.5f,0.0f,-0.5f,0.0f,1.0f,1.5f,0.5f),
        Genre.LATIN to floatArrayOf(3.0f,2.5f,2.0f,1.0f,0.0f,0.0f,0.5f,1.5f,2.0f,1.5f),
        Genre.AMBIENT to floatArrayOf(2.0f,2.0f,1.5f,1.0f,0.5f,0.5f,0.0f,-0.5f,0.0f,0.5f),
        Genre.SOUL to floatArrayOf(3.0f,2.5f,1.5f,1.0f,0.0f,0.5f,1.0f,1.5f,1.0f,0.5f),
        Genre.FUNK to floatArrayOf(4.0f,3.5f,2.0f,0.5f,0.0f,0.0f,0.5f,1.5f,2.0f,1.5f),
        Genre.PUNK to floatArrayOf(2.5f,2.0f,1.5f,0.5f,-1.0f,-0.5f,0.5f,2.0f,3.0f,2.5f),
        Genre.INDIE to floatArrayOf(1.5f,1.0f,0.5f,0.5f,0.0f,0.5f,1.5f,2.0f,2.0f,1.0f),
        Genre.ALTERNATIVE to floatArrayOf(2.0f,1.5f,1.0f,0.5f,-0.5f,0.0f,1.0f,1.5f,2.0f,1.5f),
        Genre.PODCAST to floatArrayOf(-2.0f,-1.5f,-0.5f,0.0f,0.5f,2.0f,3.0f,2.5f,1.0f,-0.5f),
        Genre.AUDIOBOOK to floatArrayOf(-3.0f,-2.0f,-1.0f,0.0f,0.5f,2.5f,3.5f,3.0f,1.0f,-1.0f),
        Genre.UNKNOWN to floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f)
    )

    // ═══ SubGenre MODIFIERS — applied on top of base genre ═══
    // These capture the character difference from the parent genre
    private val subGenreMod: Map<String, FloatArray> = mapOf(
        // Pop variants                              31   62  125  250  500   1k   2k   4k   8k  16k
        "k-pop" to      floatArrayOf(0.5f, 0.5f, 0.5f, 0.0f, 0.0f, 0.5f, 1.0f, 1.5f, 0.5f, 0.0f),    // clarity + polish
        "j-pop" to      floatArrayOf(0.3f, 0.3f, 0.0f, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f, 0.5f, 0.0f),    // bright vocals
        "synth-pop" to  floatArrayOf(1.0f, 0.5f, 0.5f, 0.0f,-0.5f, 0.0f, 0.5f, 0.5f, 1.0f, 1.0f),    // synth shimmer
        "dream-pop" to  floatArrayOf(0.5f, 1.0f, 0.5f, 0.5f, 0.0f, 0.0f,-0.5f,-0.5f, 0.5f, 1.0f),    // warm dreamy
        "electropop" to floatArrayOf(1.5f, 1.0f, 0.5f, 0.0f,-0.5f, 0.0f, 0.5f, 0.5f, 1.0f, 0.5f),    // electronic pop
        "dance-pop" to  floatArrayOf(1.5f, 1.0f, 0.5f, 0.0f,-0.5f, 0.0f, 0.5f, 1.0f, 0.5f, 0.0f),    // club energy
        "indie-pop" to  floatArrayOf(0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.5f, 1.0f, 1.0f, 0.5f, 0.0f),    // organic clarity
        "hyperpop" to   floatArrayOf(2.0f, 1.5f, 0.5f, 0.0f,-1.0f, 0.0f, 1.5f, 2.0f, 1.5f, 1.0f),    // extreme
        "bedroom-pop" to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, 0.0f, 0.0f, 0.0f,-0.5f,-0.5f, 0.0f),   // lo-fi warmth
        // Electronic variants
        "house" to      floatArrayOf(1.0f, 0.5f, 0.0f, 0.0f,-0.5f, 0.0f, 0.5f, 0.5f, 0.5f, 0.0f),
        "deep-house" to floatArrayOf(1.5f, 1.0f, 0.5f, 0.5f, 0.0f, 0.0f,-0.5f,-0.5f, 0.0f, 0.0f),    // warm sub
        "techno" to     floatArrayOf(1.5f, 1.0f, 0.0f, 0.0f,-1.0f,-0.5f, 0.0f, 0.5f, 1.0f, 0.5f),    // industrial
        "trance" to     floatArrayOf(1.0f, 0.5f, 0.0f, 0.0f,-0.5f, 0.5f, 1.0f, 1.5f, 1.5f, 1.0f),    // euphoric highs
        "dubstep" to    floatArrayOf(3.0f, 2.5f, 1.0f, 0.0f,-1.5f,-1.0f, 0.0f, 1.0f, 0.5f, 0.0f),    // sub bass monster
        "dnb" to        floatArrayOf(2.0f, 1.5f, 0.5f, 0.0f,-1.0f,-0.5f, 0.5f, 1.0f, 1.0f, 0.5f),    // fast bass
        "synthwave" to  floatArrayOf(1.5f, 1.0f, 0.5f, 0.0f,-0.5f, 0.0f, 0.5f, 0.5f, 1.0f, 1.5f),    // retro shimmer
        "lo-fi" to      floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, 0.0f, 0.0f,-0.5f,-1.0f,-0.5f,-0.5f),    // warm rolled-off
        "downtempo" to  floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, 0.0f, 0.0f,-0.5f,-0.5f, 0.0f, 0.0f),    // relaxed
        "future-bass" to floatArrayOf(2.0f, 1.5f, 0.5f, 0.0f,-0.5f, 0.5f, 1.0f, 1.5f, 1.0f, 0.5f),
        "trip-hop" to   floatArrayOf(1.0f, 1.0f, 0.5f, 0.5f, 0.0f, 0.0f,-0.5f, 0.0f, 0.5f, 0.5f),    // dark mood
        // Hip-hop variants
        "trap" to       floatArrayOf(2.0f, 1.5f, 0.5f, 0.0f,-0.5f, 0.0f, 0.5f, 1.0f, 1.0f, 0.5f),    // 808 heavy
        "drill" to      floatArrayOf(1.5f, 1.0f, 0.5f, 0.0f,-0.5f,-0.5f, 0.5f, 1.5f, 1.0f, 0.5f),    // dark energy
        "boom-bap" to   floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.0f,-0.5f,-0.5f),    // classic warm
        "phonk" to      floatArrayOf(2.5f, 2.0f, 1.0f, 0.0f,-1.0f,-0.5f, 0.0f, 0.5f, 1.0f, 0.5f),    // bass + grit
        // Rock/Metal variants
        "grunge" to     floatArrayOf(0.5f, 0.5f, 0.5f, 0.0f,-0.5f,-0.5f, 0.0f, 0.5f, 0.0f,-0.5f),    // raw mid
        "shoegaze" to   floatArrayOf(0.5f, 1.0f, 0.5f, 0.5f, 0.0f, 0.0f,-0.5f, 0.0f, 1.0f, 1.5f),    // wall of sound
        "post-rock" to  floatArrayOf(0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 0.5f, 0.5f, 1.0f, 1.5f, 1.0f),    // expansive
        "metalcore" to  floatArrayOf(0.5f, 0.5f, 0.0f, 0.0f,-1.0f,-0.5f, 0.5f, 1.5f, 1.0f, 0.5f),    // scooped + bright
        "doom-metal" to floatArrayOf(1.5f, 1.0f, 0.5f, 0.5f, 0.0f,-0.5f,-1.0f,-0.5f, 0.0f, 0.0f),    // slow heavy
        "prog-metal" to floatArrayOf(0.5f, 0.5f, 0.0f, 0.0f,-0.5f, 0.0f, 0.5f, 1.0f, 0.5f, 0.5f),    // clarity + detail
        // Other
        "neo-soul" to   floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.0f,-0.5f, 0.0f),    // warm analog
        "reggaeton" to  floatArrayOf(1.5f, 1.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.5f, 1.0f, 0.5f, 0.0f),    // dembow bass
        "bossa-nova" to floatArrayOf(-0.5f, 0.0f, 0.0f, 0.5f, 0.5f, 0.5f, 0.0f, 0.0f,-0.5f,-0.5f)    // intimate
    )

    // ═══ Tag-based micro adjustments ═══
    // Applied per-tag found in Last.fm tags — small deltas that refine character
    private val tagDelta: Map<String, FloatArray> = mapOf(
        "bright" to     floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0.3f, 0.5f, 0.3f, 0.3f),
        "dark" to       floatArrayOf(0.3f, 0.5f, 0.3f, 0f, 0f,-0.3f,-0.3f,-0.5f,-0.3f,-0.3f),
        "heavy" to      floatArrayOf(0.5f, 0.5f, 0.3f, 0f,-0.3f, 0f, 0f, 0.3f, 0.3f, 0f),
        "mellow" to     floatArrayOf(0f, 0f, 0f, 0.3f, 0f, 0f,-0.3f,-0.3f,-0.3f,-0.3f),
        "catchy" to     floatArrayOf(0f, 0f, 0f, 0f, 0.3f, 0.3f, 0.5f, 0.3f, 0f, 0f),
        "atmospheric" to floatArrayOf(0.3f, 0.3f, 0.3f, 0.3f, 0f, 0f,-0.3f, 0f, 0.3f, 0.5f),
        "groovy" to     floatArrayOf(0.3f, 0.5f, 0.3f, 0f, 0f, 0f, 0.3f, 0f, 0f, 0f),
        "aggressive" to floatArrayOf(0.5f, 0.3f, 0.3f, 0f,-0.3f, 0f, 0.3f, 0.5f, 0.3f, 0f),
        "smooth" to     floatArrayOf(0f, 0f, 0f, 0.3f, 0f, 0.3f, 0f,-0.3f,-0.3f,-0.3f),
        "epic" to       floatArrayOf(0.5f, 0.3f, 0.3f, 0f, 0f, 0.3f, 0.3f, 0.5f, 0.5f, 0.3f),
        "ethereal" to   floatArrayOf(0f, 0.3f, 0.3f, 0.3f, 0f, 0f,-0.3f, 0f, 0.3f, 0.5f),
        "raw" to        floatArrayOf(0.3f, 0.3f, 0f, 0f,-0.3f,-0.3f, 0f, 0.3f, 0f,-0.3f),
        "crisp" to      floatArrayOf(0f, 0f, 0f, 0f, 0f, 0.3f, 0.3f, 0.5f, 0.3f, 0f),
        "warm" to       floatArrayOf(0.3f, 0.3f, 0.3f, 0.3f, 0f, 0f,-0.3f,-0.5f,-0.3f,-0.3f),
        "punchy" to     floatArrayOf(0.5f, 0.3f, 0f, 0f, 0f, 0f, 0f, 0.3f, 0f, 0f),
        "lush" to       floatArrayOf(0f, 0.3f, 0.3f, 0.3f, 0f, 0.3f, 0f, 0f, 0.3f, 0.3f)
    )

    private val moodMod: Map<Mood, FloatArray> = mapOf(
        Mood.ENERGETIC to floatArrayOf(1.0f,0.5f,0.5f,0.0f,0.0f,0.0f,0.5f,0.5f,1.0f,0.5f),
        Mood.HAPPY to floatArrayOf(0.5f,0.5f,0.0f,0.0f,0.0f,0.5f,0.5f,1.0f,0.5f,0.5f),
        Mood.MELANCHOLIC to floatArrayOf(0.5f,1.0f,0.5f,0.5f,0.0f,0.0f,-0.5f,-0.5f,0.0f,0.0f),
        Mood.AGGRESSIVE to floatArrayOf(1.5f,1.0f,0.5f,0.0f,-0.5f,0.0f,0.5f,1.0f,1.5f,1.0f),
        Mood.CALM to floatArrayOf(-0.5f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,-0.5f,-0.5f,-0.5f),
        Mood.DARK to floatArrayOf(1.0f,1.5f,0.5f,0.0f,-0.5f,-0.5f,0.0f,0.0f,0.5f,0.0f),
        Mood.ROMANTIC to floatArrayOf(0.5f,0.5f,0.5f,0.5f,0.0f,0.5f,0.5f,0.0f,0.0f,0.0f),
        Mood.DREAMY to floatArrayOf(0.5f,0.5f,0.5f,0.5f,0.0f,0.0f,-0.5f,0.0f,0.5f,1.0f),
        Mood.INTENSE to floatArrayOf(1.5f,1.0f,1.0f,0.0f,-0.5f,0.0f,0.5f,1.0f,1.5f,1.0f),
        Mood.NEUTRAL to floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f)
    )

    private val routeCorr = mapOf(
        AudioRoute.SPEAKER to floatArrayOf(3.0f,2.5f,1.5f,0.5f,0.0f,0.0f,-0.5f,-1.0f,-0.5f,0.0f),
        AudioRoute.BLUETOOTH to floatArrayOf(0.5f,0.5f,0.5f,0.0f,0.0f,0.0f,0.5f,0.5f,1.0f,0.5f),
        AudioRoute.WIRED_HEADPHONES to floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f),
        AudioRoute.USB to floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f),
        AudioRoute.UNKNOWN to floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f)
    )

    fun compose(prediction: SonaraPrediction, route: AudioRoute, userOffset: FloatArray? = null, lyricsModifier: FloatArray? = null): FinalEqProfile {
        val bands = FloatArray(BAND_COUNT)

        // Layer 1: Base genre curve
        val base = genreEq[prediction.genre] ?: genreEq[Genre.UNKNOWN]!!
        for (i in bands.indices) bands[i] += base[i]

        // Layer 2: SubGenre modifier (K-pop gets different treatment than generic Pop)
        val subMod = prediction.subGenre?.let { subGenreMod[it] }
        if (subMod != null) {
            for (i in bands.indices) bands[i] += subMod[i] * 0.7f // 70% weight
        }

        // Layer 3: Tag-vector micro adjustments
        val tags = prediction.tags
        if (tags.isNotEmpty()) {
            var tagCount = 0
            for (tag in tags) {
                val delta = tagDelta.entries.firstOrNull { (k, _) -> tag.contains(k) }?.value
                if (delta != null && tagCount < 4) { // max 4 tag deltas
                    for (i in bands.indices) bands[i] += delta[i] * 0.4f // 40% weight each
                    tagCount++
                }
            }
        }

        // Layer 4: Mood modifier
        val mood = moodMod[prediction.mood] ?: moodMod[Mood.NEUTRAL]!!
        for (i in bands.indices) bands[i] += mood[i] * 0.5f

        // Layer 5: Energy scaling
        val energyScale = (prediction.energy - 0.5f) * 0.6f
        for (i in bands.indices) { if (i <= 2 || i >= 7) bands[i] += energyScale * 1.5f }

        // Layer 6: Route correction
        val rc = routeCorr[route] ?: routeCorr[AudioRoute.UNKNOWN]!!
        for (i in bands.indices) bands[i] += rc[i]

        // Layer 7.5: Lyrics modifier (küçük — Madde 14)
        if (lyricsModifier != null) {
            for (i in bands.indices) bands[i] += lyricsModifier.getOrElse(i) { 0f } * 0.5f
        }

        // Layer 7: User preference offset
        if (userOffset != null) for (i in bands.indices) bands[i] += userOffset.getOrElse(i) { 0f }

        // Safety clamp
        for (i in bands.indices) bands[i] = bands[i].coerceIn(-12f, 12f)
        val maxBand = bands.max()
        val preamp = if (maxBand > 0f) -maxBand * 0.5f else 0f

        val bassBoost = when (prediction.genre) { Genre.HIP_HOP -> 500; Genre.ELECTRONIC, Genre.DANCE -> 450; Genre.REGGAE -> 420; Genre.RNB, Genre.LATIN -> 350; Genre.METAL -> 300; Genre.ROCK, Genre.POP -> 200; else -> 100 }
        val virtualizer = when (prediction.genre) { Genre.ELECTRONIC, Genre.DANCE -> 500; Genre.AMBIENT, Genre.CLASSICAL -> 300; Genre.JAZZ -> 250; Genre.PODCAST, Genre.AUDIOBOOK -> 0; else -> 200 }
        val loudness = when { prediction.energy > 0.7f -> 200; prediction.energy < 0.3f -> 0; else -> 100 }

        return FinalEqProfile(bands, preamp, bassBoost, virtualizer, loudness, prediction)
    }
}
