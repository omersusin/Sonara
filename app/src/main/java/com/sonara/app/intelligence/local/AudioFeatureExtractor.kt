package com.sonara.app.intelligence.local

data class AudioFeatures(
    val estimatedGenre: String = "other", val estimatedMood: String = "neutral",
    val estimatedEnergy: Float = 0.5f, val bassNeed: Float = 0.5f, val trebleNeed: Float = 0.5f,
    val vocalPresence: Float = 0.5f, val brightness: Float = 0.5f, val confidence: Float = 0.3f
)

class AudioFeatureExtractor {

    private val artistGenreDb: Map<String, String> by lazy { buildArtistDb() }

    private fun buildArtistDb(): Map<String, String> {
        val db = mutableMapOf<String, String>()
        listOf("taylor swift","ed sheeran","dua lipa","billie eilish","the weeknd","adele","ariana grande","justin bieber","harry styles","olivia rodrigo","bruno mars","lady gaga","katy perry","rihanna","sia","shawn mendes","doja cat","charlie puth","sam smith","selena gomez","miley cyrus","camila cabello","bebe rexha","halsey","lizzo","sabrina carpenter","chappell roan","tate mcrae","lorde","troye sivan","ellie goulding","zara larsson","charli xcx","demi lovato","lana del rey","the 1975","5 seconds of summer","one direction","backstreet boys","britney spears","christina aguilera","madonna","michael jackson","whitney houston","mariah carey","celine dion","pink","shakira","jennifer lopez","meghan trainor","jason derulo","pitbull","ava max","benson boone","gracie abrams","madison beer","conan gray","girl in red","coldplay","imagine dragons","maroon 5","tarkan","sezen aksu","sertab erener","hadise","ajda pekkan","gülşen","aleyna tilki","mabel matiz","zeynep bastık","sefo","ece seçkin","simge","demet akalın","hande yener","bengü","irem derici","buray","murat boz","edis","bts","blackpink","twice","stray kids","aespa","newjeans","le sserafim","ive","seventeen","txt","nct","red velvet","itzy","yoasobi","ado","kenshi yonezu").forEach { db[it] = "pop" }
        listOf("queen","the beatles","led zeppelin","pink floyd","nirvana","foo fighters","arctic monkeys","radiohead","muse","u2","green day","linkin park","red hot chili peppers","the killers","oasis","pearl jam","ac/dc","guns n' roses","the rolling stones","aerosmith","bon jovi","deep purple","black sabbath","the who","weezer","blink-182","paramore","fall out boy","panic! at the disco","my chemical romance","twenty one pilots","the strokes","kings of leon","tame impala","cage the elephant","the black keys","royal blood","greta van fleet","glass animals","nothing but thieves","duman","manga","mor ve ötesi","mfö","barış manço","cem karaca","erkin koray","teoman","şebnem ferah","hayko cepkin","athena","pinhani","gripin","model","vega").forEach { db[it] = "rock" }
        listOf("drake","kendrick lamar","travis scott","kanye west","j. cole","eminem","lil baby","21 savage","future","migos","cardi b","nicki minaj","lil nas x","jack harlow","tyler, the creator","metro boomin","lil uzi vert","playboi carti","a\$ap rocky","post malone","juice wrld","xxxtentacion","pop smoke","roddy ricch","megan thee stallion","ice spice","central cee","dave","stormzy","jay-z","nas","50 cent","lil wayne","snoop dogg","dr. dre","mac miller","jid","denzel curry","baby keem","gunna","young thug","lil durk","polo g","ezhel","ben fero","ceza","sagopa kajmer","şanışer","norm ender","contra","allame","joker","hidra","uzi","lvbel c5","khontkar","massaka").forEach { db[it] = "hip-hop" }
        listOf("daft punk","calvin harris","marshmello","avicii","deadmau5","skrillex","tiesto","zedd","kygo","martin garrix","david guetta","alan walker","the chainsmokers","diplo","flume","porter robinson","illenium","odesza","disclosure","kaytranada","fred again","four tet","jamie xx","aphex twin","kraftwerk","depeche mode","new order","the chemical brothers","the prodigy","fatboy slim","above & beyond","armin van buuren","eric prydz","swedish house mafia","alesso","hardwell","peggy gou","amelie lens","charlotte de witte").forEach { db[it] = "electronic" }
        listOf("sza","frank ocean","daniel caesar","h.e.r.","summer walker","jhené aiko","bryson tiller","chris brown","usher","alicia keys","john legend","khalid","brent faiyaz","6lack","kehlani","snoh aalegra","jorja smith","miguel","anderson .paak","silk sonic","giveon","steve lacy","victoria monét","tyla","erykah badu","lauryn hill","d'angelo","marvin gaye","stevie wonder","aretha franklin","prince","janet jackson","destiny's child","mary j. blige").forEach { db[it] = "r&b" }
        listOf("metallica","iron maiden","slipknot","avenged sevenfold","system of a down","megadeth","pantera","tool","korn","rammstein","disturbed","bring me the horizon","slayer","judas priest","lamb of god","gojira","mastodon","opeth","trivium","architects","spiritbox","meshuggah","periphery","dream theater","nightwish","sabaton","powerwolf","amon amarth","behemoth","cannibal corpse","sepultura").forEach { db[it] = "metal" }
        listOf("miles davis","john coltrane","louis armstrong","duke ellington","charlie parker","thelonious monk","billie holiday","ella fitzgerald","nina simone","herbie hancock","norah jones","diana krall","michael bublé","kamasi washington","robert glasper","snarky puppy","jacob collier","thundercat","esperanza spalding","gregory porter").forEach { db[it] = "jazz" }
        listOf("beethoven","mozart","bach","chopin","vivaldi","debussy","tchaikovsky","brahms","schubert","rachmaninoff","prokofiev","shostakovich","mahler","wagner","verdi","puccini","handel","haydn","ravel","satie","stravinsky","yo-yo ma","lang lang","hans zimmer","joe hisaishi","ennio morricone","john williams","ludovico einaudi","yiruma","max richter","ólafur arnalds","nils frahm").forEach { db[it] = "classical" }
        listOf("luke combs","morgan wallen","zach bryan","chris stapleton","luke bryan","jason aldean","carrie underwood","kane brown","dolly parton","johnny cash","willie nelson","garth brooks","kacey musgraves","miranda lambert","jelly roll","noah kahan","tyler childers").forEach { db[it] = "country" }
        listOf("bon iver","fleet foxes","iron & wine","sufjan stevens","mumford & sons","the lumineers","hozier","vance joy","passenger","bob dylan","leonard cohen","joni mitchell","tracy chapman","phoebe bridgers","big thief","father john misty","müslüm gürses","neşet ertaş").forEach { db[it] = "folk" }
        listOf("b.b. king","muddy waters","robert johnson","buddy guy","eric clapton","stevie ray vaughan","joe bonamassa","gary clark jr.").forEach { db[it] = "blues" }
        listOf("bob marley","peter tosh","jimmy cliff","damian marley","chronixx","shaggy","sean paul").forEach { db[it] = "reggae" }
        listOf("bad bunny","j balvin","daddy yankee","ozuna","karol g","rauw alejandro","maluma","rosalía","peso pluma","feid","myke towers","anitta").forEach { db[it] = "latin" }
        return db
    }

