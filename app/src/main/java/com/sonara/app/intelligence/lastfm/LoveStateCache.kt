package com.sonara.app.intelligence.lastfm

import java.util.concurrent.ConcurrentHashMap

/**
 * Madde 5: Love butonu state yönetimi.
 * Track değişince önce local cache okunur, sonra arka planda doğrulanır.
 * Optimistic update: tap → filled heart → API fail → eski state'e dön.
 */
object LoveStateCache {

    private val cache = ConcurrentHashMap<String, Boolean>()

    private fun key(title: String, artist: String): String =
        "${artist.lowercase().trim()}::${title.lowercase().trim()}"

    fun isLoved(title: String, artist: String): Boolean? =
        cache[key(title, artist)]

    fun setLoved(title: String, artist: String, loved: Boolean) {
        cache[key(title, artist)] = loved
    }

    fun clear() = cache.clear()

    fun size() = cache.size
}
