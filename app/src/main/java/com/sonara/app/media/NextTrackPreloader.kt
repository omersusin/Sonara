package com.sonara.app.media

class NextTrackPreloader {
    var nextTitle: String = ""; private set
    var nextArtist: String = ""; private set
    var isPreloaded: Boolean = false; private set

    fun setNext(title: String, artist: String) {
        nextTitle = title
        nextArtist = artist
        isPreloaded = title.isNotBlank()
    }

    fun clear() {
        nextTitle = ""
        nextArtist = ""
        isPreloaded = false
    }
}
