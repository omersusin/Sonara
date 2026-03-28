package com.sonara.app.ai.cloud

import android.content.Context
import android.util.Log
import com.sonara.app.ai.classifier.KnnClassifier
import com.sonara.app.ai.models.*

class CloudLearningManager(private val context: Context, private val classifier: KnnClassifier, private val dao: TrainingExampleDao) {
    companion object { private const val TAG = "SonaraCloud" }
    val queue = ContributionQueue(context)
    private val sync = GitHubSync(context, queue)

    fun scheduleSync() {
        Log.d(TAG, "scheduleSync() called")
        DailySyncWorker.schedule(context)
        Log.d(TAG, "Sync scheduled") }

    fun addContribution(features: AudioFeatureVector, genre: String, mood: SonaraMood, energy: Float, sourceType: String = "confirmed") {
        Log.d(TAG, "addContribution() called")
        queue.enqueue(features, genre, mood, energy, sourceType)
    }

    fun setContributionEnabled(enabled: Boolean) { queue.isEnabled = enabled; if (enabled) DailySyncWorker.schedule(context) }
    fun isContributionEnabled(): Boolean = queue.isEnabled
    fun getPendingCount(): Int = queue.size()
    fun getTotalSent(): Int = queue.getTotalSent()
    fun syncNow() {
        Log.d(TAG, "syncNow() called")
        DailySyncWorker.runNow(context) }
    fun reset() { queue.reset(); DailySyncWorker.cancel(context) }
}
