package com.sonara.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sonara.app.SonaraApp
import com.sonara.app.ui.components.SonaraBottomBar
import com.sonara.app.ui.screens.dashboard.DashboardScreen
import com.sonara.app.ui.screens.debug.DebugLogScreen
import com.sonara.app.ui.screens.debug.DebugPipelineScreen
import com.sonara.app.ui.screens.equalizer.EqualizerScreen
import com.sonara.app.ui.screens.insights.InsightsScreen
import com.sonara.app.ui.screens.onboarding.OnboardingScreen
import com.sonara.app.ui.screens.presets.PresetsScreen
import com.sonara.app.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String) {
    data object Onboarding : Screen("onboarding", "Welcome")
    data object Dashboard : Screen("dashboard", "Home")
    data object Equalizer : Screen("equalizer", "EQ")
    data object Presets : Screen("presets", "Presets")
    data object Insights : Screen("insights", "Insights")
    data object Settings : Screen("settings", "Settings")
    data object DebugLog : Screen("debug_log", "Debug")
    data object DebugPipeline : Screen("debug_pipeline", "Pipeline Debug")
}

@Composable
fun SonaraNavigation() {
    val navController = rememberNavController()
    val prefs = SonaraApp.instance.preferences
    val prompted by prefs.notificationListenerPromptedFlow.collectAsState(initial = true)
    val startDest = if (!prompted) Screen.Onboarding.route else Screen.Dashboard.route
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val hideBottomBar = currentRoute == Screen.Onboarding.route || currentRoute == Screen.DebugLog.route || currentRoute == Screen.DebugPipeline.route

    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { if (!hideBottomBar) SonaraBottomBar(navController) }) { padding ->
        NavHost(navController, startDestination = startDest, Modifier.padding(padding)) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onComplete = {
                    MainScope().launch { prefs.setNotificationListenerPrompted(true) }
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                })
            }
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Equalizer.route) { EqualizerScreen() }
            composable(Screen.Presets.route) { PresetsScreen() }
            composable(Screen.Insights.route) { InsightsScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenDebugLog = { navController.navigate(Screen.DebugLog.route) },
                    onOpenPipelineDebug = { navController.navigate(Screen.DebugPipeline.route) }
                )
            }
            composable(Screen.DebugLog.route) { DebugLogScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.DebugPipeline.route) { DebugPipelineScreen() }
        }
    }
}
