package com.sonara.app.intelligence.theaudiodb

data class AudioDbArtist(
    val idArtist: String,
    val strArtist: String,
    val strThumb: String?,
    val strFanart: String?,
    val strBanner: String?,
    val strLogo: String?,
    val strBiographyEN: String?,
    val strBiographyTR: String?,
    val strGenre: String?,
    val strCountry: String?,
    val strTwitter: String? = null,
    val strFacebook: String? = null,
    val strInstagram: String? = null,
    val strWebsite: String? = null,
    val intFormedYear: String? = null,
    val intMembersNo: Int? = null
)

data class AudioDbAlbum(
    val idAlbum: String,
    val strAlbum: String,
    val strArtist: String,
    val strThumb: String?,
    val strThumbHQ: String?,
    val intYearReleased: Int?,
    val strGenre: String?,
    val strDescriptionEN: String?
)

data class AudioDbTrack(
    val idTrack: String,
    val strTrack: String,
    val intTrackNumber: Int?,
    val strDuration: String?,
    val strThumb: String?
)
