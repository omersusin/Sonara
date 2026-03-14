package com.sonara.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sonara.app.preset.Preset
import com.sonara.app.preset.PresetDao

@Database(entities = [Preset::class], version = 1, exportSchema = false)
abstract class SonaraDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile private var INSTANCE: SonaraDatabase? = null

        fun get(context: Context): SonaraDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, SonaraDatabase::class.java, "sonara.db")
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
