package com.sonara.app.brain

import android.content.Context
import com.sonara.app.data.SonaraLogger
import com.sonara.app.engine.eq.EqComposer
import com.sonara.app.intelligence.pipeline.*
import com.sonara.app.intelligence.provider.InsightProviderManager
import com.sonara.app.intelligence.provider.InsightRequest
import com.sonara.app.preset.Preset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * SonaraBrain — Hibrit Zeka Merkezi
 *
 * Tier 0 (Her zaman):  Kural tabanlı keyword parser  (offline, <1ms)
 * Tier 1 (Fallback):   Cloud AI provider             (Gemini/Groq, ~200ms)
 *
 * AI hiçbir zaman doğrudan EQ değeri üretmez.
 * AI sadece intent JSON döner → kural motoru işler.
 */
class SonaraBrain(
    private val context: Context,
    private val composer: EqComposer,
    private val insightManager: InsightProviderManager
) {
    companion object {
        private const val TAG = "SonaraBrain"
        private const val MAX_UNDO_DEPTH = 10
    }

    // ══════════════════════════════════════════════════════════════════
    // STATE
    // ══════════════════════════════════════════════════════════════════

    private val _brainState = MutableStateFlow(BrainState())
    val brainState: StateFlow<BrainState> = _brainState.asStateFlow()

    private val undoStack = ArrayDeque<FinalEqProfile>(MAX_UNDO_DEPTH)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ══════════════════════════════════════════════════════════════════
    // COMMAND PROCESSING — Tier 0: Keyword Parser (offline, instant)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Şarkı geçişinde ya da preset uygulandığında mevcut profili undo stack'e kaydeder.
     */
    fun pushToUndoStack(profile: FinalEqProfile) {
        if (undoStack.size >= MAX_UNDO_DEPTH) undoStack.removeFirst()
        undoStack.addLast(profile)
    }

    fun popUndo(): FinalEqProfile? = if (undoStack.isNotEmpty()) undoStack.removeLast() else null

    /**
     * Kullanıcı komutunu işler.
     * Önce Tier 0 keyword parser dener, yüksek güven varsa direkt uygular.
     * Düşük güvende Cloud AI'ya gönderir.
     */
    suspend fun processCommand(
        rawCommand: String,
        currentProfile: FinalEqProfile,
        currentTrack: SonaraTrackInfo?
    ): CommandResult {
        SonaraLogger.i(TAG, "Command: \"$rawCommand\"")

        // Tier 0: Keyword match
        val keywordResult = KeywordParser.parse(rawCommand)
        if (keywordResult != null && keywordResult.confidence >= 0.85f) {
            SonaraLogger.i(TAG, "Keyword hit: ${keywordResult.intent} (conf=${keywordResult.confidence})")
            return applyIntent(keywordResult, currentProfile)
        }

        // Tier 1: Cloud AI
        return try {
            val aiIntent = callCloudAi(rawCommand, currentProfile, currentTrack)
            if (aiIntent != null) applyIntent(aiIntent, currentProfile)
            else CommandResult.Error("Komut anlaşılamadı.")
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Cloud AI failed: ${e.message}")
            CommandResult.Error("AI yanıt vermedi. İnternet bağlantınızı kontrol edin.")
        }
    }

    private suspend fun callCloudAi(
        rawCommand: String,
        current: FinalEqProfile,
        track: SonaraTrackInfo?
    ): ParsedIntent? {
        val currentEqJson = buildString {
            append("{\"bands\":[")
            append(current.bands.joinToString(",") { "%.1f".format(it) })
            append("],\"bassBoost\":${current.bassBoost},\"virtualizer\":${current.virtualizer},\"loudness\":${current.loudness},\"reverb\":${current.reverb}}")
        }

        val trackInfo = track?.let { "\"${it.title}\" by \"${it.artist}\"" } ?: "unknown"

        val prompt = """
You are Sonara, an audio engineering AI. Translate the user's EQ command into a JSON intent.
Track: $trackInfo
Current EQ: $currentEqJson

Return ONLY valid JSON (no markdown):
{"intent":"INCREASE_BASS|DECREASE_BASS|INCREASE_TREBLE|DECREASE_TREBLE|CLARITY_MODE|VOCAL_BOOST|GENRE_OVERRIDE|RESET_EQ|UNDO","bandDeltas":[0,0,0,0,0,0,0,0,0,0],"bassBoostDelta":0,"virtualizerDelta":0,"loudnessDelta":0,"reverbPreset":-1,"confidence":0.9,"message":""}

User command: "$rawCommand"
""".trimIndent()

        val result = insightManager.getInsight(InsightRequest(prompt = prompt, maxTokens = 300))
        val json = result?.text?.let { extractJson(it) } ?: return null

        return try {
            val obj = JSONObject(json)
            val deltas = obj.optJSONArray("bandDeltas")?.let { arr ->
                FloatArray(10) { i -> arr.optDouble(i, 0.0).toFloat() }
            } ?: FloatArray(10)
            ParsedIntent(
                intent = CommandIntent.valueOf(obj.optString("intent", "UNKNOWN").uppercase().let {
                    if (it in CommandIntent.entries.map { e -> e.name }) it else "UNKNOWN"
                }),
                bandDeltas = deltas,
                bassBoostDelta = obj.optInt("bassBoostDelta", 0),
                virtualizerDelta = obj.optInt("virtualizerDelta", 0),
                loudnessDelta = obj.optInt("loudnessDelta", 0),
                reverbPreset = obj.optInt("reverbPreset", -1),
                confidence = obj.optDouble("confidence", 0.7).toFloat(),
                message = obj.optString("message", "")
            )
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "AI JSON parse failed: ${e.message}")
            null
        }
    }

    private fun applyIntent(intent: ParsedIntent, current: FinalEqProfile): CommandResult {
        return when (intent.intent) {
            CommandIntent.UNDO -> {
                val prev = popUndo()
                if (prev != null) CommandResult.Apply(prev, "Geri alındı.")
                else CommandResult.Error("Geri alınacak bir değişiklik yok.")
            }
            CommandIntent.RESET_EQ -> {
                val flat = FinalEqProfile(FloatArray(10), 0f, 0, 0, 0, current.prediction, 0)
                CommandResult.Apply(flat, "EQ sıfırlandı.")
            }
            CommandIntent.UNKNOWN -> CommandResult.Error("Komut anlaşılamadı.")
            else -> {
                val newBands = FloatArray(10) { i ->
                    (current.bands.getOrElse(i) { 0f } + intent.bandDeltas.getOrElse(i) { 0f }).coerceIn(-12f, 12f)
                }
                val newBass = (current.bassBoost + intent.bassBoostDelta).coerceIn(0, 1000)
                val newVirt = (current.virtualizer + intent.virtualizerDelta).coerceIn(0, 1000)
                val newLoud = (current.loudness + intent.loudnessDelta).coerceIn(0, 3000)
                val newReverb = if (intent.reverbPreset >= 0) intent.reverbPreset.coerceIn(0, 6) else current.reverb
                val newProfile = FinalEqProfile(newBands, current.preamp, newBass, newVirt, newLoud, current.prediction, newReverb)
                CommandResult.Apply(newProfile, intent.message.ifBlank { "Uygulandı." })
            }
        }
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else null
    }

    fun destroy() { scope.cancel() }
}

