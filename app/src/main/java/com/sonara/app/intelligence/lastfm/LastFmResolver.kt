package com.sonara.app.intelligence.lastfm

import com.sonara.app.data.models.TrackInfo

class LastFmResolver {

    suspend fun resolve(title: String, artist: String, apiKey: String): TrackInfo? {
        if (apiKey.isBlank() || title.isBlank()) return null
        return try {
            val response = LastFmClient.api.getTrackInfo(title, artist, apiKey)
            val track = response.track ?: return tryArtistFallback(artist, apiKey, title)
            val tags = track.toptags?.tag ?: emptyList()
            if (tags.isEmpty()) return tryArtistFallback(artist, apiKey, title)
            val rawTags = tags.map { it.name.lowercase().trim() }
            val genre = classifyGenre(tags)
            val subGenre = extractSubGenre(rawTags, genre)
            val mood = classifyMood(tags)
            val energy = estimateEnergy(tags, genre)
            TrackInfo(track.name.ifBlank { title }, track.artist?.name ?: artist,
                track.album?.title ?: "", genre, subGenre, mood, energy,
                calculateConfidence(tags), "lastfm", rawTags)
        } catch (e: Exception) { null }
    }

    private suspend fun tryArtistFallback(artist: String, apiKey: String, title: String): TrackInfo? {
        if (artist.isBlank()) return null
        return try {
            val response = LastFmClient.api.getArtistTags(artist, apiKey)
            val tags = response.toptags?.tag ?: return null
            if (tags.isEmpty()) return null
            val rawTags = tags.map { it.name.lowercase().trim() }
            val genre = classifyGenre(tags)
            val subGenre = extractSubGenre(rawTags, genre)
            val mood = classifyMood(tags)
            TrackInfo(title, artist, "", genre, subGenre, mood,
                estimateEnergy(tags, genre), calculateConfidence(tags) * 0.7f,
                "lastfm-artist", rawTags)
        } catch (e: Exception) { null }
    }

    /**
     * Extract the most specific subgenre from raw Last.fm tags.
     * E.g. tags=[k-pop, dance pop, electronic, pop] → subGenre="k-pop"
     */
    private fun extractSubGenre(tags: List<String>, primaryGenre: String): String {
        // Subgenre patterns: more specific than parent genre
        val subGenreMap = mapOf(
            // Pop subgenres
            "k-pop" to "k-pop", "kpop" to "k-pop", "korean pop" to "k-pop",
            "j-pop" to "j-pop", "jpop" to "j-pop",
            "c-pop" to "c-pop", "cpop" to "c-pop",
            "synth-pop" to "synth-pop", "synthpop" to "synth-pop",
            "dream pop" to "dream-pop", "dreampop" to "dream-pop",
            "electropop" to "electropop", "electro pop" to "electropop",
            "dance pop" to "dance-pop", "dance-pop" to "dance-pop",
            "indie pop" to "indie-pop", "indiepop" to "indie-pop",
            "art pop" to "art-pop",
            "hyperpop" to "hyperpop", "hyper pop" to "hyperpop",
            "bedroom pop" to "bedroom-pop",
            "power pop" to "power-pop",
            "chamber pop" to "chamber-pop",
            "pop punk" to "pop-punk",
            "pop rock" to "pop-rock",
            "turkish pop" to "turkish-pop",
            // Rock subgenres
            "alternative rock" to "alt-rock", "alt-rock" to "alt-rock",
            "indie rock" to "indie-rock", "post-rock" to "post-rock",
            "psychedelic rock" to "psych-rock", "progressive rock" to "prog-rock",
            "garage rock" to "garage-rock", "classic rock" to "classic-rock",
            "hard rock" to "hard-rock", "punk rock" to "punk-rock",
            "grunge" to "grunge", "shoegaze" to "shoegaze",
            "post-punk" to "post-punk", "new wave" to "new-wave",
            "britpop" to "britpop", "emo" to "emo",
            // Electronic subgenres
            "house" to "house", "deep house" to "deep-house",
            "tech house" to "tech-house", "techno" to "techno",
            "trance" to "trance", "dubstep" to "dubstep",
            "drum and bass" to "dnb", "dnb" to "dnb",
            "future bass" to "future-bass", "trap" to "trap-edm",
            "synthwave" to "synthwave", "chillwave" to "chillwave",
            "downtempo" to "downtempo", "idm" to "idm",
            "hardstyle" to "hardstyle", "breakbeat" to "breakbeat",
            "lo-fi" to "lo-fi", "lofi" to "lo-fi",
            // Hip-hop subgenres
            "boom bap" to "boom-bap", "trap" to "trap",
            "drill" to "drill", "uk drill" to "uk-drill",
            "cloud rap" to "cloud-rap", "emo rap" to "emo-rap",
            "phonk" to "phonk", "grime" to "grime",
            // Metal subgenres
            "death metal" to "death-metal", "black metal" to "black-metal",
            "thrash metal" to "thrash-metal", "metalcore" to "metalcore",
            "doom metal" to "doom-metal", "progressive metal" to "prog-metal",
            "nu metal" to "nu-metal", "symphonic metal" to "symphonic-metal",
            "djent" to "djent",
            // Other
            "neo-soul" to "neo-soul", "trip-hop" to "trip-hop",
            "trip hop" to "trip-hop", "bossa nova" to "bossa-nova",
            "reggaeton" to "reggaeton", "cumbia" to "cumbia",
            "latin trap" to "latin-trap"
        )

        // Find the most specific matching tag
        for (tag in tags) {
            subGenreMap[tag]?.let { return it }
        }

        // Try partial matches
        for (tag in tags) {
            for ((key, value) in subGenreMap) {
                if (tag.contains(key) && key.length >= 4) return value
            }
        }

        return ""
    }

