package com.sonara.app.intelligence.lyrics

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
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class LyricsCacheDao_Impl(
  __db: RoomDatabase,
) : LyricsCacheDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfLyricsCacheEntity: EntityInsertAdapter<LyricsCacheEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfLyricsCacheEntity = object : EntityInsertAdapter<LyricsCacheEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `lyrics_cache` (`id`,`cacheKey`,`syncedLyrics`,`plainLyrics`,`source`,`cachedAt`,`translatedLyrics`,`translationLanguage`,`translationMode`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LyricsCacheEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.cacheKey)
        val _tmpSyncedLyrics: String? = entity.syncedLyrics
        if (_tmpSyncedLyrics == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpSyncedLyrics)
        }
        val _tmpPlainLyrics: String? = entity.plainLyrics
        if (_tmpPlainLyrics == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpPlainLyrics)
        }
        statement.bindText(5, entity.source)
        statement.bindLong(6, entity.cachedAt)
        statement.bindText(7, entity.translatedLyrics)
        statement.bindText(8, entity.translationLanguage)
        statement.bindText(9, entity.translationMode)
      }
    }
  }

  public override suspend fun insert(entity: LyricsCacheEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfLyricsCacheEntity.insert(_connection, entity)
  }

  public override suspend fun getByKey(key: String): LyricsCacheEntity? {
    val _sql: String = "SELECT * FROM lyrics_cache WHERE cacheKey = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, key)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCacheKey: Int = getColumnIndexOrThrow(_stmt, "cacheKey")
        val _columnIndexOfSyncedLyrics: Int = getColumnIndexOrThrow(_stmt, "syncedLyrics")
        val _columnIndexOfPlainLyrics: Int = getColumnIndexOrThrow(_stmt, "plainLyrics")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfCachedAt: Int = getColumnIndexOrThrow(_stmt, "cachedAt")
        val _columnIndexOfTranslatedLyrics: Int = getColumnIndexOrThrow(_stmt, "translatedLyrics")
        val _columnIndexOfTranslationLanguage: Int = getColumnIndexOrThrow(_stmt,
            "translationLanguage")
        val _columnIndexOfTranslationMode: Int = getColumnIndexOrThrow(_stmt, "translationMode")
        val _result: LyricsCacheEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCacheKey: String
          _tmpCacheKey = _stmt.getText(_columnIndexOfCacheKey)
          val _tmpSyncedLyrics: String?
          if (_stmt.isNull(_columnIndexOfSyncedLyrics)) {
            _tmpSyncedLyrics = null
          } else {
            _tmpSyncedLyrics = _stmt.getText(_columnIndexOfSyncedLyrics)
          }
          val _tmpPlainLyrics: String?
          if (_stmt.isNull(_columnIndexOfPlainLyrics)) {
            _tmpPlainLyrics = null
          } else {
            _tmpPlainLyrics = _stmt.getText(_columnIndexOfPlainLyrics)
          }
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpCachedAt: Long
          _tmpCachedAt = _stmt.getLong(_columnIndexOfCachedAt)
          val _tmpTranslatedLyrics: String
          _tmpTranslatedLyrics = _stmt.getText(_columnIndexOfTranslatedLyrics)
          val _tmpTranslationLanguage: String
          _tmpTranslationLanguage = _stmt.getText(_columnIndexOfTranslationLanguage)
          val _tmpTranslationMode: String
          _tmpTranslationMode = _stmt.getText(_columnIndexOfTranslationMode)
          _result =
              LyricsCacheEntity(_tmpId,_tmpCacheKey,_tmpSyncedLyrics,_tmpPlainLyrics,_tmpSource,_tmpCachedAt,_tmpTranslatedLyrics,_tmpTranslationLanguage,_tmpTranslationMode)
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
    val _sql: String = "SELECT COUNT(*) FROM lyrics_cache"
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

  public override suspend fun evictOlderThan(cutoff: Long) {
    val _sql: String = "DELETE FROM lyrics_cache WHERE cachedAt < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, cutoff)
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
