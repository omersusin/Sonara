package com.sonara.app.intelligence.lastfm

data class LastFmTrackResponse(val track: LastFmTrack? = null)
data class LastFmTrack(val name: String = "", val artist: LastFmArtist? = null, val toptags: LastFmTopTags? = null)
data class LastFmArtist(val name: String = "")
data class LastFmTopTags(val tag: List<LastFmTag> = emptyList())
data class LastFmTag(val name: String = "", val count: Int = 0)
