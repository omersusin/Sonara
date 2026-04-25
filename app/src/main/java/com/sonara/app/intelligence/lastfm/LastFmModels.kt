package com.sonara.app.intelligence.lastfm

import com.google.gson.annotations.SerializedName

data class LastFmTrackResponse(val track: LastFmTrack? = null)
data class LastFmTrack(
    val name: String = "",
    val artist: LastFmArtist? = null,
    val album: LastFmAlbum? = null,
    val toptags: LastFmTopTags? = null,
    val duration: String? = null,
    val listeners: String? = null,
    val playcount: String? = null
)
data class LastFmArtist(val name: String = "")
data class LastFmAlbum(
    val title: String = "",
    val image: List<LastFmImage> = emptyList()
) {
    val imageUrl: String? get() = image.lastOrNull { it.text.isNotBlank() && !it.text.contains("2a96cbd8b46e") }?.text
}
data class LastFmTopTags(val tag: List<LastFmTag> = emptyList())
data class LastFmTag(val name: String = "", val count: Int = 0)

data class LastFmArtistTagsResponse(
    @SerializedName("toptags") val toptags: LastFmTopTags? = null
)


// ═══ User Stats Models ═══
data class LastFmUserInfoResponse(val user: LastFmUser? = null)
data class LastFmUser(
    val name: String = "",
    val playcount: String = "0",
    val artist_count: String = "0",
    val track_count: String = "0",
    val registered: LastFmRegistered? = null,
    val image: List<LastFmImage> = emptyList(),
    val country: String = ""
)
data class LastFmRegistered(val unixtime: String = "0")
data class LastFmImage(
    @SerializedName("#text") val text: String = "",
    val size: String = ""
)

data class LastFmTopArtistsResponse(val topartists: LastFmTopArtistsList? = null)
data class LastFmTopArtistsList(val artist: List<LastFmTopArtist> = emptyList())
data class LastFmTopArtist(
    val name: String = "",
    val playcount: String = "0",
    val image: List<LastFmImage> = emptyList(),
    @SerializedName("@attr") val attr: LastFmRank? = null
) {
    val imageUrl: String? get() = image.lastOrNull { it.text.isNotBlank() }?.text
}
data class LastFmRank(val rank: String = "0")

data class LastFmTopTracksResponse(val toptracks: LastFmTopTracksList? = null)
data class LastFmTopTracksList(val track: List<LastFmTopTrack> = emptyList())
data class LastFmTopTrack(
    val name: String = "",
    val artist: LastFmArtist? = null,
    val playcount: String = "0",
    val image: List<LastFmImage> = emptyList(),
    @SerializedName("@attr") val attr: LastFmRank? = null
) {
    val imageUrl: String? get() = image.lastOrNull { it.text.isNotBlank() && !it.text.contains("2a96cbd8b46e") }?.text
}

data class LastFmWeeklyChartResponse(val weeklytrackchart: LastFmWeeklyChart? = null)
data class LastFmWeeklyChart(val track: List<LastFmTopTrack> = emptyList())

data class LastFmTopAlbumsResponse(val topalbums: LastFmTopAlbumsList? = null)
data class LastFmTopAlbumsList(val album: List<LastFmTopAlbum> = emptyList())
data class LastFmTopAlbum(
    val name: String = "",
    val playcount: String = "0",
    val artist: LastFmArtist? = null,
    val image: List<LastFmImage> = emptyList()
) {
    val imageUrl: String? get() = image.lastOrNull { it.text.isNotBlank() && !it.text.contains("2a96cbd8b46e") }?.text
}

data class LastFmRecentTracksResponse(val recenttracks: LastFmRecentTracks? = null)
data class LastFmRecentTracks(val track: List<LastFmRecentTrack> = emptyList())
data class LastFmRecentTrack(val name: String = "", val artist: LastFmRecentArtist? = null, val album: LastFmRecentAlbum? = null, val image: List<LastFmImage> = emptyList(), val date: LastFmDate? = null, @SerializedName("@attr") val attr: LastFmNowPlaying? = null) { val imageUrl: String? get() = image.lastOrNull { it.text.isNotBlank() }?.text; val isNowPlaying: Boolean get() = attr?.nowplaying == "true" }
data class LastFmRecentArtist(@SerializedName("#text") val text: String = "")
data class LastFmRecentAlbum(@SerializedName("#text") val text: String = "")
data class LastFmDate(@SerializedName("#text") val text: String = "", val uts: String = "0")
data class LastFmNowPlaying(val nowplaying: String = "")

data class LastFmAlbumInfoResponse(val album: LastFmAlbumInfo? = null)
data class LastFmAlbumInfo(
    val name: String = "",
    val artist: String = "",
    val playcount: String = "0",
    val listeners: String = "0",
    val image: List<LastFmImage> = emptyList(),
    val tracks: LastFmAlbumTracks? = null,
    val wiki: LastFmWiki? = null
) {
    val imageUrl: String? get() = image.lastOrNull { it.text.isNotBlank() && !it.text.contains("2a96cbd8b46e") }?.text
}
data class LastFmAlbumTracks(val track: List<LastFmAlbumTrack> = emptyList())
data class LastFmAlbumTrack(
    val name: String = "",
    val duration: String = "0",
    @SerializedName("@attr") val attr: LastFmAlbumTrackAttr? = null,
    val artist: LastFmArtist? = null,
    val url: String = ""
)
data class LastFmAlbumTrackAttr(val rank: String = "0")
data class LastFmWiki(val summary: String = "")

data class LastFmArtistInfoResponse(val artist: LastFmArtistInfo? = null)
data class LastFmArtistInfo(
    val name: String = "",
    val bio: LastFmBio? = null,
    val stats: LastFmArtistStats? = null,
    val tags: LastFmTopTags? = null
)
data class LastFmBio(val summary: String = "", val content: String = "")
data class LastFmArtistStats(val listeners: String = "", val playcount: String = "")

// ═══ Similar Artists / Tracks ═══
data class LastFmSimilarArtistsResponse(val similarartists: LastFmSimilarArtistsList? = null)
data class LastFmSimilarArtistsList(val artist: List<LastFmSimilarArtist> = emptyList())
data class LastFmSimilarArtist(
    val name: String = "",
    val match: String = "0",
    val image: List<LastFmImage> = emptyList()
) {
    val imageUrl: String? get() = image.lastOrNull { it.text.isNotBlank() && !it.text.contains("2a96cbd8b46e") }?.text
}

data class LastFmSimilarTracksResponse(val similartracks: LastFmSimilarTracksList? = null)
data class LastFmSimilarTracksList(val track: List<LastFmSimilarTrack> = emptyList())
data class LastFmSimilarTrack(
    val name: String = "",
    val artist: LastFmArtist? = null,
    val match: String = "0",
    val image: List<LastFmImage> = emptyList()
) {
    val imageUrl: String? get() = image.lastOrNull { it.text.isNotBlank() && !it.text.contains("2a96cbd8b46e") }?.text
}
