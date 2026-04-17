/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
import com.sonara.app.ai.models.TrainingExample
import com.sonara.app.ai.models.TrainingExampleDao

@Database(
    entities = [
        Preset::class,
        TrackCacheEntity::class,
        UserEqPreference::class,
        UserFeedback::class,
        PendingScrobble::class,
        TrainingExample::class
    ],
    version = 5,
    exportSchema = false
)
abstract class SonaraDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun trackCacheDao(): TrackCacheDao
    abstract fun userEqPreferenceDao(): UserEqPreferenceDao
    abstract fun userFeedbackDao(): UserFeedbackDao
    abstract fun pendingScrobbleDao(): PendingScrobbleDao
    abstract fun trainingExampleDao(): TrainingExampleDao

    companion object {
        @Volatile private var INSTANCE: SonaraDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS sonara_training_examples (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    featureVector TEXT NOT NULL,
                    genre TEXT NOT NULL,
                    moodValence REAL NOT NULL,
                    moodArousal REAL NOT NULL,
                    energy REAL NOT NULL,
                    source TEXT NOT NULL,
                    trackTitle TEXT NOT NULL DEFAULT '',
                    trackArtist TEXT NOT NULL DEFAULT '',
                    timestamp INTEGER NOT NULL,
                    useCount INTEGER NOT NULL DEFAULT 0
                )""")
            }
        }

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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }

        /** Alias for DailySyncWorker compatibility */
        fun getInstance(context: Context): SonaraDatabase = get(context)
    }
}
