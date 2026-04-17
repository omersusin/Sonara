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
