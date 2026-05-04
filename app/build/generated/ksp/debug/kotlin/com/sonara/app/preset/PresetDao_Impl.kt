package com.sonara.app.preset

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class PresetDao_Impl(
  __db: RoomDatabase,
) : PresetDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPreset: EntityInsertAdapter<Preset>

  private val __deleteAdapterOfPreset: EntityDeleteOrUpdateAdapter<Preset>

  private val __updateAdapterOfPreset: EntityDeleteOrUpdateAdapter<Preset>
  init {
    this.__db = __db
    this.__insertAdapterOfPreset = object : EntityInsertAdapter<Preset>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `presets` (`id`,`name`,`bands`,`preamp`,`bassBoost`,`virtualizer`,`loudness`,`isBuiltIn`,`category`,`headphoneId`,`genre`,`reverb`,`isFavorite`,`lastUsed`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Preset) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.bands)
        statement.bindDouble(4, entity.preamp.toDouble())
        statement.bindLong(5, entity.bassBoost.toLong())
        statement.bindLong(6, entity.virtualizer.toLong())
        statement.bindLong(7, entity.loudness.toLong())
        val _tmp: Int = if (entity.isBuiltIn) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindText(9, entity.category)
        val _tmpHeadphoneId: String? = entity.headphoneId
        if (_tmpHeadphoneId == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpHeadphoneId)
        }
        val _tmpGenre: String? = entity.genre
        if (_tmpGenre == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpGenre)
        }
        statement.bindLong(12, entity.reverb.toLong())
        val _tmp_1: Int = if (entity.isFavorite) 1 else 0
        statement.bindLong(13, _tmp_1.toLong())
        statement.bindLong(14, entity.lastUsed)
      }
    }
    this.__deleteAdapterOfPreset = object : EntityDeleteOrUpdateAdapter<Preset>() {
      protected override fun createQuery(): String = "DELETE FROM `presets` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Preset) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfPreset = object : EntityDeleteOrUpdateAdapter<Preset>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `presets` SET `id` = ?,`name` = ?,`bands` = ?,`preamp` = ?,`bassBoost` = ?,`virtualizer` = ?,`loudness` = ?,`isBuiltIn` = ?,`category` = ?,`headphoneId` = ?,`genre` = ?,`reverb` = ?,`isFavorite` = ?,`lastUsed` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Preset) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.bands)
        statement.bindDouble(4, entity.preamp.toDouble())
        statement.bindLong(5, entity.bassBoost.toLong())
        statement.bindLong(6, entity.virtualizer.toLong())
        statement.bindLong(7, entity.loudness.toLong())
        val _tmp: Int = if (entity.isBuiltIn) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindText(9, entity.category)
        val _tmpHeadphoneId: String? = entity.headphoneId
        if (_tmpHeadphoneId == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpHeadphoneId)
        }
        val _tmpGenre: String? = entity.genre
        if (_tmpGenre == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpGenre)
        }
        statement.bindLong(12, entity.reverb.toLong())
        val _tmp_1: Int = if (entity.isFavorite) 1 else 0
        statement.bindLong(13, _tmp_1.toLong())
        statement.bindLong(14, entity.lastUsed)
        statement.bindLong(15, entity.id)
      }
    }
  }

  public override suspend fun insert(preset: Preset): Long = performSuspending(__db, false, true) {
      _connection ->
    val _result: Long = __insertAdapterOfPreset.insertAndReturnId(_connection, preset)
    _result
  }

  public override suspend fun insertAll(presets: List<Preset>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfPreset.insert(_connection, presets)
  }

  public override suspend fun delete(preset: Preset): Unit = performSuspending(__db, false, true) {
      _connection ->
    __deleteAdapterOfPreset.handle(_connection, preset)
  }

  public override suspend fun update(preset: Preset): Unit = performSuspending(__db, false, true) {
      _connection ->
    __updateAdapterOfPreset.handle(_connection, preset)
  }

  public override fun getAllPresets(): Flow<List<Preset>> {
    val _sql: String = "SELECT * FROM presets ORDER BY isBuiltIn DESC, lastUsed DESC"
    return createFlow(__db, false, arrayOf("presets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfBands: Int = getColumnIndexOrThrow(_stmt, "bands")
        val _columnIndexOfPreamp: Int = getColumnIndexOrThrow(_stmt, "preamp")
        val _columnIndexOfBassBoost: Int = getColumnIndexOrThrow(_stmt, "bassBoost")
        val _columnIndexOfVirtualizer: Int = getColumnIndexOrThrow(_stmt, "virtualizer")
        val _columnIndexOfLoudness: Int = getColumnIndexOrThrow(_stmt, "loudness")
        val _columnIndexOfIsBuiltIn: Int = getColumnIndexOrThrow(_stmt, "isBuiltIn")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfHeadphoneId: Int = getColumnIndexOrThrow(_stmt, "headphoneId")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfReverb: Int = getColumnIndexOrThrow(_stmt, "reverb")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastUsed: Int = getColumnIndexOrThrow(_stmt, "lastUsed")
        val _result: MutableList<Preset> = mutableListOf()
        while (_stmt.step()) {
          val _item: Preset
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpBands: String
          _tmpBands = _stmt.getText(_columnIndexOfBands)
          val _tmpPreamp: Float
          _tmpPreamp = _stmt.getDouble(_columnIndexOfPreamp).toFloat()
          val _tmpBassBoost: Int
          _tmpBassBoost = _stmt.getLong(_columnIndexOfBassBoost).toInt()
          val _tmpVirtualizer: Int
          _tmpVirtualizer = _stmt.getLong(_columnIndexOfVirtualizer).toInt()
          val _tmpLoudness: Int
          _tmpLoudness = _stmt.getLong(_columnIndexOfLoudness).toInt()
          val _tmpIsBuiltIn: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBuiltIn).toInt()
          _tmpIsBuiltIn = _tmp != 0
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpHeadphoneId: String?
          if (_stmt.isNull(_columnIndexOfHeadphoneId)) {
            _tmpHeadphoneId = null
          } else {
            _tmpHeadphoneId = _stmt.getText(_columnIndexOfHeadphoneId)
          }
          val _tmpGenre: String?
          if (_stmt.isNull(_columnIndexOfGenre)) {
            _tmpGenre = null
          } else {
            _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          }
          val _tmpReverb: Int
          _tmpReverb = _stmt.getLong(_columnIndexOfReverb).toInt()
          val _tmpIsFavorite: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp_1 != 0
          val _tmpLastUsed: Long
          _tmpLastUsed = _stmt.getLong(_columnIndexOfLastUsed)
          _item =
              Preset(_tmpId,_tmpName,_tmpBands,_tmpPreamp,_tmpBassBoost,_tmpVirtualizer,_tmpLoudness,_tmpIsBuiltIn,_tmpCategory,_tmpHeadphoneId,_tmpGenre,_tmpReverb,_tmpIsFavorite,_tmpLastUsed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getBuiltInPresets(): Flow<List<Preset>> {
    val _sql: String = "SELECT * FROM presets WHERE isBuiltIn = 1"
    return createFlow(__db, false, arrayOf("presets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfBands: Int = getColumnIndexOrThrow(_stmt, "bands")
        val _columnIndexOfPreamp: Int = getColumnIndexOrThrow(_stmt, "preamp")
        val _columnIndexOfBassBoost: Int = getColumnIndexOrThrow(_stmt, "bassBoost")
        val _columnIndexOfVirtualizer: Int = getColumnIndexOrThrow(_stmt, "virtualizer")
        val _columnIndexOfLoudness: Int = getColumnIndexOrThrow(_stmt, "loudness")
        val _columnIndexOfIsBuiltIn: Int = getColumnIndexOrThrow(_stmt, "isBuiltIn")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfHeadphoneId: Int = getColumnIndexOrThrow(_stmt, "headphoneId")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfReverb: Int = getColumnIndexOrThrow(_stmt, "reverb")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastUsed: Int = getColumnIndexOrThrow(_stmt, "lastUsed")
        val _result: MutableList<Preset> = mutableListOf()
        while (_stmt.step()) {
          val _item: Preset
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpBands: String
          _tmpBands = _stmt.getText(_columnIndexOfBands)
          val _tmpPreamp: Float
          _tmpPreamp = _stmt.getDouble(_columnIndexOfPreamp).toFloat()
          val _tmpBassBoost: Int
          _tmpBassBoost = _stmt.getLong(_columnIndexOfBassBoost).toInt()
          val _tmpVirtualizer: Int
          _tmpVirtualizer = _stmt.getLong(_columnIndexOfVirtualizer).toInt()
          val _tmpLoudness: Int
          _tmpLoudness = _stmt.getLong(_columnIndexOfLoudness).toInt()
          val _tmpIsBuiltIn: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBuiltIn).toInt()
          _tmpIsBuiltIn = _tmp != 0
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpHeadphoneId: String?
          if (_stmt.isNull(_columnIndexOfHeadphoneId)) {
            _tmpHeadphoneId = null
          } else {
            _tmpHeadphoneId = _stmt.getText(_columnIndexOfHeadphoneId)
          }
          val _tmpGenre: String?
          if (_stmt.isNull(_columnIndexOfGenre)) {
            _tmpGenre = null
          } else {
            _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          }
          val _tmpReverb: Int
          _tmpReverb = _stmt.getLong(_columnIndexOfReverb).toInt()
          val _tmpIsFavorite: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp_1 != 0
          val _tmpLastUsed: Long
          _tmpLastUsed = _stmt.getLong(_columnIndexOfLastUsed)
          _item =
              Preset(_tmpId,_tmpName,_tmpBands,_tmpPreamp,_tmpBassBoost,_tmpVirtualizer,_tmpLoudness,_tmpIsBuiltIn,_tmpCategory,_tmpHeadphoneId,_tmpGenre,_tmpReverb,_tmpIsFavorite,_tmpLastUsed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCustomPresets(): Flow<List<Preset>> {
    val _sql: String = "SELECT * FROM presets WHERE isBuiltIn = 0"
    return createFlow(__db, false, arrayOf("presets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfBands: Int = getColumnIndexOrThrow(_stmt, "bands")
        val _columnIndexOfPreamp: Int = getColumnIndexOrThrow(_stmt, "preamp")
        val _columnIndexOfBassBoost: Int = getColumnIndexOrThrow(_stmt, "bassBoost")
        val _columnIndexOfVirtualizer: Int = getColumnIndexOrThrow(_stmt, "virtualizer")
        val _columnIndexOfLoudness: Int = getColumnIndexOrThrow(_stmt, "loudness")
        val _columnIndexOfIsBuiltIn: Int = getColumnIndexOrThrow(_stmt, "isBuiltIn")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfHeadphoneId: Int = getColumnIndexOrThrow(_stmt, "headphoneId")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfReverb: Int = getColumnIndexOrThrow(_stmt, "reverb")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastUsed: Int = getColumnIndexOrThrow(_stmt, "lastUsed")
        val _result: MutableList<Preset> = mutableListOf()
        while (_stmt.step()) {
          val _item: Preset
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpBands: String
          _tmpBands = _stmt.getText(_columnIndexOfBands)
          val _tmpPreamp: Float
          _tmpPreamp = _stmt.getDouble(_columnIndexOfPreamp).toFloat()
          val _tmpBassBoost: Int
          _tmpBassBoost = _stmt.getLong(_columnIndexOfBassBoost).toInt()
          val _tmpVirtualizer: Int
          _tmpVirtualizer = _stmt.getLong(_columnIndexOfVirtualizer).toInt()
          val _tmpLoudness: Int
          _tmpLoudness = _stmt.getLong(_columnIndexOfLoudness).toInt()
          val _tmpIsBuiltIn: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBuiltIn).toInt()
          _tmpIsBuiltIn = _tmp != 0
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpHeadphoneId: String?
          if (_stmt.isNull(_columnIndexOfHeadphoneId)) {
            _tmpHeadphoneId = null
          } else {
            _tmpHeadphoneId = _stmt.getText(_columnIndexOfHeadphoneId)
          }
          val _tmpGenre: String?
          if (_stmt.isNull(_columnIndexOfGenre)) {
            _tmpGenre = null
          } else {
            _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          }
          val _tmpReverb: Int
          _tmpReverb = _stmt.getLong(_columnIndexOfReverb).toInt()
          val _tmpIsFavorite: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp_1 != 0
          val _tmpLastUsed: Long
          _tmpLastUsed = _stmt.getLong(_columnIndexOfLastUsed)
          _item =
              Preset(_tmpId,_tmpName,_tmpBands,_tmpPreamp,_tmpBassBoost,_tmpVirtualizer,_tmpLoudness,_tmpIsBuiltIn,_tmpCategory,_tmpHeadphoneId,_tmpGenre,_tmpReverb,_tmpIsFavorite,_tmpLastUsed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getFavorites(): Flow<List<Preset>> {
    val _sql: String = "SELECT * FROM presets WHERE isFavorite = 1"
    return createFlow(__db, false, arrayOf("presets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfBands: Int = getColumnIndexOrThrow(_stmt, "bands")
        val _columnIndexOfPreamp: Int = getColumnIndexOrThrow(_stmt, "preamp")
        val _columnIndexOfBassBoost: Int = getColumnIndexOrThrow(_stmt, "bassBoost")
        val _columnIndexOfVirtualizer: Int = getColumnIndexOrThrow(_stmt, "virtualizer")
        val _columnIndexOfLoudness: Int = getColumnIndexOrThrow(_stmt, "loudness")
        val _columnIndexOfIsBuiltIn: Int = getColumnIndexOrThrow(_stmt, "isBuiltIn")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfHeadphoneId: Int = getColumnIndexOrThrow(_stmt, "headphoneId")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfReverb: Int = getColumnIndexOrThrow(_stmt, "reverb")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastUsed: Int = getColumnIndexOrThrow(_stmt, "lastUsed")
        val _result: MutableList<Preset> = mutableListOf()
        while (_stmt.step()) {
          val _item: Preset
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpBands: String
          _tmpBands = _stmt.getText(_columnIndexOfBands)
          val _tmpPreamp: Float
          _tmpPreamp = _stmt.getDouble(_columnIndexOfPreamp).toFloat()
          val _tmpBassBoost: Int
          _tmpBassBoost = _stmt.getLong(_columnIndexOfBassBoost).toInt()
          val _tmpVirtualizer: Int
          _tmpVirtualizer = _stmt.getLong(_columnIndexOfVirtualizer).toInt()
          val _tmpLoudness: Int
          _tmpLoudness = _stmt.getLong(_columnIndexOfLoudness).toInt()
          val _tmpIsBuiltIn: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBuiltIn).toInt()
          _tmpIsBuiltIn = _tmp != 0
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpHeadphoneId: String?
          if (_stmt.isNull(_columnIndexOfHeadphoneId)) {
            _tmpHeadphoneId = null
          } else {
            _tmpHeadphoneId = _stmt.getText(_columnIndexOfHeadphoneId)
          }
          val _tmpGenre: String?
          if (_stmt.isNull(_columnIndexOfGenre)) {
            _tmpGenre = null
          } else {
            _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          }
          val _tmpReverb: Int
          _tmpReverb = _stmt.getLong(_columnIndexOfReverb).toInt()
          val _tmpIsFavorite: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp_1 != 0
          val _tmpLastUsed: Long
          _tmpLastUsed = _stmt.getLong(_columnIndexOfLastUsed)
          _item =
              Preset(_tmpId,_tmpName,_tmpBands,_tmpPreamp,_tmpBassBoost,_tmpVirtualizer,_tmpLoudness,_tmpIsBuiltIn,_tmpCategory,_tmpHeadphoneId,_tmpGenre,_tmpReverb,_tmpIsFavorite,_tmpLastUsed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getByCategory(category: String): Flow<List<Preset>> {
    val _sql: String = "SELECT * FROM presets WHERE category = ?"
    return createFlow(__db, false, arrayOf("presets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfBands: Int = getColumnIndexOrThrow(_stmt, "bands")
        val _columnIndexOfPreamp: Int = getColumnIndexOrThrow(_stmt, "preamp")
        val _columnIndexOfBassBoost: Int = getColumnIndexOrThrow(_stmt, "bassBoost")
        val _columnIndexOfVirtualizer: Int = getColumnIndexOrThrow(_stmt, "virtualizer")
        val _columnIndexOfLoudness: Int = getColumnIndexOrThrow(_stmt, "loudness")
        val _columnIndexOfIsBuiltIn: Int = getColumnIndexOrThrow(_stmt, "isBuiltIn")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfHeadphoneId: Int = getColumnIndexOrThrow(_stmt, "headphoneId")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfReverb: Int = getColumnIndexOrThrow(_stmt, "reverb")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastUsed: Int = getColumnIndexOrThrow(_stmt, "lastUsed")
        val _result: MutableList<Preset> = mutableListOf()
        while (_stmt.step()) {
          val _item: Preset
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpBands: String
          _tmpBands = _stmt.getText(_columnIndexOfBands)
          val _tmpPreamp: Float
          _tmpPreamp = _stmt.getDouble(_columnIndexOfPreamp).toFloat()
          val _tmpBassBoost: Int
          _tmpBassBoost = _stmt.getLong(_columnIndexOfBassBoost).toInt()
          val _tmpVirtualizer: Int
          _tmpVirtualizer = _stmt.getLong(_columnIndexOfVirtualizer).toInt()
          val _tmpLoudness: Int
          _tmpLoudness = _stmt.getLong(_columnIndexOfLoudness).toInt()
          val _tmpIsBuiltIn: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBuiltIn).toInt()
          _tmpIsBuiltIn = _tmp != 0
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpHeadphoneId: String?
          if (_stmt.isNull(_columnIndexOfHeadphoneId)) {
            _tmpHeadphoneId = null
          } else {
            _tmpHeadphoneId = _stmt.getText(_columnIndexOfHeadphoneId)
          }
          val _tmpGenre: String?
          if (_stmt.isNull(_columnIndexOfGenre)) {
            _tmpGenre = null
          } else {
            _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          }
          val _tmpReverb: Int
          _tmpReverb = _stmt.getLong(_columnIndexOfReverb).toInt()
          val _tmpIsFavorite: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp_1 != 0
          val _tmpLastUsed: Long
          _tmpLastUsed = _stmt.getLong(_columnIndexOfLastUsed)
          _item =
              Preset(_tmpId,_tmpName,_tmpBands,_tmpPreamp,_tmpBassBoost,_tmpVirtualizer,_tmpLoudness,_tmpIsBuiltIn,_tmpCategory,_tmpHeadphoneId,_tmpGenre,_tmpReverb,_tmpIsFavorite,_tmpLastUsed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getByHeadphone(hpId: String): Flow<List<Preset>> {
    val _sql: String = "SELECT * FROM presets WHERE headphoneId = ?"
    return createFlow(__db, false, arrayOf("presets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, hpId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfBands: Int = getColumnIndexOrThrow(_stmt, "bands")
        val _columnIndexOfPreamp: Int = getColumnIndexOrThrow(_stmt, "preamp")
        val _columnIndexOfBassBoost: Int = getColumnIndexOrThrow(_stmt, "bassBoost")
        val _columnIndexOfVirtualizer: Int = getColumnIndexOrThrow(_stmt, "virtualizer")
        val _columnIndexOfLoudness: Int = getColumnIndexOrThrow(_stmt, "loudness")
        val _columnIndexOfIsBuiltIn: Int = getColumnIndexOrThrow(_stmt, "isBuiltIn")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfHeadphoneId: Int = getColumnIndexOrThrow(_stmt, "headphoneId")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfReverb: Int = getColumnIndexOrThrow(_stmt, "reverb")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastUsed: Int = getColumnIndexOrThrow(_stmt, "lastUsed")
        val _result: MutableList<Preset> = mutableListOf()
        while (_stmt.step()) {
          val _item: Preset
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpBands: String
          _tmpBands = _stmt.getText(_columnIndexOfBands)
          val _tmpPreamp: Float
          _tmpPreamp = _stmt.getDouble(_columnIndexOfPreamp).toFloat()
          val _tmpBassBoost: Int
          _tmpBassBoost = _stmt.getLong(_columnIndexOfBassBoost).toInt()
          val _tmpVirtualizer: Int
          _tmpVirtualizer = _stmt.getLong(_columnIndexOfVirtualizer).toInt()
          val _tmpLoudness: Int
          _tmpLoudness = _stmt.getLong(_columnIndexOfLoudness).toInt()
          val _tmpIsBuiltIn: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBuiltIn).toInt()
          _tmpIsBuiltIn = _tmp != 0
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpHeadphoneId: String?
          if (_stmt.isNull(_columnIndexOfHeadphoneId)) {
            _tmpHeadphoneId = null
          } else {
            _tmpHeadphoneId = _stmt.getText(_columnIndexOfHeadphoneId)
          }
          val _tmpGenre: String?
          if (_stmt.isNull(_columnIndexOfGenre)) {
            _tmpGenre = null
          } else {
            _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          }
          val _tmpReverb: Int
          _tmpReverb = _stmt.getLong(_columnIndexOfReverb).toInt()
          val _tmpIsFavorite: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp_1 != 0
          val _tmpLastUsed: Long
          _tmpLastUsed = _stmt.getLong(_columnIndexOfLastUsed)
          _item =
              Preset(_tmpId,_tmpName,_tmpBands,_tmpPreamp,_tmpBassBoost,_tmpVirtualizer,_tmpLoudness,_tmpIsBuiltIn,_tmpCategory,_tmpHeadphoneId,_tmpGenre,_tmpReverb,_tmpIsFavorite,_tmpLastUsed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): Preset? {
    val _sql: String = "SELECT * FROM presets WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfBands: Int = getColumnIndexOrThrow(_stmt, "bands")
        val _columnIndexOfPreamp: Int = getColumnIndexOrThrow(_stmt, "preamp")
        val _columnIndexOfBassBoost: Int = getColumnIndexOrThrow(_stmt, "bassBoost")
        val _columnIndexOfVirtualizer: Int = getColumnIndexOrThrow(_stmt, "virtualizer")
        val _columnIndexOfLoudness: Int = getColumnIndexOrThrow(_stmt, "loudness")
        val _columnIndexOfIsBuiltIn: Int = getColumnIndexOrThrow(_stmt, "isBuiltIn")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfHeadphoneId: Int = getColumnIndexOrThrow(_stmt, "headphoneId")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfReverb: Int = getColumnIndexOrThrow(_stmt, "reverb")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastUsed: Int = getColumnIndexOrThrow(_stmt, "lastUsed")
        val _result: Preset?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpBands: String
          _tmpBands = _stmt.getText(_columnIndexOfBands)
          val _tmpPreamp: Float
          _tmpPreamp = _stmt.getDouble(_columnIndexOfPreamp).toFloat()
          val _tmpBassBoost: Int
          _tmpBassBoost = _stmt.getLong(_columnIndexOfBassBoost).toInt()
          val _tmpVirtualizer: Int
          _tmpVirtualizer = _stmt.getLong(_columnIndexOfVirtualizer).toInt()
          val _tmpLoudness: Int
          _tmpLoudness = _stmt.getLong(_columnIndexOfLoudness).toInt()
          val _tmpIsBuiltIn: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBuiltIn).toInt()
          _tmpIsBuiltIn = _tmp != 0
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpHeadphoneId: String?
          if (_stmt.isNull(_columnIndexOfHeadphoneId)) {
            _tmpHeadphoneId = null
          } else {
            _tmpHeadphoneId = _stmt.getText(_columnIndexOfHeadphoneId)
          }
          val _tmpGenre: String?
          if (_stmt.isNull(_columnIndexOfGenre)) {
            _tmpGenre = null
          } else {
            _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          }
          val _tmpReverb: Int
          _tmpReverb = _stmt.getLong(_columnIndexOfReverb).toInt()
          val _tmpIsFavorite: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp_1 != 0
          val _tmpLastUsed: Long
          _tmpLastUsed = _stmt.getLong(_columnIndexOfLastUsed)
          _result =
              Preset(_tmpId,_tmpName,_tmpBands,_tmpPreamp,_tmpBassBoost,_tmpVirtualizer,_tmpLoudness,_tmpIsBuiltIn,_tmpCategory,_tmpHeadphoneId,_tmpGenre,_tmpReverb,_tmpIsFavorite,_tmpLastUsed)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun builtInCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM presets WHERE isBuiltIn = 1"
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

  public override suspend fun getBuiltInsOnce(): List<Preset> {
    val _sql: String = "SELECT * FROM presets WHERE isBuiltIn = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfBands: Int = getColumnIndexOrThrow(_stmt, "bands")
        val _columnIndexOfPreamp: Int = getColumnIndexOrThrow(_stmt, "preamp")
        val _columnIndexOfBassBoost: Int = getColumnIndexOrThrow(_stmt, "bassBoost")
        val _columnIndexOfVirtualizer: Int = getColumnIndexOrThrow(_stmt, "virtualizer")
        val _columnIndexOfLoudness: Int = getColumnIndexOrThrow(_stmt, "loudness")
        val _columnIndexOfIsBuiltIn: Int = getColumnIndexOrThrow(_stmt, "isBuiltIn")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfHeadphoneId: Int = getColumnIndexOrThrow(_stmt, "headphoneId")
        val _columnIndexOfGenre: Int = getColumnIndexOrThrow(_stmt, "genre")
        val _columnIndexOfReverb: Int = getColumnIndexOrThrow(_stmt, "reverb")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfLastUsed: Int = getColumnIndexOrThrow(_stmt, "lastUsed")
        val _result: MutableList<Preset> = mutableListOf()
        while (_stmt.step()) {
          val _item: Preset
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpBands: String
          _tmpBands = _stmt.getText(_columnIndexOfBands)
          val _tmpPreamp: Float
          _tmpPreamp = _stmt.getDouble(_columnIndexOfPreamp).toFloat()
          val _tmpBassBoost: Int
          _tmpBassBoost = _stmt.getLong(_columnIndexOfBassBoost).toInt()
          val _tmpVirtualizer: Int
          _tmpVirtualizer = _stmt.getLong(_columnIndexOfVirtualizer).toInt()
          val _tmpLoudness: Int
          _tmpLoudness = _stmt.getLong(_columnIndexOfLoudness).toInt()
          val _tmpIsBuiltIn: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBuiltIn).toInt()
          _tmpIsBuiltIn = _tmp != 0
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpHeadphoneId: String?
          if (_stmt.isNull(_columnIndexOfHeadphoneId)) {
            _tmpHeadphoneId = null
          } else {
            _tmpHeadphoneId = _stmt.getText(_columnIndexOfHeadphoneId)
          }
          val _tmpGenre: String?
          if (_stmt.isNull(_columnIndexOfGenre)) {
            _tmpGenre = null
          } else {
            _tmpGenre = _stmt.getText(_columnIndexOfGenre)
          }
          val _tmpReverb: Int
          _tmpReverb = _stmt.getLong(_columnIndexOfReverb).toInt()
          val _tmpIsFavorite: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp_1 != 0
          val _tmpLastUsed: Long
          _tmpLastUsed = _stmt.getLong(_columnIndexOfLastUsed)
          _item =
              Preset(_tmpId,_tmpName,_tmpBands,_tmpPreamp,_tmpBassBoost,_tmpVirtualizer,_tmpLoudness,_tmpIsBuiltIn,_tmpCategory,_tmpHeadphoneId,_tmpGenre,_tmpReverb,_tmpIsFavorite,_tmpLastUsed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setFavorite(id: Long, fav: Boolean) {
    val _sql: String = "UPDATE presets SET isFavorite = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (fav) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateLastUsed(id: Long, time: Long) {
    val _sql: String = "UPDATE presets SET lastUsed = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, time)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllCustom() {
    val _sql: String = "DELETE FROM presets WHERE isBuiltIn = 0"
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
