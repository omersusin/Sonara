package com.sonara.app.ai.classifier

import android.util.Log
import com.sonara.app.ai.models.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.sqrt

class KnnClassifier(
    private val dao: TrainingExampleDao,
    private val k: Int = 7
) {
    companion object {
        private const val TAG = "SonaraKNN"
        private const val MIN_SIMILARITY = 0.72f
    }

    private val mutex = Mutex()
    private var cache: List<TrainingExample> = emptyList()
    private var cacheValid = false

    suspend fun refreshCache() = mutex.withLock {
        cache = dao.getAll(); cacheValid = true
        Log.d(TAG, "Cache: ${cache.size} examples")
    }

    suspend fun classify(features: AudioFeatureVector): SonaraAiResult {
        if (!cacheValid) refreshCache()
        val query = features.toFloatArray()
        if (cache.isEmpty()) return ruleBasedFallback(features)

        val scored = cache.mapNotNull { ex ->
            try {
                val vec = ex.getFeatureArray()
                if (vec.size < AudioFeatureVector.SIZE) return@mapNotNull null
                val sim = cosineSimilarity(query, vec)
                if (sim >= MIN_SIMILARITY) Scored(ex, sim) else null
            } catch (_: Exception) { null }
        }.sortedByDescending { it.sim }

        if (scored.isEmpty()) return ruleBasedFallback(features)
        val topK = scored.take(k)

        val genreVotes = mutableMapOf<String, Float>()
        var tVal = 0f; var tAro = 0f; var tEne = 0f; var tW = 0f

        for (s in topK) {
            val w = s.sim * s.sim
            genreVotes.merge(s.ex.genre, w) { a, b -> a + b }
            tVal += s.ex.moodValence * w; tAro += s.ex.moodArousal * w
            tEne += s.ex.energy * w; tW += w
            try { dao.incrementUseCount(s.ex.id) } catch (_: Exception) {}
        }

        val maxVote = genreVotes.values.maxOrNull() ?: 1f
        if (maxVote > 0) genreVotes.replaceAll { _, v -> v / maxVote }
        if (tW <= 0) tW = 1f

        val bestSim = topK.first().sim
        val confidence = when {
            bestSim > 0.92f -> 0.85f; bestSim > 0.85f -> 0.70f
            bestSim > 0.80f -> 0.55f; else -> 0.40f
        }
        val isLearned = topK.any { it.ex.source in listOf("user_confirmed", "user_corrected") }

        return SonaraAiResult(
            genres = genreVotes.toMap(),
            mood = SonaraMood(valence = (tVal / tW).coerceIn(-1f, 1f), arousal = (tAro / tW).coerceIn(0f, 1f)),
            energy = (tEne / tW).coerceIn(0f, 1f), confidence = confidence,
            confidenceLevel = when {
                confidence > 0.65f -> SonaraConfidence.HIGH
                confidence > 0.35f -> SonaraConfidence.MODERATE
                else -> SonaraConfidence.LOW
            },
            source = if (isLearned) SonaraSource.AUDIO_LEARNED else SonaraSource.AUDIO_PROTOTYPE,
            spectralProfile = features.bandEnergies
        )
    }

    suspend fun learn(features: AudioFeatureVector, genre: String, mood: SonaraMood, energy: Float,
                      source: String, title: String = "", artist: String = "") {
        dao.insert(TrainingExample.create(features = features.toFloatArray(), genre = genre,
            valence = mood.valence, arousal = mood.arousal, energy = energy, source = source,
            title = title, artist = artist))
        cacheValid = false
        Log.d(TAG, "Learned: $genre ($source) $title")
    }

    suspend fun loadPrototypes(prototypes: List<TrainingExample>) {
        dao.deletePrototypes(); dao.insertAll(prototypes); cacheValid = false
        Log.d(TAG, "Loaded ${prototypes.size} prototypes")
    }

    suspend fun getLearnedCount(): Int = dao.getLearnedCount()

    private fun ruleBasedFallback(f: AudioFeatureVector): SonaraAiResult {
        val g = mutableMapOf<String, Float>()
        if (f.bassRatio > 0.45f) {
            if (f.onsetDensity > 0.3f) { g["electronic"] = 0.4f + f.onsetDensity * 0.4f; g["hip-hop"] = 0.3f + f.bassRatio * 0.3f }
            else { g["r&b"] = 0.4f; g["hip-hop"] = 0.3f }
        }
        if (f.spectralCentroid > 0.45f && f.bassRatio < 0.25f && f.dynamicRange > 0.4f) { g["classical"] = 0.4f + f.dynamicRange * 0.3f; g["acoustic"] = 0.3f }
        if (f.spectralCentroid in 0.25f..0.45f && f.spectralFlatness < 0.25f && f.midRatio > 0.35f) { g["jazz"] = 0.4f + (1f - f.spectralFlatness) * 0.3f; g["soul"] = 0.3f }
        if (f.spectralFluxNorm > 0.6f && f.rmsEnergy > 0.45f && f.spectralBandwidth > 0.25f) { g["rock"] = 0.5f + f.rmsEnergy * 0.3f; if (f.rmsEnergy > 0.65f) g["metal"] = 0.4f }
        if (f.rmsEnergy < 0.25f && f.spectralBandwidth < 0.2f) { g["ambient"] = 0.5f; g["chill"] = 0.4f }
        if (f.onsetDensity > 0.45f && f.rmsEnergy > 0.4f) { g["dance"] = 0.4f + f.onsetDensity * 0.3f; g.merge("electronic", 0.35f) { a, b -> maxOf(a, b) } }
        if (f.trebleRatio > 0.3f && f.rmsEnergy in 0.3f..0.6f) { g.merge("pop", 0.4f) { a, b -> maxOf(a, b) } }
        if (g.isEmpty() || g.values.maxOrNull()!! < 0.25f) g["pop"] = 0.3f
        val mx = g.values.maxOrNull() ?: 1f
        if (mx > 0) g.replaceAll { _, v -> v / mx }
        return SonaraAiResult(genres = g,
            mood = SonaraMood(valence = ((f.spectralCentroid - 0.4f) * 2.5f).coerceIn(-1f, 1f), arousal = (f.rmsEnergy * 0.5f + f.onsetDensity * 0.5f).coerceIn(0f, 1f)),
            energy = f.rmsEnergy, confidence = 0.30f, confidenceLevel = SonaraConfidence.LOW,
            source = SonaraSource.AUDIO_RULES, spectralProfile = f.bandEnergies)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val len = minOf(a.size, b.size)
        var dot = 0f; var nA = 0f; var nB = 0f
        for (i in 0 until len) { dot += a[i] * b[i]; nA += a[i] * a[i]; nB += b[i] * b[i] }
        val denom = sqrt(nA) * sqrt(nB)
        return if (denom > 0f) (dot / denom).coerceIn(-1f, 1f) else 0f
    }

    private data class Scored(val ex: TrainingExample, val sim: Float)
}
