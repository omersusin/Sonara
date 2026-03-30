package com.sonara.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.sonara.app.ai.SonaraAi
import com.sonara.app.ai.bridge.AudioSessionTracker

class AudioEffectSessionReceiver : BroadcastReceiver() {
    companion object { @Volatile var bridgeCallback: ((String, Int, String?) -> Unit)? = null }
    override fun onReceive(ctx: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        // Validate: only accept known audio effect actions
        if (action != AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION &&
            action != AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION) return
        val sid = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
        if (sid > 0) {
            bridgeCallback?.invoke(intent.action ?: "", sid, pkg)
            when (intent.action) {
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                    AudioSessionTracker.set(sid)
                    SonaraAi.getInstance()?.onSessionChanged(sid)
                }
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                    AudioSessionTracker.clear(sid)
                }
            }
        }
    }
}
