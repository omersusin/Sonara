package com.sonara.app.intelligence.lastfm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest

class ScrobblingManager {

    private val client = OkHttpClient()

    suspend fun updateNowPlaying(
        track: String,
        artist: String,
        apiKey: String,
        sharedSecret: String,
        sessionKey: String
    ): Boolean {
        if (track.isBlank() || artist.isBlank() || apiKey.isBlank() || sessionKey.isBlank()) return false

        return try {
            withContext(Dispatchers.IO) {
                val params = sortedMapOf(
                    "method" to "track.updateNowPlaying",
                    "track" to track,
                    "artist" to artist,
                    "api_key" to apiKey,
                    "sk" to sessionKey
                )
                val sig = generateSignature(params, sharedSecret)

                val body = FormBody.Builder()
                params.forEach { (k, v) -> body.add(k, v) }
                body.add("api_sig", sig)
                body.add("format", "json")

                val request = Request.Builder()
                    .url(LastFmApi.BASE_URL)
                    .post(body.build())
                    .build()

                val response = client.newCall(request).execute()
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun scrobble(
        track: String,
        artist: String,
        album: String,
        timestamp: Long,
        apiKey: String,
        sharedSecret: String,
        sessionKey: String
    ): Boolean {
        if (track.isBlank() || artist.isBlank() || apiKey.isBlank() || sessionKey.isBlank()) return false

        return try {
            withContext(Dispatchers.IO) {
                val params = sortedMapOf(
                    "method" to "track.scrobble",
                    "track" to track,
                    "artist" to artist,
                    "album" to album,
                    "timestamp" to (timestamp / 1000).toString(),
                    "api_key" to apiKey,
                    "sk" to sessionKey
                )
                val sig = generateSignature(params, sharedSecret)

                val body = FormBody.Builder()
                params.forEach { (k, v) -> body.add(k, v) }
                body.add("api_sig", sig)
                body.add("format", "json")

                val request = Request.Builder()
                    .url(LastFmApi.BASE_URL)
                    .post(body.build())
                    .build()

                val response = client.newCall(request).execute()
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSessionKey(token: String, apiKey: String, sharedSecret: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                val params = sortedMapOf(
                    "method" to "auth.getSession",
                    "token" to token,
                    "api_key" to apiKey
                )
                val sig = generateSignature(params, sharedSecret)

                val body = FormBody.Builder()
                params.forEach { (k, v) -> body.add(k, v) }
                body.add("api_sig", sig)
                body.add("format", "json")

                val request = Request.Builder()
                    .url(LastFmApi.BASE_URL)
                    .post(body.build())
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@withContext null

                val regex = """"key"\s*:\s*"([^"]+)"""".toRegex()
                regex.find(responseBody)?.groupValues?.get(1)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun generateSignature(params: Map<String, String>, secret: String): String {
        val raw = params.entries.joinToString("") { "${it.key}${it.value}" } + secret
        return md5(raw)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
