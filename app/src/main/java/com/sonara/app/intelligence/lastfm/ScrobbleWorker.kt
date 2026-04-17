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

package com.sonara.app.intelligence.lastfm

import android.content.Context
import androidx.work.*
import com.sonara.app.SonaraApp
import com.sonara.app.data.SonaraLogger
import java.util.concurrent.TimeUnit

/**
 * Madde 2: Ağ yoksa queue'ya yaz, WorkManager ile sonra tekrar dene.
 */
class ScrobbleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SonaraApp ?: return Result.failure()
        val dao = app.database.pendingScrobbleDao()
        val pending = dao.getPending()

        if (pending.isEmpty()) return Result.success()

        val apiKey = app.secureSecrets.getLastFmApiKey().takeIf { it.isNotBlank() } ?: return Result.retry()
        val secret = app.secureSecrets.getLastFmSharedSecret()
        val sessionKey = app.secureSecrets.getLastFmSessionKey()
        if (sessionKey.isBlank()) return Result.retry()

        val scrobbler = ScrobblingManager()
        var successCount = 0

        for (scrobble in pending) {
            try {
                val ok = scrobbler.scrobble(
                    scrobble.track, scrobble.artist, scrobble.album,
                    scrobble.timestamp, apiKey, secret, sessionKey
                )
                if (ok) {
                    dao.deleteById(scrobble.id)
                    successCount++
                } else {
                    dao.incrementRetry(scrobble.id)
                }
            } catch (_: Exception) {
                dao.incrementRetry(scrobble.id)
            }
        }

        dao.pruneStale()
        SonaraLogger.i("ScrobbleWorker", "Processed: $successCount/${pending.size} scrobbles")
        return if (successCount > 0) Result.success() else Result.retry()
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ScrobbleWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("sonara_scrobble", ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun runOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ScrobbleWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
