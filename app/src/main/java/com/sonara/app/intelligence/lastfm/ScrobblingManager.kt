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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ScrobblingManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun updateNowPlaying(
        track: String, artist: String,
        apiKey: String, sharedSecret: String, sessionKey: String
    ): Boolean {
        if (track.isBlank() || artist.isBlank() || apiKey.isBlank() || sessionKey.isBlank()) return false
        return apiCall(sortedMapOf(
            "method" to "track.updateNowPlaying",
            "track" to track, "artist" to artist,
            "api_key" to apiKey, "sk" to sessionKey
        ), sharedSecret)
    }

    suspend fun scrobble(
        track: String, artist: String, album: String, timestamp: Long,
        apiKey: String, sharedSecret: String, sessionKey: String
    ): Boolean {
        if (track.isBlank() || artist.isBlank() || apiKey.isBlank() || sessionKey.isBlank()) return false
        return apiCall(sortedMapOf(
            "method" to "track.scrobble",
            "track" to track, "artist" to artist, "album" to album,
            "timestamp" to (timestamp / 1000).toString(),
            "api_key" to apiKey, "sk" to sessionKey
        ), sharedSecret)
    }

    suspend fun loveTrack(
        track: String, artist: String,
        apiKey: String, sharedSecret: String, sessionKey: String
    ): Boolean {
        if (track.isBlank() || artist.isBlank() || apiKey.isBlank() || sessionKey.isBlank()) return false
        return apiCall(sortedMapOf(
            "method" to "track.love",
            "track" to track, "artist" to artist,
            "api_key" to apiKey, "sk" to sessionKey
        ), sharedSecret)
    }

    suspend fun unloveTrack(
        track: String, artist: String,
        apiKey: String, sharedSecret: String, sessionKey: String
    ): Boolean {
        if (track.isBlank() || artist.isBlank() || apiKey.isBlank() || sessionKey.isBlank()) return false
        return apiCall(sortedMapOf(
            "method" to "track.unlove",
            "track" to track, "artist" to artist,
            "api_key" to apiKey, "sk" to sessionKey
        ), sharedSecret)
    }

    suspend fun getSessionKey(token: String, apiKey: String, sharedSecret: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                val params = sortedMapOf("method" to "auth.getSession", "token" to token, "api_key" to apiKey)
                val sig = generateSignature(params, sharedSecret)
                val body = FormBody.Builder()
                params.forEach { (k, v) -> body.add(k, v) }
                body.add("api_sig", sig); body.add("format", "json")
                val request = Request.Builder().url(LastFmApi.BASE_URL).post(body.build()).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@withContext null
                val regex = """"key"\s*:\s*"([^"]+)"""".toRegex()
                regex.find(responseBody)?.groupValues?.get(1)
            }
        } catch (e: Exception) { null }
    }

    private suspend fun apiCall(params: java.util.SortedMap<String, String>, sharedSecret: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val sig = generateSignature(params, sharedSecret)
                val body = FormBody.Builder()
                params.forEach { (k, v) -> body.add(k, v) }
                body.add("api_sig", sig); body.add("format", "json")
                val request = Request.Builder().url(LastFmApi.BASE_URL).post(body.build()).build()
                val response = client.newCall(request).execute()
                response.use { resp -> val ok = resp.isSuccessful; if (!ok) { val body = resp.body?.string(); com.sonara.app.data.SonaraLogger.w("LastFm", "API ${resp.code}: $body") }; ok }
            }
        } catch (e: Exception) { false }
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