    data class WK(val keyword: String, val weight: Int)

    private val genreKeywords: Map<String, List<WK>> = mapOf(
        "pop" to listOf(WK("(radio edit)",5),WK("(single version)",5),WK("pop version",5),WK("dance remix",4),WK("tonight",2),WK("summer",2),WK("forever",2),WK("radio",2),WK("love",1),WK("baby",1),WK("heart",1)),
        "hip-hop" to listOf(WK("freestyle",5),WK("cypher",5),WK("diss",5),WK("drill",5),WK("trap",4),WK("gang",3),WK("hustle",3),WK("grind",3),WK("flex",3),WK("choppa",4),WK("drip",3),WK("lil ",2),WK("yung ",2),WK("feat.",1),WK("ft.",1)),
        "electronic" to listOf(WK("(extended mix)",5),WK("(club mix)",5),WK("(original mix)",5),WK("(dj ",4),WK("techno",4),WK("house",3),WK("trance",4),WK("dubstep",4),WK("edm",4),WK("rave",3),WK("synth",2),WK("drop",2),WK("remix",1)),
        "rock" to listOf(WK("rock",3),WK("anthem",2),WK("rebel",2),WK("thunder",2),WK("electric",1),WK("guitar",2),WK("punk",4),WK("grunge",4)),
        "metal" to listOf(WK("death",2),WK("blood",2),WK("demon",3),WK("hell",2),WK("chaos",2),WK("infernal",4),WK("brutal",3),WK("destruction",3),WK("doom",3),WK("corpse",4),WK("scream",2),WK("rage",2)),
        "classical" to listOf(WK("symphony",5),WK("concerto",5),WK("opus",5),WK("op.",4),WK("sonata",5),WK("orchestra",5),WK("quartet",3),WK("prelude",4),WK("nocturne",5),WK("etude",5),WK("requiem",5),WK("in d minor",5),WK("in c major",5),WK("allegro",5),WK("adagio",5),WK("fugue",5)),
        "jazz" to listOf(WK("swing",3),WK("bop",4),WK("bebop",5),WK("smooth jazz",5),WK("cool jazz",5),WK("blue note",4),WK("jazz",4)),
        "folk" to listOf(WK("folk",3),WK("unplugged",2),WK("traditional",3),WK("campfire",3),WK("ballad",2),WK("indie folk",5)),
        "country" to listOf(WK("country",4),WK("cowboy",4),WK("truck",3),WK("whiskey",3),WK("nashville",4),WK("honky tonk",5),WK("rodeo",4),WK("bourbon",3)),
        "blues" to listOf(WK("blues",4),WK("12-bar",5),WK("delta",2),WK("chicago blues",5),WK("boogie",3)),
        "r&b" to listOf(WK("r&b",5),WK("rnb",5),WK("slow jam",4),WK("neo-soul",5),WK("neo soul",5),WK("groove",2),WK("soul",2),WK("funk",3)),
        "reggae" to listOf(WK("reggae",5),WK("ska",4),WK("dub",3),WK("dancehall",5),WK("rasta",4),WK("riddim",4)),
        "latin" to listOf(WK("reggaeton",5),WK("salsa",4),WK("bachata",5),WK("cumbia",5),WK("bossa nova",5),WK("perreo",5),WK("dembow",5),WK("corrido",5))
    )

