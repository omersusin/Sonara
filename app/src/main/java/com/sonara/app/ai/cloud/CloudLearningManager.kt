/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
