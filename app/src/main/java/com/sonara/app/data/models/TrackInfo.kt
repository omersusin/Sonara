package com.sonara.app.data.models

data class TrackInfo(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
    val subGenre: String = "",
    val mood: String = "",
    val energy: Float = 0.5f,
    val confidence: Float = 0f,
    val source: String = "unknown",
    val tags: List<String> = emptyList()
)
