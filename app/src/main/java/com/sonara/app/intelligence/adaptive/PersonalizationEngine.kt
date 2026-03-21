package com.sonara.app.intelligence.adaptive

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.pipeline.AudioRoute
import com.sonara.app.intelligence.pipeline.Genre
import java.io.File

/**
 * Madde 16/17: Adaptive Personalization Engine.
 * "Self-training AI" değil, "adaptive personalization engine".
 *
 * Toplanan veriler:
 * - genre/subgenre/tags
 * - route/device
 * - önerilen EQ vs kullanıcının yaptığı değişiklik
 * - favorite / skip / revert
 *
 * Final mantık:
 * Base prediction + Personal delta + Route correction + Safety clamp
 */
class PersonalizationEngine(private val context: Context) {

    companion object {
        private const val FILE_PROFILES = "sonara_personalization.json"
        private const val FILE_STATS = "sonara_listen_stats.json"
        private const val LEARNING_RATE = 0.15f
        private const val MAX_DELTA = 4.0f
        private const val BANDS = 10
    }

    data class PersonalProfile(
        val offsets: List<Float>,
        val samples: Int,
        val lastUpdated: Long,
        val bassBias: Float = 0f,     // kullanıcı bass tercihi: >0 bass sever
        val trebleBias: Float = 0f,   // kullanıcı treble tercihi
        val acceptRate: Float = 1f    // önerilen EQ'yu kabul oranı
    )

    data class ListenStats(
        val totalTracks: Int = 0,
        val genreCounts: Map<String, Int> = emptyMap(),
        val routeCounts: Map<String, Int> = emptyMap(),
        val skipCount: Int = 0,
        val revertCount: Int = 0,
        val favoriteCount: Int = 0
    )

    private var profiles = mutableMapOf<String, PersonalProfile>()
    private var stats = ListenStats()
    private val gson = Gson()

    fun load() {
        try {
            val pFile = File(context.filesDir, FILE_PROFILES)
            if (pFile.exists()) {
                val type = object : TypeToken<Map<String, PersonalProfile>>() {}.type
                profiles = gson.fromJson(pFile.readText(), type) ?: mutableMapOf()
            }
            val sFile = File(context.filesDir, FILE_STATS)
            if (sFile.exists()) {
                stats = gson.fromJson(sFile.readText(), ListenStats::class.java) ?: ListenStats()
            }
        } catch (e: Exception) {
            SonaraLogger.w("Personalization", "Load error: ${e.message}")
            profiles = mutableMapOf()
        }
    }

    private fun save() {
        try {
            File(context.filesDir, FILE_PROFILES).writeText(gson.toJson(profiles))
            File(context.filesDir, FILE_STATS).writeText(gson.toJson(stats))
        } catch (_: Exception) {}
    }

    /** Genre + Route kombinasyonuna göre kişisel offset al */
    fun getPersonalOffset(genre: Genre, route: AudioRoute, subGenre: String? = null): FloatArray? {
        // Önce genre+route+subgenre ara
        if (!subGenre.isNullOrBlank()) {
            profiles["${genre.name}::${route.name}::$subGenre"]?.let {
                if (it.samples >= 2) return clampOffset(it.offsets.toFloatArray())
            }
        }
        // Sonra genre+route
        profiles["${genre.name}::${route.name}"]?.let {
            if (it.samples >= 2) return clampOffset(it.offsets.toFloatArray())
        }
        // En son sadece genre
        profiles[genre.name]?.let {
            if (it.samples >= 3) return clampOffset(it.offsets.toFloatArray())
        }
        return null
    }

    /** Global bass/treble bias al */
    fun getGlobalBias(): Pair<Float, Float> {
        val allProfiles = profiles.values
        if (allProfiles.isEmpty()) return 0f to 0f
        val avgBass = allProfiles.map { it.bassBias }.average().toFloat()
        val avgTreble = allProfiles.map { it.trebleBias }.average().toFloat()
        return avgBass to avgTreble
    }

    /**
     * Kullanıcı feedback'i kaydet.
     * aiSuggestion: AI'nın önerdiği EQ
     * userFinal: kullanıcının ayarladığı EQ
     */
    fun recordAdjustment(
        genre: Genre, route: AudioRoute, subGenre: String?,
        aiSuggestion: FloatArray, userFinal: FloatArray
    ) {
        val keys = mutableListOf("${genre.name}::${route.name}")
        if (!subGenre.isNullOrBlank()) keys.add("${genre.name}::${route.name}::$subGenre")
        keys.add(genre.name) // genel genre profili de güncelle

        for (key in keys) {
            val existing = profiles[key] ?: PersonalProfile(List(BANDS) { 0f }, 0, System.currentTimeMillis())
            val delta = FloatArray(BANDS) { i ->
                if (i < aiSuggestion.size && i < userFinal.size) userFinal[i] - aiSuggestion[i] else 0f
            }

            val newOffsets = List(BANDS) { i ->
                LEARNING_RATE * delta[i] + (1f - LEARNING_RATE) * existing.offsets[i]
            }

            // Bass/treble bias hesapla
            val bassDelta = (0..2).map { delta.getOrElse(it) { 0f } }.average().toFloat()
            val trebleDelta = (7..9).map { delta.getOrElse(it) { 0f } }.average().toFloat()

            profiles[key] = existing.copy(
                offsets = newOffsets,
                samples = existing.samples + 1,
                lastUpdated = System.currentTimeMillis(),
                bassBias = LEARNING_RATE * bassDelta + (1f - LEARNING_RATE) * existing.bassBias,
                trebleBias = LEARNING_RATE * trebleDelta + (1f - LEARNING_RATE) * existing.trebleBias
            )
        }
        save()
        SonaraLogger.ai("Personalization: recorded adjustment for ${genre.name}/${route.name}")
    }

    fun recordAccepted(genre: Genre, route: AudioRoute) {
        val key = "${genre.name}::${route.name}"
        val existing = profiles[key]
        if (existing != null) {
            val newRate = (existing.acceptRate * existing.samples + 1f) / (existing.samples + 1)
            profiles[key] = existing.copy(acceptRate = newRate, samples = existing.samples + 1, lastUpdated = System.currentTimeMillis())
            save()
        }
    }

    fun recordSkip() { stats = stats.copy(skipCount = stats.skipCount + 1); save() }
    fun recordRevert() { stats = stats.copy(revertCount = stats.revertCount + 1); save() }
    fun recordFavorite() { stats = stats.copy(favoriteCount = stats.favoriteCount + 1); save() }

    fun recordListen(genre: String, route: String) {
        val gc = stats.genreCounts.toMutableMap()
        gc[genre] = (gc[genre] ?: 0) + 1
        val rc = stats.routeCounts.toMutableMap()
        rc[route] = (rc[route] ?: 0) + 1
        stats = stats.copy(totalTracks = stats.totalTracks + 1, genreCounts = gc, routeCounts = rc)
        save()
    }

    fun getTotalSamples(): Int = profiles.values.sumOf { it.samples }
    fun getStats(): ListenStats = stats

    fun reset() {
        profiles.clear()
        stats = ListenStats()
        save()
        SonaraLogger.i("Personalization", "Tüm kişiselleştirme sıfırlandı")
    }

    private fun clampOffset(arr: FloatArray): FloatArray {
        return FloatArray(arr.size) { arr[it].coerceIn(-MAX_DELTA, MAX_DELTA) }
    }
}
