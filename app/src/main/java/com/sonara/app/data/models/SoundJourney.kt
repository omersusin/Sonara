package com.sonara.app.data.models

data class SoundJourney(
    val totalSongsThisWeek: Int = 0,
    val genreBreakdown: Map<String, Int> = emptyMap(),
    val averageEnergy: Float = 0.5f,
    val peakListeningHour: Int = -1,
    val mostPlayedGenre: String = "Unknown",
    val bassPreference: Float = 0.5f, // 0=low bass, 1=high bass user
    val nightBassIncrease: Boolean = false,
    val totalListeningMinutes: Int = 0
)
