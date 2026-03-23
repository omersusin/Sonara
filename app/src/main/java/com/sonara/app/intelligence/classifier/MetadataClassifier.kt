package com.sonara.app.intelligence.classifier

import com.sonara.app.intelligence.pipeline.*

data class ClassificationResult(val genre: Genre, val mood: Mood, val energy: Float, val confidence: Float, val mediaType: MediaType, val reasoning: List<String>)

class MetadataClassifier {
    data class GenreProb(val genre: Genre, val prob: Float)

    private val artistPatterns = listOf(
        Regex("metallica|slayer|megadeth|iron maiden|judas priest|pantera|opeth|gojira|meshuggah|lamb of god|trivium|nightwish|rammstein|slipknot|system of a down|tool|korn|disturbed|bring me the horizon", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.METAL, 0.90f)),
        Regex("kendrick|drake|kanye|jay-z|eminem|nas|tupac|j\\. cole|travis scott|migos|future|lil wayne|lil uzi|21 savage|post malone|cardi b|nicki minaj|tyler.*creator|mac miller|jid|denzel curry|ezhel|ceza|sagopa|ben fero", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.HIP_HOP, 0.90f)),
        Regex("deadmau5|skrillex|avicii|marshmello|tiesto|armin van buuren|martin garrix|david guetta|calvin harris|daft punk|zedd|kygo|alan walker|the chainsmokers|swedish house mafia|flume|odesza|disclosure|diplo|illenium|kaskade|kx5|tommy trash|i_o|john summit|r3hab|thefatrat|cartoon|notd|gryffin|jason ross|clean bandit|major lazer", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.ELECTRONIC, 0.85f)),
        Regex("miles davis|john coltrane|bill evans|thelonious monk|charlie parker|duke ellington|herbie hancock|norah jones|kamasi washington|snarky puppy|robert glasper", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.JAZZ, 0.90f)),
        Regex("beethoven|mozart|bach|chopin|tchaikovsky|vivaldi|brahms|debussy|ravel|hans zimmer|ludovico einaudi|yo-yo ma|lang lang", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.CLASSICAL, 0.95f)),
        Regex("taylor swift|ariana grande|ed sheeran|billie eilish|dua lipa|the weeknd|justin bieber|selena gomez|shawn mendes|halsey|lizzo|doja cat|olivia rodrigo|harry styles|adele|sia|charlie puth|bruno mars|lady gaga|katy perry|rihanna|bts|blackpink|twice|stray kids|aespa|newjeans|le sserafim|ive|seventeen|txt|enhypen|ateez|tarkan|hadise|aleyna tilki|mabel matiz|sezen aksu|ajda pekkan|demet akalin|ebru gundes|gulsen|sibel can|yildiz tilbe|sila|simge|hande unsal|derya ulug|merve ozbey|petek dincoz|ziynet sali|kibariye|gokhan turkmen|berkay|serdar ortac|mustafa sandal|manga|mor ve otesi|baris manco|cem karaca|sertab erener|kenan dogulu|murat boz|edis|reynmen|ece seckin|irem derici|bensu soral|coldplay|imagine dragons|maroon 5|one direction", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.POP, 0.85f)),
        Regex("led zeppelin|pink floyd|queen|ac/dc|the beatles|rolling stones|nirvana|foo fighters|radiohead|u2|muse|arctic monkeys|linkin park|green day|red hot chili|oasis|pearl jam|twenty one pilots|the killers|duman", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.ROCK, 0.85f)),
        Regex("sza|frank ocean|daniel caesar|h\\.e\\.r|summer walker|jhené aiko|khalid|usher|alicia keys|john legend|marvin gaye|stevie wonder|aretha franklin", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.RNB, 0.85f)),
        Regex("johnny cash|dolly parton|willie nelson|luke combs|morgan wallen|chris stapleton|carrie underwood|zach bryan", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.COUNTRY, 0.90f)),
        Regex("bob marley|damian marley|chronixx|sean paul|shaggy", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.REGGAE, 0.90f)),
        Regex("bad bunny|j balvin|daddy yankee|ozuna|karol g|maluma|rosalía|shakira|peso pluma", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.LATIN, 0.88f)),
        Regex("modern talking|haddaway|ace of base|gala|kaoma|laura branigan|snap|army of lovers|boney m|rick astley|a-ha|berlin|cutting crew|eurythmics|kylie minogue|britney spears|madonna|michael jackson|george michael|bee gees|phil collins|chicago|the police|abba|sade|kate bush|pet shop boys|depeche mode", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.POP, 0.88f)),
        Regex("bob dylan|joni mitchell|mumford|the lumineers|bon iver|fleet foxes|hozier|vance joy|phoebe bridgers", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.FOLK, 0.85f)),
        Regex("b\\.b\\. king|muddy waters|buddy guy|stevie ray vaughan|eric clapton|joe bonamassa|gary clark jr", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.BLUES, 0.88f)),
        Regex("tame impala|mac demarco|sufjan stevens|japanese breakfast|mitski|beach house|glass animals|the national|arcade fire", RegexOption.IGNORE_CASE) to listOf(GenreProb(Genre.INDIE, 0.80f)),
    )

    private val moodPatterns = listOf(
        Regex("love|heart|kiss|baby|darling|romance", RegexOption.IGNORE_CASE) to Mood.ROMANTIC,
        Regex("sad|cry|tears|lonely|broken|pain|hurt|miss you", RegexOption.IGNORE_CASE) to Mood.MELANCHOLIC,
        Regex("happy|joy|fun|party|celebrate|dance|yeah", RegexOption.IGNORE_CASE) to Mood.HAPPY,
        Regex("rage|anger|hate|kill|destroy|war|fight|fury", RegexOption.IGNORE_CASE) to Mood.AGGRESSIVE,
        Regex("peace|calm|relax|chill|gentle|soft|quiet", RegexOption.IGNORE_CASE) to Mood.CALM,
        Regex("dark|shadow|night|death|demon|evil|doom", RegexOption.IGNORE_CASE) to Mood.DARK,
        Regex("fire|burn|thunder|storm|power|beast|energy", RegexOption.IGNORE_CASE) to Mood.ENERGETIC,
        Regex("dream|float|fly|cloud|sky|star|cosmic", RegexOption.IGNORE_CASE) to Mood.DREAMY,
    )

    private val packageMedia = listOf(
        Regex("spotify|music|deezer|tidal|soundcloud|pandora|poweramp|vlc|musicolet|anghami", RegexOption.IGNORE_CASE) to MediaType.MUSIC,
        Regex("podcast|pocket\\.casts|castbox|overcast", RegexOption.IGNORE_CASE) to MediaType.PODCAST,
        Regex("audible|audiobook|libby|storytel", RegexOption.IGNORE_CASE) to MediaType.AUDIOBOOK,
        Regex("youtube(?!\\.music)|twitch|netflix|disney|hbo|prime\\.video", RegexOption.IGNORE_CASE) to MediaType.VIDEO,
    )

    private val defaultEnergy = mapOf(Genre.POP to 0.65f, Genre.ROCK to 0.70f, Genre.METAL to 0.85f, Genre.HIP_HOP to 0.75f, Genre.ELECTRONIC to 0.80f, Genre.DANCE to 0.85f, Genre.JAZZ to 0.40f, Genre.BLUES to 0.45f, Genre.CLASSICAL to 0.35f, Genre.COUNTRY to 0.55f, Genre.FOLK to 0.40f, Genre.REGGAE to 0.55f, Genre.LATIN to 0.75f, Genre.AMBIENT to 0.20f, Genre.RNB to 0.50f, Genre.SOUL to 0.50f, Genre.FUNK to 0.70f, Genre.PUNK to 0.80f, Genre.INDIE to 0.50f, Genre.ALTERNATIVE to 0.55f)
    private val defaultMood = mapOf(Genre.POP to Mood.HAPPY, Genre.ROCK to Mood.ENERGETIC, Genre.METAL to Mood.AGGRESSIVE, Genre.HIP_HOP to Mood.ENERGETIC, Genre.ELECTRONIC to Mood.ENERGETIC, Genre.JAZZ to Mood.CALM, Genre.CLASSICAL to Mood.CALM, Genre.COUNTRY to Mood.HAPPY, Genre.FOLK to Mood.CALM, Genre.REGGAE to Mood.HAPPY, Genre.LATIN to Mood.ENERGETIC, Genre.AMBIENT to Mood.DREAMY, Genre.RNB to Mood.ROMANTIC, Genre.BLUES to Mood.MELANCHOLIC)

    fun classify(track: SonaraTrackInfo): ClassificationResult {
        val reasons = mutableListOf<String>()
        val scores = mutableMapOf<Genre, Float>()

        // Artist match
        for ((regex, probs) in artistPatterns) {
            if (regex.containsMatchIn(track.artist)) {
                for (gp in probs) scores[gp.genre] = (scores[gp.genre] ?: 0f) + gp.prob * 0.85f
                reasons.add("Artist '${track.artist}' matched → ${probs.first().genre}")
                break
            }
        }

        // Media type
        var mediaType = MediaType.UNKNOWN
        for ((regex, type) in packageMedia) { if (regex.containsMatchIn(track.packageName)) { mediaType = type; break } }
        if (mediaType == MediaType.UNKNOWN && track.durationMs > 0) {
            mediaType = when { track.durationMs > 20 * 60 * 1000 -> MediaType.PODCAST; track.durationMs in 60_000..600_000 -> MediaType.MUSIC; else -> MediaType.UNKNOWN }
        }
        if (mediaType == MediaType.PODCAST) { scores[Genre.PODCAST] = (scores[Genre.PODCAST] ?: 0f) + 0.7f; reasons.add("Package suggests podcast") }

        // Genre from scores
        val topEntry = scores.maxByOrNull { it.value }
        val genre = topEntry?.key ?: Genre.UNKNOWN
        val totalScore = scores.values.sum().coerceAtLeast(0.01f)
        val confidence = if (topEntry != null) (topEntry.value / totalScore).coerceIn(0.1f, 0.95f) else 0.1f

        // Mood from title
        var mood = defaultMood[genre] ?: Mood.NEUTRAL
        for ((regex, m) in moodPatterns) { if (regex.containsMatchIn(track.title)) { mood = m; reasons.add("Title mood → $m"); break } }

        val energy = defaultEnergy[genre] ?: 0.5f

        return ClassificationResult(genre, mood, energy, confidence, if (mediaType == MediaType.UNKNOWN) MediaType.MUSIC else mediaType, reasons)
    }
}
