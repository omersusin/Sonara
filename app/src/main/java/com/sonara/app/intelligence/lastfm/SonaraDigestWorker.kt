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

        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.WEEK_OF_YEAR, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val request = OneTimeWorkRequestBuilder<SonaraDigestWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
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
        val username = app.lastFmAuth.getConnectionInfo().username
        val apiKey = app.lastFmAuth.getActiveApiKey()

        if (username.isBlank() || apiKey.isBlank()) {
            schedule(ctx)
            return Result.success()
        }

        val digestEnabled = runCatching {
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

                if (artists.isEmpty() && tracks.isEmpty()) {
                    schedule(ctx)
                    return@coroutineScope
                }

                val sb = StringBuilder()
                if (artists.isNotEmpty()) sb.appendLine("Artists: ${artists.joinToString(", ")}")
                if (tracks.isNotEmpty()) sb.appendLine("Tracks: ${tracks.joinToString(", ")}")

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

        schedule(ctx)
        return Result.success()
    }
}
