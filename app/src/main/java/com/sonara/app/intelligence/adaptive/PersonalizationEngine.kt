package com.sonara.app.intelligence.adaptive

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.pipeline.AudioRoute
import com.sonara.app.intelligence.pipeline.Genre
import java.io.File

/**
 * Adaptive Personalization Engine.
 * Genre+route kombinasyonuna göre bass/treble bias öğrenir.
 *
 * FIX: Gson tüm sayıları Double olarak deserialize eder.
 * Profile/Stats içindeki Int/Float alanlar buna göre güvenli okunur.
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
        val bassBias: Float = 0f,
        val trebleBias: Float = 0f,
        val acceptRate: Float = 1f
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
                profiles = loadProfilesSafe(pFile.readText())
            }
            val sFile = File(context.filesDir, FILE_STATS)
            if (sFile.exists()) {
                stats = loadStatsSafe(sFile.readText())
            }
        } catch (e: Exception) {
            SonaraLogger.w("Personalization", "Load error (clearing corrupted): ${e.message}")
            profiles = mutableMapOf()
            stats = ListenStats()
            try { File(context.filesDir, FILE_PROFILES).delete() } catch (_: Exception) {}
            try { File(context.filesDir, FILE_STATS).delete() } catch (_: Exception) {}
        }
    }

    /**
     * Gson Double→Int safe deserialize.
     * Gson reads all numbers as Double by default.
     * We manually convert to correct types.
     */
    @Suppress("UNCHECKED_CAST")
    private fun loadProfilesSafe(json: String): MutableMap<String, PersonalProfile> {
        return try {
            val type = object : TypeToken<Map<String, Map<String, Any>>>() {}.type
            val raw: Map<String, Map<String, Any>> = gson.fromJson(json, type) ?: return mutableMapOf()
            val result = mutableMapOf<String, PersonalProfile>()
            for ((key, map) in raw) {
                try {
                    val offsets = (map["offsets"] as? List<*>)?.map { (it as Number).toFloat() } ?: List(BANDS) { 0f }
                    result[key] = PersonalProfile(
                        offsets = offsets,
                        samples = (map["samples"] as? Number)?.toInt() ?: 0,
                        lastUpdated = (map["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        bassBias = (map["bassBias"] as? Number)?.toFloat() ?: 0f,
                        trebleBias = (map["trebleBias"] as? Number)?.toFloat() ?: 0f,
                        acceptRate = (map["acceptRate"] as? Number)?.toFloat() ?: 1f
                    )
                } catch (_: Exception) {}
            }
            result
        } catch (e: Exception) {
            SonaraLogger.w("Personalization", "Profile parse error: ${e.message}")
            mutableMapOf()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadStatsSafe(json: String): ListenStats {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val raw: Map<String, Any> = gson.fromJson(json, type) ?: return ListenStats()
            ListenStats(
                totalTracks = (raw["totalTracks"] as? Number)?.toInt() ?: 0,
                genreCounts = (raw["genreCounts"] as? Map<*, *>)
                    ?.mapNotNull { (k, v) -> if (k is String && v is Number) k to v.toInt() else null }
                    ?.toMap() ?: emptyMap(),
                routeCounts = (raw["routeCounts"] as? Map<*, *>)
                    ?.mapNotNull { (k, v) -> if (k is String && v is Number) k to v.toInt() else null }
                    ?.toMap() ?: emptyMap(),
                skipCount = (raw["skipCount"] as? Number)?.toInt() ?: 0,
                revertCount = (raw["revertCount"] as? Number)?.toInt() ?: 0,
                favoriteCount = (raw["favoriteCount"] as? Number)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            SonaraLogger.w("Personalization", "Stats parse error: ${e.message}")
            ListenStats()
        }
    }

    private fun save() {
        try {
            File(context.filesDir, FILE_PROFILES).writeText(gson.toJson(profiles))
            File(context.filesDir, FILE_STATS).writeText(gson.toJson(stats))
        } catch (_: Exception) {}
    }

    fun getPersonalOffset(genre: Genre, route: AudioRoute, subGenre: String? = null): FloatArray? {
        if (!subGenre.isNullOrBlank()) {
            profiles["${genre.name}::${route.name}::$subGenre"]?.let {
                if (it.samples >= 2) return clampOffset(it.offsets.toFloatArray())
            }
        }
        profiles["${genre.name}::${route.name}"]?.let {
            if (it.samples >= 2) return clampOffset(it.offsets.toFloatArray())
        }
        profiles[genre.name]?.let {
            if (it.samples >= 3) return clampOffset(it.offsets.toFloatArray())
        }
        return null
    }

    fun getGlobalBias(): Pair<Float, Float> {
        val allProfiles = profiles.values
        if (allProfiles.isEmpty()) return 0f to 0f
        val avgBass = allProfiles.map { it.bassBias }.average().toFloat()
        val avgTreble = allProfiles.map { it.trebleBias }.average().toFloat()
        return avgBass to avgTreble
    }

    fun recordAdjustment(genre: Genre, route: AudioRoute, subGenre: String?, aiSuggestion: FloatArray, userFinal: FloatArray) {
        val keys = mutableListOf("${genre.name}::${route.name}")
        if (!subGenre.isNullOrBlank()) keys.add("${genre.name}::${route.name}::$subGenre")
        keys.add(genre.name)

        for (key in keys) {
            val existing = profiles[key] ?: PersonalProfile(List(BANDS) { 0f }, 0, System.currentTimeMillis())
            val delta = FloatArray(BANDS) { i ->
                if (i < aiSuggestion.size && i < userFinal.size) userFinal[i] - aiSuggestion[i] else 0f
            }
            val newOffsets = List(BANDS) { i -> LEARNING_RATE * delta[i] + (1f - LEARNING_RATE) * existing.offsets[i] }
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
    }

    private fun clampOffset(arr: FloatArray): FloatArray {
        return FloatArray(arr.size) { arr[it].coerceIn(-MAX_DELTA, MAX_DELTA) }
    }
}
