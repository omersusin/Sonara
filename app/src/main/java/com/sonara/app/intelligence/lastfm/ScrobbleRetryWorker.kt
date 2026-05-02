package com.sonara.app.intelligence.lastfm

import android.content.Context
import androidx.work.*
import com.sonara.app.SonaraApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ScrobbleRetryWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? SonaraApp ?: return@withContext Result.failure()
        val dao = app.database.pendingScrobbleDao()
        val pending = dao.getPending()
        if (pending.isEmpty()) return@withContext Result.success()
        val apiKey = app.secureSecrets.getLastFmApiKey().takeIf { it.isNotBlank() } ?: return@withContext Result.retry()
        val secret = app.secureSecrets.getLastFmSharedSecret()
        val sessionKey = app.secureSecrets.getLastFmSessionKey()
        if (sessionKey.isBlank()) return@withContext Result.retry()
        val scrobbler = ScrobblingManager()
        pending.forEach { ps ->
            try {
                val ok = scrobbler.scrobble(ps.track, ps.artist, ps.album, ps.timestamp, apiKey, secret, sessionKey)
                if (ok) dao.deleteById(ps.id)
                else if (ps.retryCount >= 5) dao.deleteById(ps.id)
                else dao.incrementRetry(ps.id)
            } catch (_: Exception) {
                if (ps.retryCount >= 5) dao.deleteById(ps.id) else dao.incrementRetry(ps.id)
            }
        }
        dao.pruneStale()
        if (dao.getPending().isEmpty()) Result.success() else Result.retry()
    }

    companion object {
        fun schedule(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "scrobble_retry", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<ScrobbleRetryWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            )
        }
    }
}
