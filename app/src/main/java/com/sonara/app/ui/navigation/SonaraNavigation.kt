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
import com.sonara.app.ui.screens.insights.ArtistDetailScreen
import com.sonara.app.ui.screens.insights.TrackDetailScreen
import com.sonara.app.ui.screens.insights.InsightsScreen
import com.sonara.app.ui.screens.insights.TopArtistsListScreen
import com.sonara.app.ui.screens.insights.TopTracksListScreen
import com.sonara.app.ui.screens.insights.TopAlbumsListScreen
import com.sonara.app.ui.screens.settings.AppPickerScreen
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
    data object AppPicker : Screen("app_picker", "Choose Apps")
    data object TopArtistsList : Screen("top_artists_list", "Top Artists")
    data object TopTracksList : Screen("top_tracks_list", "Top Tracks")
    data object TopAlbumsList : Screen("top_albums_list", "Top Albums")
    data object ArtistDetail : Screen("artist_detail/{name}", "Artist") {
        fun createRoute(name: String) = "artist_detail/${'$'}{java.net.URLEncoder.encode(name, "UTF-8")}"
    }
    data object TrackDetail : Screen("track_detail/{title}/{artist}", "Track") {
        fun createRoute(title: String, artist: String) = "track_detail/${'$'}{java.net.URLEncoder.encode(title, "UTF-8")}/${'$'}{java.net.URLEncoder.encode(artist, "UTF-8")}"
    }
}

@Composable
fun SonaraNavigation() {
    val navController = rememberNavController()
    val prefs = SonaraApp.instance.preferences
    val prompted by prefs.notificationListenerPromptedFlow.collectAsState(initial = true)
    val startDest = if (!prompted) Screen.Onboarding.route else Screen.Dashboard.route
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val mainTabs = setOf(Screen.Dashboard.route, Screen.Equalizer.route, Screen.Presets.route, Screen.Insights.route, Screen.Settings.route)
    val hideBottomBar = currentRoute !in mainTabs

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
            composable(Screen.Insights.route) {
                InsightsScreen(
                    onArtistClick = { name -> navController.navigate(Screen.ArtistDetail.createRoute(name)) },
                    onTrackClick = { title, artist -> navController.navigate(Screen.TrackDetail.createRoute(title, artist)) },
                    onSeeAllArtists = { navController.navigate(Screen.TopArtistsList.route) },
                    onSeeAllTracks = { navController.navigate(Screen.TopTracksList.route) },
                    onSeeAllAlbums = { navController.navigate(Screen.TopAlbumsList.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenDebugLog = { navController.navigate(Screen.DebugLog.route) },
                    onOpenPipelineDebug = { navController.navigate(Screen.DebugPipeline.route) },
                    onOpenAppPicker = { navController.navigate(Screen.AppPicker.route) }
                )
            }
            composable(Screen.AppPicker.route) { AppPickerScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.TopArtistsList.route) {
                TopArtistsListScreen(
                    onBack = { navController.popBackStack() },
                    onArtistClick = { name -> navController.navigate(Screen.ArtistDetail.createRoute(name)) }
                )
            }
            composable(Screen.TopTracksList.route) {
                TopTracksListScreen(
                    onBack = { navController.popBackStack() },
                    onTrackClick = { title, artist -> navController.navigate(Screen.TrackDetail.createRoute(title, artist)) }
                )
            }
            composable(Screen.TopAlbumsList.route) {
                TopAlbumsListScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.ArtistDetail.route) { entry ->
                val name = java.net.URLDecoder.decode(entry.arguments?.getString("name") ?: "", "UTF-8")
                ArtistDetailScreen(artistName = name, onBack = { navController.popBackStack() },
                    onTrackClick = { title, artist -> navController.navigate(Screen.TrackDetail.createRoute(title, artist)) })
            }
            composable(Screen.TrackDetail.route) { entry ->
                val title = java.net.URLDecoder.decode(entry.arguments?.getString("title") ?: "", "UTF-8")
                val artist = java.net.URLDecoder.decode(entry.arguments?.getString("artist") ?: "", "UTF-8")
                TrackDetailScreen(trackTitle = title, trackArtist = artist, onBack = { navController.popBackStack() },
                    onArtistClick = { name -> navController.navigate(Screen.ArtistDetail.createRoute(name)) })
            }
            composable(Screen.DebugLog.route) { DebugLogScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.DebugPipeline.route) { DebugPipelineScreen() }
        }
    }
}
