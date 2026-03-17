package com.sonara.app.engine.learning

import android.content.Context
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.models.UserEqPreference
import com.sonara.app.data.models.UserFeedback
import com.sonara.app.engine.classifier.TextGenreClassifier
import com.sonara.app.engine.eq.EqSessionController
import kotlinx.coroutines.*

class UserPreferenceLearner(context: Context, private val classifier: TextGenreClassifier) {
    private val db = SonaraDatabase.get(context)
    private val prefDao = db.userEqPreferenceDao()
    private val feedbackDao = db.userFeedbackDao()
    private val driftGuard = DriftGuard(feedbackDao)
    private val weightStore = WeightStore(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun onAccepted(title: String?, artist: String?, genre: String, bands: ShortArray, route: EqSessionController.AudioRoute, confidence: Float) {
        val g = driftGuard.check(genre, null, confidence); if (!g.allow) return
        feedbackDao.insert(UserFeedback(trackTitle = title, trackArtist = artist, suggestedGenre = genre, accepted = true, audioRoute = route.name, confidence = confidence, suggestedBands = bands.joinToString(",", "[", "]")))
        upsertPref(genre, null, route, bands, confidence)
    }

    suspend fun onGenreCorrected(title: String?, artist: String?, suggestedGenre: String, correctedGenre: String, bands: ShortArray, route: EqSessionController.AudioRoute, confidence: Float, tokens: Set<String>) {
        val g = driftGuard.check(suggestedGenre, correctedGenre, confidence); if (!g.allow) return
        feedbackDao.insert(UserFeedback(trackTitle = title, trackArtist = artist, suggestedGenre = suggestedGenre, correctedGenre = correctedGenre, accepted = false, audioRoute = route.name, confidence = confidence, suggestedBands = bands.joinToString(",", "[", "]")))
        classifier.adaptWeights(suggestedGenre, correctedGenre, tokens)
        weightStore.save(classifier.exportWeights())
    }

    suspend fun onBandsManuallyAdjusted(genre: String, mood: String?, route: EqSessionController.AudioRoute, bands: ShortArray, energy: Float) { upsertPref(genre, mood, route, bands, 1f) }
    suspend fun restoreWeights() { weightStore.load()?.let { classifier.importWeights(it) } }
    fun release() { scope.cancel() }

    private suspend fun upsertPref(genre: String, mood: String?, route: EqSessionController.AudioRoute, bands: ShortArray, confidence: Float) {
        val existing = prefDao.getBest(genre, route.name)
        if (existing != null) {
            val old = existing.bandLevels.trim('[', ']').split(",").mapNotNull { it.trim().toShortOrNull() }.toShortArray()
            val blend = ShortArray(minOf(old.size, bands.size)) { ((old[it] * 0.7f) + (bands[it] * 0.3f)).toInt().toShort() }
            prefDao.upsert(existing.copy(bandLevels = blend.joinToString(",", "[", "]"), usageCount = existing.usageCount + 1, lastUsed = System.currentTimeMillis(), mood = mood ?: existing.mood))
        } else {
            prefDao.upsert(UserEqPreference(genre = genre, mood = mood, audioRoute = route.name, bandLevels = bands.joinToString(",", "[", "]"), energy = confidence))
        }
    }
}
