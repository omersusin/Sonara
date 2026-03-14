package com.sonara.app.intelligence.lastfm

import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmApi {
    @GET("?method=track.getInfo&format=json")
    suspend fun getTrackInfo(
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String
    ): LastFmTrackResponse

    @GET("?method=track.getTags&format=json")
    suspend fun getTrackTags(
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String
    ): LastFmTrackResponse

    @GET("?method=artist.getTopTags&format=json")
    suspend fun getArtistTags(
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String
    ): LastFmArtistTagsResponse

    companion object {
        const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
    }
}
