package com.sonara.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sonara.app.SonaraApp
import com.sonara.app.ui.components.SonaraBottomBar
import com.sonara.app.ui.screens.dashboard.DashboardScreen
import com.sonara.app.ui.screens.debug.DebugLogScreen
import com.sonara.app.ui.screens.debug.DebugPipelineScreen
import com.sonara.app.ui.screens.equalizer.EqualizerScreen
import com.sonara.app.ui.screens.insights.AlbumDetailScreen
import com.sonara.app.ui.screens.insights.AllGenresScreen
import com.sonara.app.ui.screens.insights.ArtistDetailScreen
import com.sonara.app.ui.screens.insights.ArtistDiscographyScreen
import com.sonara.app.ui.screens.insights.ListeningActivityScreen
import com.sonara.app.ui.screens.insights.SimilarArtistsScreen
import com.sonara.app.ui.screens.insights.RecentTracksScreen
import com.sonara.app.ui.screens.insights.TrackDetailScreen
import com.sonara.app.ui.screens.insights.InsightsScreen
import com.sonara.app.ui.screens.insights.TopArtistsListScreen
import com.sonara.app.ui.screens.insights.TopTracksListScreen
import com.sonara.app.ui.screens.insights.TopAlbumsListScreen
import com.sonara.app.ui.screens.settings.AboutSettingsScreen
import com.sonara.app.ui.screens.settings.AppPickerScreen
import com.sonara.app.ui.screens.settings.BackupSettingsScreen
import com.sonara.app.ui.screens.settings.BehaviorSettingsScreen
import com.sonara.app.ui.screens.settings.LookAndFeelSettingsScreen
import com.sonara.app.ui.screens.settings.LyricsSettingsScreen
import com.sonara.app.ui.screens.settings.NotificationsSettingsScreen
import com.sonara.app.ui.screens.onboarding.HearTheDifferenceScreen
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
    data object SettingsLookAndFeel : Screen("settings_look_feel", "Look & Feel")
    data object SettingsBehavior : Screen("settings_behavior", "Audio & AI")
    data object SettingsNotifications : Screen("settings_notifications", "Notifications")
    data object SettingsBackup : Screen("settings_backup", "Backup & Restore")
    data object SettingsAbout : Screen("settings_about", "About")
    data object SettingsLyrics : Screen("settings_lyrics", "Lyrics")
    data object TopArtistsList : Screen("top_artists_list", "Top Artists")
    data object TopTracksList : Screen("top_tracks_list", "Top Tracks")
    data object TopAlbumsList : Screen("top_albums_list", "Top Albums")
    data object RecentTracks : Screen("recent_tracks", "Recently Played")
    data object AllGenres : Screen("all_genres", "Your Genres")
    data object ListeningActivity : Screen("listening_activity", "Listening Activity")
    data object AlbumDetail : Screen("album_detail/{name}/{artist}/{plays}/{imageUrl}", "Album") {
        fun createRoute(name: String, artist: String, plays: String, imageUrl: String) =
            "album_detail/${java.net.URLEncoder.encode(name, "UTF-8")}/${java.net.URLEncoder.encode(artist, "UTF-8")}/${java.net.URLEncoder.encode(plays.ifBlank { "-" }, "UTF-8")}/${java.net.URLEncoder.encode(imageUrl.ifBlank { "-" }, "UTF-8")}"
    }
    data object ArtistDetail : Screen("artist_detail/{name}", "Artist") {
        fun createRoute(name: String) =
            "artist_detail/${java.net.URLEncoder.encode(name, "UTF-8")}"
        fun createRouteWithTrack(name: String, trackTitle: String) =
            "artist_detail/${java.net.URLEncoder.encode(name, "UTF-8")}" +
                "?trackTitle=${java.net.URLEncoder.encode(trackTitle, "UTF-8")}"
    }
    data object TrackDetail : Screen("track_detail/{title}/{artist}", "Track") {
        fun createRoute(title: String, artist: String) = "track_detail/${java.net.URLEncoder.encode(title, "UTF-8")}/${java.net.URLEncoder.encode(artist, "UTF-8")}"
    }
    data object ArtistDiscography : Screen("artist_discography/{name}", "Discography") {
        fun createRoute(name: String) = "artist_discography/${java.net.URLEncoder.encode(name, "UTF-8")}"
    }
    data object SimilarArtists : Screen("similar_artists/{name}", "Similar Artists") {
        fun createRoute(name: String) = "similar_artists/${java.net.URLEncoder.encode(name, "UTF-8")}"
    }
    data object HearTheDifference : Screen("hear_the_diff", "Hear The Difference")
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
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(150)) }
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onComplete = {
                    MainScope().launch { prefs.setNotificationListenerPrompted(true) }
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                })
            }
            composable(Screen.HearTheDifference.route) {
                HearTheDifferenceScreen(
                    onContinue = {
                        MainScope().launch { prefs.setHasSeenHearTheDifference(true) }
                        navController.popBackStack()
                    }
                )
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
                    onSeeAllAlbums = { navController.navigate(Screen.TopAlbumsList.route) },
                    onSeeAllRecentTracks = { navController.navigate(Screen.RecentTracks.route) },
                    onSeeAllGenres = { navController.navigate(Screen.AllGenres.route) },
                    onSeeAllListeningActivity = { navController.navigate(Screen.ListeningActivity.route) },
                    onConnectLastFm = { navController.navigate(Screen.SettingsBehavior.route) },
                    onAlbumClick = { name, artist, plays, imageUrl ->
                        navController.navigate(Screen.AlbumDetail.createRoute(name, artist, plays, imageUrl))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateLookAndFeel = { navController.navigate(Screen.SettingsLookAndFeel.route) },
                    onNavigateBehavior = { navController.navigate(Screen.SettingsBehavior.route) },
                    onNavigateNotifications = { navController.navigate(Screen.SettingsNotifications.route) },
                    onNavigateBackup = { navController.navigate(Screen.SettingsBackup.route) },
                    onNavigateAbout = { navController.navigate(Screen.SettingsAbout.route) },
                    onNavigateLyrics = { navController.navigate(Screen.SettingsLyrics.route) },
                    onOpenDebugLog = { navController.navigate(Screen.DebugLog.route) },
                    onOpenPipelineDebug = { navController.navigate(Screen.DebugPipeline.route) },
                    onOpenAppPicker = { navController.navigate(Screen.AppPicker.route) }
                )
            }
            composable(Screen.AppPicker.route) { AppPickerScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.SettingsLookAndFeel.route) { LookAndFeelSettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.SettingsBehavior.route) {
                BehaviorSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAppPicker = { navController.navigate(Screen.AppPicker.route) }
                )
            }
            composable(Screen.SettingsNotifications.route) { NotificationsSettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.SettingsBackup.route) { BackupSettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.SettingsAbout.route) {
                AboutSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDebugLog = { navController.navigate(Screen.DebugLog.route) },
                    onOpenPipelineDebug = { navController.navigate(Screen.DebugPipeline.route) }
                )
            }
            composable(Screen.SettingsLyrics.route) { LyricsSettingsScreen(onBack = { navController.popBackStack() }) }
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
                TopAlbumsListScreen(
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { name, artist, plays, imageUrl ->
                        navController.navigate(Screen.AlbumDetail.createRoute(name, artist, plays, imageUrl))
                    }
                )
            }
            composable(Screen.AlbumDetail.route) { entry ->
                val name = java.net.URLDecoder.decode(entry.arguments?.getString("name") ?: "", "UTF-8")
                val artist = java.net.URLDecoder.decode(entry.arguments?.getString("artist") ?: "", "UTF-8")
                val plays = java.net.URLDecoder.decode(entry.arguments?.getString("plays") ?: "", "UTF-8").let { if (it == "-") "" else it }
                val imageUrl = java.net.URLDecoder.decode(entry.arguments?.getString("imageUrl") ?: "", "UTF-8").let { if (it == "-") "" else it }
                AlbumDetailScreen(
                    albumName = name, artistName = artist, albumPlays = plays, albumImageUrl = imageUrl,
                    onBack = { navController.popBackStack() },
                    onTrackClick = { title, art -> navController.navigate(Screen.TrackDetail.createRoute(title, art)) },
                    onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) }
                )
            }
            composable(
                route = "${Screen.ArtistDetail.route}?trackTitle={trackTitle}",
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType },
                    navArgument("trackTitle") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val name = java.net.URLDecoder.decode(entry.arguments?.getString("name") ?: "", "UTF-8")
                val trackTitle = java.net.URLDecoder.decode(entry.arguments?.getString("trackTitle") ?: "", "UTF-8")
                ArtistDetailScreen(
                    artistName = name,
                    trackTitle = trackTitle,
                    onBack = { navController.popBackStack() },
                    onTrackClick = { title, artist -> navController.navigate(Screen.TrackDetail.createRoute(title, artist)) },
                    onAlbumClick = { albumName, artist, plays, imageUrl ->
                        navController.navigate(Screen.AlbumDetail.createRoute(albumName, artist, plays, imageUrl))
                    },
                    onArtistClick = { a -> navController.navigate(Screen.ArtistDetail.createRoute(a)) },
                    onSeeAllDiscography = { a -> navController.navigate(Screen.ArtistDiscography.createRoute(a)) },
                    onSeeAllSimilar = { a -> navController.navigate(Screen.SimilarArtists.createRoute(a)) }
                )
            }
            composable(Screen.TrackDetail.route) { entry ->
                val title = java.net.URLDecoder.decode(entry.arguments?.getString("title") ?: "", "UTF-8")
                val artist = java.net.URLDecoder.decode(entry.arguments?.getString("artist") ?: "", "UTF-8")
                TrackDetailScreen(
                    trackTitle = title, trackArtist = artist,
                    onBack = { navController.popBackStack() },
                    onArtistClick = { name ->
                        navController.navigate(Screen.ArtistDetail.createRouteWithTrack(name, title))
                    },
                    onTrackClick = { t, a -> navController.navigate(Screen.TrackDetail.createRoute(t, a)) }
                )
            }
            composable(Screen.ArtistDiscography.route) { entry ->
                val name = java.net.URLDecoder.decode(entry.arguments?.getString("name") ?: "", "UTF-8")
                ArtistDiscographyScreen(
                    artistName = name,
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { albumName, artist, plays, imageUrl ->
                        navController.navigate(Screen.AlbumDetail.createRoute(albumName, artist, plays, imageUrl))
                    }
                )
            }
            composable(Screen.SimilarArtists.route) { entry ->
                val name = java.net.URLDecoder.decode(entry.arguments?.getString("name") ?: "", "UTF-8")
                SimilarArtistsScreen(
                    artistName = name,
                    onBack = { navController.popBackStack() },
                    onArtistClick = { a -> navController.navigate(Screen.ArtistDetail.createRoute(a)) }
                )
            }
            composable(Screen.RecentTracks.route) {
                RecentTracksScreen(
                    onBack = { navController.popBackStack() },
                    onTrackClick = { title, artist -> navController.navigate(Screen.TrackDetail.createRoute(title, artist)) }
                )
            }
            composable(Screen.AllGenres.route) { AllGenresScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.ListeningActivity.route) { ListeningActivityScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.DebugLog.route) { DebugLogScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.DebugPipeline.route) { DebugPipelineScreen() }
        }
    }
}
