package com.sonara.app.preset

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object PresetExporter {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    data class ExportablePreset(
        val name: String,
        val bands: List<Float>,
        val preamp: Float,
        val bassBoost: Int,
        val virtualizer: Int,
        val loudness: Int,
        val category: String,
        val genre: String?,
        val headphoneId: String?
    )

    data class ExportBundle(
        val version: Int = 1,
        val app: String = "Sonara",
        val exportedAt: Long = System.currentTimeMillis(),
        val presets: List<ExportablePreset>
    )

    fun exportToJson(presets: List<Preset>): String {
        val exportable = presets.map { preset ->
            ExportablePreset(
                name = preset.name,
                bands = preset.bandsArray().toList(),
                preamp = preset.preamp,
                bassBoost = preset.bassBoost,
                virtualizer = preset.virtualizer,
                loudness = preset.loudness,
                category = preset.category,
                genre = preset.genre,
                headphoneId = preset.headphoneId
            )
        }
        return gson.toJson(ExportBundle(presets = exportable))
    }

    fun exportSingleToJson(preset: Preset): String {
        return exportToJson(listOf(preset))
    }

    fun importFromJson(json: String): List<Preset>? {
        return try {
            val bundle = gson.fromJson(json, ExportBundle::class.java)
            if (bundle.app != "Sonara") return null

            bundle.presets.map { ep ->
                Preset(
                    name = ep.name,
                    bands = Preset.fromArray(ep.bands.toFloatArray()),
                    preamp = ep.preamp,
                    bassBoost = ep.bassBoost,
                    virtualizer = ep.virtualizer,
                    loudness = ep.loudness,
                    category = ep.category,
                    genre = ep.genre,
                    headphoneId = ep.headphoneId,
                    isBuiltIn = false,
                    lastUsed = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    fun validateJson(json: String): Boolean {
        return try {
            val bundle = gson.fromJson(json, ExportBundle::class.java)
            bundle.app == "Sonara" && bundle.presets.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
