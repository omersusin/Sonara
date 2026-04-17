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

package com.sonara.app.intelligence.cache

import com.sonara.app.data.models.TrackInfo

class TrackCache(private val dao: TrackCacheDao) {

    suspend fun get(title: String, artist: String): TrackInfo? {
        val key = TrackCacheEntity.makeKey(title, artist)
        val entity = dao.get(key) ?: return null
        if (entity.isExpired()) return null
        return TrackInfo(
            title = entity.title, artist = entity.artist, album = entity.album,
            genre = entity.genre, mood = entity.mood, energy = entity.energy,
            confidence = entity.confidence, source = "${entity.source}-cached"
        )
    }

    suspend fun put(info: TrackInfo) {
        val key = TrackCacheEntity.makeKey(info.title, info.artist)
        dao.insert(TrackCacheEntity(
            cacheKey = key, title = info.title, artist = info.artist,
            album = info.album, genre = info.genre, mood = info.mood,
            energy = info.energy, confidence = info.confidence, source = info.source
        ))
    }

    suspend fun cleanup() = dao.cleanup()
    suspend fun clear() = dao.clearAll()
    suspend fun size() = dao.count()
}
