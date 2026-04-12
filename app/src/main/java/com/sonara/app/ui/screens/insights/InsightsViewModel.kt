package com.sonara.app.ui.screens.insights

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.ai.SonaraAi
import com.sonara.app.ai.SonaraAiState
import com.sonara.app.intelligence.cache.TrackCache
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.intelligence.lastfm.LastFmAuthManager
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.ui.components.DisplayLabelMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class InsightsUiState(
    val trackTitle: String = "", val trackArtist: String = "",
    val genre: String = "Unknown", val mood: String = "Unknown",
    val energy: Float = 0.5f, val confidence: Float = 0f,
    val dataSource: String = "None",
    val headphoneConnected: Boolean = false, val headphoneName: String = "",
    val isAiEnabled: Boolean = true, val isPlaying: Boolean = false,
    val cacheSize: Int = 0, val eqActive: Boolean = false,
    val songsLearned: Int = 0, val songsViaLastFm: Int = 0, val songsViaLocal: Int = 0,
    val genreDistribution: Map<String, Int> = emptyMap(),
    val apiAccuracy: Int = 0, val eqStrategy: String = "none",
    val aiModelSamples: Int = 0, val route: String = "Unknown",
    val personalSamples: Int = 0,
    // Last.fm stats
    val lastFmConnected: Boolean = false,
    val lastFmUsername: String = "",
    val totalScrobbles: String = "0",
    val totalArtists: String = "0",
    val topArtists: List<Triple<String, String, String>> = emptyList(),
    val topTracks: List<TopTrackItem> = emptyList(),
    val weeklyTracks: List<Triple<String, String, String>> = emptyList(),
    val recentTracks: List<RecentTrackItem> = emptyList(),
    val selectedPeriod: String = "overall",
    val avgDailyScrobbles: Int = 0,
    val trackCount: String = "0",
    val registeredUnix: Long = 0
)

data class TopTrackItem(val title: String, val artist: String, val plays: String, val imageUrl: String = "")