    private val moodKeywords = mapOf(
        "melancholic" to listOf("sad","cry","tears","alone","lonely","pain","rain","broken","lost","miss","goodbye"),
        "happy" to listOf("happy","joy","sun","smile","dance","party","fun","good","yeah","celebration"),
        "energetic" to listOf("fire","power","fight","run","wild","crazy","fast","beast","bang","roar"),
        "romantic" to listOf("love","kiss","heart","forever","beautiful","dream","angel","darling"),
        "chill" to listOf("night","sleep","calm","peace","quiet","soft","gentle","slow","breeze"),
        "intense" to listOf("burn","scream","chaos","storm","fury","wrath","fierce")
    )

    fun extract(title: String, artist: String): AudioFeatures {
        val lT = title.lowercase().trim(); val lA = artist.lowercase().trim(); val combined = "$lT $lA"
        var genre = "other"; var confidence = 0.1f
        val artistMatch = artistGenreDb.entries.firstOrNull { (k, _) -> lA.contains(k) || lA == k }
        if (artistMatch != null) { genre = artistMatch.value; confidence = 0.75f }
        else {
            val scores = mutableMapOf<String, Int>()
            genreKeywords.forEach { (g, kws) -> var s = 0; kws.forEach { if (combined.contains(it.keyword)) { s += it.weight; if (lT.contains(it.keyword)) s += it.weight / 2 } }; if (s > 0) scores[g] = s }
            if (scores.isNotEmpty()) {
                val sorted = scores.entries.sortedByDescending { it.value }; val top = sorted.first(); val runner = sorted.getOrNull(1)?.value ?: 0
                if (top.value >= 3 && (top.value - runner) >= 2) { genre = top.key; confidence = (top.value / 12f).coerceIn(0.3f, 0.55f) }
                else if (top.value >= 5) { genre = top.key; confidence = (top.value / 15f).coerceIn(0.25f, 0.45f) }
            }
            if (genre == "other" && (combined.contains("feat") || combined.contains("ft."))) { genre = "pop"; confidence = 0.35f }
        }
        val mood = detectMood(combined); val energy = estimateEnergy(genre, mood)
        return AudioFeatures(genre, mood, energy, bassFor(genre), trebleFor(genre), vocalFor(genre), brightFor(genre, mood), confidence)
    }

