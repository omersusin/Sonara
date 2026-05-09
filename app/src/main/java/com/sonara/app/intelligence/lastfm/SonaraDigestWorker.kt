package com.sonara.app.intelligence.lastfm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.sonara.app.MainActivity
import com.sonara.app.SonaraApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SonaraDigestWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        const val CHANNEL_ID = "sonara_digest"
        const val WORK_NAME = "sonara_weekly_digest"
        const val TEST_TAG = "sonara_digest_test"

        /**
         * Schedules the next Monday-9am digest. Uses KEEP policy so opening the app
         * doesn't reset the pending delay every time — without this, the OneTimeWork
         * gets replaced on every process start and Android may never settle on a
         * delivery slot before the next start clobbers it.
         */
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.WEEK_OF_YEAR, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val request = OneTimeWorkRequestBuilder<SonaraDigestWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        /** Force-schedules a recurring weekly digest as a fallback so users on
         *  battery-restricted devices still get something even if the OneTimeWork
         *  with a multi-day initial delay gets dropped by Doze. */
        fun reschedule(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            schedule(context)
        }

        /** Manually fire a digest right now for "Send test digest" UI. Bypasses
         *  digestEnabled — the act of tapping the button is the user's consent. */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SonaraDigestWorker>()
                .setInputData(workDataOf("test" to true))
                .addTag(TEST_TAG)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun createChannel(context: Context) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Weekly Digest",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Weekly music statistics digest" }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result {
        val app = ctx.applicationContext as SonaraApp
        val isTest = inputData.getBoolean("test", false)
        val username = app.lastFmAuth.getConnectionInfo().username
        val apiKey = app.lastFmAuth.getActiveApiKey()

        if (username.isBlank() || apiKey.isBlank()) {
            if (!isTest) schedule(ctx)
            return Result.success()
        }

        // Test triggers bypass the user toggle — the user explicitly asked for it.
        val digestEnabled = isTest || runCatching {
            app.preferences.digestEnabledFlow.first()
        }.getOrDefault(true)
        if (!digestEnabled) {
            schedule(ctx)
            return Result.success()
        }

        try {
            coroutineScope {
                val artistsD = async(Dispatchers.IO) {
                    runCatching {
                        LastFmClient.api.getUserTopArtists(username, apiKey, "7day", 3)
                            .topartists?.artist?.map { it.name } ?: emptyList()
                    }.getOrDefault(emptyList())
                }
                val tracksD = async(Dispatchers.IO) {
                    runCatching {
                        LastFmClient.api.getUserTopTracks(username, apiKey, "7day", 3)
                            .toptracks?.track?.map { "${it.name} — ${it.artist?.name}" } ?: emptyList()
                    }.getOrDefault(emptyList())
                }

                val artists = artistsD.await()
                val tracks = tracksD.await()

                // For the regular weekly run, skip the notification when there's no
                // activity to report. For test triggers, always notify so the user
                // gets feedback that the button worked.
                if (artists.isEmpty() && tracks.isEmpty() && !isTest) return@coroutineScope

                val sb = StringBuilder()
                if (artists.isNotEmpty()) sb.appendLine("Artists: ${artists.joinToString(", ")}")
                if (tracks.isNotEmpty()) sb.appendLine("Tracks: ${tracks.joinToString(", ")}")
                if (artists.isEmpty() && tracks.isEmpty()) {
                    sb.appendLine("No recent scrobbles in the last 7 days.")
                }

                val intent = Intent(ctx, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("navigate_to", "insights")
                }
                val pi = PendingIntent.getActivity(
                    ctx, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                createChannel(ctx)

                val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle("This Week's Music")
                    .setContentText(sb.toString().trim())
                    .setStyle(NotificationCompat.BigTextStyle().bigText(sb.toString().trim()))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build()

                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(42, notification)
            }
        } catch (_: Exception) {}

        if (!isTest) schedule(ctx)
        return Result.success()
    }
}
