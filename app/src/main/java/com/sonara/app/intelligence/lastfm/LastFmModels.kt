package com.sonara.app.intelligence.lastfm

import com.google.gson.annotations.SerializedName

data class LastFmTrackResponse(val track: LastFmTrack? = null)
data class LastFmTrack(
    val name: String = "",
    val artist: LastFmArtist? = null,
    val album: LastFmAlbum? = null,
    val toptags: LastFmTopTags? = null,
    val duration: String? = null,
    val listeners: String? = null
)
data class LastFmArtist(val name: String = "")
data class LastFmAlbum(val title: String = "")
data class LastFmTopTags(val tag: List<LastFmTag> = emptyList())
data class LastFmTag(val name: String = "", val count: Int = 0)

data class LastFmArtistTagsResponse(
    @SerializedName("toptags") val toptags: LastFmTopTags? = null
)
