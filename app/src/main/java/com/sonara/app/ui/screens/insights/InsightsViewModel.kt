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
    // Last.fm
    val lastFmConnected: Boolean = false, val lastFmUsername: String = "",
    val totalScrobbles: String = "0", val totalArtists: String = "0",
    val topArtists: List<Triple<String, String, String>> = emptyList(),
    val topTracks: List<TopTrackItem> = emptyList(),
    val topAlbums: List<TopAlbumItem> = emptyList(),
    val weeklyTracks: List<Triple<String, String, String>> = emptyList(),
    val recentTracks: List<RecentTrackItem> = emptyList(),
    val selectedPeriod: String = "overall",
    val avgDailyScrobbles: Int = 0,
    val trackCount: String = "0",
    val registeredUnix: Long = 0,
    // Derived stats
    val listeningHours: Int = 0,
    val weeklyActivity: List<Pair<String, Int>> = emptyList(),
    val streakDays: Int = 0,
    val peakHour: Int = -1,
    val heatmap: Map<String, Int> = emptyMap(),
    val selectedPeriodLabel: String = "All Time"
)

data class TopTrackItem(val title: String, val artist: String, val plays: String, val imageUrl: String = "")
data class TopAlbumItem(val name: String, val artist: String, val plays: String, val imageUrl: String = "")
data class RecentTrackItem(val title: String, val artist: String, val album: String, val imageUrl: String, val isNowPlaying: Boolean, val date: String, val uts: Long = 0L)

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
    val aiState: StateFlow<SonaraAiState> = SonaraAi.getInstance()?.state ?: MutableStateFlow(SonaraAiState()).asStateFlow()

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
            val formatted = map.mapKeys { (k, _) -> DisplayLabelMapper.formatGenre(k) }.filterKeys { it.lowercase() != "unknown" && it.isNotBlank() }
            _uiState.update { it.copy(genreDistribution = formatted) }
        } }
        viewModelScope.launch { app.currentRoute.collect { route ->
            val isHP = route != com.sonara.app.intelligence.pipeline.AudioRoute.SPEAKER && route != com.sonara.app.intelligence.pipeline.AudioRoute.UNKNOWN
            _uiState.update { it.copy(headphoneConnected = isHP, headphoneName = if (isHP) route.displayName else "") }
        } }
        viewModelScope.launch { _uiState.update { it.copy(cacheSize = cache.size(), aiModelSamples = app.adaptiveLearning.getTotalSamples(), personalSamples = app.personalization.getTotalSamples()) } }

        // Now playing sync
        viewModelScope.launch {
            SonaraNotificationListener.nowPlaying.collect { np ->
                if (np.title.isNotBlank()) {
                    _uiState.update { st ->
                        val nowItem = RecentTrackItem(np.title, np.artist, np.album, "", np.isPlaying, "Now")
                        val filtered = st.recentTracks.filter { !(it.title == np.title && it.artist == np.artist && it.isNowPlaying) }.map { it.copy(isNowPlaying = false) }
                        st.copy(recentTracks = listOf(nowItem) + filtered)
                    }
                    val u = _uiState.value.lastFmUsername
                    if (u.isNotBlank()) { kotlinx.coroutines.delay(5000); refreshRecentTracks(u) }
                }
            }
        }

        // Last.fm auth
        viewModelScope.launch { app.lastFmAuth.authState.collect { state -> _uiState.update { it.copy(lastFmConnected = state == LastFmAuthManager.AuthState.CONNECTED) } } }
        viewModelScope.launch { app.lastFmAuth.username.collect { name -> _uiState.update { it.copy(lastFmUsername = name) }; if (name.isNotBlank()) fetchLastFmStats(name) } }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            val st = _uiState.value
            if (st.lastFmConnected && st.lastFmUsername.isBlank()) {
                val info = app.lastFmAuth.getConnectionInfo()
                if (info.username.isNotBlank()) { _uiState.update { it.copy(lastFmUsername = info.username) }; fetchLastFmStats(info.username) }
            }
        }
    }

    private fun fetchLastFmStats(username: String) {
        val apiKey = app.lastFmAuth.getActiveApiKey()
        if (apiKey.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            // User info
            try {
                val info = LastFmClient.api.getUserInfo(username, apiKey)
                info.user?.let { u ->
                    val regUnix = u.registered?.unixtime?.toLongOrNull() ?: 0L
                    val daysSince = if (regUnix > 0) ((System.currentTimeMillis() / 1000 - regUnix) / 86400).toInt().coerceAtLeast(1) else 1
                    val totalSc = u.playcount.toLongOrNull() ?: 0
                    val avgDaily = (totalSc / daysSince).toInt()
                    val hours = (totalSc * 3.5 / 60).toInt()
                    _uiState.update { it.copy(totalScrobbles = u.playcount, totalArtists = u.artist_count, trackCount = u.track_count, registeredUnix = regUnix, avgDailyScrobbles = avgDaily, listeningHours = hours) }
                }
            } catch (_: Exception) {}
            // Top artists
            try {
                val artists = LastFmClient.api.getUserTopArtists(username, apiKey, "overall", 10)
                val list = artists.topartists?.artist?.map { Triple(it.name, it.playcount, it.imageUrl ?: "") } ?: emptyList()
                _uiState.update { it.copy(topArtists = list) }
                val enriched = list.map { (name, plays, img) -> Triple(name, plays, if (img.isNotBlank() && !img.contains("2a96cbd8b46e")) img else DeezerImageResolver.getArtistImageWithFallback(name) ?: "") }
                _uiState.update { it.copy(topArtists = enriched) }
            } catch (_: Exception) {}
            // Top tracks
            try {
                val tracks = LastFmClient.api.getUserTopTracks(username, apiKey, "overall", 10)
                val list = tracks.toptracks?.track?.map { TopTrackItem(it.name, it.artist?.name ?: "", it.playcount, it.imageUrl ?: "") } ?: emptyList()
                _uiState.update { it.copy(topTracks = list) }
                val enriched = list.map { t -> t.copy(imageUrl = if (t.imageUrl.isNotBlank()) t.imageUrl else DeezerImageResolver.getTrackImageWithFallback(t.title, t.artist) ?: "") }
                _uiState.update { it.copy(topTracks = enriched) }
            } catch (_: Exception) {}
            // Genres from Last.fm (aggregate top artists tags)
            try {
                val genreMap = mutableMapOf<String, Int>()
                val topForGenre = _uiState.value.topArtists.take(5)
                for (artist in topForGenre) {
                    try {
                        val tagsResp = LastFmClient.api.getArtistTags(artist.first, apiKey)
                        tagsResp.toptags?.tag?.take(3)?.forEach { tag ->
                            val name = tag.name.lowercase().replaceFirstChar { it.uppercase() }
                            if (name.isNotBlank() && name.lowercase() != "seen live" && name.lowercase() != "favorites") {
                                genreMap[name] = (genreMap[name] ?: 0) + (tag.count.coerceAtLeast(1))
                            }
                        }
                    } catch (_: Exception) {}
                }
                if (genreMap.isNotEmpty()) {
                    _uiState.update { it.copy(genreDistribution = genreMap) }
                }
            } catch (_: Exception) {}
            // Top albums
            try {
                val albums = LastFmClient.api.getUserTopAlbums(username, apiKey, "overall", 6)
                val list = albums.topalbums?.album?.map { TopAlbumItem(it.name, it.artist?.name ?: "", it.playcount, it.imageUrl ?: "") } ?: emptyList()
                _uiState.update { it.copy(topAlbums = list) }
                val enriched = list.map { a -> a.copy(imageUrl = if (a.imageUrl.isNotBlank() && !a.imageUrl.contains("2a96cbd8b46e")) a.imageUrl else DeezerImageResolver.getTrackImageWithFallback(a.name, a.artist) ?: "") }
                _uiState.update { it.copy(topAlbums = enriched) }
            } catch (_: Exception) {}
            // Recent tracks + weekly activity + streak + peak hour + heatmap
            try {
                val recent = LastFmClient.api.getRecentTracks(username, apiKey, 200)
                val list = recent.recenttracks?.track?.map { t -> RecentTrackItem(t.name, t.artist?.text ?: "", t.album?.text ?: "", t.imageUrl ?: "", t.isNowPlaying, t.date?.text ?: "Now", t.date?.uts?.toLongOrNull() ?: 0L) } ?: emptyList()
                _uiState.update { it.copy(recentTracks = list) }
                // Derive weekly activity + heatmap + streak + peak hour from timestamps
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val dayCounts = IntArray(7)
                val hourBuckets = IntArray(24)
                val heatmap = mutableMapOf<String, Int>()
                val daySet = mutableSetOf<String>()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                recent.recenttracks?.track?.filter { it.date != null }?.forEach { t ->
                    val uts = t.date?.uts?.toLongOrNull() ?: return@forEach
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = uts * 1000 }
                    val dow = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
                    dayCounts[dow]++
                    hourBuckets[cal.get(java.util.Calendar.HOUR_OF_DAY)]++
                    val dayKey = sdf.format(cal.time)
                    daySet.add(dayKey)
                    heatmap[dayKey] = (heatmap[dayKey] ?: 0) + 1
                }
                val activity = days.mapIndexed { i, d -> d to dayCounts[i] }
                // Streak: consecutive days ending today
                var streak = 0
                val check = java.util.Calendar.getInstance()
                check.set(java.util.Calendar.HOUR_OF_DAY, 0); check.set(java.util.Calendar.MINUTE, 0)
                check.set(java.util.Calendar.SECOND, 0); check.set(java.util.Calendar.MILLISECOND, 0)
                while (sdf.format(check.time) in daySet) {
                    streak++
                    check.add(java.util.Calendar.DAY_OF_YEAR, -1)
                }
                val peakHour = hourBuckets.indices.maxByOrNull { hourBuckets[it] }?.takeIf { hourBuckets[it] > 0 } ?: -1
                _uiState.update { it.copy(weeklyActivity = activity, streakDays = streak, peakHour = peakHour, heatmap = heatmap) }
                // Enrich images
                val enrichedRecent = list.map { t -> if (t.imageUrl.isBlank() || t.imageUrl.contains("2a96cbd8b46e")) t.copy(imageUrl = DeezerImageResolver.getTrackImageWithFallback(t.title, t.artist) ?: "") else t }
                _uiState.update { it.copy(recentTracks = enrichedRecent) }
            } catch (_: Exception) {}
            // Weekly chart
            try {
                val weekly = LastFmClient.api.getWeeklyTrackChart(username, apiKey)
                val list = weekly.weeklytrackchart?.track?.take(8)?.map { Triple(it.name, it.artist?.name ?: "", it.playcount) } ?: emptyList()
                _uiState.update { it.copy(weeklyTracks = list) }
            } catch (_: Exception) {}
        }
    }

    fun refreshAllRecentTracks() {
        val u = _uiState.value.lastFmUsername
        val k = app.lastFmAuth.getActiveApiKey()
        if (u.isBlank() || k.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = LastFmClient.api.getRecentTracks(u, k, 200, 1)
                val list = resp.recenttracks?.track?.map { t ->
                    RecentTrackItem(t.name, t.artist?.text ?: "", t.album?.text ?: "",
                        t.imageUrl?.takeIf { !it.contains("2a96cbd8b46e") } ?: "",
                        t.isNowPlaying, t.date?.text ?: "Now", t.date?.uts?.toLongOrNull() ?: 0L)
                } ?: emptyList()
                val enriched = list.map { t -> if (t.imageUrl.isBlank()) t.copy(imageUrl = DeezerImageResolver.getTrackImageWithFallback(t.title, t.artist) ?: "") else t }
                _uiState.update { it.copy(recentTracks = enriched) }
            } catch (_: Exception) {}
        }
    }

    private fun refreshRecentTracks(username: String) {
        val apiKey = app.lastFmAuth.getActiveApiKey()
        if (apiKey.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recent = LastFmClient.api.getRecentTracks(username, apiKey, 10)
                val apiList = recent.recenttracks?.track?.map { t -> RecentTrackItem(t.name, t.artist?.text ?: "", t.album?.text ?: "", t.imageUrl ?: "", t.isNowPlaying, t.date?.text ?: "Now", t.date?.uts?.toLongOrNull() ?: 0L) } ?: emptyList()
                val np = SonaraNotificationListener.nowPlaying.value
                _uiState.update { st ->
                    if (np.isPlaying && np.title.isNotBlank()) {
                        val nowItem = RecentTrackItem(np.title, np.artist, np.album, apiList.firstOrNull { it.title == np.title }?.imageUrl ?: "", true, "Now")
                        val rest = apiList.filter { !(it.title == np.title && it.artist == np.artist && it.isNowPlaying) }.map { it.copy(isNowPlaying = false) }
                        st.copy(recentTracks = listOf(nowItem) + rest)
                    } else st.copy(recentTracks = apiList)
                }
                val enriched = _uiState.value.recentTracks.map { t -> if (t.imageUrl.isBlank() || t.imageUrl.contains("2a96cbd8b46e")) t.copy(imageUrl = DeezerImageResolver.getTrackImageWithFallback(t.title, t.artist) ?: "") else t }
                _uiState.update { it.copy(recentTracks = enriched) }
            } catch (_: Exception) {}
        }
    }

    fun setCustomPeriod(fromSec: Long, toSec: Long) {
        val label = buildString {
            val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.US)
            append(sdf.format(java.util.Date(fromSec * 1000)))
            append(" – ")
            append(sdf.format(java.util.Date(toSec * 1000)))
        }
        _uiState.update { it.copy(selectedPeriod = "custom", selectedPeriodLabel = label) }
        val u = _uiState.value.lastFmUsername; val k = app.lastFmAuth.getActiveApiKey()
        if (u.isBlank() || k.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = LastFmClient.api.getRecentTracksRange(u, k, fromSec, toSec, 200)
                val tracks = resp.recenttracks?.track?.filter { it.date != null } ?: emptyList()
                // Aggregate top artists from track history
                val artistCounts = tracks.groupBy { it.artist?.text ?: "" }
                    .filterKeys { it.isNotBlank() }.mapValues { it.value.size }
                    .entries.sortedByDescending { it.value }.take(10)
                val topArtists = artistCounts.map { (name, count) ->
                    Triple(name, count.toString(), DeezerImageResolver.getArtistImageWithFallback(name) ?: "")
                }
                // Aggregate top tracks
                val trackCounts = tracks.groupBy { it.name to (it.artist?.text ?: "") }
                    .mapValues { it.value.size }.entries.sortedByDescending { it.value }.take(10)
                val topTracks = trackCounts.map { (titleArtist, count) ->
                    TopTrackItem(titleArtist.first, titleArtist.second, count.toString(),
                        DeezerImageResolver.getTrackImageWithFallback(titleArtist.first, titleArtist.second) ?: "")
                }
                // Aggregate top albums
                val albumCounts = tracks.groupBy { (it.album?.text ?: "") to (it.artist?.text ?: "") }
                    .filterKeys { it.first.isNotBlank() }.mapValues { it.value.size }
                    .entries.sortedByDescending { it.value }.take(6)
                val topAlbums = albumCounts.map { (nameArtist, count) ->
                    TopAlbumItem(nameArtist.first, nameArtist.second, count.toString(),
                        DeezerImageResolver.getTrackImageWithFallback(nameArtist.first, nameArtist.second) ?: "")
                }
                _uiState.update { it.copy(topArtists = topArtists, topTracks = topTracks, topAlbums = topAlbums) }
            } catch (_: Exception) {}
        }
    }

    fun setPeriod(period: String) {
        if (period == "custom") return // custom periods go through setCustomPeriod
        val label = when (period) {
            "7day" -> "7 Days"; "1month" -> "1 Month"; "3month" -> "3 Months"
            "6month" -> "6 Months"; "12month" -> "1 Year"; else -> "All Time"
        }
        _uiState.update { it.copy(selectedPeriod = period, selectedPeriodLabel = label) }
        val u = _uiState.value.lastFmUsername; val k = app.lastFmAuth.getActiveApiKey()
        if (u.isBlank() || k.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val a = LastFmClient.api.getUserTopArtists(u, k, period, 10)
                val enriched = (a.topartists?.artist?.map { Triple(it.name, it.playcount, it.imageUrl ?: "") } ?: emptyList()).map { t -> Triple(t.first, t.second, if (t.third.isNotBlank() && !t.third.contains("2a96cbd8b46e")) t.third else DeezerImageResolver.getArtistImageWithFallback(t.first) ?: "") }
                _uiState.update { it.copy(topArtists = enriched) }
            } catch (_: Exception) {}
            try {
                val t = LastFmClient.api.getUserTopTracks(u, k, period, 10)
                val enriched = (t.toptracks?.track?.map { TopTrackItem(it.name, it.artist?.name ?: "", it.playcount, it.imageUrl ?: "") } ?: emptyList()).map { tr -> tr.copy(imageUrl = if (tr.imageUrl.isNotBlank()) tr.imageUrl else DeezerImageResolver.getTrackImageWithFallback(tr.title, tr.artist) ?: "") }
                _uiState.update { it.copy(topTracks = enriched) }
            } catch (_: Exception) {}
            try {
                val al = LastFmClient.api.getUserTopAlbums(u, k, period, 6)
                val enriched = (al.topalbums?.album?.map { TopAlbumItem(it.name, it.artist?.name ?: "", it.playcount, it.imageUrl ?: "") } ?: emptyList()).map { a -> a.copy(imageUrl = if (a.imageUrl.isNotBlank() && !a.imageUrl.contains("2a96cbd8b46e")) a.imageUrl else DeezerImageResolver.getTrackImageWithFallback(a.name, a.artist) ?: "") }
                _uiState.update { it.copy(topAlbums = enriched) }
            } catch (_: Exception) {}
            // Refresh genres from updated top artists
            try {
                val topArtistNames = _uiState.value.topArtists.take(5).map { it.first }
                val genreMap = mutableMapOf<String, Int>()
                for (name in topArtistNames) {
                    try {
                        val tagsResp = LastFmClient.api.getArtistTags(name, k)
                        tagsResp.toptags?.tag?.take(3)?.forEach { tag ->
                            val tagName = tag.name.lowercase().replaceFirstChar { it.uppercase() }
                            if (tagName.isNotBlank() && tagName.lowercase() != "seen live" && tagName.lowercase() != "favorites") {
                                genreMap[tagName] = (genreMap[tagName] ?: 0) + (tag.count.coerceAtLeast(1))
                            }
                        }
                    } catch (_: Exception) {}
                }
                if (genreMap.isNotEmpty()) _uiState.update { it.copy(genreDistribution = genreMap) }
            } catch (_: Exception) {}
        }
    }
}
