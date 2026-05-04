package com.sonara.app.`data`.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.sonara.app.`data`.models.UserFeedback
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class UserFeedbackDao_Impl(
  __db: RoomDatabase,
) : UserFeedbackDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfUserFeedback: EntityInsertAdapter<UserFeedback>
  init {
    this.__db = __db
    this.__insertAdapterOfUserFeedback = object : EntityInsertAdapter<UserFeedback>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `user_feedback` (`id`,`trackTitle`,`trackArtist`,`suggestedGenre`,`correctedGenre`,`suggestedBands`,`correctedBands`,`accepted`,`audioRoute`,`confidence`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: UserFeedback) {
        statement.bindLong(1, entity.id)
        val _tmpTrackTitle: String? = entity.trackTitle
        if (_tmpTrackTitle == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpTrackTitle)
        }
        val _tmpTrackArtist: String? = entity.trackArtist
        if (_tmpTrackArtist == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpTrackArtist)
        }
        statement.bindText(4, entity.suggestedGenre)
        val _tmpCorrectedGenre: String? = entity.correctedGenre
        if (_tmpCorrectedGenre == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpCorrectedGenre)
        }
        val _tmpSuggestedBands: String? = entity.suggestedBands
        if (_tmpSuggestedBands == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpSuggestedBands)
        }
        val _tmpCorrectedBands: String? = entity.correctedBands
        if (_tmpCorrectedBands == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpCorrectedBands)
        }
        val _tmp: Int = if (entity.accepted) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindText(9, entity.audioRoute)
        statement.bindDouble(10, entity.confidence.toDouble())
        statement.bindLong(11, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(fb: UserFeedback): Long = performSuspending(__db, false, true)
      { _connection ->
    val _result: Long = __insertAdapterOfUserFeedback.insertAndReturnId(_connection, fb)
    _result
  }

  public override suspend fun forGenre(genre: String, limit: Int): List<UserFeedback> {
    val _sql: String =
        "SELECT * FROM user_feedback WHERE suggestedGenre = ? ORDER BY timestamp DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, genre)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTrackTitle: Int = getColumnIndexOrThrow(_stmt, "trackTitle")
        val _columnIndexOfTrackArtist: Int = getColumnIndexOrThrow(_stmt, "trackArtist")
        val _columnIndexOfSuggestedGenre: Int = getColumnIndexOrThrow(_stmt, "suggestedGenre")
        val _columnIndexOfCorrectedGenre: Int = getColumnIndexOrThrow(_stmt, "correctedGenre")
        val _columnIndexOfSuggestedBands: Int = getColumnIndexOrThrow(_stmt, "suggestedBands")
        val _columnIndexOfCorrectedBands: Int = getColumnIndexOrThrow(_stmt, "correctedBands")
        val _columnIndexOfAccepted: Int = getColumnIndexOrThrow(_stmt, "accepted")
        val _columnIndexOfAudioRoute: Int = getColumnIndexOrThrow(_stmt, "audioRoute")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<UserFeedback> = mutableListOf()
        while (_stmt.step()) {
          val _item: UserFeedback
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTrackTitle: String?
          if (_stmt.isNull(_columnIndexOfTrackTitle)) {
            _tmpTrackTitle = null
          } else {
            _tmpTrackTitle = _stmt.getText(_columnIndexOfTrackTitle)
          }
          val _tmpTrackArtist: String?
          if (_stmt.isNull(_columnIndexOfTrackArtist)) {
            _tmpTrackArtist = null
          } else {
            _tmpTrackArtist = _stmt.getText(_columnIndexOfTrackArtist)
          }
          val _tmpSuggestedGenre: String
          _tmpSuggestedGenre = _stmt.getText(_columnIndexOfSuggestedGenre)
          val _tmpCorrectedGenre: String?
          if (_stmt.isNull(_columnIndexOfCorrectedGenre)) {
            _tmpCorrectedGenre = null
          } else {
            _tmpCorrectedGenre = _stmt.getText(_columnIndexOfCorrectedGenre)
          }
          val _tmpSuggestedBands: String?
          if (_stmt.isNull(_columnIndexOfSuggestedBands)) {
            _tmpSuggestedBands = null
          } else {
            _tmpSuggestedBands = _stmt.getText(_columnIndexOfSuggestedBands)
          }
          val _tmpCorrectedBands: String?
          if (_stmt.isNull(_columnIndexOfCorrectedBands)) {
            _tmpCorrectedBands = null
          } else {
            _tmpCorrectedBands = _stmt.getText(_columnIndexOfCorrectedBands)
          }
          val _tmpAccepted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfAccepted).toInt()
          _tmpAccepted = _tmp != 0
          val _tmpAudioRoute: String
          _tmpAudioRoute = _stmt.getText(_columnIndexOfAudioRoute)
          val _tmpConfidence: Float
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence).toFloat()
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              UserFeedback(_tmpId,_tmpTrackTitle,_tmpTrackArtist,_tmpSuggestedGenre,_tmpCorrectedGenre,_tmpSuggestedBands,_tmpCorrectedBands,_tmpAccepted,_tmpAudioRoute,_tmpConfidence,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun acceptanceRate(genre: String): Float {
    val _sql: String =
        "SELECT CAST(SUM(CASE WHEN accepted = 1 THEN 1 ELSE 0 END) AS FLOAT) / MAX(COUNT(*), 1) FROM user_feedback WHERE suggestedGenre = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, genre)
        val _result: Float
        if (_stmt.step()) {
          val _tmp: Float
          _tmp = _stmt.getDouble(0).toFloat()
          _result = _tmp
        } else {
          _result = 0f
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun topCorrection(genre: String): String? {
    val _sql: String =
        "SELECT correctedGenre FROM user_feedback WHERE suggestedGenre = ? AND correctedGenre IS NOT NULL GROUP BY correctedGenre ORDER BY COUNT(*) DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, genre)
        val _result: String?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getText(0)
          }
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
    val _sql: String = "SELECT COUNT(*) FROM user_feedback"
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

  public override suspend fun prune(cutoff: Long) {
    val _sql: String = "DELETE FROM user_feedback WHERE timestamp < ?"
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