data class RecentTrackItem(val title: String, val artist: String, val album: String, val imageUrl: String, val isNowPlaying: Boolean, val date: String)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val prefs = app.preferences
    private val cache = TrackCache(app.database.trackCacheDao())

    private val _uiState = MutableStateFlow(InsightsUiState(
        eqActive = app.audioSessionManager.isInitialized,
        eqStrategy = app.audioSessionManager.activeStrategy.value
    ))
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
    val albumArt: StateFlow<Bitmap?> = SonaraNotificationListener.albumArt

    // AI state
    val aiState: StateFlow<SonaraAiState> =
        SonaraAi.getInstance()?.state ?: MutableStateFlow(SonaraAiState()).asStateFlow()

    init {
        viewModelScope.launch { SonaraNotificationListener.nowPlaying.collect { np -> _uiState.update { it.copy(trackTitle = np.title, trackArtist = np.artist, isPlaying = np.isPlaying) } } }
        viewModelScope.launch { SonaraNotificationListener.currentGenre.collect { g -> if (g.isNotBlank()) _uiState.update { it.copy(genre = DisplayLabelMapper.formatGenre(g)) } } }
        viewModelScope.launch { SonaraNotificationListener.currentMood.collect { m -> if (m.isNotBlank()) _uiState.update { it.copy(mood = DisplayLabelMapper.formatMood(m)) } } }
        viewModelScope.launch { SonaraNotificationListener.currentEnergy.collect { e -> _uiState.update { it.copy(energy = e) } } }
        viewModelScope.launch { SonaraNotificationListener.currentConfidence.collect { c -> _uiState.update { it.copy(confidence = c) } } }
        viewModelScope.launch { SonaraNotificationListener.currentSource.collect { s -> if (s.isNotBlank()) _uiState.update { it.copy(dataSource = DisplayLabelMapper.formatSource(s)) } } }
        viewModelScope.launch { app.audioSessionManager.activeStrategy.collect { s -> _uiState.update { it.copy(eqStrategy = s, eqActive = s != "none") } } }
        viewModelScope.launch { app.currentRoute.collect { r -> _uiState.update { it.copy(route = r.displayName) } } }
        viewModelScope.launch { prefs.aiEnabledFlow.collect { e -> _uiState.update { it.copy(isAiEnabled = e) } } }
        viewModelScope.launch { prefs.songsLearnedFlow.collect { n -> _uiState.update { it.copy(songsLearned = n) } } }
        viewModelScope.launch { prefs.songsViaLastFmFlow.collect { n -> _uiState.update { st -> st.copy(songsViaLastFm = n, apiAccuracy = if (st.songsLearned > 0) minOf(100, n * 100 / st.songsLearned) else 0) } } }
        viewModelScope.launch { prefs.songsViaLocalFlow.collect { n -> _uiState.update { it.copy(songsViaLocal = n) } } }
        viewModelScope.launch { prefs.genreStatsFlow.collect { raw ->
            val map = if (raw.isBlank()) emptyMap()
            else raw.split(";").mapNotNull { entry -> val parts = entry.split(":"); if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null }.toMap()
            val formatted = map.mapKeys { (k, _) -> DisplayLabelMapper.formatGenre(k) }
                .filterKeys { it.lowercase() != "unknown" && it.isNotBlank() }
            _uiState.update { it.copy(genreDistribution = formatted) }
        } }
        viewModelScope.launch {
            app.currentRoute.collect { route ->
                val isHP = route != com.sonara.app.intelligence.pipeline.AudioRoute.SPEAKER && route != com.sonara.app.intelligence.pipeline.AudioRoute.UNKNOWN
                _uiState.update { it.copy(headphoneConnected = isHP, headphoneName = if (isHP) route.displayName else "") }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(
                cacheSize = cache.size(),
                aiModelSamples = app.adaptiveLearning.getTotalSamples(),
                personalSamples = app.personalization.getTotalSamples()
            ) }
        }

        // Sync now-playing into recently played list + refresh on track change
        viewModelScope.launch {
            SonaraNotificationListener.nowPlaying.collect { np ->
                if (np.title.isNotBlank()) {
                    _uiState.update { st ->
                        val nowItem = RecentTrackItem(
                            title = np.title,
                            artist = np.artist,
                            album = np.album,
                            imageUrl = "",
                            isNowPlaying = np.isPlaying,
                            date = "Now"
                        )
                        // Remove any existing entry for same track, prepend current
                        val filtered = st.recentTracks.filter {
                            !(it.title == np.title && it.artist == np.artist && it.isNowPlaying)
                        }
                        // Clear old nowPlaying flags
                        val cleared = filtered.map { it.copy(isNowPlaying = false) }
                        st.copy(recentTracks = listOf(nowItem) + cleared)
                    }
                    // Refresh from Last.fm after a delay (new track may take a moment to appear)
                    val u = _uiState.value.lastFmUsername
                    if (u.isNotBlank()) {
                        kotlinx.coroutines.delay(5000)
                        refreshRecentTracks(u)
                    }
                }
            }
        }

        // Last.fm connection + stats
        viewModelScope.launch {
            app.lastFmAuth.authState.collect { state ->
                _uiState.update { it.copy(lastFmConnected = state == LastFmAuthManager.AuthState.CONNECTED) }
            }
        }
        viewModelScope.launch {
            app.lastFmAuth.username.collect { name ->
                _uiState.update { it.copy(lastFmUsername = name) }
                if (name.isNotBlank()) fetchLastFmStats(name)
            }
        }
        // Fallback: if auth says connected but username didn't flow yet, trigger fetch
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // Wait for flows to settle
            val st = _uiState.value
            if (st.lastFmConnected && st.lastFmUsername.isBlank()) {
                // Try to get username from connection info
                val info = app.lastFmAuth.getConnectionInfo()
                if (info.username.isNotBlank()) {
                    _uiState.update { it.copy(lastFmUsername = info.username) }
                    fetchLastFmStats(info.username)
                }
            }
        }
        // Fallback: if auth says connected but username didn't flow yet, trigger fetch
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // Wait for flows to settle
            val st = _uiState.value
            if (st.lastFmConnected && st.lastFmUsername.isBlank()) {
                // Try to get username from connection info
                val info = app.lastFmAuth.getConnectionInfo()
                if (info.username.isNotBlank()) {
                    _uiState.update { it.copy(lastFmUsername = info.username) }
                    fetchLastFmStats(info.username)
                }
            }
        }
    }

    private fun fetchLastFmStats(username: String) {
        val apiKey = app.lastFmAuth.getActiveApiKey()
        if (apiKey.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // User info
                val info = LastFmClient.api.getUserInfo(username, apiKey)
                info.user?.let { u ->
                    val regUnix = u.registered?.unixtime?.toLongOrNull() ?: 0L
                    val daysSince = if (regUnix > 0) ((System.currentTimeMillis() / 1000 - regUnix) / 86400).toInt().coerceAtLeast(1) else 1
                    val totalSc = u.playcount.toLongOrNull() ?: 0
                    val avgDaily = (totalSc / daysSince).toInt()
                    _uiState.update { it.copy(
                        totalScrobbles = u.playcount,
                        totalArtists = u.artist_count,
                        trackCount = u.track_count,
                        registeredUnix = regUnix,
                        avgDailyScrobbles = avgDaily
                    ) }
                }
            } catch (_: Exception) {}
            try {
                // Top artists
                val artists = LastFmClient.api.getUserTopArtists(username, apiKey, "overall", 8)
                val list = artists.topartists?.artist?.map { a -> Triple(a.name, a.playcount, a.imageUrl ?: "") } ?: emptyList()
                _uiState.update { it.copy(topArtists = list) }
                // Fetch Deezer images (Last.fm deprecated artist images in 2020)
                val enriched = list.map { (name, plays, lfmImg) ->
                    val img = if (lfmImg.isNotBlank() && !lfmImg.contains("2a96cbd8b46e")) lfmImg
                              else DeezerImageResolver.getArtistImageWithFallback(name) ?: ""
                    Triple(name, plays, img)
                }
                _uiState.update { it.copy(topArtists = enriched) }
            } catch (_: Exception) {}
            try {
                // Top tracks
                val tracks = LastFmClient.api.getUserTopTracks(username, apiKey, "overall", 8)
                val list = tracks.toptracks?.track?.map { t -> TopTrackItem(t.name, t.artist?.name ?: "", t.playcount, t.imageUrl ?: "") } ?: emptyList()
                _uiState.update { it.copy(topTracks = list) }
                // Enrich track images via Deezer
                val enrichedTracks = list.map { t ->
                    val img = if (t.imageUrl.isNotBlank()) t.imageUrl else DeezerImageResolver.getTrackImageWithFallback(t.title, t.artist) ?: ""
                    t.copy(imageUrl = img)
                }
                _uiState.update { it.copy(topTracks = enrichedTracks) }
            } catch (_: Exception) {}
            try {
                // Weekly chart
                val weekly = LastFmClient.api.getWeeklyTrackChart(username, apiKey)
                val list = weekly.weeklytrackchart?.track?.take(8)?.map { t -> Triple(t.name, t.artist?.name ?: "", t.playcount) } ?: emptyList()
                _uiState.update { it.copy(weeklyTracks = list) }
            } catch (_: Exception) {}
            // Recent tracks
            try {
                val recent = LastFmClient.api.getRecentTracks(username, apiKey, 10)
                val list = recent.recenttracks?.track?.map { t -> RecentTrackItem(title = t.name, artist = t.artist?.text ?: "", album = t.album?.text ?: "", imageUrl = t.imageUrl ?: "", isNowPlaying = t.isNowPlaying, date = t.date?.text ?: "Now") } ?: emptyList()
                _uiState.update { it.copy(recentTracks = list) }
            } catch (_: Exception) {}
            // Enrich artists with Deezer images
            try {
                val enriched = _uiState.value.topArtists.map { t ->
                    val img = t.third
                    val resolved = if (img.isNotBlank() && !img.contains("2a96cbd8b46e")) img else DeezerImageResolver.getArtistImageWithFallback(t.first) ?: ""
                    Triple(t.first, t.second, resolved)
                }
                _uiState.update { it.copy(topArtists = enriched) }
            } catch (_: Exception) {}
        }
    }

    private fun refreshRecentTracks(username: String) {
        val apiKey = app.lastFmAuth.getActiveApiKey()
        if (apiKey.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recent = LastFmClient.api.getRecentTracks(username, apiKey, 10)
                val apiList = recent.recenttracks?.track?.map { t ->
                    RecentTrackItem(
                        title = t.name,
                        artist = t.artist?.text ?: "",
                        album = t.album?.text ?: "",
                        imageUrl = t.imageUrl ?: "",
                        isNowPlaying = t.isNowPlaying,
                        date = t.date?.text ?: "Now"
                    )
                } ?: emptyList()

                // Merge: keep local now-playing at top if still playing
                val np = SonaraNotificationListener.nowPlaying.value
                _uiState.update { st ->
                    if (np.isPlaying && np.title.isNotBlank()) {
                        val nowItem = RecentTrackItem(
                            title = np.title, artist = np.artist, album = np.album,
                            imageUrl = apiList.firstOrNull { it.title == np.title }?.imageUrl ?: "",
                            isNowPlaying = true, date = "Now"
                        )
                        val rest = apiList.filter {
                            !(it.title == np.title && it.artist == np.artist && it.isNowPlaying)
                        }.map { it.copy(isNowPlaying = false) }
                        st.copy(recentTracks = listOf(nowItem) + rest)
                    } else {
                        st.copy(recentTracks = apiList)
                    }
                }
                // Enrich images via Deezer for items missing art
                val enriched = _uiState.value.recentTracks.map { t ->
                    if (t.imageUrl.isBlank() || t.imageUrl.contains("2a96cbd8b46e")) {
                        val img = DeezerImageResolver.getTrackImageWithFallback(t.title, t.artist) ?: ""
                        t.copy(imageUrl = img)
                    } else t
                }
                _uiState.update { it.copy(recentTracks = enriched) }
            } catch (_: Exception) {}
        }
    }

    fun setPeriod(period: String) {
        _uiState.update { it.copy(selectedPeriod = period) }
        val u = _uiState.value.lastFmUsername; val k = app.lastFmAuth.getActiveApiKey()
        if (u.isBlank() || k.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try { val a = LastFmClient.api.getUserTopArtists(u, k, period, 8)
                val l = a.topartists?.artist?.map { Triple(it.name, it.playcount, it.imageUrl ?: "") } ?: emptyList()
                val enriched = l.map { t -> val img = if (t.third.isNotBlank() && !t.third.contains("2a96cbd8b46e")) t.third else DeezerImageResolver.getArtistImageWithFallback(t.first) ?: ""; Triple(t.first, t.second, img) }
                _uiState.update { it.copy(topArtists = enriched) }
            } catch (_: Exception) {}
            try { val t = LastFmClient.api.getUserTopTracks(u, k, period, 8)
                val tl = t.toptracks?.track?.map { tr -> TopTrackItem(tr.name, tr.artist?.name ?: "", tr.playcount, tr.imageUrl ?: "") } ?: emptyList()
                val enrichedT = tl.map { tr -> val img = if (tr.imageUrl.isNotBlank()) tr.imageUrl else DeezerImageResolver.getTrackImageWithFallback(tr.title, tr.artist) ?: ""; tr.copy(imageUrl = img) }
                _uiState.update { it.copy(topTracks = enrichedT) }
            } catch (_: Exception) {}
        }
    }
}