    private data class GK(val genre: String, val specificity: Int)

    private fun classifyGenre(tags: List<LastFmTag>): String {
        if (tags.isEmpty()) return "other"
        val km = mutableMapOf<String, GK>()
        mapOf("rock" to 1,"alternative" to 2,"alternative rock" to 3,"indie rock" to 3,"punk" to 2,"punk rock" to 3,"grunge" to 3,"hard rock" to 3,"classic rock" to 3,"post-punk" to 3,"garage rock" to 3,"psychedelic rock" to 3,"progressive rock" to 3,"britpop" to 3,"new wave" to 2,"post-rock" to 3,"emo" to 2,"shoegaze" to 3,"pop punk" to 3).forEach { (k, s) -> km[k] = GK("rock", s) }
        mapOf("pop" to 1,"synth-pop" to 3,"synthpop" to 3,"electropop" to 3,"indie pop" to 3,"dream pop" to 3,"k-pop" to 3,"kpop" to 3,"j-pop" to 3,"dance pop" to 3,"art pop" to 3,"chamber pop" to 3,"power pop" to 3,"pop rock" to 2,"hyperpop" to 3,"bedroom pop" to 3,"turkish pop" to 3,"turkish" to 1,"c-pop" to 3).forEach { (k, s) -> km[k] = GK("pop", s) }
        mapOf("hip-hop" to 2,"hip hop" to 2,"rap" to 2,"trap" to 3,"boom bap" to 3,"drill" to 3,"uk drill" to 3,"grime" to 3,"gangsta rap" to 3,"cloud rap" to 3,"emo rap" to 3,"old school hip hop" to 3,"southern hip hop" to 3,"phonk" to 3,"lo-fi hip hop" to 3,"turkish hip hop" to 3,"turkish rap" to 3).forEach { (k, s) -> km[k] = GK("hip-hop", s) }
        mapOf("electronic" to 1,"edm" to 2,"house" to 2,"deep house" to 3,"tech house" to 3,"techno" to 2,"trance" to 2,"dubstep" to 3,"dnb" to 3,"drum and bass" to 3,"ambient" to 2,"idm" to 3,"chillwave" to 3,"synthwave" to 3,"future bass" to 3,"hardstyle" to 3,"electronica" to 2,"downtempo" to 2,"trip-hop" to 3,"trip hop" to 3,"breakbeat" to 3,"lo-fi" to 2,"lofi" to 2).forEach { (k, s) -> km[k] = GK("electronic", s) }
        mapOf("r&b" to 2,"rnb" to 2,"soul" to 2,"neo-soul" to 3,"neo soul" to 3,"funk" to 2,"contemporary r&b" to 3,"new jack swing" to 3,"motown" to 3,"disco" to 2,"alternative r&b" to 3).forEach { (k, s) -> km[k] = GK("r&b", s) }
        mapOf("metal" to 1,"heavy metal" to 3,"death metal" to 3,"black metal" to 3,"thrash metal" to 3,"metalcore" to 3,"doom metal" to 3,"progressive metal" to 3,"power metal" to 3,"symphonic metal" to 3,"nu metal" to 3,"groove metal" to 3,"djent" to 3,"deathcore" to 3).forEach { (k, s) -> km[k] = GK("metal", s) }
        mapOf("jazz" to 2,"smooth jazz" to 3,"bebop" to 3,"fusion" to 2,"swing" to 2,"cool jazz" to 3,"free jazz" to 3,"jazz fusion" to 3,"acid jazz" to 3,"big band" to 3,"vocal jazz" to 3,"latin jazz" to 3,"nu jazz" to 3).forEach { (k, s) -> km[k] = GK("jazz", s) }
        mapOf("classical" to 2,"orchestra" to 2,"symphony" to 3,"baroque" to 3,"opera" to 3,"choral" to 3,"contemporary classical" to 3,"neoclassical" to 3,"soundtrack" to 1,"film score" to 2,"orchestral" to 2,"piano" to 1).forEach { (k, s) -> km[k] = GK("classical", s) }
        mapOf("country" to 2,"bluegrass" to 3,"americana" to 2,"country rock" to 3,"outlaw country" to 3,"alt-country" to 3,"country pop" to 3,"honky tonk" to 3).forEach { (k, s) -> km[k] = GK("country", s) }
        mapOf("folk" to 2,"acoustic" to 1,"singer-songwriter" to 2,"indie folk" to 3,"folk rock" to 2,"traditional folk" to 3,"celtic" to 2).forEach { (k, s) -> km[k] = GK("folk", s) }
        mapOf("blues" to 2,"delta blues" to 3,"chicago blues" to 3,"electric blues" to 3,"blues rock" to 2).forEach { (k, s) -> km[k] = GK("blues", s) }
        mapOf("reggae" to 2,"ska" to 2,"dub" to 2,"dancehall" to 3,"roots reggae" to 3,"rocksteady" to 3).forEach { (k, s) -> km[k] = GK("reggae", s) }
        mapOf("latin" to 1,"salsa" to 3,"reggaeton" to 3,"bossa nova" to 3,"cumbia" to 3,"bachata" to 3,"latin pop" to 3,"corrido" to 3,"dembow" to 3,"urbano latino" to 3,"latin trap" to 3).forEach { (k, s) -> km[k] = GK("latin", s) }

        val scores = mutableMapOf<String, Float>()
        tags.map { it.name.lowercase().trim() }.forEachIndexed { i, tag ->
            val pw = 1f - (i.toFloat() / tags.size.coerceAtLeast(1)) * 0.5f
            km[tag]?.let { scores[it.genre] = (scores[it.genre] ?: 0f) + it.specificity * 3f * pw; return@forEachIndexed }
            km.entries.filter { (k, _) -> tag.contains(k) && k.length >= 3 }.sortedByDescending { it.key.length }.take(2).forEach { (k, gk) ->
                val mr = k.length.toFloat() / tag.length.coerceAtLeast(1)
                scores[gk.genre] = (scores[gk.genre] ?: 0f) + gk.specificity * mr * 2f * pw
            }
        }
        if (scores.isEmpty()) return "other"
        val best = scores.entries.sortedByDescending { it.value }.first()
        return if (best.value < 1.5f) "other" else best.key
    }

