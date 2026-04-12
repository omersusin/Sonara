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

    @GET("?method=user.getTopAlbums&format=json")
    suspend fun getUserTopAlbums(
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("period") period: String = "overall",
        @Query("limit") limit: Int = 5
    ): LastFmTopAlbumsResponse

    @GET("?method=user.getRecentTracks&format=json")
    suspend fun getRecentTracks(@Query("user") user: String, @Query("api_key") apiKey: String, @Query("limit") limit: Int = 10): LastFmRecentTracksResponse

    companion object {
        const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
    }
}
