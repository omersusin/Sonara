package com.sonara.app.ai.cloud

import android.content.Context
import android.util.Log
import androidx.work.*
import com.sonara.app.ai.classifier.KnnClassifier
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.data.preferences.SecureSecrets
import java.util.concurrent.TimeUnit

class DailySyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        private const val TAG = "SonaraDailySync"
        private const val WORK_NAME = "sonara_daily_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()
            val request = PeriodicWorkRequestBuilder<DailySyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints).setInitialDelay(1, TimeUnit.HOURS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES).addTag("sonara_sync").build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
        fun cancel(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME) }
        fun runNow(context: Context) {
            val c = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<DailySyncWorker>().setConstraints(c).addTag("sonara_sync_manual").build())
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Daily sync started")
        val ctx = applicationContext; val queue = ContributionQueue(ctx); val sync = GitHubSync(ctx, queue)
        try {
            val db = SonaraDatabase.get(ctx); val dao = db.trainingExampleDao(); val classifier = KnnClassifier(dao)
            val dlCount = sync.checkAndDownloadPrototypes()
            // VULN-11: Removed duplicate raw URL download — checkAndDownloadPrototypes() handles this
            if (dlCount > 0) {
                Log.d(TAG, "Downloaded $dlCount prototypes via GitHubSync")
            }
            if (queue.isEnabled && queue.size() > 0) {
                val token = SecureSecrets.getGitHubToken(ctx)
                if (!token.isNullOrBlank()) sync.uploadContributions(token)
            }
            Log.d(TAG, "Sync completed"); return Result.success()
        } catch (e: Exception) { Log.e(TAG, "Sync failed: ${e.message}"); return if (runAttemptCount < 5) Result.retry() else Result.failure() }
    }
}
