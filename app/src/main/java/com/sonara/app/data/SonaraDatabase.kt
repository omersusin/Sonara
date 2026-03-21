package com.sonara.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sonara.app.data.dao.UserEqPreferenceDao
import com.sonara.app.data.dao.UserFeedbackDao
import com.sonara.app.data.models.UserEqPreference
import com.sonara.app.data.models.UserFeedback
import com.sonara.app.intelligence.cache.TrackCacheDao
import com.sonara.app.intelligence.cache.TrackCacheEntity
import com.sonara.app.preset.Preset
import com.sonara.app.preset.PresetDao
import com.sonara.app.intelligence.lastfm.PendingScrobble
import com.sonara.app.intelligence.lastfm.PendingScrobbleDao

@Database(entities = [Preset::class, TrackCacheEntity::class, UserEqPreference::class, UserFeedback::class, PendingScrobble::class], version = 4, exportSchema = false)
abstract class SonaraDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun trackCacheDao(): TrackCacheDao
    abstract fun userEqPreferenceDao(): UserEqPreferenceDao
    abstract fun userFeedbackDao(): UserFeedbackDao
    abstract fun pendingScrobbleDao(): PendingScrobbleDao

    companion object {
        @Volatile private var INSTANCE: SonaraDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS pending_scrobbles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, track TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL, timestamp INTEGER NOT NULL, createdAt INTEGER NOT NULL, retryCount INTEGER NOT NULL DEFAULT 0)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS user_eq_preferences (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, genre TEXT NOT NULL, mood TEXT, energy REAL NOT NULL DEFAULT 0.5, audioRoute TEXT NOT NULL, bandLevels TEXT NOT NULL, usageCount INTEGER NOT NULL DEFAULT 1, lastUsed INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_eq_preferences_genre_audioRoute ON user_eq_preferences (genre, audioRoute)")
                db.execSQL("CREATE TABLE IF NOT EXISTS user_feedback (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, trackTitle TEXT, trackArtist TEXT, suggestedGenre TEXT NOT NULL, correctedGenre TEXT, suggestedBands TEXT, correctedBands TEXT, accepted INTEGER NOT NULL DEFAULT 1, audioRoute TEXT NOT NULL, confidence REAL NOT NULL DEFAULT 0, timestamp INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_feedback_suggestedGenre ON user_feedback (suggestedGenre)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_feedback_timestamp ON user_feedback (timestamp)")
            }
        }

        fun get(context: Context): SonaraDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, SonaraDatabase::class.java, "sonara.db")
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
