package com.sonara.app.intelligence.musicbrainz

import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Madde 4/13/15: MusicBrainz WS/2 arama.
 * MBID, tags, genre bilgisi döner.
 * Rate limit: 1 req/sn — uygulama tarafında saygı gösterilmeli.
 */
object MusicBrainzClient {

    private const val BASE_URL = "https://musicbrainz.org/ws/2"
    private const val USER_AGENT = "Sonara/1.0.0 (contact@sonara.app)"

    data class MbMatch(
        val mbid: String,
        val title: String,
        val artist: String,
        val tags: List<String>,
        val score: Int
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private var lastRequestTime = 0L

    /** Recording arama: title + artist ile en iyi eşleşme */
    suspend fun searchRecording(title: String, artist: String): MbMatch? {
        if (title.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                enforceRateLimit()
                val query = buildString {
                    append("recording:")
                    append(URLEncoder.encode("\"$title\"", "UTF-8"))
                    if (artist.isNotBlank()) {
                        append(" AND artist:")
                        append(URLEncoder.encode("\"$artist\"", "UTF-8"))
                    }
                }
                val url = "$BASE_URL/recording?query=$query&limit=3&fmt=json"
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext null

                val json = JSONObject(body)
                val recordings = json.optJSONArray("recordings") ?: return@withContext null
                if (recordings.length() == 0) return@withContext null

                val best = recordings.getJSONObject(0)
                val score = best.optInt("score", 0)
                if (score < 60) return@withContext null // düşük eşleşme

                val mbid = best.optString("id", "")
                val recTitle = best.optString("title", title)
                val artistCredits = best.optJSONArray("artist-credit")
                val recArtist = artistCredits?.optJSONObject(0)?.optJSONObject("artist")
                    ?.optString("name", artist) ?: artist

                val tags = mutableListOf<String>()
                best.optJSONArray("tags")?.let { tagArray ->
                    for (i in 0 until tagArray.length()) {
                        tagArray.optJSONObject(i)?.optString("name")?.let { tags.add(it.lowercase()) }
                    }
                }

                SonaraLogger.ai("MusicBrainz match: $recTitle by $recArtist (score=$score, mbid=$mbid)")
                MbMatch(mbid, recTitle, recArtist, tags, score)
            } catch (e: Exception) {
                SonaraLogger.w("MusicBrainz", "Search error: ${e.message}")
                null
            }
        }
    }

    data class MbArtistUrls(
        val mbid: String,
        val name: String,
        val website: String? = null,
        val twitter: String? = null,
        val facebook: String? = null,
        val instagram: String? = null,
        val youtube: String? = null,
        val discogs: String? = null,
        val soundcloud: String? = null
    )

    /** Artist arama + URL ilişkileri. Social media / web links için kullanılır. */
    suspend fun searchArtistUrls(artistName: String): MbArtistUrls? {
        if (artistName.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                enforceRateLimit()
                val query = "artist:" + URLEncoder.encode("\"$artistName\"", "UTF-8")
                val searchUrl = "$BASE_URL/artist?query=$query&limit=1&fmt=json"
                val searchReq = Request.Builder().url(searchUrl).get().build()
                val searchBody = client.newCall(searchReq).execute().body?.string() ?: return@withContext null
                val artists = JSONObject(searchBody).optJSONArray("artists") ?: return@withContext null
                if (artists.length() == 0) return@withContext null

                val best = artists.getJSONObject(0)
                val score = best.optInt("score", 0)
                if (score < 60) return@withContext null

                val mbid = best.optString("id").takeIf { it.isNotBlank() } ?: return@withContext null
                val name = best.optString("name", artistName)

                // Fetch URL relationships
                enforceRateLimit()
                val detailUrl = "$BASE_URL/artist/$mbid?inc=url-rels&fmt=json"
                val detailReq = Request.Builder().url(detailUrl).get().build()
                val detailBody = client.newCall(detailReq).execute().body?.string() ?: return@withContext null
                val detailJson = JSONObject(detailBody)

                val relations = detailJson.optJSONArray("relations") ?: return@withContext MbArtistUrls(mbid, name)

                var website: String? = null
                var twitter: String? = null
                var facebook: String? = null
                var instagram: String? = null
                var youtube: String? = null
                var discogs: String? = null
                var soundcloud: String? = null

                for (i in 0 until relations.length()) {
                    val rel = relations.optJSONObject(i) ?: continue
                    val relType = rel.optString("type").lowercase()
                    val urlObj = rel.optJSONObject("url") ?: continue
                    val href = urlObj.optString("resource").takeIf { it.isNotBlank() } ?: continue

                    when {
                        relType == "official homepage" || relType == "official website" -> website = href
                        relType.contains("twitter") || href.contains("twitter.com") || href.contains("x.com") -> twitter = href
                        relType.contains("facebook") || href.contains("facebook.com") -> facebook = href
                        relType.contains("instagram") || href.contains("instagram.com") -> instagram = href
                        relType.contains("youtube") || href.contains("youtube.com") -> youtube = href
                        relType.contains("discogs") || href.contains("discogs.com") -> discogs = href
                        relType.contains("soundcloud") || href.contains("soundcloud.com") -> soundcloud = href
                    }
                }

                SonaraLogger.d("MusicBrainz", "Artist URLs for $name: tw=$twitter fb=$facebook ig=$instagram web=$website")
                MbArtistUrls(mbid, name, website, twitter, facebook, instagram, youtube, discogs, soundcloud)
            } catch (e: Exception) {
                SonaraLogger.w("MusicBrainz", "Artist URL search error: ${e.message}")
                null
            }
        }
    }

    private suspend fun enforceRateLimit() {
        val now = System.currentTimeMillis()
        val diff = now - lastRequestTime
        if (diff < 1100) { // 1 req/sn
            kotlinx.coroutines.delay(1100 - diff)
        }
        lastRequestTime = System.currentTimeMillis()
    }
}
