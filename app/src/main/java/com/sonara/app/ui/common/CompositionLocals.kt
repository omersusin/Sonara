package com.sonara.app.ui.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.ui.data.provider.AppSeedColors
import com.sonara.app.ui.data.provider.SeedColor
import com.sonara.app.ui.domain.provider.SeedColorProvider
import com.sonara.app.ui.screens.settings.SettingsViewModel

val LocalDarkMode = staticCompositionLocalOf<Boolean> {
    error("No dark mode pref provided")
}

val LocalHighContrastDarkMode = staticCompositionLocalOf<Boolean> {
    error("No high contrast dark mode pref provided")
}

val LocalSeedColor = staticCompositionLocalOf<SeedColor> {
    error("No seed color provided")
}
val LocalTonalPalette = staticCompositionLocalOf<List<AppSeedColors>> {
    error("No tonal palette provided")
}
val LocalDynamicColor = staticCompositionLocalOf<Boolean> {
    error("No dynamic color pref provided")
}

@Composable
fun CompositionLocals(content: @Composable () -> Unit) {
    val settingsViewModel: SettingsViewModel = viewModel()

    val themeMode by settingsViewModel.themeMode.collectAsState(initial = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    val isDarkTheme = when (themeMode) {
        AppCompatDelegate.MODE_NIGHT_YES -> true
        AppCompatDelegate.MODE_NIGHT_NO -> false
        else -> isSystemInDarkTheme()
    }

    val highContrastDarkMode by settingsViewModel.highContrastDarkMode.collectAsState(
        initial = false
    )

    val dynamicColorEnabled by settingsViewModel.dynamicColorEnabled.collectAsState(initial = true)

    val primarySeed by settingsViewModel.primarySeed.collectAsState(initial = SeedColorProvider.primary)
    val secondarySeed by settingsViewModel.secondarySeed.collectAsState(initial = SeedColorProvider.secondary)
    val tertiarySeed by settingsViewModel.tertiarySeed.collectAsState(initial = SeedColorProvider.tertiary)

    val seedColor = SeedColor(primarySeed, secondarySeed, tertiarySeed)

    val tonalPalette = listOf(
        AppSeedColors.Color01,
        AppSeedColors.Color02,
        AppSeedColors.Color03,
        AppSeedColors.Color04,
        AppSeedColors.Color05,
        AppSeedColors.Color06,
        AppSeedColors.Color07,
        AppSeedColors.Color08,
        AppSeedColors.Color09,
        AppSeedColors.Color10,
        AppSeedColors.Color11,
        AppSeedColors.Color12,
        AppSeedColors.Color13,
        AppSeedColors.Color14,
        AppSeedColors.Color15,
        AppSeedColors.Color16,
        AppSeedColors.Color17,
        AppSeedColors.Color18,
        AppSeedColors.Color19,
        AppSeedColors.Color20
    )

    CompositionLocalProvider(
        LocalDarkMode provides isDarkTheme,
        LocalHighContrastDarkMode provides highContrastDarkMode,
        LocalSeedColor provides seedColor,
        LocalTonalPalette provides tonalPalette,
        LocalDynamicColor provides dynamicColorEnabled
    ) {
        content()
    }
}