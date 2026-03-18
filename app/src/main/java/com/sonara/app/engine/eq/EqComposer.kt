package com.sonara.app.engine.eq

import com.sonara.app.intelligence.pipeline.*

class EqComposer {
    companion object { const val BAND_COUNT = 10 }

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

    fun compose(prediction: SonaraPrediction, route: AudioRoute, userOffset: FloatArray? = null): FinalEqProfile {
        val bands = FloatArray(BAND_COUNT)
        val base = genreEq[prediction.genre] ?: genreEq[Genre.UNKNOWN]!!
        for (i in bands.indices) bands[i] += base[i]

        val mood = moodMod[prediction.mood] ?: moodMod[Mood.NEUTRAL]!!
        for (i in bands.indices) bands[i] += mood[i] * 0.5f

        val energyScale = (prediction.energy - 0.5f) * 0.6f
        for (i in bands.indices) { if (i <= 2 || i >= 7) bands[i] += energyScale * 1.5f }

        val rc = routeCorr[route] ?: routeCorr[AudioRoute.UNKNOWN]!!
        for (i in bands.indices) bands[i] += rc[i]

        if (userOffset != null) for (i in bands.indices) bands[i] += userOffset.getOrElse(i) { 0f }

        // Safety limit
        for (i in bands.indices) bands[i] = bands[i].coerceIn(-12f, 12f)
        val maxBand = bands.max()
        val preamp = if (maxBand > 0f) -maxBand * 0.5f else 0f

        val bassBoost = when (prediction.genre) { Genre.HIP_HOP -> 500; Genre.ELECTRONIC, Genre.DANCE -> 450; Genre.REGGAE -> 420; Genre.RNB, Genre.LATIN -> 350; Genre.METAL -> 300; Genre.ROCK, Genre.POP -> 200; else -> 100 }
        val virtualizer = when (prediction.genre) { Genre.ELECTRONIC, Genre.DANCE -> 500; Genre.AMBIENT, Genre.CLASSICAL -> 300; Genre.JAZZ -> 250; Genre.PODCAST, Genre.AUDIOBOOK -> 0; else -> 200 }
        val loudness = when { prediction.energy > 0.7f -> 200; prediction.energy < 0.3f -> 0; else -> 100 }

        return FinalEqProfile(bands, preamp, bassBoost, virtualizer, loudness, prediction)
    }
}
