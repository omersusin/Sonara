package com.sonara.app.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.net.Uri
import android.util.Log

object ArtworkResolver {

    fun extract(metadata: MediaMetadata, contentResolver: ContentResolver): Bitmap? {
        // 1. Direct bitmap (fastest)
        val directBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        if (directBitmap != null) return directBitmap

        // 2. URI-based artwork (many players use this instead of embedded bitmap)
        val artUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

        if (!artUri.isNullOrBlank()) {
            return loadFromUri(artUri, contentResolver)
        }

        return null
    }

    private fun loadFromUri(uriString: String, contentResolver: ContentResolver): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            contentResolver.openInputStream(uri)?.use { stream ->
                // Decode with size limit to prevent OOM
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)

                // Reset stream
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
}
