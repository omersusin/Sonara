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
