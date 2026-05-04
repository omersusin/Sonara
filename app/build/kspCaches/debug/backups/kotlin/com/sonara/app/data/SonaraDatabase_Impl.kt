package com.sonara.app.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.sonara.app.`data`.dao.UserEqPreferenceDao
import com.sonara.app.`data`.dao.UserEqPreferenceDao_Impl
import com.sonara.app.`data`.dao.UserFeedbackDao
import com.sonara.app.`data`.dao.UserFeedbackDao_Impl
import com.sonara.app.ai.models.TrainingExampleDao
import com.sonara.app.ai.models.TrainingExampleDao_Impl
import com.sonara.app.intelligence.cache.TrackCacheDao
import com.sonara.app.intelligence.cache.TrackCacheDao_Impl
import com.sonara.app.intelligence.lastfm.PendingScrobbleDao
import com.sonara.app.intelligence.lastfm.PendingScrobbleDao_Impl
import com.sonara.app.intelligence.lyrics.LyricsCacheDao
import com.sonara.app.intelligence.lyrics.LyricsCacheDao_Impl
import com.sonara.app.preset.PresetDao
import com.sonara.app.preset.PresetDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SonaraDatabase_Impl : SonaraDatabase() {
  private val _presetDao: Lazy<PresetDao> = lazy {
    PresetDao_Impl(this)
  }

  private val _trackCacheDao: Lazy<TrackCacheDao> = lazy {
    TrackCacheDao_Impl(this)
  }

  private val _userEqPreferenceDao: Lazy<UserEqPreferenceDao> = lazy {
    UserEqPreferenceDao_Impl(this)
  }

  private val _userFeedbackDao: Lazy<UserFeedbackDao> = lazy {
    UserFeedbackDao_Impl(this)
  }

  private val _pendingScrobbleDao: Lazy<PendingScrobbleDao> = lazy {
    PendingScrobbleDao_Impl(this)
  }

  private val _trainingExampleDao: Lazy<TrainingExampleDao> = lazy {
    TrainingExampleDao_Impl(this)
  }

  private val _lyricsCacheDao: Lazy<LyricsCacheDao> = lazy {
    LyricsCacheDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(8,
        "cfa274b9f85f0d86f4c7ffc9cb0d9d50", "f628288594f9e8e5eaa44abd7c5d3390") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `presets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `bands` TEXT NOT NULL, `preamp` REAL NOT NULL, `bassBoost` INTEGER NOT NULL, `virtualizer` INTEGER NOT NULL, `loudness` INTEGER NOT NULL, `isBuiltIn` INTEGER NOT NULL, `category` TEXT NOT NULL, `headphoneId` TEXT, `genre` TEXT, `reverb` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `lastUsed` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `track_cache` (`cacheKey` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `genre` TEXT NOT NULL, `mood` TEXT NOT NULL, `energy` REAL NOT NULL, `confidence` REAL NOT NULL, `source` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`cacheKey`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `user_eq_preferences` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `genre` TEXT NOT NULL, `mood` TEXT, `energy` REAL NOT NULL, `audioRoute` TEXT NOT NULL, `bandLevels` TEXT NOT NULL, `usageCount` INTEGER NOT NULL, `lastUsed` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_user_eq_preferences_genre_audioRoute` ON `user_eq_preferences` (`genre`, `audioRoute`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `user_feedback` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trackTitle` TEXT, `trackArtist` TEXT, `suggestedGenre` TEXT NOT NULL, `correctedGenre` TEXT, `suggestedBands` TEXT, `correctedBands` TEXT, `accepted` INTEGER NOT NULL, `audioRoute` TEXT NOT NULL, `confidence` REAL NOT NULL, `timestamp` INTEGER NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_user_feedback_suggestedGenre` ON `user_feedback` (`suggestedGenre`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_user_feedback_timestamp` ON `user_feedback` (`timestamp`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `pending_scrobbles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `track` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `retryCount` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sonara_training_examples` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `featureVector` TEXT NOT NULL, `genre` TEXT NOT NULL, `moodValence` REAL NOT NULL, `moodArousal` REAL NOT NULL, `energy` REAL NOT NULL, `source` TEXT NOT NULL, `trackTitle` TEXT NOT NULL, `trackArtist` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `useCount` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `lyrics_cache` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cacheKey` TEXT NOT NULL, `syncedLyrics` TEXT, `plainLyrics` TEXT, `source` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, `translatedLyrics` TEXT NOT NULL, `translationLanguage` TEXT NOT NULL, `translationMode` TEXT NOT NULL)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_lyrics_cache_cacheKey` ON `lyrics_cache` (`cacheKey`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'cfa274b9f85f0d86f4c7ffc9cb0d9d50')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `presets`")
        connection.execSQL("DROP TABLE IF EXISTS `track_cache`")
        connection.execSQL("DROP TABLE IF EXISTS `user_eq_preferences`")
        connection.execSQL("DROP TABLE IF EXISTS `user_feedback`")
        connection.execSQL("DROP TABLE IF EXISTS `pending_scrobbles`")
        connection.execSQL("DROP TABLE IF EXISTS `sonara_training_examples`")
        connection.execSQL("DROP TABLE IF EXISTS `lyrics_cache`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsPresets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPresets.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("bands", TableInfo.Column("bands", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("preamp", TableInfo.Column("preamp", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("bassBoost", TableInfo.Column("bassBoost", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("virtualizer", TableInfo.Column("virtualizer", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("loudness", TableInfo.Column("loudness", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("isBuiltIn", TableInfo.Column("isBuiltIn", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("headphoneId", TableInfo.Column("headphoneId", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("genre", TableInfo.Column("genre", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("reverb", TableInfo.Column("reverb", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("isFavorite", TableInfo.Column("isFavorite", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPresets.put("lastUsed", TableInfo.Column("lastUsed", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPresets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPresets: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPresets: TableInfo = TableInfo("presets", _columnsPresets, _foreignKeysPresets,
            _indicesPresets)
        val _existingPresets: TableInfo = read(connection, "presets")
        if (!_infoPresets.equals(_existingPresets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |presets(com.sonara.app.preset.Preset).
              | Expected:
              |""".trimMargin() + _infoPresets + """
              |
              | Found:
              |""".trimMargin() + _existingPresets)
        }
        val _columnsTrackCache: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTrackCache.put("cacheKey", TableInfo.Column("cacheKey", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrackCache.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrackCache.put("artist", TableInfo.Column("artist", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrackCache.put("album", TableInfo.Column("album", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrackCache.put("genre", TableInfo.Column("genre", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrackCache.put("mood", TableInfo.Column("mood", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrackCache.put("energy", TableInfo.Column("energy", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrackCache.put("confidence", TableInfo.Column("confidence", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrackCache.put("source", TableInfo.Column("source", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTrackCache.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTrackCache: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTrackCache: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTrackCache: TableInfo = TableInfo("track_cache", _columnsTrackCache,
            _foreignKeysTrackCache, _indicesTrackCache)
        val _existingTrackCache: TableInfo = read(connection, "track_cache")
        if (!_infoTrackCache.equals(_existingTrackCache)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |track_cache(com.sonara.app.intelligence.cache.TrackCacheEntity).
              | Expected:
              |""".trimMargin() + _infoTrackCache + """
              |
              | Found:
              |""".trimMargin() + _existingTrackCache)
        }
        val _columnsUserEqPreferences: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsUserEqPreferences.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUserEqPreferences.put("genre", TableInfo.Column("genre", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUserEqPreferences.put("mood", TableInfo.Column("mood", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUserEqPreferences.put("energy", TableInfo.Column("energy", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUserEqPreferences.put("audioRoute", TableInfo.Column("audioRoute", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUserEqPreferences.put("bandLevels", TableInfo.Column("bandLevels", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUserEqPreferences.put("usageCount", TableInfo.Column("usageCount", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUserEqPreferences.put("lastUsed", TableInfo.Column("lastUsed", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUserEqPreferences.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysUserEqPreferences: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesUserEqPreferences: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesUserEqPreferences.add(TableInfo.Index("index_user_eq_preferences_genre_audioRoute",
            false, listOf("genre", "audioRoute"), listOf("ASC", "ASC")))
        val _infoUserEqPreferences: TableInfo = TableInfo("user_eq_preferences",
            _columnsUserEqPreferences, _foreignKeysUserEqPreferences, _indicesUserEqPreferences)
        val _existingUserEqPreferences: TableInfo = read(connection, "user_eq_preferences")
        if (!_infoUserEqPreferences.equals(_existingUserEqPreferences)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |user_eq_preferences(com.sonara.app.data.models.UserEqPreference).
              | Expected:
              |""".trimMargin() + _infoUserEqPreferences + """
              |
              | Found:
              |""".trimMargin() + _existingUserEqPreferences)
        }
        val _columnsUserFeedback: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsUserFeedback.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUserFeedback.put("trackTitle", TableInfo.Column("trackTitle", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUserFeedback.put("trackArtist", TableInfo.Column("trackArtist", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUserFeedback.put("suggestedGenre", TableInfo.Column("suggestedGenre", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUserFeedback.put("correctedGenre", TableInfo.Column("correctedGenre", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUserFeedback.put("suggestedBands", TableInfo.Column("suggestedBands", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUserFeedback.put("correctedBands", TableInfo.Column("correctedBands", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUserFeedback.put("accepted", TableInfo.Column("accepted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUserFeedback.put("audioRoute", TableInfo.Column("audioRoute", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUserFeedback.put("confidence", TableInfo.Column("confidence", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUserFeedback.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysUserFeedback: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesUserFeedback: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesUserFeedback.add(TableInfo.Index("index_user_feedback_suggestedGenre", false,
            listOf("suggestedGenre"), listOf("ASC")))
        _indicesUserFeedback.add(TableInfo.Index("index_user_feedback_timestamp", false,
            listOf("timestamp"), listOf("ASC")))
        val _infoUserFeedback: TableInfo = TableInfo("user_feedback", _columnsUserFeedback,
            _foreignKeysUserFeedback, _indicesUserFeedback)
        val _existingUserFeedback: TableInfo = read(connection, "user_feedback")
        if (!_infoUserFeedback.equals(_existingUserFeedback)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |user_feedback(com.sonara.app.data.models.UserFeedback).
              | Expected:
              |""".trimMargin() + _infoUserFeedback + """
              |
              | Found:
              |""".trimMargin() + _existingUserFeedback)
        }
        val _columnsPendingScrobbles: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPendingScrobbles.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPendingScrobbles.put("track", TableInfo.Column("track", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPendingScrobbles.put("artist", TableInfo.Column("artist", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPendingScrobbles.put("album", TableInfo.Column("album", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPendingScrobbles.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPendingScrobbles.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPendingScrobbles.put("retryCount", TableInfo.Column("retryCount", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPendingScrobbles: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPendingScrobbles: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPendingScrobbles: TableInfo = TableInfo("pending_scrobbles",
            _columnsPendingScrobbles, _foreignKeysPendingScrobbles, _indicesPendingScrobbles)
        val _existingPendingScrobbles: TableInfo = read(connection, "pending_scrobbles")
        if (!_infoPendingScrobbles.equals(_existingPendingScrobbles)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |pending_scrobbles(com.sonara.app.intelligence.lastfm.PendingScrobble).
              | Expected:
              |""".trimMargin() + _infoPendingScrobbles + """
              |
              | Found:
              |""".trimMargin() + _existingPendingScrobbles)
        }
        val _columnsSonaraTrainingExamples: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSonaraTrainingExamples.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSonaraTrainingExamples.put("featureVector", TableInfo.Column("featureVector",
            "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSonaraTrainingExamples.put("genre", TableInfo.Column("genre", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSonaraTrainingExamples.put("moodValence", TableInfo.Column("moodValence", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSonaraTrainingExamples.put("moodArousal", TableInfo.Column("moodArousal", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSonaraTrainingExamples.put("energy", TableInfo.Column("energy", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSonaraTrainingExamples.put("source", TableInfo.Column("source", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSonaraTrainingExamples.put("trackTitle", TableInfo.Column("trackTitle", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSonaraTrainingExamples.put("trackArtist", TableInfo.Column("trackArtist", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSonaraTrainingExamples.put("timestamp", TableInfo.Column("timestamp", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSonaraTrainingExamples.put("useCount", TableInfo.Column("useCount", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSonaraTrainingExamples: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSonaraTrainingExamples: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSonaraTrainingExamples: TableInfo = TableInfo("sonara_training_examples",
            _columnsSonaraTrainingExamples, _foreignKeysSonaraTrainingExamples,
            _indicesSonaraTrainingExamples)
        val _existingSonaraTrainingExamples: TableInfo = read(connection,
            "sonara_training_examples")
        if (!_infoSonaraTrainingExamples.equals(_existingSonaraTrainingExamples)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |sonara_training_examples(com.sonara.app.ai.models.TrainingExample).
              | Expected:
              |""".trimMargin() + _infoSonaraTrainingExamples + """
              |
              | Found:
              |""".trimMargin() + _existingSonaraTrainingExamples)
        }
        val _columnsLyricsCache: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLyricsCache.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLyricsCache.put("cacheKey", TableInfo.Column("cacheKey", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLyricsCache.put("syncedLyrics", TableInfo.Column("syncedLyrics", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLyricsCache.put("plainLyrics", TableInfo.Column("plainLyrics", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLyricsCache.put("source", TableInfo.Column("source", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLyricsCache.put("cachedAt", TableInfo.Column("cachedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLyricsCache.put("translatedLyrics", TableInfo.Column("translatedLyrics", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLyricsCache.put("translationLanguage", TableInfo.Column("translationLanguage",
            "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLyricsCache.put("translationMode", TableInfo.Column("translationMode", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLyricsCache: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesLyricsCache: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesLyricsCache.add(TableInfo.Index("index_lyrics_cache_cacheKey", true,
            listOf("cacheKey"), listOf("ASC")))
        val _infoLyricsCache: TableInfo = TableInfo("lyrics_cache", _columnsLyricsCache,
            _foreignKeysLyricsCache, _indicesLyricsCache)
        val _existingLyricsCache: TableInfo = read(connection, "lyrics_cache")
        if (!_infoLyricsCache.equals(_existingLyricsCache)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |lyrics_cache(com.sonara.app.intelligence.lyrics.LyricsCacheEntity).
              | Expected:
              |""".trimMargin() + _infoLyricsCache + """
              |
              | Found:
              |""".trimMargin() + _existingLyricsCache)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "presets", "track_cache",
        "user_eq_preferences", "user_feedback", "pending_scrobbles", "sonara_training_examples",
        "lyrics_cache")
  }

  public override fun clearAllTables() {
    super.performClear(false, "presets", "track_cache", "user_eq_preferences", "user_feedback",
        "pending_scrobbles", "sonara_training_examples", "lyrics_cache")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(PresetDao::class, PresetDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TrackCacheDao::class, TrackCacheDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(UserEqPreferenceDao::class,
        UserEqPreferenceDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(UserFeedbackDao::class, UserFeedbackDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(PendingScrobbleDao::class,
        PendingScrobbleDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TrainingExampleDao::class,
        TrainingExampleDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(LyricsCacheDao::class, LyricsCacheDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun presetDao(): PresetDao = _presetDao.value

  public override fun trackCacheDao(): TrackCacheDao = _trackCacheDao.value

  public override fun userEqPreferenceDao(): UserEqPreferenceDao = _userEqPreferenceDao.value

  public override fun userFeedbackDao(): UserFeedbackDao = _userFeedbackDao.value

  public override fun pendingScrobbleDao(): PendingScrobbleDao = _pendingScrobbleDao.value

  public override fun trainingExampleDao(): TrainingExampleDao = _trainingExampleDao.value

  public override fun lyricsCacheDao(): LyricsCacheDao = _lyricsCacheDao.value
}
