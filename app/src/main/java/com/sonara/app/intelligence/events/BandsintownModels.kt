package com.sonara.app.intelligence.events

data class BandsintownVenue(
    val name: String,
    val city: String,
    val country: String,
    val region: String
) {
    val displayLocation: String get() = buildString {
        append(city)
        if (region.isNotBlank() && region != city) { append(", "); append(region) }
        if (country.isNotBlank()) { append(", "); append(country) }
    }
}

data class BandsintownEvent(
    val id: String,
    val datetime: String,
    val title: String,
    val url: String,
    val venue: BandsintownVenue
) {
    val displayDate: String get() = runCatching {
        val instant = java.time.Instant.parse(datetime)
        val zdt = instant.atZone(java.time.ZoneId.systemDefault())
        java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy").format(zdt)
    }.getOrElse { datetime.take(10) }
}
