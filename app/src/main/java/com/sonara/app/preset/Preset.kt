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

    fun toShareString(): String {
        // Format: SONARA_PRESET|name|bands|preamp|bass|virt|loud|reverb
        return "SONARA_PRESET|${name}|${bands}|${String.format(java.util.Locale.US, "%.1f", preamp)}|${bassBoost}|${virtualizer}|${loudness}|${reverb}"
    }

    companion object {
        fun fromArray(arr: FloatArray): String = arr.joinToString(",") { String.format(java.util.Locale.US, "%.1f", it) }

        fun fromShareString(s: String): Preset? {
            return try {
                val parts = s.split("|")
                if (parts.size < 8 || parts[0] != "SONARA_PRESET") return null
                Preset(
                    name = parts[1],
                    bands = parts[2],
                    preamp = parts[3].toFloat(),
                    bassBoost = parts[4].toInt(),
                    virtualizer = parts[5].toInt(),
                    loudness = parts[6].toInt(),
                    reverb = parts[7].toInt()
                )
            } catch (_: Exception) { null }
        }
    }
}
