package com.sonara.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect

class AudioEffectSessionReceiver : BroadcastReceiver() {
    companion object { @Volatile var bridgeCallback: ((String, Int, String?) -> Unit)? = null }
    override fun onReceive(ctx: Context?, intent: Intent?) {
        val sid = intent?.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1) ?: return; val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
        if (sid > 0) bridgeCallback?.invoke(intent.action ?: "", sid, pkg)
    }
}
