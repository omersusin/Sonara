package com.sonara.app.preset

object BuiltInPresets {
    val ALL: List<Preset> = listOf(
        p("Flat", floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f), "neutral"),
        p("Bass Boost", floatArrayOf(5f,4.5f,3.5f,2f,0.5f,0f,0f,0f,0f,0f), "bass"),
        p("Bass Heavy", floatArrayOf(7f,6f,5f,3f,1f,0f,-1f,-1f,0f,0f), "bass"),
        p("Treble Boost", floatArrayOf(0f,0f,0f,0f,0.5f,1.5f,3f,4f,5f,5.5f), "treble"),
        p("V-Shape", floatArrayOf(5f,4f,2f,0f,-2f,-2f,0f,2f,4f,5f), "fun"),
        p("Vocal", floatArrayOf(-1f,-0.5f,0f,2f,4f,4f,3f,1f,0f,-1f), "vocal"),
        p("Podcast", floatArrayOf(-2f,-1f,0f,3f,5f,5f,3f,1f,-1f,-2f), "vocal"),
        p("Rock", floatArrayOf(4f,3f,1f,0f,-1f,0f,2f,3.5f,4f,4f), "genre"),
        p("Pop", floatArrayOf(-1f,0f,2f,3f,4f,3f,1f,0f,-1f,-1f), "genre"),
        p("Hip-Hop", floatArrayOf(5f,4.5f,3f,1f,0f,-0.5f,1f,0.5f,2f,3f), "genre"),
        p("Jazz", floatArrayOf(2f,1.5f,0f,1f,-1f,-1f,0f,1f,2.5f,3f), "genre"),
        p("Classical", floatArrayOf(3f,2f,0f,0f,-1f,-1f,0f,1f,2f,3.5f), "genre"),
        p("Electronic", floatArrayOf(4f,3.5f,2f,0f,-1f,0f,1f,3f,4f,4.5f), "genre"),
        p("R&B", floatArrayOf(3f,4f,3f,1f,-1f,0f,1f,2f,2.5f,2f), "genre"),
        p("Acoustic", floatArrayOf(2f,1f,0f,1f,2f,2f,1.5f,2f,2.5f,2f), "genre"),
        p("Late Night", floatArrayOf(1f,1.5f,1f,0f,-1f,-2f,-1f,0f,0.5f,0.5f), "mood"),
        p("Morning", floatArrayOf(0f,0f,1f,2f,2.5f,2f,1.5f,2f,2.5f,2f), "mood"),
        p("Workout", floatArrayOf(5f,4f,2f,1f,0f,1f,2f,3f,3.5f,4f), "mood"),
        p("Chill", floatArrayOf(1f,1.5f,1f,0f,-0.5f,0f,0.5f,1f,1.5f,1f), "mood"),
        p("Loudness", floatArrayOf(4f,3f,1f,0f,0f,0f,0f,1f,3f,4f), "fun"),
        p("Small Speaker", floatArrayOf(6f,5f,4f,2f,1f,0f,0f,1f,2f,2f), "device"),
        p("Headphone", floatArrayOf(2f,1f,0f,0f,-0.5f,0f,0.5f,1f,2f,2.5f), "device"),
        p("Earbud Fix", floatArrayOf(3f,2.5f,1f,0f,-1f,0f,1f,0f,-1f,-2f), "device")
    )

    private fun p(name: String, bands: FloatArray, cat: String) = Preset(
        name = name,
        bands = Preset.fromArray(bands),
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