    private fun detectMood(t: String): String { var b = "neutral"; var bs = 0; moodKeywords.forEach { (m, kws) -> val s = kws.count { t.contains(it) }; if (s > bs) { bs = s; b = m } }; return b }
    private fun estimateEnergy(g: String, m: String): Float { val ge = mapOf("metal" to 0.9f,"electronic" to 0.8f,"hip-hop" to 0.7f,"rock" to 0.7f,"latin" to 0.7f,"pop" to 0.6f,"reggae" to 0.55f,"r&b" to 0.5f,"country" to 0.5f,"blues" to 0.45f,"jazz" to 0.4f,"folk" to 0.35f,"classical" to 0.3f); val me = mapOf("energetic" to 0.2f,"intense" to 0.15f,"happy" to 0.1f,"romantic" to -0.05f,"chill" to -0.15f,"melancholic" to -0.1f); return ((ge[g] ?: 0.5f) + (me[m] ?: 0f)).coerceIn(0.1f, 1f) }
    private fun bassFor(g: String) = mapOf("hip-hop" to 0.85f,"electronic" to 0.8f,"latin" to 0.75f,"r&b" to 0.7f,"reggae" to 0.7f,"metal" to 0.65f,"rock" to 0.6f,"pop" to 0.55f,"blues" to 0.55f,"country" to 0.45f,"jazz" to 0.4f,"classical" to 0.3f,"folk" to 0.3f)[g] ?: 0.5f
    private fun trebleFor(g: String) = mapOf("classical" to 0.75f,"jazz" to 0.7f,"folk" to 0.65f,"pop" to 0.6f,"rock" to 0.6f,"country" to 0.6f,"electronic" to 0.55f,"blues" to 0.55f,"metal" to 0.5f,"r&b" to 0.5f,"latin" to 0.55f,"hip-hop" to 0.4f,"reggae" to 0.45f)[g] ?: 0.5f
    private fun vocalFor(g: String) = mapOf("pop" to 0.8f,"r&b" to 0.8f,"folk" to 0.75f,"country" to 0.75f,"hip-hop" to 0.7f,"blues" to 0.7f,"latin" to 0.7f,"reggae" to 0.65f,"jazz" to 0.6f,"rock" to 0.6f,"metal" to 0.5f,"electronic" to 0.3f,"classical" to 0.2f)[g] ?: 0.5f
    private fun brightFor(g: String, m: String): Float { val b = mapOf("pop" to 0.7f,"classical" to 0.65f,"jazz" to 0.6f,"folk" to 0.6f,"country" to 0.6f,"electronic" to 0.6f,"latin" to 0.6f,"rock" to 0.5f,"blues" to 0.5f,"r&b" to 0.55f,"reggae" to 0.5f,"hip-hop" to 0.45f,"metal" to 0.4f)[g] ?: 0.5f; val mm = if (m == "melancholic") -0.1f else if (m == "happy") 0.1f else 0f; return (b + mm).coerceIn(0.1f, 1f) }
}
