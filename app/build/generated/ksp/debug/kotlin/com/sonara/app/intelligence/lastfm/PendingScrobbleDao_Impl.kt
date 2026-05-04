package com.sonara.app.intelligence.lastfm

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PendingScrobbleDao_Impl(
  __db: RoomDatabase,
) : PendingScrobbleDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPendingScrobble: EntityInsertAdapter<PendingScrobble>

  private val __deleteAdapterOfPendingScrobble: EntityDeleteOrUpdateAdapter<PendingScrobble>
  init {
    this.__db = __db
    this.__insertAdapterOfPendingScrobble = object : EntityInsertAdapter<PendingScrobble>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `pending_scrobbles` (`id`,`track`,`artist`,`album`,`timestamp`,`createdAt`,`retryCount`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PendingScrobble) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.track)
        statement.bindText(3, entity.artist)
        statement.bindText(4, entity.album)
        statement.bindLong(5, entity.timestamp)
        statement.bindLong(6, entity.createdAt)
        statement.bindLong(7, entity.retryCount.toLong())
      }
    }
    this.__deleteAdapterOfPendingScrobble = object : EntityDeleteOrUpdateAdapter<PendingScrobble>()
        {
      protected override fun createQuery(): String =
          "DELETE FROM `pending_scrobbles` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PendingScrobble) {
        statement.bindLong(1, entity.id)
      }
    }
  }

  public override suspend fun insert(scrobble: PendingScrobble): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfPendingScrobble.insert(_connection, scrobble)
  }

  public override suspend fun delete(scrobble: PendingScrobble): Unit = performSuspending(__db,
      false, true) { _connection ->
    __deleteAdapterOfPendingScrobble.handle(_connection, scrobble)
  }

  public override suspend fun getPending(): List<PendingScrobble> {
    val _sql: String = "SELECT * FROM pending_scrobbles ORDER BY timestamp ASC LIMIT 50"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTrack: Int = getColumnIndexOrThrow(_stmt, "track")
        val _columnIndexOfArtist: Int = getColumnIndexOrThrow(_stmt, "artist")
        val _columnIndexOfAlbum: Int = getColumnIndexOrThrow(_stmt, "album")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfRetryCount: Int = getColumnIndexOrThrow(_stmt, "retryCount")
        val _result: MutableList<PendingScrobble> = mutableListOf()
        while (_stmt.step()) {
          val _item: PendingScrobble
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTrack: String
          _tmpTrack = _stmt.getText(_columnIndexOfTrack)
          val _tmpArtist: String
          _tmpArtist = _stmt.getText(_columnIndexOfArtist)
          val _tmpAlbum: String
          _tmpAlbum = _stmt.getText(_columnIndexOfAlbum)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpRetryCount: Int
          _tmpRetryCount = _stmt.getLong(_columnIndexOfRetryCount).toInt()
          _item =
              PendingScrobble(_tmpId,_tmpTrack,_tmpArtist,_tmpAlbum,_tmpTimestamp,_tmpCreatedAt,_tmpRetryCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM pending_scrobbles"
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

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM pending_scrobbles WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun pruneStale() {
    val _sql: String = "DELETE FROM pending_scrobbles WHERE retryCount > 5"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun incrementRetry(id: Long) {
    val _sql: String = "UPDATE pending_scrobbles SET retryCount = retryCount + 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
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
