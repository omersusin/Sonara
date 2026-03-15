package com.sonara.app.audio.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.sonara.app.SonaraApp

class AudioSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        if (sessionId <= 0) return
        val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: ""

        val app = context.applicationContext as? SonaraApp ?: return

        when (intent.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                app.onAudioSessionOpen(sessionId, pkg)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                app.onAudioSessionClose(sessionId)
            }
        }
    }
}
