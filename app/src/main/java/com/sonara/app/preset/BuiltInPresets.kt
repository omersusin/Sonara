package com.sonara.app.preset

object BuiltInPresets {
    // Reverb presets: 0=Off 1=SmallRoom 2=MediumRoom 3=LargeRoom 4=MediumHall 5=LargeHall 6=Plate
    val ALL: List<Preset> = listOf(
        // ── Neutral ──
        p("Flat", floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f), "neutral"),

        // ── Bass ──
        p("Bass Boost", floatArrayOf(5f,4.5f,3.5f,2f,0.5f,0f,0f,0f,0f,0f), "bass",
            bass = 400, virt = 0, loud = 0),
        p("Bass Heavy", floatArrayOf(7f,6f,5f,3f,1f,0f,-1f,-1f,0f,0f), "bass",
            bass = 650, virt = 100, loud = 0),
        p("Deep Bass", floatArrayOf(10f,0f,-9.4f,-9f,-3.5f,-6.1f,-1.5f,-5f,0.6f,3f), "bass",
            bass = 800, virt = 50, loud = 0),
        p("Sub Bass", floatArrayOf(9.4f,8.5f,4.5f,1.5f,0f,0f,0f,0f,0f,0f), "bass",
            bass = 550, virt = 0, loud = 0),

        // ── Treble ──
        p("Treble Boost", floatArrayOf(0f,0f,0f,0f,0.5f,1.5f,3f,4f,5f,5.5f), "treble",
            bass = 0, virt = 150, loud = 0),
        p("Clarity", floatArrayOf(4.5f,6.5f,8.8f,6.5f,3f,1.3f,6f,9f,10.5f,9f), "treble",
            bass = 0, virt = 200, loud = 0),

        // ── Shape ──
        p("V-Shape", floatArrayOf(5f,4f,2f,0f,-2f,-2f,0f,2f,4f,5f), "fun",
            bass = 400, virt = 250, loud = 0),
        p("Loudness", floatArrayOf(4f,3f,1f,0f,0f,0f,0f,1f,3f,4f), "fun",
            bass = 300, virt = 100, loud = 500),
        p("Volume Boost", floatArrayOf(-1.8f,-3f,-1.8f,1.5f,3.5f,3.5f,2.5f,1.5f,0f,-1.5f), "fun",
            bass = 0, virt = 100, loud = 1500),

        // ── Vocal ──
        p("Vocal", floatArrayOf(-1f,-0.5f,0f,2f,4f,4f,3f,1f,0f,-1f), "vocal",
            bass = 0, virt = 100, loud = 0),
        p("Podcast", floatArrayOf(-2f,-1f,0f,3f,5f,5f,3f,1f,-1f,-2f), "vocal",
            bass = 0, virt = 50, loud = 300),

        // ── Genre ──
        p("Rock", floatArrayOf(0f,0f,3f,-10f,-2.5f,0.8f,3f,3f,3f,3f), "genre",
            bass = 300, virt = 200, loud = 0, reverb = 1),
        p("Pop", floatArrayOf(0f,0f,0f,1.3f,2.3f,5f,-1.8f,-3f,-3f,-3f), "genre",
            bass = 200, virt = 150, loud = 0),
        p("Hip-Hop", floatArrayOf(4.4f,4f,2f,3f,-1.3f,-1.5f,0.8f,-1f,0.8f,3f), "genre",
            bass = 600, virt = 200, loud = 0),
        p("Jazz", floatArrayOf(0f,0f,3f,5.9f,-5.2f,-2.5f,1.8f,-0.8f,-0.8f,-0.8f), "genre",
            bass = 150, virt = 250, loud = 0, reverb = 2),
        p("Classical", floatArrayOf(0f,-3.5f,-5f,0f,2f,0f,0f,4.4f,9f,9f), "genre",
            bass = 0, virt = 300, loud = 0, reverb = 4),
        p("Electronic", floatArrayOf(4f,3.5f,0.5f,-0.5f,-1f,2f,0f,1f,3.5f,4.5f), "genre",
            bass = 500, virt = 400, loud = 0),
        p("R&B", floatArrayOf(3f,7f,5.3f,1.5f,-1.8f,-1.5f,2.3f,3f,3.7f,4f), "genre",
            bass = 400, virt = 200, loud = 0, reverb = 1),
        p("Acoustic", floatArrayOf(4.8f,4f,2.5f,1f,1.5f,2f,3.3f,4f,3.4f,3f), "genre",
            bass = 100, virt = 100, loud = 0, reverb = 2),
        p("Metal", floatArrayOf(10.5f,7.5f,1f,5.5f,0f,0f,3.1f,0f,8.1f,12f), "genre",
            bass = 500, virt = 300, loud = 0),
        p("Dubstep", floatArrayOf(11f,0.5f,-2f,-5f,-4.9f,-4.5f,-1.8f,0f,-2.5f,0f), "genre",
            bass = 850, virt = 500, loud = 0),
        p("Hardstyle", floatArrayOf(6.6f,12f,0.6f,-12f,0.3f,6.5f,-1.1f,-4.5f,-7.7f,-10f), "genre",
            bass = 750, virt = 400, loud = 0),

