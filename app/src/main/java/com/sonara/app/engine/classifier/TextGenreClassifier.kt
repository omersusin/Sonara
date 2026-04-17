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

package com.sonara.app.engine.classifier

import android.util.Log

class TextGenreClassifier {
    companion object { private const val TAG = "TextGenreClf" }
    data class Prediction(val genre: String, val mood: String, val energy: Float, val confidence: Float)

    private val genreKeywords: MutableMap<String, MutableMap<String, Float>> = mutableMapOf(
        "rock" to mutableMapOf("rock" to 1f,"guitar" to .6f,"metal" to .8f,"punk" to .7f,"grunge" to .7f,"hard" to .4f,"riff" to .5f,"electric" to .4f,"alternative" to .5f),
        "pop" to mutableMapOf("pop" to 1f,"hit" to .4f,"dance" to .5f,"love" to .3f,"radio" to .3f,"single" to .2f,"feat" to .3f,"remix" to .4f),
        "hiphop" to mutableMapOf("rap" to 1f,"hip" to .9f,"hop" to .9f,"trap" to .8f,"beat" to .4f,"flow" to .5f,"mc" to .6f,"freestyle" to .7f,"drill" to .7f),
        "electronic" to mutableMapOf("edm" to 1f,"techno" to .9f,"house" to .8f,"trance" to .8f,"synth" to .7f,"dubstep" to .8f,"ambient" to .5f,"bass" to .4f,"drop" to .5f,"dj" to .5f),
        "classical" to mutableMapOf("symphony" to 1f,"sonata" to 1f,"concerto" to 1f,"opus" to .9f,"orchestra" to .9f,"piano" to .5f,"violin" to .6f,"mozart" to .8f,"beethoven" to .8f,"bach" to .8f,"classical" to 1f),
        "jazz" to mutableMapOf("jazz" to 1f,"swing" to .7f,"blues" to .6f,"bossa" to .7f,"sax" to .6f,"bebop" to .8f,"fusion" to .4f,"quartet" to .5f),
        "rnb" to mutableMapOf("r&b" to 1f,"rnb" to 1f,"soul" to .8f,"groove" to .5f,"smooth" to .4f,"funk" to .6f,"motown" to .7f),
        "country" to mutableMapOf("country" to 1f,"cowboy" to .7f,"nashville" to .8f,"bluegrass" to .8f,"folk" to .5f,"banjo" to .7f,"western" to .5f,"honky" to .7f)
    )
    private val moodKeywords = mapOf(
        "energetic" to listOf("fire","energy","power","fast","wild","rage","hype","lit"),
        "chill" to listOf("chill","relax","calm","slow","easy","peaceful","mellow","soft"),
        "sad" to listOf("sad","cry","tears","broken","alone","lonely","pain","lost"),
        "happy" to listOf("happy","joy","fun","party","celebrate","smile","sunshine"),
        "dark" to listOf("dark","death","shadow","night","evil","demon","hell","blood"),
        "romantic" to listOf("love","heart","kiss","baby","darling","romance","together")
    )
    private val genreEnergy = mapOf("rock" to 0.75f,"pop" to 0.6f,"hiphop" to 0.7f,"electronic" to 0.8f,"classical" to 0.3f,"jazz" to 0.4f,"rnb" to 0.5f,"country" to 0.5f)

    fun classify(title: String?, artist: String?, album: String?, tags: List<String>? = null): Prediction {
        val combined = buildString { title?.let { append(it).append(' ') }; artist?.let { append(it).append(' ') }; album?.let { append(it).append(' ') }; tags?.forEach { append(it).append(' ') } }.lowercase().replace(Regex("[^a-z0-9&\\s]"), " ")
        val tokens = combined.split(Regex("\\s+")).filter { it.length > 1 }.toSet()
        if (tokens.isEmpty()) return Prediction("other", "neutral", 0.5f, 0f)

        val scores = mutableMapOf<String, Float>()
        for ((genre, kws) in genreKeywords) { var s = 0f; for ((kw, w) in kws) { if (kw in tokens || tokens.any { it.contains(kw) }) s += w }; if (s > 0f) scores[genre] = s }
        val best = scores.maxByOrNull { it.value }
        val genre: String; val confidence: Float
        if (best == null || best.value < 0.3f) { genre = "other"; confidence = 0.1f } else { genre = best.key; val total = scores.values.sum().coerceAtLeast(0.01f); confidence = (best.value / total).coerceIn(0f, 1f) * (1f - 1f / (1f + best.value)) }

        val moodScores = mutableMapOf<String, Int>()
        for ((mood, words) in moodKeywords) { val hits = words.count { w -> w in tokens || tokens.any { it.contains(w) } }; if (hits > 0) moodScores[mood] = hits }
        val bestMood = moodScores.maxByOrNull { it.value }?.key ?: "neutral"
        var energy = genreEnergy[genre] ?: 0.5f
        when (bestMood) { "energetic" -> energy = (energy + 0.15f).coerceAtMost(1f); "chill", "sad" -> energy = (energy - 0.15f).coerceAtLeast(0f) }

        return Prediction(genre, bestMood, energy, confidence)
    }

    fun adaptWeights(wrongGenre: String, correctGenre: String, tokens: Set<String>) {
        val lr = 0.1f
        genreKeywords[wrongGenre]?.let { kws -> for (t in tokens) kws[t]?.let { kws[t] = (it - lr).coerceAtLeast(0f) } }
        val correct = genreKeywords.getOrPut(correctGenre) { mutableMapOf() }
        for (t in tokens) { correct[t] = ((correct[t] ?: 0f) + lr).coerceAtMost(1.5f) }
    }

    fun exportWeights(): Map<String, Map<String, Float>> = genreKeywords.mapValues { it.value.toMap() }
    fun importWeights(weights: Map<String, Map<String, Float>>) { genreKeywords.clear(); for ((g, kws) in weights) genreKeywords[g] = kws.toMutableMap() }
}
