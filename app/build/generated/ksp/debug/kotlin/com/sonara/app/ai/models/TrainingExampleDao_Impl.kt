package com.sonara.app.ai.models

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
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
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TrainingExampleDao_Impl(
  __db: RoomDatabase,
) : TrainingExampleDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTrainingExample: EntityInsertAdapter<TrainingExample>
  init {
    this.__db = __db
    this.__insertAdapterOfTrainingExample = object : EntityInsertAdapter<TrainingExample>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `sonara_training_examples` (`id`,`featureVector`,`genre`,`moodValence`,`moodArousal`,`energy`,`source`,`trackTitle`,`trackArtist`,`timestamp`,`useCount`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TrainingExample) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.featureVector)
        statement.bindText(3, entity.genre)
        statement.bindDouble(4, entity.moodValence.toDouble())
        statement.bindDouble(5, entity.moodArousal.toDouble())
        statement.bindDouble(6, entity.energy.toDouble())
        statement.bindText(7, entity.source)
        statement.bindText(8, entity.trackTitle)
        statement.bindText(9, entity.trackArtist)
        statement.bindLong(10, entity.timestamp)
        statement.bindLong(11, entity.useCount.toLong())
      }
    }
  }

  public override suspend fun insert(example: TrainingExample): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfTrainingExample.insertAndReturnId(_connection, example)
    _result
  }

  public override suspend fun insertAll(examples: List<TrainingExample>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTrainingExample.insert(_connection, examples)
  }

  public override suspend fun getAll(): List<TrainingExample> {
    val _sql: String = "SELECT * FROM sonara_training_examples ORDER BY timestamp DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFeatureVector: Int = getColumnIndexOrThrow(_stmt, "featureVector")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfMoodValence: Int = getColumnIndexOrThrow(_stmt, "moodValence")
        val _columnIndexOfMoodArousal: Int = getColumnIndexOrThrow(_stmt, "moodArousal")
        val _columnIndexOfEnergy: Int = getColumnIndexOrThrow(_stmt, "energy")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfTrackTitle: Int = getColumnIndexOrThrow(_stmt, "trackTitle")
        val _columnIndexOfTrackArtist: Int = getColumnIndexOrThrow(_stmt, "trackArtist")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfUseCount: Int = getColumnIndexOrThrow(_stmt, "useCount")
        val _result: MutableList<TrainingExample> = mutableListOf()
        while (_stmt.step()) {
          val _item: TrainingExample
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpFeatureVector: String
          _tmpFeatureVector = _stmt.getText(_columnIndexOfFeatureVector)
          val _tmpGenre: String
          _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          val _tmpMoodValence: Float
          _tmpMoodValence = _stmt.getDouble(_columnIndexOfMoodValence).toFloat()
          val _tmpMoodArousal: Float
          _tmpMoodArousal = _stmt.getDouble(_columnIndexOfMoodArousal).toFloat()
          val _tmpEnergy: Float
          _tmpEnergy = _stmt.getDouble(_columnIndexOfEnergy).toFloat()
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpTrackTitle: String
          _tmpTrackTitle = _stmt.getText(_columnIndexOfTrackTitle)
          val _tmpTrackArtist: String
          _tmpTrackArtist = _stmt.getText(_columnIndexOfTrackArtist)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpUseCount: Int
          _tmpUseCount = _stmt.getLong(_columnIndexOfUseCount).toInt()
          _item =
              TrainingExample(_tmpId,_tmpFeatureVector,_tmpGenre,_tmpMoodValence,_tmpMoodArousal,_tmpEnergy,_tmpSource,_tmpTrackTitle,_tmpTrackArtist,_tmpTimestamp,_tmpUseCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM sonara_training_examples"
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

  public override suspend fun getLearnedCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM sonara_training_examples WHERE source != 'prototype'"
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

  public override fun getLearnedCountFlow(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM sonara_training_examples WHERE source != 'prototype'"
    return createFlow(__db, false, arrayOf("sonara_training_examples")) { _connection ->
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

  public override suspend fun incrementUseCount(id: Long) {
    val _sql: String = "UPDATE sonara_training_examples SET useCount = useCount + 1 WHERE id = ?"
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

  public override suspend fun deletePrototypes() {
    val _sql: String = "DELETE FROM sonara_training_examples WHERE source = 'prototype'"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCommunity() {
    val _sql: String = "DELETE FROM sonara_training_examples WHERE source = 'community'"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM sonara_training_examples"
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