// ══════════════════════════════════════════════════════════════════
// KEYWORD PARSER — Tier 0, offline, ~70 pattern (TR + EN)
// ══════════════════════════════════════════════════════════════════

object KeywordParser {
    // Band index reference: 0=31Hz 1=62Hz 2=125Hz 3=250Hz 4=500Hz 5=1kHz 6=2kHz 7=4kHz 8=8kHz 9=16kHz

    private data class Pattern(val regex: Regex, val intent: CommandIntent, val bandDeltas: FloatArray, val bassBoostDelta: Int = 0, val virtualizerDelta: Int = 0, val reverbPreset: Int = -1)

    private val PATTERNS = listOf(
        // ── Bass artır ──
        Pattern(Regex("(daha fazla|çok|artır|boost|more|increase).*(bas|bass|alçak|low)", RegexOption.IGNORE_CASE),
            CommandIntent.INCREASE_BASS, floatArrayOf(3f, 4f, 3f, 1f, 0f, 0f, 0f, 0f, 0f, 0f), bassBoostDelta = 150),
        Pattern(Regex("^(bass\\+|bass up|bass boost)$", RegexOption.IGNORE_CASE),
            CommandIntent.INCREASE_BASS, floatArrayOf(3f, 4f, 3f, 1f, 0f, 0f, 0f, 0f, 0f, 0f), bassBoostDelta = 150),

        // ── Bass azalt ──
        Pattern(Regex("(az|azalt|daha az|less|decrease|cut|düşür).*(bas|bass|alçak|low)", RegexOption.IGNORE_CASE),
            CommandIntent.DECREASE_BASS, floatArrayOf(-3f, -4f, -3f, -1f, 0f, 0f, 0f, 0f, 0f, 0f), bassBoostDelta = -150),

        // ── Tiz artır ──
        Pattern(Regex("(daha fazla|artır|more|increase|boost).*(tiz|treble|yüksek|high|parlak|bright)", RegexOption.IGNORE_CASE),
            CommandIntent.INCREASE_TREBLE, floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 3f, 3f)),
        // ── Tiz azalt ──
        Pattern(Regex("(az|azalt|less|cut|düşür).*(tiz|treble|yüksek|high)", RegexOption.IGNORE_CASE),
            CommandIntent.DECREASE_TREBLE, floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, -2f, -3f, -3f)),

        // ── Vokal öne çıkar ──
        Pattern(Regex("(vokal|vocal|ses|şarkıcı|singer).*(öne|ön|forward|boost|artır|net|clear)", RegexOption.IGNORE_CASE),
            CommandIntent.VOCAL_BOOST, floatArrayOf(0f, -1f, 0f, 1f, 2f, 3f, 2f, 1f, 0f, -1f)),
        Pattern(Regex("(bring out|boost).*(vocal|voice|singing)", RegexOption.IGNORE_CASE),
            CommandIntent.VOCAL_BOOST, floatArrayOf(0f, -1f, 0f, 1f, 2f, 3f, 2f, 1f, 0f, -1f)),

        // ── Sesi temizle / Clarity ──
        Pattern(Regex("(sesi temizle|temizle|netleştir|clarity|clean|clear|crisp)", RegexOption.IGNORE_CASE),
            CommandIntent.CLARITY_MODE, floatArrayOf(0f, 0f, 0f, -1f, 0f, 1f, 2f, 3f, 1f, 0f)),

        // ── Reverb ──
        Pattern(Regex("(reverb|echo|yankı|salon|hall|room|büyük.*mekan).*(aç|ekle|on|add|açık)", RegexOption.IGNORE_CASE),
            CommandIntent.REVERB_ON, FloatArray(10), reverbPreset = 3),
        Pattern(Regex("(reverb|echo|yankı).*(kapat|kapa|off|sil|kaldır)", RegexOption.IGNORE_CASE),
            CommandIntent.REVERB_OFF, FloatArray(10), reverbPreset = 0),
        Pattern(Regex("küçük.*oda|small.*room", RegexOption.IGNORE_CASE),
            CommandIntent.REVERB_ON, FloatArray(10), reverbPreset = 1),
        Pattern(Regex("büyük.*salon|large.*hall|concert.*hall|konser.*salonu", RegexOption.IGNORE_CASE),
            CommandIntent.REVERB_ON, FloatArray(10), reverbPreset = 5),

        // ── Surround / Virtualizer ──
        Pattern(Regex("(surround|geniş|wide|spatial|mekânsal|sürraund).*(aç|artır|on|add)", RegexOption.IGNORE_CASE),
            CommandIntent.SURROUND_ON, FloatArray(10), virtualizerDelta = 300),
        Pattern(Regex("(surround|geniş).*(kapat|off|azalt|kaldır)", RegexOption.IGNORE_CASE),
            CommandIntent.SURROUND_OFF, FloatArray(10), virtualizerDelta = -300),

        // ── Sıfırla ──
        Pattern(Regex("^(sıfırla|sıfır|reset|düzelt|flat|düz)$", RegexOption.IGNORE_CASE),
            CommandIntent.RESET_EQ, FloatArray(10)),
        // ── Geri al ──
        Pattern(Regex("^(geri al|geri|undo|önceki|previous)$", RegexOption.IGNORE_CASE),
            CommandIntent.UNDO, FloatArray(10))
    )

    fun parse(rawCommand: String): ParsedIntent? {
        val cmd = rawCommand.trim()
        for (p in PATTERNS) {
            if (p.regex.containsMatchIn(cmd)) {
                return ParsedIntent(
                    intent = p.intent,
                    bandDeltas = p.bandDeltas,
                    bassBoostDelta = p.bassBoostDelta,
                    virtualizerDelta = p.virtualizerDelta,
                    reverbPreset = p.reverbPreset,
                    confidence = 0.90f,
                    message = ""
                )
            }
        }
        return null
    }
}

// ══════════════════════════════════════════════════════════════════
// DATA MODELS
// ══════════════════════════════════════════════════════════════════

enum class CommandIntent {
    INCREASE_BASS, DECREASE_BASS,
    INCREASE_TREBLE, DECREASE_TREBLE,
    VOCAL_BOOST, CLARITY_MODE,
    REVERB_ON, REVERB_OFF,
    SURROUND_ON, SURROUND_OFF,
    GENRE_OVERRIDE, RESET_EQ, UNDO, UNKNOWN;

    companion object {
        val entries = values().toList()
    }
}

data class ParsedIntent(
    val intent: CommandIntent,
    val bandDeltas: FloatArray = FloatArray(10),
    val bassBoostDelta: Int = 0,
    val virtualizerDelta: Int = 0,
    val loudnessDelta: Int = 0,
    val reverbPreset: Int = -1,
    val confidence: Float = 0f,
    val message: String = ""
)

data class BrainState(
    val isProcessing: Boolean = false,
    val lastCommandMessage: String = "",
    val canUndo: Boolean = false
)

sealed class CommandResult {
    data class Apply(val profile: FinalEqProfile, val message: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
}
