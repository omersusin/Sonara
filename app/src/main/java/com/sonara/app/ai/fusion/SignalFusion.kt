package com.sonara.app.ai.fusion

import android.util.Log
import com.sonara.app.ai.enrichment.EnrichmentBundle
import com.sonara.app.ai.models.*

class SignalFusion {
    companion object {
        private const val TAG = "SonaraFusion"
        private const val W_AUDIO = 0.55f; private const val W_LFM = 0.25f; private const val W_API = 0.15f; private const val W_LYR = 0.05f
    }

    fun fuse(audioResult: SonaraAiResult, enrichment: EnrichmentBundle?): SonaraAiResult {
        if (enrichment == null || !enrichment.hasAnyData) return audioResult
        val fusedGenres = mutableMapOf<String, Float>(); var totalWeight = 0f
        val aw = W_AUDIO * audioResult.confidence
        for ((g, s) in audioResult.genres) fusedGenres.merge(g, s * aw) { a, b -> a + b }; totalWeight += aw
        if (enrichment.lastFm.isValid) { val w = W_LFM * enrichment.lastFm.confidence; for ((g, s) in enrichment.lastFm.genreHints) fusedGenres.merge(g, s * w) { a, b -> a + b }; totalWeight += w }
        if (enrichment.apiAi.isValid) { val w = W_API * enrichment.apiAi.confidence; for ((g, s) in enrichment.apiAi.genreHints) fusedGenres.merge(g, s * w) { a, b -> a + b }; totalWeight += w }
        if (totalWeight > 0) fusedGenres.replaceAll { _, v -> v / totalWeight }
        val topGenres = fusedGenres.entries.filter { it.value > 0.05f }.sortedByDescending { it.value }.take(5).associate { it.key to it.value }

        var fVal = audioResult.mood.valence * W_AUDIO; var fAro = audioResult.mood.arousal * W_AUDIO; var mw = W_AUDIO
        val sources = listOf(enrichment.lastFm, enrichment.lyrics, enrichment.apiAi); val weights = listOf(W_LFM, W_LYR + 0.05f, W_API)
        for ((sig, w) in sources.zip(weights)) { if (sig.isValid) { sig.moodValence?.let { fVal += it * w * sig.confidence; mw += w * sig.confidence }; sig.moodArousal?.let { fAro += it * w * sig.confidence } } }
        if (mw > 0) { fVal /= mw; fAro /= mw }

        var fEne = audioResult.energy * W_AUDIO; var ew = W_AUDIO
        for ((sig, w) in sources.zip(weights)) { if (sig.isValid && sig.energy != null) { fEne += sig.energy * w * sig.confidence; ew += w * sig.confidence } }
        if (ew > 0) fEne /= ew

        val atg = audioResult.genres.maxByOrNull { it.value }?.key; val ltg = enrichment.lastFm.genreHints.maxByOrNull { it.value }?.key; val aptg = enrichment.apiAi.genreHints.maxByOrNull { it.value }?.key
        var ab = 0f; if (atg != null) { if (atg == ltg) ab += 0.10f; if (atg == aptg) ab += 0.05f; if (ltg == aptg && ltg != null) ab += 0.05f }
        val fc = (audioResult.confidence + ab).coerceIn(0f, 1f)

        return audioResult.copy(genres = topGenres, mood = SonaraMood(fVal.coerceIn(-1f, 1f), fAro.coerceIn(0f, 1f)),
            energy = fEne.coerceIn(0f, 1f), confidence = fc,
            confidenceLevel = when { fc > 0.65f -> SonaraConfidence.HIGH; fc > 0.35f -> SonaraConfidence.MODERATE; else -> SonaraConfidence.LOW })
    }
}