        // ── Mood ──
        p("Spatial", floatArrayOf(0f,0f,0f,1f,1.5f,0f,0f,0f,0f,0f), "mood",
            bass = 200, virt = 800, loud = 400),
        p("Night", floatArrayOf(1f,0.5f,0f,0f,0f,0f,-1f,-2f,-3f,0f), "mood",
            bass = 0, virt = 200, loud = 800),
        p("Late Night", floatArrayOf(1f,1.5f,1f,0f,-1f,-2f,-1f,0f,0.5f,0.5f), "mood",
            bass = 200, virt = 100, loud = 0, reverb = 1),
        p("Morning", floatArrayOf(0f,0f,1f,2f,2.5f,2f,1.5f,2f,2.5f,2f), "mood",
            bass = 100, virt = 150, loud = 200),
        p("Workout", floatArrayOf(5f,4f,2f,1f,0f,1f,2f,3f,3.5f,4f), "mood",
            bass = 500, virt = 250, loud = 800),
        p("Chill", floatArrayOf(1f,1.5f,1f,0f,-0.5f,0f,0.5f,1f,1.5f,1f), "mood",
            bass = 150, virt = 200, loud = 0, reverb = 1),
        p("Cinema", floatArrayOf(3f,6.1f,8.8f,7f,6.1f,5f,5.8f,3.5f,9f,8f), "mood",
            bass = 400, virt = 800, loud = 300, reverb = 5),

        // ── Rhythm ──
        p("Rhythm Cut", floatArrayOf(-5.3f,-4.5f,-3.9f,-3f,-1f,0f,0f,0f,0f,0f), "fun",
            bass = 0, virt = 100, loud = 0),

        // ── Device ──
        p("Small Speaker", floatArrayOf(6f,5f,4f,2f,1f,0f,0f,1f,2f,2f), "device",
            bass = 600, virt = 300, loud = 500),
        p("Headphone", floatArrayOf(2f,1f,0f,0f,-0.5f,0f,0.5f,1f,2f,2.5f), "device",
            bass = 100, virt = 400, loud = 0),
        p("Earbud Fix", floatArrayOf(3f,2.5f,1f,0f,-1f,0f,1f,0f,-1f,-2f), "device",
            bass = 300, virt = 500, loud = 0)
    )

    private fun p(
        name: String,
        bands: FloatArray,
        cat: String,
        bass: Int = 0,
        virt: Int = 0,
        loud: Int = 0,
        preamp: Float = 0f,
        reverb: Int = 0
    ) = Preset(
        name = name,
        bands = Preset.fromArray(bands),
        preamp = preamp,
        bassBoost = bass,
        virtualizer = virt,
        loudness = loud,
        reverb = reverb,
        isBuiltIn = true,
        category = cat
    )

    val CATEGORIES = mapOf(
        "neutral" to "Neutral",
        "bass" to "Bass",
        "treble" to "Treble",
        "vocal" to "Vocal",
        "genre" to "Genre",
        "mood" to "Mood",
        "fun" to "Fun",
        "device" to "Device",
        "custom" to "Custom"
    )
}
