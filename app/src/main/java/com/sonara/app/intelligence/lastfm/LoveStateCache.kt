package com.sonara.app.intelligence.lastfm

import android.content.Context
import android.content.SharedPreferences
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

    fun clear() = cache.clear()

    fun size() = cache.size
}
