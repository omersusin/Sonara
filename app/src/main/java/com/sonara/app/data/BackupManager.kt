package com.sonara.app.data

import android.content.Context
import android.os.Environment
import com.google.gson.GsonBuilder
import com.sonara.app.SonaraApp
import com.sonara.app.preset.Preset
import kotlinx.coroutines.flow.first
import java.io.File

object BackupManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    data class FullBackup(
        val version: Int = 2,
        val app: String = "Sonara",
        val exportedAt: Long = System.currentTimeMillis(),
        val settings: Map<String, Any?> = emptyMap(),
        val presets: List<PresetBackup> = emptyList(),
        val genreStats: String = "",
        val songsLearned: Int = 0,
        val songsViaLastFm: Int = 0,
        val songsViaLocal: Int = 0
    )

    data class PresetBackup(
        val name: String, val bands: String, val preamp: Float,
        val bassBoost: Int, val virtualizer: Int, val loudness: Int,
        val category: String, val genre: String?, val headphoneId: String?,
        val isFavorite: Boolean
    )

    suspend fun exportFull(app: SonaraApp): String {
        val prefs = app.preferences
        val settings = mutableMapOf<String, Any?>()

        // Collect all preferences
        settings["ai_enabled"] = prefs.aiEnabledFlow.first()
        settings["auto_eq_enabled"] = prefs.autoEqEnabledFlow.first()
        settings["smooth_transitions"] = prefs.smoothTransitionsFlow.first()
        settings["safety_limiter"] = prefs.safetyLimiterFlow.first()
        settings["scrobbling_enabled"] = prefs.scrobblingEnabledFlow.first()
        settings["auto_preset"] = prefs.autoPresetFlow.first()
        settings["accent_color"] = prefs.accentColorFlow.first().name
        settings["theme_mode"] = prefs.themeModeFlow.first()
        settings["dynamic_colors"] = prefs.dynamicColorsFlow.first()
        settings["high_contrast"] = prefs.highContrastFlow.first()
        settings["amoled_mode"] = prefs.amoledModeFlow.first()
        settings["keep_notification_paused"] = prefs.keepNotificationPausedFlow.first()
        settings["ai_provider"] = prefs.aiProviderFlow.first()
        settings["gemini_enabled"] = prefs.geminiEnabledFlow.first()
        settings["gemini_model"] = prefs.geminiModelFlow.first()
        settings["openrouter_model"] = prefs.openRouterModelFlow.first()
        settings["groq_model"] = prefs.groqModelFlow.first()
        settings["source_lastfm"] = prefs.sourceLastFmEnabledFlow.first()
        settings["source_local_ai"] = prefs.sourceLocalAiEnabledFlow.first()
        settings["source_lyrics"] = prefs.sourceLyricsEnabledFlow.first()
        settings["community_sync_interval"] = prefs.communitySyncIntervalFlow.first()

        // All presets (including custom)
        val allPresets = app.presetRepository.allPresets().first()
        val presetBackups = allPresets.filter { !it.isBuiltIn }.map { p ->
            PresetBackup(p.name, p.bands, p.preamp, p.bassBoost, p.virtualizer,
                p.loudness, p.category, p.genre, p.headphoneId, p.isFavorite)
        }

        val backup = FullBackup(
            settings = settings,
            presets = presetBackups,
            genreStats = prefs.genreStatsFlow.first(),
            songsLearned = prefs.songsLearnedFlow.first(),
            songsViaLastFm = prefs.songsViaLastFmFlow.first(),
            songsViaLocal = prefs.songsViaLocalFlow.first()
        )
        return gson.toJson(backup)
    }

    suspend fun importFull(app: SonaraApp, json: String): String {
        return try {
            val backup = gson.fromJson(json, FullBackup::class.java)
            if (backup.app != "Sonara") return "Not a Sonara backup file"
            val prefs = app.preferences
            val s = backup.settings

            // Restore settings
            (s["ai_enabled"] as? Boolean)?.let { prefs.setAiEnabled(it) }
            (s["auto_eq_enabled"] as? Boolean)?.let { prefs.setAutoEqEnabled(it) }
            (s["smooth_transitions"] as? Boolean)?.let { prefs.setSmoothTransitions(it) }
            (s["safety_limiter"] as? Boolean)?.let { prefs.setSafetyLimiter(it) }
            (s["scrobbling_enabled"] as? Boolean)?.let { prefs.setScrobblingEnabled(it) }
            (s["auto_preset"] as? Boolean)?.let { prefs.setAutoPreset(it) }
            (s["theme_mode"] as? String)?.let { prefs.setThemeMode(it) }
            (s["amoled_mode"] as? Boolean)?.let { prefs.setAmoledMode(it) }
            (s["ai_provider"] as? String)?.let { prefs.setAiProvider(it) }
            (s["gemini_enabled"] as? Boolean)?.let { prefs.setGeminiEnabled(it) }
            (s["gemini_model"] as? String)?.let { prefs.setGeminiModel(it) }
            (s["source_lastfm"] as? Boolean)?.let { prefs.setSourceLastFmEnabled(it) }
            (s["source_local_ai"] as? Boolean)?.let { prefs.setSourceLocalAiEnabled(it) }
            (s["source_lyrics"] as? Boolean)?.let { prefs.setSourceLyricsEnabled(it) }
            (s["keep_notification_paused"] as? Boolean)?.let { prefs.setKeepNotificationPaused(it) }

            // Restore presets
            var presetCount = 0
            backup.presets.forEach { pb ->
                app.presetRepository.save(Preset(
                    name = pb.name, bands = pb.bands, preamp = pb.preamp,
                    bassBoost = pb.bassBoost, virtualizer = pb.virtualizer,
                    loudness = pb.loudness, category = pb.category,
                    genre = pb.genre, headphoneId = pb.headphoneId,
                    isFavorite = pb.isFavorite, isBuiltIn = false,
                    lastUsed = System.currentTimeMillis()
                ))
                presetCount++
            }

            "Restored ${backup.settings.size} settings, $presetCount presets"
        } catch (e: Exception) {
            "Import failed: ${e.message}"
        }
    }

    fun saveToFile(json: String): File? {
        return try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, "sonara_full_backup_${System.currentTimeMillis() / 1000}.json")
            file.writeText(json)
            file
        } catch (_: Exception) { null }
    }
}
