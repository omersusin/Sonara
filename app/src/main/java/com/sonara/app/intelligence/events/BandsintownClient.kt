package com.sonara.app.intelligence.events

import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object BandsintownClient {
    private const val TAG = "Bandsintown"
    private const val BASE = "https://rest.bandsintown.com"
    private const val APP_ID = "sonara"

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    suspend fun getUpcomingEvents(artistName: String): List<BandsintownEvent> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE/artists/${enc(artistName)}/events?app_id=$APP_ID&date=upcoming"
            val req = Request.Builder().url(url).build()
            val body = http.newCall(req).execute().use { it.body?.string() } ?: return@withContext emptyList()
            val arr = JSONArray(body)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val venueObj = obj.optJSONObject("venue")
                BandsintownEvent(
                    id = obj.optString("id"),
                    datetime = obj.optString("datetime"),
                    title = obj.optString("title").ifBlank { artistName },
                    url = obj.optString("url"),
                    venue = BandsintownVenue(
                        name = venueObj?.optString("name") ?: "",
                        city = venueObj?.optString("city") ?: "",
                        country = venueObj?.optString("country") ?: "",
                        region = venueObj?.optString("region") ?: ""
                    )
                )
            }
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Events request failed: ${e.message}")
            emptyList()
        }
    }
}
