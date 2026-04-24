package com.sonara.app.intelligence.coverart

import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Cover Art Archive — MBID ile albüm kapağı URL'i döner.
 * GET /release-group/{mbid}/front → 302 redirect → gerçek resim URL'i.
 * followRedirects(false) ile 302 Location header'ından URL alınır.
 */
object CoverArtArchiveClient {
    private const val TAG = "CoverArtArchive"
    private const val BASE = "https://coverartarchive.org"

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    /** MBID → kapak görseli URL'i (null = bulunamadı) */
    suspend fun getFrontCoverUrl(mbid: String): String? = withContext(Dispatchers.IO) {
        if (mbid.isBlank()) return@withContext null
        try {
            val req = Request.Builder()
                .url("$BASE/release-group/$mbid/front")
                .header("User-Agent", "Sonara/1.0.0 (contact@sonara.app)")
                .build()
            val response = http.newCall(req).execute()
            val location = response.header("Location")
            response.close()
            if (response.code == 307 || response.code == 302 || response.code == 301) {
                location
            } else null
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Cover art fetch failed for $mbid: ${e.message}")
            null
        }
    }

    /** Release-level lookup (daha kesin eşleşme gerektiğinde) */
    suspend fun getReleaseCoverUrl(releaseMbid: String): String? = withContext(Dispatchers.IO) {
        if (releaseMbid.isBlank()) return@withContext null
        try {
            val req = Request.Builder()
                .url("$BASE/release/$releaseMbid/front")
                .header("User-Agent", "Sonara/1.0.0 (contact@sonara.app)")
                .build()
            val response = http.newCall(req).execute()
            val location = response.header("Location")
            response.close()
            if (response.code in 301..307) location else null
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Release cover fetch failed for $releaseMbid: ${e.message}")
            null
        }
    }
}
