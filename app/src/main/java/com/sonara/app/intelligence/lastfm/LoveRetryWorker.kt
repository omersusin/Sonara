package com.sonara.app.intelligence.lastfm

import android.content.Context
import androidx.work.*
import com.sonara.app.SonaraApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class LoveRetryWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pending = PendingLoveQueue.peekAll()
        if (pending.isEmpty()) return@withContext Result.success()
        val app = applicationContext as? SonaraApp ?: return@withContext Result.failure()
        val apiKey = app.secureSecrets.getLastFmApiKey().takeIf { it.isNotBlank() } ?: return@withContext Result.retry()
        val secret = app.secureSecrets.getLastFmSharedSecret()
        val sessionKey = app.secureSecrets.getLastFmSessionKey()
        if (sessionKey.isBlank()) return@withContext Result.retry()
        val mgr = ScrobblingManager()
        pending.forEach { p ->
            try {
                val ok = if (p.loved) mgr.loveTrack(p.title, p.artist, apiKey, secret, sessionKey)
                         else mgr.unloveTrack(p.title, p.artist, apiKey, secret, sessionKey)
                if (ok) PendingLoveQueue.dequeue(p.id)
                else PendingLoveQueue.markFailed(p.id, "api_fail")
            } catch (e: Exception) {
                PendingLoveQueue.markFailed(p.id, e.message ?: "unknown")
            }
        }
        if (PendingLoveQueue.peekAll().isEmpty()) Result.success() else Result.retry()
    }

    companion object {
        fun schedule(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "love_retry", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<LoveRetryWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            )
        }
    }
}
