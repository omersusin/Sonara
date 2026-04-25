package com.sonara.app.intelligence.theaudiodb

import com.sonara.app.data.SonaraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * TheAudioDB API istemcisi.
 * Free tier: key = "2", 30 req/dk, kayıt gerekmez.
 * Kullanım: sanatçı görseli, albüm kapağı, tracklist, diskografi.
 */
object TheAudioDbClient {
    private const val TAG = "TheAudioDb"
    private const val BASE = "https://www.theaudiodb.com/api/v1/json/2"

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun get(url: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).build()
            val body = http.newCall(req).execute().use { it.body?.string() }
            if (body.isNullOrBlank()) null else JSONObject(body)
        } catch (e: Exception) {
            SonaraLogger.w(TAG, "Request failed: ${e.message}")
            null
        }
    }

    suspend fun searchArtist(name: String): AudioDbArtist? {
        val json = get("$BASE/search.php?s=${enc(name)}") ?: return null
        val arr = json.optJSONArray("artists") ?: return null
        if (arr.length() == 0) return null
        val obj = arr.getJSONObject(0)
        return AudioDbArtist(
            idArtist = obj.optString("idArtist"),
            strArtist = obj.optString("strArtist"),
            strThumb = obj.optString("strArtistThumb").takeIf { it.isNotBlank() },
            strFanart = obj.optString("strArtistFanart").takeIf { it.isNotBlank() },
            strBanner = obj.optString("strArtistBanner").takeIf { it.isNotBlank() },
            strLogo = obj.optString("strArtistLogo").takeIf { it.isNotBlank() },
            strBiographyEN = obj.optString("strBiographyEN").takeIf { it.isNotBlank() },
            strBiographyTR = obj.optString("strBiographyTR").takeIf { it.isNotBlank() },
            strGenre = obj.optString("strGenre").takeIf { it.isNotBlank() },
            strCountry = obj.optString("strCountry").takeIf { it.isNotBlank() },
            strTwitter = obj.optString("strTwitter").takeIf { it.isNotBlank() },
            strFacebook = obj.optString("strFacebook").takeIf { it.isNotBlank() },
            strInstagram = obj.optString("strInstagram").takeIf { it.isNotBlank() },
            strWebsite = obj.optString("strWebsite").takeIf { it.isNotBlank() },
            intFormedYear = obj.optString("intFormedYear").takeIf { it.isNotBlank() },
            intMembersNo = obj.optString("intMembersNo").toIntOrNull()
        )
    }

    suspend fun searchAlbum(artist: String, album: String): AudioDbAlbum? {
        val json = get("$BASE/searchalbum.php?s=${enc(artist)}&a=${enc(album)}") ?: return null
        val arr = json.optJSONArray("album") ?: return null
        if (arr.length() == 0) return null
        val obj = arr.getJSONObject(0)
        return parseAlbumObject(obj, artist)
    }

    /** Fetch a specific album by its TheAudioDB ID — faster and more reliable than name search. */
    suspend fun getAlbumById(albumId: String): AudioDbAlbum? {
        if (albumId.isBlank()) return null
        val json = get("$BASE/album.php?i=$albumId") ?: return null
        val arr = json.optJSONArray("album") ?: return null
        if (arr.length() == 0) return null
        val obj = arr.getJSONObject(0)
        return parseAlbumObject(obj, obj.optString("strArtist"))
    }

    private fun parseAlbumObject(obj: org.json.JSONObject, fallbackArtist: String): AudioDbAlbum =
        AudioDbAlbum(
            idAlbum = obj.optString("idAlbum"),
            strAlbum = obj.optString("strAlbum"),
            strArtist = obj.optString("strArtist").ifBlank { fallbackArtist },
            strThumb = obj.optString("strAlbumThumb").takeIf { it.isNotBlank() },
            strThumbHQ = obj.optString("strAlbumThumbHQ").takeIf { it.isNotBlank() },
            intYearReleased = obj.optString("intYearReleased").toIntOrNull(),
            strGenre = obj.optString("strGenre").takeIf { it.isNotBlank() },
            strDescriptionEN = obj.optString("strDescriptionEN").takeIf { it.isNotBlank() }
        )

    /** Belirli bir albümün track listesini çeker. Önce searchAlbum ile idAlbum al. */
    suspend fun getAlbumTracks(audioDbAlbumId: String): List<AudioDbTrack> {
        val json = get("$BASE/track.php?m=$audioDbAlbumId") ?: return emptyList()
        val arr = json.optJSONArray("track") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            AudioDbTrack(
                idTrack = obj.optString("idTrack"),
                strTrack = obj.optString("strTrack"),
                intTrackNumber = obj.optString("intTrackNumber").toIntOrNull(),
                strDuration = obj.optString("intDuration").takeIf { it.isNotBlank() },
                strThumb = obj.optString("strTrackThumb").takeIf { it.isNotBlank() }
            )
        }.sortedBy { it.intTrackNumber ?: Int.MAX_VALUE }
    }

    suspend fun getTop10Tracks(artistName: String): List<AudioDbTrack> {
        val json = get("$BASE/track-top10.php?s=${enc(artistName)}") ?: return emptyList()
        val arr = json.optJSONArray("track") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            AudioDbTrack(
                idTrack = obj.optString("idTrack"),
                strTrack = obj.optString("strTrack"),
                intTrackNumber = i + 1,
                strDuration = obj.optString("intDuration").takeIf { it.isNotBlank() },
                strThumb = obj.optString("strTrackThumb").takeIf { it.isNotBlank() }
            )
        }
    }

    suspend fun getDiscography(artistName: String): List<AudioDbAlbum> {
        val json = get("$BASE/discography.php?s=${enc(artistName)}") ?: return emptyList()
        val arr = json.optJSONArray("album") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            AudioDbAlbum(
                idAlbum = obj.optString("idAlbum"),
                strAlbum = obj.optString("strAlbum"),
                strArtist = artistName,
                strThumb = obj.optString("strAlbumThumb").takeIf { it.isNotBlank() },
                strThumbHQ = obj.optString("strAlbumThumbHQ").takeIf { it.isNotBlank() },
                intYearReleased = obj.optString("intYearReleased").toIntOrNull(),
                strGenre = obj.optString("strGenre").takeIf { it.isNotBlank() },
                strDescriptionEN = null
            )
        }
    }
}
