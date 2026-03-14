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
