package com.sonara.app.preset

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val bands: String = "0,0,0,0,0,0,0,0,0,0",
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val isBuiltIn: Boolean = false,
    val category: String = "custom",
    val headphoneId: String? = null,
    val genre: String? = null,
    val reverb: Int = 0,
    val isFavorite: Boolean = false,
    val lastUsed: Long = 0
) {
    fun bandsArray(): FloatArray = bands.split(",").map { it.trim().replace(",", ".").toFloatOrNull() ?: 0f }.toFloatArray()

    companion object {
        fun fromArray(arr: FloatArray): String = arr.joinToString(",") { String.format(java.util.Locale.US, "%.1f", it) }
    }
}
