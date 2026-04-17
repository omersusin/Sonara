/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    @GET("?method=user.getRecentTracks&format=json")
    suspend fun getRecentTracks(@Query("user") user: String, @Query("api_key") apiKey: String, @Query("limit") limit: Int = 10): LastFmRecentTracksResponse

    companion object {
        const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
    }
}
