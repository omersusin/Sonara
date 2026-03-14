package com.sonara.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sonara.app.ui.components.SonaraBottomBar
import com.sonara.app.ui.screens.dashboard.DashboardScreen
import com.sonara.app.ui.screens.equalizer.EqualizerScreen
import com.sonara.app.ui.screens.presets.PresetsScreen
import com.sonara.app.ui.screens.insights.InsightsScreen
import com.sonara.app.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String, val label: String) {
    data object Dashboard : Screen("dashboard", "Home")
    data object Equalizer : Screen("equalizer", "EQ")
    data object Presets : Screen("presets", "Presets")
    data object Insights : Screen("insights", "Insights")
    data object Settings : Screen("settings", "Settings")
}

@Composable
fun SonaraNavigation() {
    val navController = rememberNavController()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { SonaraBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Equalizer.route) { EqualizerScreen() }
            composable(Screen.Presets.route) { PresetsScreen() }
            composable(Screen.Insights.route) { InsightsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
