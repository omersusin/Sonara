package com.sonara.app.intelligence.lastfm

import com.google.gson.annotations.SerializedName

// ═══ Loved Tracks ═══
data class LastFmLovedTracksResponse(val lovedtracks: LovedTracksWrapper?)
data class LovedTracksWrapper(val track: List<LovedTrackItem>?, @SerializedName("@attr") val attr: PaginationAttr?)
data class LovedTrackItem(val name: String?, val artist: LovedTrackArtist?, val date: ScrobbleDate?, val url: String?)
data class LovedTrackArtist(val name: String?, val url: String?)
data class ScrobbleDate(@SerializedName("uts") val uts: String?, @SerializedName("#text") val text: String?)

// ═══ Friends ═══
data class LastFmFriendsResponse(val friends: FriendsWrapper?)
data class FriendsWrapper(val user: List<FriendUser>?)
data class FriendUser(val name: String?, val realname: String?, val country: String?,
    val playcount: String?, val image: List<LastFmImage>?, val registered: RegisteredInfo?)
data class RegisteredInfo(val unixtime: String?)

// ═══ Weekly Artist/Album Charts ═══
data class LastFmWeeklyArtistChartResponse(val weeklyartistchart: WeeklyArtistChartWrapper?)
data class WeeklyArtistChartWrapper(val artist: List<WeeklyChartArtist>?)
data class WeeklyChartArtist(val name: String?, val playcount: String?, val url: String?)

data class LastFmWeeklyAlbumChartResponse(val weeklyalbumchart: WeeklyAlbumChartWrapper?)
data class WeeklyAlbumChartWrapper(val album: List<WeeklyChartAlbum>?)
data class WeeklyChartAlbum(val name: String?, val artist: ArtistName?, val playcount: String?, val url: String?)
data class ArtistName(@SerializedName("#text") val text: String?)

// ═══ Tag Top Artists ═══
data class LastFmTagTopArtistsResponse(val topartists: TagTopArtistsWrapper?)
data class TagTopArtistsWrapper(val artist: List<TagArtistItem>?)
data class TagArtistItem(val name: String?, val url: String?, val image: List<LastFmImage>?)

// ═══ User Top Tags ═══
data class LastFmUserTopTagsResponse(val toptags: UserTopTagsWrapper?)
data class UserTopTagsWrapper(val tag: List<UserTagItem>?)
data class UserTagItem(val name: String?, val count: String?, val url: String?)

// ═══ Track Search ═══
data class LastFmTrackSearchResponse(val results: TrackSearchResults?)
data class TrackSearchResults(val trackmatches: TrackMatches?)
data class TrackMatches(val track: List<SearchTrackItem>?)
data class SearchTrackItem(val name: String?, val artist: String?, val listeners: String?, val url: String?)

// ═══ Pagination helper ═══
data class PaginationAttr(val page: String?, val perPage: String?, val totalPages: String?, val total: String?)