    private fun classifyMood(tags: List<LastFmTag>): String {
        val tn = tags.map { it.name.lowercase() }; val ms = mutableMapOf<String, Int>()
        mapOf("energetic" to listOf("energetic","upbeat","party","dance","fast","power","aggressive","anthemic"),
            "chill" to listOf("chill","relax","calm","ambient","mellow","downtempo","ethereal","dreamy"),
            "melancholic" to listOf("sad","melancholy","dark","emotional","depressing","gloomy","haunting"),
            "happy" to listOf("happy","fun","cheerful","uplifting","feel good","bright","joyful","catchy"),
            "intense" to listOf("intense","heavy","brutal","hard","loud","raw","epic","dramatic"),
            "romantic" to listOf("romantic","love","sensual","smooth","sexy","intimate","passionate")
        ).forEach { (mood, kws) -> var s = 0; tn.forEachIndexed { i, t -> kws.forEach { if (t.contains(it)) s += (tags.size - i) } }; if (s > 0) ms[mood] = s }
        return ms.maxByOrNull { it.value }?.key ?: "neutral"
    }

    private fun estimateEnergy(tags: List<LastFmTag>, genre: String): Float {
        val base = when (genre) { "metal","electronic","hip-hop","latin" -> 0.75f; "classical","jazz","folk","blues" -> 0.35f; else -> 0.55f }
        val tn = tags.map { it.name.lowercase() }; var mod = 0f
        if (tn.any { it.contains("chill") || it.contains("slow") || it.contains("ambient") }) mod -= 0.15f
        if (tn.any { it.contains("fast") || it.contains("energy") || it.contains("party") || it.contains("dance") }) mod += 0.15f
        return (base + mod).coerceIn(0f, 1f)
    }

    private fun calculateConfidence(tags: List<LastFmTag>): Float {
        if (tags.isEmpty()) return 0f
        val cs = ((tags.firstOrNull()?.count ?: 0) / 100f).coerceAtMost(0.5f)
        val ds = (tags.size.coerceAtMost(10) / 10f) * 0.5f
        return (cs + ds).coerceIn(0.1f, 1f)
    }
}
