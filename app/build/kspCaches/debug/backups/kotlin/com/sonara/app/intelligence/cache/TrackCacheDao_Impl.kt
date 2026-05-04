package com.sonara.app.intelligence.cache

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TrackCacheDao_Impl(
  __db: RoomDatabase,
) : TrackCacheDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTrackCacheEntity: EntityInsertAdapter<TrackCacheEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTrackCacheEntity = object : EntityInsertAdapter<TrackCacheEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `track_cache` (`cacheKey`,`title`,`artist`,`album`,`genre`,`mood`,`energy`,`confidence`,`source`,`timestamp`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TrackCacheEntity) {
        statement.bindText(1, entity.cacheKey)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.artist)
        statement.bindText(4, entity.album)
        statement.bindText(5, entity.genre)
        statement.bindText(6, entity.mood)
        statement.bindDouble(7, entity.energy.toDouble())
        statement.bindDouble(8, entity.confidence.toDouble())
        statement.bindText(9, entity.source)
        statement.bindLong(10, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(entry: TrackCacheEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfTrackCacheEntity.insert(_connection, entry)
  }

  public override suspend fun `get`(key: String): TrackCacheEntity? {
    val _sql: String = "SELECT * FROM track_cache WHERE cacheKey = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, key)
        val _columnIndexOfCacheKey: Int = getColumnIndexOrThrow(_stmt, "cacheKey")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfArtist: Int = getColumnIndexOrThrow(_stmt, "artist")
        val _columnIndexOfAlbum: Int = getColumnIndexOrThrow(_stmt, "album")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfMood: Int = getColumnIndexOrThrow(_stmt, "mood")
        val _columnIndexOfEnergy: Int = getColumnIndexOrThrow(_stmt, "energy")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: TrackCacheEntity?
        if (_stmt.step()) {
          val _tmpCacheKey: String
          _tmpCacheKey = _stmt.getText(_columnIndexOfCacheKey)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpArtist: String
          _tmpArtist = _stmt.getText(_columnIndexOfArtist)
          val _tmpAlbum: String
          _tmpAlbum = _stmt.getText(_columnIndexOfAlbum)
          val _tmpGenre: String
          _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          val _tmpMood: String
          _tmpMood = _stmt.getText(_columnIndexOfMood)
          val _tmpEnergy: Float
          _tmpEnergy = _stmt.getDouble(_columnIndexOfEnergy).toFloat()
          val _tmpConfidence: Float
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence).toFloat()
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _result =
              TrackCacheEntity(_tmpCacheKey,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpGenre,_tmpMood,_tmpEnergy,_tmpConfidence,_tmpSource,_tmpTimestamp)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM track_cache"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun cleanup(now: Long, ttl: Long) {
    val _sql: String = "DELETE FROM track_cache WHERE ? - timestamp > ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, now)
        _argIndex = 2
        _stmt.bindLong(_argIndex, ttl)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAll() {
    val _sql: String = "DELETE FROM track_cache"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
