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

package com.sonara.app.data.preferences

/**
 * Madde 9 FIX: Theme ayarları artık SonaraPreferences içinde gerçek flow'larla çalışıyor.
 * Bu dosya geriye uyumluluk için korunuyor ama asıl logic SonaraPreferences'ta.
 * Tüm theme preference'ları: themeModeFlow, dynamicColorsFlow, highContrastFlow
 * SonaraPreferences'tan okunur ve Theme.kt'ye geçirilir.
 */
class ThemePreferences(private val prefs: SonaraPreferences) {
    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    /** SonaraPreferences'taki flow'u döndür */
    val themeModeFlow get() = prefs.themeModeFlow
    val dynamicColorsFlow get() = prefs.dynamicColorsFlow
    val highContrastFlow get() = prefs.highContrastFlow

    suspend fun setThemeMode(mode: String) = prefs.setThemeMode(mode)
    suspend fun setDynamicColors(enabled: Boolean) = prefs.setDynamicColors(enabled)
    suspend fun setHighContrast(enabled: Boolean) = prefs.setHighContrast(enabled)
}
