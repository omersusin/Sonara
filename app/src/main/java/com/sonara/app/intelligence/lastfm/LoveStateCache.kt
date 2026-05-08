package com.sonara.app.intelligence.lastfm

import android.content.Context
import android.content.SharedPreferences
import com.sonara.app.SonaraApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object LoveStateCache {

    private val cache = ConcurrentHashMap<String, Boolean>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("sonara_love_cache", Context.MODE_PRIVATE)
        prefs?.all?.forEach { (k, v) -> if (v is Boolean) cache[k] = v }
    }

    private fun key(title: String, artist: String): String =
        "${artist.lowercase().trim()}::${title.lowercase().trim()}"

    fun isLoved(title: String, artist: String): Boolean? = cache[key(title, artist)]

    fun setLoved(title: String, artist: String, loved: Boolean) {
        val k = key(title, artist)
        cache[k] = loved
        prefs?.edit()?.putBoolean(k, loved)?.apply()
    }

    /**
     * Hits Last.fm `track.getInfo?username=…` to read `userloved`, which reflects
     * loves made from any client (Pano Scrobbler, the Last.fm site, etc.). Returns
     * the freshly resolved state, or the cached value if the call fails.
     */
    suspend fun refresh(title: String, artist: String): Boolean? = withContext(Dispatchers.IO) {
        if (title.isBlank() || artist.isBlank()) return@withContext null
        try {
            val app = SonaraApp.instance
            val apiKey = app.lastFmAuth.getActiveApiKey()
            val username = app.lastFmAuth.getConnectionInfo().username
            if (apiKey.isBlank() || username.isBlank()) return@withContext cache[key(title, artist)]
            val resp = LastFmClient.api.getTrackInfo(title, artist, apiKey, username)
            val loved = resp.track?.userloved == "1"
            setLoved(title, artist, loved)
            loved
        } catch (_: Exception) {
            cache[key(title, artist)]
        }
    }

    fun clear() = cache.clear()

    fun size() = cache.size
}
