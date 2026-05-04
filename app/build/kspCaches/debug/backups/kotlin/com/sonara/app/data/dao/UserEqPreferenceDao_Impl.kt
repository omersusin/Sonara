package com.sonara.app.`data`.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.sonara.app.`data`.models.UserEqPreference
import javax.`annotation`.processing.Generated
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class UserEqPreferenceDao_Impl(
  __db: RoomDatabase,
) : UserEqPreferenceDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfUserEqPreference: EntityInsertAdapter<UserEqPreference>
  init {
    this.__db = __db
    this.__insertAdapterOfUserEqPreference = object : EntityInsertAdapter<UserEqPreference>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `user_eq_preferences` (`id`,`genre`,`mood`,`energy`,`audioRoute`,`bandLevels`,`usageCount`,`lastUsed`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: UserEqPreference) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.genre)
        val _tmpMood: String? = entity.mood
        if (_tmpMood == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpMood)
        }
        statement.bindDouble(4, entity.energy.toDouble())
        statement.bindText(5, entity.audioRoute)
        statement.bindText(6, entity.bandLevels)
        statement.bindLong(7, entity.usageCount.toLong())
        statement.bindLong(8, entity.lastUsed)
        statement.bindLong(9, entity.createdAt)
      }
    }
  }

  public override suspend fun upsert(pref: UserEqPreference): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfUserEqPreference.insertAndReturnId(_connection, pref)
    _result
  }

  public override suspend fun getBest(genre: String, route: String): UserEqPreference? {
    val _sql: String =
        "SELECT * FROM user_eq_preferences WHERE genre = ? AND audioRoute = ? ORDER BY usageCount DESC, lastUsed DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, genre)
        _argIndex = 2
        _stmt.bindText(_argIndex, route)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfMood: Int = getColumnIndexOrThrow(_stmt, "mood")
        val _columnIndexOfEnergy: Int = getColumnIndexOrThrow(_stmt, "energy")
        val _columnIndexOfAudioRoute: Int = getColumnIndexOrThrow(_stmt, "audioRoute")
        val _columnIndexOfBandLevels: Int = getColumnIndexOrThrow(_stmt, "bandLevels")
        val _columnIndexOfUsageCount: Int = getColumnIndexOrThrow(_stmt, "usageCount")
        val _columnIndexOfLastUsed: Int = getColumnIndexOrThrow(_stmt, "lastUsed")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: UserEqPreference?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpGenre: String
          _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          val _tmpMood: String?
          if (_stmt.isNull(_columnIndexOfMood)) {
            _tmpMood = null
          } else {
            _tmpMood = _stmt.getText(_columnIndexOfMood)
          }
          val _tmpEnergy: Float
          _tmpEnergy = _stmt.getDouble(_columnIndexOfEnergy).toFloat()
          val _tmpAudioRoute: String
          _tmpAudioRoute = _stmt.getText(_columnIndexOfAudioRoute)
          val _tmpBandLevels: String
          _tmpBandLevels = _stmt.getText(_columnIndexOfBandLevels)
          val _tmpUsageCount: Int
          _tmpUsageCount = _stmt.getLong(_columnIndexOfUsageCount).toInt()
          val _tmpLastUsed: Long
          _tmpLastUsed = _stmt.getLong(_columnIndexOfLastUsed)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _result =
              UserEqPreference(_tmpId,_tmpGenre,_tmpMood,_tmpEnergy,_tmpAudioRoute,_tmpBandLevels,_tmpUsageCount,_tmpLastUsed,_tmpCreatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllForGenre(genre: String, limit: Int): List<UserEqPreference> {
    val _sql: String =
        "SELECT * FROM user_eq_preferences WHERE genre = ? ORDER BY usageCount DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, genre)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfMood: Int = getColumnIndexOrThrow(_stmt, "mood")
        val _columnIndexOfEnergy: Int = getColumnIndexOrThrow(_stmt, "energy")
        val _columnIndexOfAudioRoute: Int = getColumnIndexOrThrow(_stmt, "audioRoute")
        val _columnIndexOfBandLevels: Int = getColumnIndexOrThrow(_stmt, "bandLevels")
        val _columnIndexOfUsageCount: Int = getColumnIndexOrThrow(_stmt, "usageCount")
        val _columnIndexOfLastUsed: Int = getColumnIndexOrThrow(_stmt, "lastUsed")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<UserEqPreference> = mutableListOf()
        while (_stmt.step()) {
          val _item: UserEqPreference
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpGenre: String
          _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          val _tmpMood: String?
          if (_stmt.isNull(_columnIndexOfMood)) {
            _tmpMood = null
          } else {
            _tmpMood = _stmt.getText(_columnIndexOfMood)
          }
          val _tmpEnergy: Float
          _tmpEnergy = _stmt.getDouble(_columnIndexOfEnergy).toFloat()
          val _tmpAudioRoute: String
          _tmpAudioRoute = _stmt.getText(_columnIndexOfAudioRoute)
          val _tmpBandLevels: String
          _tmpBandLevels = _stmt.getText(_columnIndexOfBandLevels)
          val _tmpUsageCount: Int
          _tmpUsageCount = _stmt.getLong(_columnIndexOfUsageCount).toInt()
          val _tmpLastUsed: Long
          _tmpLastUsed = _stmt.getLong(_columnIndexOfLastUsed)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item =
              UserEqPreference(_tmpId,_tmpGenre,_tmpMood,_tmpEnergy,_tmpAudioRoute,_tmpBandLevels,_tmpUsageCount,_tmpLastUsed,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM user_eq_preferences"
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

  public override suspend fun touch(id: Long, now: Long) {
    val _sql: String =
        "UPDATE user_eq_preferences SET usageCount = usageCount + 1, lastUsed = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, now)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun prune(cutoff: Long) {
    val _sql: String = "DELETE FROM user_eq_preferences WHERE lastUsed < ?"
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
