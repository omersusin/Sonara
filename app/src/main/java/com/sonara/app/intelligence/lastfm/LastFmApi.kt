package com.sonara.app.intelligence.lastfm

import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmApi {
    @GET("?method=track.getInfo&format=json")
    suspend fun getTrackInfo(
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("autocorrect") autocorrect: Int = 1
    ): LastFmTrackResponse

    @GET("?method=track.getTags&format=json")
    suspend fun getTrackTags(
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("autocorrect") autocorrect: Int = 1
    ): LastFmTrackResponse

    @GET("?method=artist.getTopTags&format=json")
    suspend fun getArtistTags(
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("autocorrect") autocorrect: Int = 1
    ): LastFmArtistTagsResponse

    @GET("?method=artist.getInfo&format=json")
    suspend fun getArtistInfo(
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("username") username: String = "",
        @Query("autocorrect") autocorrect: Int = 1
    ): LastFmArtistInfoResponse

    @GET("?method=user.getInfo&format=json")
    suspend fun getUserInfo(
        @Query("user") user: String,
        @Query("api_key") apiKey: String
    ): LastFmUserInfoResponse

    @GET("?method=user.getTopArtists&format=json")
    suspend fun getUserTopArtists(
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("period") period: String = "overall",
        @Query("limit") limit: Int = 10
    ): LastFmTopArtistsResponse

    @GET("?method=user.getTopTracks&format=json")
    suspend fun getUserTopTracks(
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("period") period: String = "overall",
        @Query("limit") limit: Int = 10
    ): LastFmTopTracksResponse

    @GET("?method=user.getWeeklyTrackChart&format=json")
    suspend fun getWeeklyTrackChart(
        @Query("user") user: String,
        @Query("api_key") apiKey: String
    ): LastFmWeeklyChartResponse

    @GET("?method=user.getTopAlbums&format=json")
    suspend fun getUserTopAlbums(
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("period") period: String = "overall",
        @Query("limit") limit: Int = 5
    ): LastFmTopAlbumsResponse

    @GET("?method=user.getRecentTracks&format=json")
    suspend fun getRecentTracks(@Query("user") user: String, @Query("api_key") apiKey: String, @Query("limit") limit: Int = 10, @Query("page") page: Int = 1): LastFmRecentTracksResponse

    @GET("?method=user.getRecentTracks&format=json")
    suspend fun getRecentTracksRange(
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("limit") limit: Int = 200,
        @Query("page") page: Int = 1
    ): LastFmRecentTracksResponse

    @GET("?method=artist.getSimilar&format=json")
    suspend fun getSimilarArtists(
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 6,
        @Query("autocorrect") autocorrect: Int = 1
    ): LastFmSimilarArtistsResponse

    @GET("?method=user.getTrackScrobbles&format=json")
    suspend fun getUserTrackScrobbles(
        @Query("user") user: String,
        @Query("artist") artist: String,
        @Query("track") track: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 200,
        @Query("page") page: Int = 1
    ): LastFmRecentTracksResponse

    @GET("?method=track.getSimilar&format=json")
    suspend fun getSimilarTracks(
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 6,
        @Query("autocorrect") autocorrect: Int = 1
    ): LastFmSimilarTracksResponse

    @GET("?method=album.getInfo&format=json")
    suspend fun getAlbumInfo(
        @Query("artist") artist: String,
        @Query("album") album: String,
        @Query("api_key") apiKey: String,
        @Query("autocorrect") autocorrect: Int = 1
    ): LastFmAlbumInfoResponse

    @GET("?method=user.getLovedTracks&format=json")
    suspend fun getLovedTracks(
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1
    ): LastFmLovedTracksResponse

    @GET("?method=user.getFriends&format=json")
    suspend fun getFriends(
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 10
    ): LastFmFriendsResponse

    @GET("?method=user.getWeeklyArtistChart&format=json")
    suspend fun getWeeklyArtistChart(
        @Query("user") user: String,
        @Query("api_key") apiKey: String
    ): LastFmWeeklyArtistChartResponse

    @GET("?method=user.getWeeklyAlbumChart&format=json")
    suspend fun getWeeklyAlbumChart(
        @Query("user") user: String,
        @Query("api_key") apiKey: String
    ): LastFmWeeklyAlbumChartResponse

    @GET("?method=tag.getTopArtists&format=json")
    suspend fun getTagTopArtists(
        @Query("tag") tag: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 5
    ): LastFmTagTopArtistsResponse

    @GET("?method=user.getTopTags&format=json")
    suspend fun getUserTopTags(
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 20
    ): LastFmUserTopTagsResponse

    @GET("?method=track.search&format=json")
    suspend fun searchTrack(
        @Query("track") track: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 5
    ): LastFmTrackSearchResponse

    companion object {
        const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
    }
}
