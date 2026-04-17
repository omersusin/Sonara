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

package com.sonara.app.engine.eq

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sonara.app.data.SonaraDatabase
import com.sonara.app.engine.classifier.TextGenreClassifier
import com.sonara.app.engine.learning.UserPreferenceLearner
import kotlinx.coroutines.*

class AudioSessionBridge(private val context: Context) {
    companion object { private const val TAG = "SessionBridge"; private const val DEBOUNCE = 400L; private const val AUTO_ACCEPT = 15000L }

    val eqController = EqSessionController(context)
    val classifier = TextGenreClassifier()
    val learner = UserPreferenceLearner(context, classifier)
    private val compensator = RouteEqCompensator()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private val debounceToken = Any(); private val acceptToken = Any()
    private val db by lazy { SonaraDatabase.get(context) }

    data class CurrentTrackState(val title: String? = null, val artist: String? = null, val album: String? = null, val genre: String = "other", val mood: String = "neutral", val energy: Float = 0.5f, val confidence: Float = 0f, val appliedBands: ShortArray = ShortArray(5), val route: EqSessionController.AudioRoute = EqSessionController.AudioRoute.SPEAKER, val timestamp: Long = 0L)
    @Volatile var currentState = CurrentTrackState(); private set
    var onTrackAnalyzed: ((CurrentTrackState) -> Unit)? = null
    private var currentTrackKey = ""; private var activeController: MediaController? = null; private var metaCb: MediaController.Callback? = null

    fun onActiveSessionChanged(controller: MediaController?) {
        detach(); if (controller == null) return; activeController = controller
        val sid = extractSessionId(controller); if (sid > 0) eqController.attachSession(sid)
        val cb = object : MediaController.Callback() {
            override fun onMetadataChanged(m: MediaMetadata?) { handler.removeCallbacksAndMessages(debounceToken); handler.postAtTime({ handleMeta(controller, m) }, debounceToken, android.os.SystemClock.uptimeMillis() + DEBOUNCE) }
            override fun onPlaybackStateChanged(s: PlaybackState?) { val n = extractSessionId(controller); if (n > 0) eqController.updateSessionId(n) }
            override fun onSessionDestroyed() { detach() }
        }
        metaCb = cb; try { controller.registerCallback(cb, handler) } catch (_: Exception) {}
        scope.launch { learner.restoreWeights() }
        controller.metadata?.let { handleMeta(controller, it) }
    }

    fun onExternalSessionId(sid: Int, pkg: String?) { if (sid > 0) { eqController.attachSession(sid); eqController.reapplyCurrentEq() } }

    fun release() { detach(); handler.removeCallbacksAndMessages(null); eqController.release(); learner.release(); scope.cancel() }

    private fun handleMeta(controller: MediaController, meta: MediaMetadata?) {
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE); val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST); val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val key = "${title.orEmpty()}|${artist.orEmpty()}".lowercase(); if (key == currentTrackKey && key.isNotEmpty()) return
        handler.removeCallbacksAndMessages(acceptToken); currentTrackKey = key
        val sid = extractSessionId(controller); if (sid > 0) eqController.updateSessionId(sid)
        scope.launch {
            try {
                val pred = classifier.classify(title, artist, album); val route = eqController.detectRoute()
                val userPref = db.userEqPreferenceDao().getBest(pred.genre, route.name)
                val bands = if (userPref != null) { db.userEqPreferenceDao().touch(userPref.id); userPref.bandLevels.trim('[', ']').split(",").mapNotNull { it.trim().toShortOrNull() }.toShortArray() } else getDefault(pred.genre, pred.energy)
                val compensated = compensator.apply(bands, route)
                currentState = CurrentTrackState(title, artist, album, pred.genre, pred.mood, pred.energy, pred.confidence, compensated, route, System.currentTimeMillis())
                withContext(Dispatchers.Main) { eqController.applyBands(compensated); onTrackAnalyzed?.invoke(currentState) }
                scheduleAccept()
                Log.d(TAG, "Applied: ${pred.genre}/${pred.mood} c=${pred.confidence} r=$route")
            } catch (e: Exception) { Log.e(TAG, "Meta error: ${e.message}"); withContext(Dispatchers.Main) { eqController.reapplyCurrentEq() } }
        }
    }

    private fun scheduleAccept() {
        handler.removeCallbacksAndMessages(acceptToken)
        handler.postAtTime({ val s = currentState; if (s.confidence >= 0.4f) scope.launch { learner.onAccepted(s.title, s.artist, s.genre, s.appliedBands, s.route, s.confidence) } }, acceptToken, android.os.SystemClock.uptimeMillis() + AUTO_ACCEPT)
    }

    private fun extractSessionId(c: MediaController): Int = try { val pi = c.playbackInfo; if (pi != null) { val f = pi.javaClass.getDeclaredField("mAudioSessionId"); f.isAccessible = true; f.getInt(pi).takeIf { it > 0 } ?: 0 } else 0 } catch (_: Exception) { 0 }
    private fun detach() { metaCb?.let { try { activeController?.unregisterCallback(it) } catch (_: Exception) {} }; metaCb = null; activeController = null }

    private fun getDefault(genre: String, energy: Float): ShortArray {
        val b = when (genre) { "rock" -> shortArrayOf(400, 200, -100, 300, 500); "pop" -> shortArrayOf(-100, 200, 400, 300, 100); "hiphop" -> shortArrayOf(500, 300, -100, 100, 200); "electronic" -> shortArrayOf(500, 200, 0, 200, 400); "classical" -> shortArrayOf(0, 0, 0, 100, 200); "jazz" -> shortArrayOf(200, 0, 100, 200, 100); "rnb" -> shortArrayOf(300, 200, 0, 100, -100); "country" -> shortArrayOf(100, 0, 200, 300, 200); else -> shortArrayOf(0, 0, 0, 0, 0) }
        return if (energy > 0.7f) ShortArray(b.size) { if (it == 0 || it == b.size - 1) (b[it] + 100).toShort() else b[it] } else b
    }
}
