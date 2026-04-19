package com.sonara.app.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.net.Uri
import android.util.Log
import com.sonara.app.intelligence.coverart.CoverArtArchiveClient
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.intelligence.musicbrainz.MusicBrainzClient
import com.sonara.app.intelligence.theaudiodb.TheAudioDbClient

object ArtworkResolver {

    // ── Local: extract bitmap embedded in MediaMetadata ───────────────────────

    fun extract(metadata: MediaMetadata, contentResolver: ContentResolver): Bitmap? {
        val directBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        if (directBitmap != null) return directBitmap

        val artUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

        if (!artUri.isNullOrBlank()) return loadFromUri(artUri, contentResolver)
        return null
    }

    private fun loadFromUri(uriString: String, contentResolver: ContentResolver): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                stream.close()
                contentResolver.openInputStream(uri)?.use { stream2 ->
                    val opts = BitmapFactory.Options().apply {
                        val maxDim = maxOf(options.outWidth, options.outHeight)
                        inSampleSize = if (maxDim > 512) maxDim / 512 else 1
                    }
                    BitmapFactory.decodeStream(stream2, null, opts)
                }
            }
        } catch (e: Exception) {
            Log.w("ArtworkResolver", "URI artwork failed: ${e.message}")
            null
        }
    }

    // ── Remote: priority chain Last.fm → CoverArtArchive → TheAudioDB → Deezer ──

    /**
     * Returns a URL for the best available album art, or null if none found.
     * Priority: Last.fm album image → Cover Art Archive (via MusicBrainz MBID)
     *           → TheAudioDB album thumb → Deezer track image.
     *
     * @param lastFmApiKey Required for Last.fm lookup; pass empty to skip.
     */
    suspend fun resolveUrl(
        title: String,
        artist: String,
        album: String,
        lastFmApiKey: String = ""
    ): String? {
        // 1. Last.fm — fast, high quality if key provided
        if (lastFmApiKey.isNotBlank() && album.isNotBlank()) {
            runCatching {
                val resp = LastFmClient.api.getAlbumInfo(artist, album, lastFmApiKey)
                resp.album?.imageUrl?.takeIf { it.isNotBlank() && !it.contains("2a96cbd8b46e") }
                    ?.let { return it }
            }
        }

        // 2. Cover Art Archive via MusicBrainz MBID (rate-limited: 1 req/s inside MusicBrainzClient)
        runCatching {
            val mbMatch = MusicBrainzClient.searchRecording(title, artist)
            if (mbMatch != null && mbMatch.mbid.isNotBlank()) {
                CoverArtArchiveClient.getFrontCoverUrl(mbMatch.mbid)?.let { return it }
            }
        }

        // 3. TheAudioDB — album thumb (no auth required)
        if (artist.isNotBlank() && album.isNotBlank()) {
            runCatching {
                TheAudioDbClient.searchAlbum(artist, album)
                    ?.let { (it.strThumbHQ ?: it.strThumb)?.takeIf { u -> u.isNotBlank() } }
                    ?.let { return it }
            }
        }

        // 4. Deezer — free fallback
        runCatching {
            DeezerImageResolver.getTrackImageWithFallback(title, artist)?.let { return it }
        }

        return null
    }
}
